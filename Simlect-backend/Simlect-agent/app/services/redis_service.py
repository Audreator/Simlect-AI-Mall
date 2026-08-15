"""Redis 服务模块：封装 Agent 会话态、取消标志、咨询商品、待办操作、Prompt 缓存与 WebSocket 发布。"""

import json  # import statement — JSON 序列化
import uuid  # 生成锁持有者标识（防误删他人锁）

import time  # import statement — 时间戳

from typing import Any  # import statement — 泛型 Any

import redis.asyncio as aioredis  # import statement — 异步 Redis 客户端，类似 Lettuce/Jedis 异步版

from app.config.settings import get_settings  # import statement — redis_url 配置

from app.constants import (  # import statement — Redis 键前缀与 TTL 常量
    CANCEL_FLAG_TTL,
    CONSULT_ACTIVE_TTL,
    CONSULT_PRODUCT_TTL,
    PENDING_ACTION_TTL,
    PENDING_MSG_TTL,
    REDIS_AGENT_CONSULT_ACTIVE,
    REDIS_AGENT_CONSULT_PRODUCT,
    REDIS_AGENT_PENDING_ACTION,
    REDIS_AGENT_PENDING_MSG,
    REDIS_AGENT_HISTORY_CONDENSED,
    REDIS_CANCEL_AGENT,
    REDIS_HEARTBEAT_TTL,
    REDIS_PROMPT,
    REDIS_SENSITIVE_WORD_PAYLOAD,
    REDIS_WS_USER_HEARTBEAT,
    WS_MESSAGE_TOPIC_AGENT,
)

