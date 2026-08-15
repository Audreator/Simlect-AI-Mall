"""
会话记忆持久化服务（Session Memory Service）。

职责：读写用户的 Agent 会话记忆（summary 摘要 + state 运行时状态）。
采用 Redis 缓存 + MySQL 持久化的双层存储，类似 Java 中 Cache-Aside 模式：
先读 Redis，未命中再读 DB 并回写缓存。

表：agent_session_memory（user_id 为主键或唯一键）
Redis Key：REDIS_AGENT_SESSION + user_id
"""

from __future__ import annotations  # 延迟注解，Python 3.7+ 推荐

import json  # JSON 序列化/反序列化，类似 Jackson ObjectMapper
from datetime import datetime  # 日期时间，类似 java.time.LocalDateTime
from typing import Any  # 泛型 Any，类似 Java Object

import structlog  # 结构化日志，类似 SLF4J + Logback

from app.config.settings import get_settings  # 配置：Redis TTL、锁 TTL 等
from app.constants import REDIS_AGENT_SESSION, REDIS_AGENT_SESSION_COMPRESS_LOCK  # Redis 键前缀常量
from app.db.pool import acquire  # 异步 MySQL 连接池，with 类似 try-with-resources
from app.memory.models import SessionMemory, empty_state, empty_summary  # 记忆模型与空默认值

logger = structlog.get_logger()  # 获取本模块 logger，类似 LoggerFactory.getLogger()

_TABLE_ENSURED = False  # 模块级标志：DDL 是否已执行，避免重复建表