class RedisService:  # Redis 服务 — 类似 Java @Service RedisTemplate 封装

    def __init__(self):  # 构造器 — 延迟初始化连接

        self._client: aioredis.Redis | None = None  # @Nullable Redis 客户端

    async def connect(self) -> None:  # 建立 Redis 连接 — 幂等

        if self._client is not None:  # 已连接则跳过
            return

        settings = get_settings()

        self._client = aioredis.from_url(  # 从 URL 创建连接池
            settings.redis_url,
            decode_responses=True,  # 自动 decode 为 str
            protocol=2,
            max_connections=20,
        )

    async def ensure_connected(self) -> None:  # 确保已连接 — Agent 与 MCP 进程共用
        """Idempotent connect for Agent lifespan and MCP server process."""
        await self.connect()

    async def close(self) -> None:  # 关闭连接

        if self._client:
            await self._client.aclose()  # 异步关闭
            self._client = None

    @property  # @property 类似 Java getter
    def client(self) -> aioredis.Redis:  # 获取 Redis 客户端 — 未连接则抛异常

        if not self._client:
            raise RuntimeError("Redis not connected")
        return self._client

    async def set_cancel_flag(self, user_id: str, message_id: int) -> None:  # 设置消息取消标志

        key = f"{REDIS_CANCEL_AGENT}{user_id}:msg:{message_id}"

        await self.client.setex(key, CANCEL_FLAG_TTL, "1")  # SETEX — 带 TTL

    async def is_cancelled(self, user_id: str, message_id: int) -> bool:  # 检查是否已取消

        key = f"{REDIS_CANCEL_AGENT}{user_id}:msg:{message_id}"
        return await self.client.exists(key) > 0  # EXISTS > 0 表示存在

    async def save_consult_product(self, user_id: str, product: dict) -> None:  # 缓存咨询商品快照

        key = f"{REDIS_AGENT_CONSULT_PRODUCT}{user_id}"

        await self.client.setex(key, CONSULT_PRODUCT_TTL, json.dumps(product, ensure_ascii=False))

    async def get_consult_product(self, user_id: str) -> dict | None:  # 读取咨询商品 — @Nullable

        key = f"{REDIS_AGENT_CONSULT_PRODUCT}{user_id}"
        data = await self.client.get(key)
        return json.loads(data) if data else None  # JSON 反序列化为 Map

    async def set_consult_active(self, user_id: str) -> None:  # 标记咨询上下文激活

        await self.client.setex(f"{REDIS_AGENT_CONSULT_ACTIVE}{user_id}", CONSULT_ACTIVE_TTL, "1")

    async def is_consult_active(self, user_id: str) -> bool:  # 咨询上下文是否激活

        return await self.client.exists(f"{REDIS_AGENT_CONSULT_ACTIVE}{user_id}") > 0

    async def clear_consult(self, user_id: str) -> None:  # 清除咨询商品与激活标志

        await self.client.delete(
            f"{REDIS_AGENT_CONSULT_PRODUCT}{user_id}",
            f"{REDIS_AGENT_CONSULT_ACTIVE}{user_id}",
        )

    async def pause_consult(self, user_id: str) -> None:  # 暂停咨询 — 仅删除激活标志

        await self.client.delete(f"{REDIS_AGENT_CONSULT_ACTIVE}{user_id}")

    async def bind_message_id(self, user_id: str, message_id: int) -> None:  # 绑定当前处理中的 messageId

        await self.client.setex(f"{REDIS_AGENT_PENDING_MSG}{user_id}", PENDING_MSG_TTL, str(message_id))

    async def get_bound_message_id(self, user_id: str) -> int | None:  # 获取绑定的 messageId

        val = await self.client.get(f"{REDIS_AGENT_PENDING_MSG}{user_id}")
        return int(val) if val else None

    async def clear_bound_message_id(self, user_id: str) -> None:  # 清除 messageId 绑定

        await self.client.delete(f"{REDIS_AGENT_PENDING_MSG}{user_id}")

    async def save_history_condensed(self, user_id: str, message_id: int, text: str) -> None:  # 缓存压缩后的助手回复

        key = f"{REDIS_AGENT_HISTORY_CONDENSED}{user_id}:msg:{message_id}"
        await self.client.setex(key, CONSULT_PRODUCT_TTL, text)

    async def get_history_condensed(self, user_id: str, message_id: int) -> str | None:  # 读取压缩历史

        return await self.client.get(f"{REDIS_AGENT_HISTORY_CONDENSED}{user_id}:msg:{message_id}")

    async def get_prompt(self, prompt_key: str) -> str | None:  # 从 Redis 读取 Prompt 模板

        return await self.client.get(f"{REDIS_PROMPT}{prompt_key}")

    async def save_pending_action(self, token: str, action: dict) -> None:  # 保存待确认操作

        await self.client.setex(
            f"{REDIS_AGENT_PENDING_ACTION}{token}",
            PENDING_ACTION_TTL,
            json.dumps(action, ensure_ascii=False),
        )

    async def get_pending_action(self, token: str) -> dict | None:  # 读取待确认操作

        data = await self.client.get(f"{REDIS_AGENT_PENDING_ACTION}{token}")
        return json.loads(data) if data else None

    async def try_mark_pending_executing(self, token: str, ttl_seconds: int = 300) -> bool:
        """执行前原子置「处理中」标记（SET NX）：防 delete 失败/进程崩溃后重试导致写操作二次执行。"""
        key = f"{REDIS_AGENT_PENDING_ACTION}exec:{token}"
        ok = await self.client.set(key, "1", nx=True, ex=max(1, int(ttl_seconds)))
        return bool(ok)

    async def clear_pending_executing(self, token: str) -> None:
        key = f"{REDIS_AGENT_PENDING_ACTION}exec:{token}"
        await self.client.delete(key)

    async def delete_pending_action(self, token: str) -> None:  # 删除待确认操作

        await self.client.delete(f"{REDIS_AGENT_PENDING_ACTION}{token}")

    # 原子 CAS 状态流转：仅当当前状态等于期望值时改为目标值
    _PENDING_CAS_LUA = """local key = KEYS[1]
local expect = ARGV[1]
local target = ARGV[2]
local ttl = tonumber(ARGV[3])
local v = redis.call('get', key)
if v == false then return -1 end
local ok, obj = pcall(cjson.decode, v)
if not ok or type(obj) ~= 'table' then return 0 end
if tostring(obj['status']) ~= expect then return 0 end
obj['status'] = tonumber(target)
redis.call('set', key, cjson.encode(obj), 'EX', ttl)
return 1
"""

    async def cas_pending_status(self, token: str, expect: int, target: int,
                                 ttl_seconds: int | None = None) -> bool:
        """原子 CAS：当前状态 == expect 时改为 target（并发 confirm/cancel 与执行前落定状态用）。

        返回 True 表示本次调用拥有状态变更权；key 不存在返回 False（视为已清理）。
        """
        ttl = int(ttl_seconds if ttl_seconds is not None else PENDING_ACTION_TTL)
        key = f"{REDIS_AGENT_PENDING_ACTION}{token}"
        result = await self.client.eval(
            self._PENDING_CAS_LUA, 1, key, str(expect), str(target), str(ttl)
        )
        return result == 1

    async def try_lock_pending_action(self, token: str, ttl_seconds: int = 120):
        """NX lock with owner value: confirm/cancel cannot double-execute the same token."""
        key = f"{REDIS_AGENT_PENDING_ACTION}lock:{token}"
        owner = uuid.uuid4().hex  # 持有者标识：解锁时校验，防 TTL 过期后误删他人锁
        ok = await self.client.set(key, owner, nx=True, ex=max(1, int(ttl_seconds)))  # NX=不存在才写入
        return owner if ok else None

    async def unlock_pending_action(self, token: str, owner) -> None:
        """释放锁：仅当自己仍是持有者时删除（Lua 校验，防误删他人锁）"""
        if not owner:
            return
        key = f"{REDIS_AGENT_PENDING_ACTION}lock:{token}"
        script = """
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
end
return 0
"""
        await self.client.eval(script, 1, key, owner)

    async def get_sensitive_words(self) -> list[dict]:  # 读取敏感词库

        raw = await self.client.get(REDIS_SENSITIVE_WORD_PAYLOAD)
        if not raw:
            return []
        try:
            return json.loads(raw)  # List<Map>
        except json.JSONDecodeError:
            return []

    async def publish_ws(self, payload: dict) -> None:  # 发布 WebSocket 消息到 Redis 频道

        await self.client.publish(
            WS_MESSAGE_TOPIC_AGENT, json.dumps(payload, ensure_ascii=False)
        )

    async def save_user_heartbeat(self, user_id: str) -> None:  # 保存用户 WebSocket 心跳时间戳

        key = f"{REDIS_WS_USER_HEARTBEAT}{user_id}"
        await self.client.setex(key, REDIS_HEARTBEAT_TTL, str(int(time.time() * 1000)))

redis_service = RedisService()  # 模块级单例