class SessionMemoryService:
    """
    会话记忆 CRUD 服务类。

    类似 Java @Service SessionMemoryService，提供 load/save 及压缩锁。
    """

    async def ensure_table(self) -> None:
        """
        确保 agent_session_memory 表存在（懒加载执行 DDL）。

        首次调用时读取 scripts/sql/agent_session_memory.sql 并逐条执行。
        全局 _TABLE_ENSURED 保证进程内只执行一次。
        """
        global _TABLE_ENSURED  # 声明修改模块级变量
        if _TABLE_ENSURED:  # 已建表则直接返回
            return

        # 动态定位 SQL 文件：当前文件上两级到 app/，再进 scripts/sql/
        sql_path = (
            __import__("pathlib").Path(__file__).resolve().parents[2]
            / "scripts"
            / "sql"
            / "agent_session_memory.sql"
        )
        ddl = sql_path.read_text(encoding="utf-8")  # 读取 DDL 全文

        async with acquire() as cur:  # 从连接池取游标，退出时自动归还

            for stmt in ddl.split(";"):  # 按分号拆多条 SQL
                stmt = stmt.strip()
                if stmt:  # 跳过空语句
                    await cur.execute(stmt)
        _TABLE_ENSURED = True  # 标记已完成

    async def load(self, user_id: str, redis_client) -> SessionMemory:
        """
        加载用户会话记忆：Redis 优先，DB 兜底，都没有则新建空记忆。

        参数:
            user_id: 用户 ID
            redis_client: 异步 Redis 客户端（由调用方注入）

        返回:
            SessionMemory 实例，永不为 None
        """
        await self.ensure_table()  # 保证表存在
        key = f"{REDIS_AGENT_SESSION}{user_id}"  # 拼接 Redis key
        cached = await redis_client.get(key)  # GET，类似 Jedis.get
        if cached:  # 缓存命中
            try:
                data = json.loads(cached)  # 字节/字符串 → dict

                # from_storage 工厂方法解析 summary/state
                return SessionMemory.from_storage(user_id, data.get("summary"), data.get("state"))
            except json.JSONDecodeError:  # 缓存损坏
                logger.warning("session_memory_redis_corrupt", user_id=user_id)

        # Redis 未命中或损坏 → 读 MySQL
        row = await self._load_from_db(user_id)
        if row:
            mem = SessionMemory.from_storage(user_id, row.get("summary_json"), row.get("state_json"))
            await self._save_redis(user_id, mem, redis_client)  # 回写缓存（Cache-Aside 写回）
            return mem

        # 新用户：空 summary + 空 state
        return SessionMemory(user_id=user_id)

    async def save(self, memory: SessionMemory, redis_client) -> None:
        """
        保存会话记忆到 Redis 和 MySQL（双写）。

        参数:
            memory: 要持久化的 SessionMemory
            redis_client: Redis 客户端
        """
        await self.ensure_table()
        await self._save_redis(memory.user_id, memory, redis_client)  # 先写缓存（读多写少场景常见）
        await self._save_db(memory)  # 再写 DB 持久化

    async def try_acquire_compress_lock(self, user_id: str, redis_client) -> bool:
        """
        尝试获取会话压缩分布式锁（防止同一用户并发压缩）。

        使用 Redis SET key value NX EX ttl：仅当 key 不存在时设置，并带过期时间。
        类似 Java Redisson tryLock()。

        参数:
            user_id: 用户 ID
            redis_client: Redis 客户端

        返回:
            True 表示获锁成功；False 表示已有其他进程在压缩
        """
        key = f"{REDIS_AGENT_SESSION_COMPRESS_LOCK}{user_id}"
        settings = get_settings()
        # nx=True 等价 SETNX；ex=秒级 TTL
        return bool(await redis_client.set(key, "1", nx=True, ex=settings.session_compress_lock_ttl))

    async def release_compress_lock(self, user_id: str, redis_client) -> None:
        """
        释放压缩锁（删除 Redis key）。

        参数:
            user_id: 用户 ID
            redis_client: Redis 客户端
        """
        await redis_client.delete(f"{REDIS_AGENT_SESSION_COMPRESS_LOCK}{user_id}")

    async def _save_redis(self, user_id: str, memory: SessionMemory, redis_client) -> None:
        """
        将会话记忆写入 Redis（带 TTL）。

        参数:
            user_id: 用户 ID
            memory: 记忆对象
            redis_client: Redis 客户端
        """
        settings = get_settings()
        payload = json.dumps(  # dict → JSON 字符串
            {"summary": memory.summary, "state": memory.state},
            ensure_ascii=False,  # 中文不转 \uXXXX
        )
        await redis_client.setex(  # SET + EXPIRE 原子操作
            f"{REDIS_AGENT_SESSION}{user_id}",
            settings.session_redis_ttl,  # 过期秒数
            payload,
        )

    async def _save_db(self, memory: SessionMemory) -> None:
        """
        将会话记忆 UPSERT 到 MySQL agent_session_memory 表。

        INSERT ... ON DUPLICATE KEY UPDATE：存在则更新，类似 MySQL MERGE 或 JPA save。

        参数:
            memory: 记忆对象
        """
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")  # 格式化为 SQL 友好字符串
        turn_count = int(memory.state.get("turnCount") or 0)  # 对话轮次计数
        async with acquire() as cur:
            await cur.execute(
                """INSERT INTO agent_session_memory (user_id, summary_json, state_json, turn_count, updated_at)
                   VALUES (%s, %s, %s, %s, %s)
                   ON DUPLICATE KEY UPDATE
                     summary_json=VALUES(summary_json),
                     state_json=VALUES(state_json),
                     turn_count=VALUES(turn_count),
                     updated_at=VALUES(updated_at)""",
                (
                    memory.user_id,
                    json.dumps(memory.summary, ensure_ascii=False),  # summary 存 JSON 列
                    json.dumps(memory.state, ensure_ascii=False),
                    turn_count,
                    now,
                ),
            )

    async def _load_from_db(self, user_id: str) -> dict[str, Any] | None:
        """
        从 MySQL 按 user_id 加载一行会话记忆。

        参数:
            user_id: 用户 ID

        返回:
            含 summary_json、state_json 的 dict；无记录返回 None
        """
        async with acquire() as cur:
            await cur.execute(
                "SELECT summary_json, state_json FROM agent_session_memory WHERE user_id=%s",
                (user_id,),
            )
            row = await cur.fetchone()  # 取一行，类似 ResultSet.next()
        if not row:  # 无此用户记录
            return None
        summary = row.get("summary_json")
        state = row.get("state_json")

        # MySQL JSON 列有时返回 str，有时已是 dict，需统一解析
        if isinstance(summary, str):
            summary = json.loads(summary)
        if isinstance(state, str):
            state = json.loads(state)
        # 空值用 empty_summary/empty_state 默认结构
        return {"summary_json": summary or empty_summary(), "state_json": state or empty_state()}


session_memory_service = SessionMemoryService()  # 模块单例，全项目 import 此实例
