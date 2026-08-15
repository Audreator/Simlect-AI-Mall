"""
================================================================================
文件：api/websocket.py
角色：WebSocket 接入层 + Redis 订阅转发（类比「网关推送通道」）
================================================================================

【这个文件干什么】
1) 浏览器带着用户 token 连上 /ws，ConnectionManager 按 userId 管连接。
2) 进程启动时订阅 Redis 频道；stream_service 往频道 publish 的消息，
   由此处 listener 取出并 send_json 给对应用户。

【为什么不直连推】
Agent 推理在异步任务里；经 Redis Pub/Sub，产出端与持有 WS 的端可解耦。

【流程】
main.lifespan → start_ws_listener
LLM/finalize → stream_service.push_* → redis.publish
  → _topic_listener → manager.send_json(userId, data) → 前端

【如何使用】
前端：wss://域名/ws?token=...（经 Gateway 反代到 :7050）
心跳：客户端发 "ping"，服务端回 "pong"。

【关联】stream_service、auth.token_service、constants.WS_MESSAGE_TOPIC_AGENT
================================================================================
"""

import asyncio  # 标准库：create_task、CancelledError、Task 类型（类比 CompletableFuture）
import json  # 标准库：解析 Redis 频道里的 JSON 字符串（类比 Jackson readValue）
from urllib.parse import urlparse  # 标准库：解析 Origin URL 提取 hostname（精确白名单用）

import structlog  # 结构化日志
from fastapi import WebSocket, WebSocketDisconnect  # WebSocket 连接类型与正常断开异常

from app.auth.token_service import get_user_by_token  # 用 token 换用户信息（userId），类比 JwtParser
from app.constants import WS_MESSAGE_TOPIC_AGENT  # Redis Pub/Sub 频道名常量（类比 MQ Topic 名）
from app.services.redis_service import redis_service  # 心跳写入等 Redis 操作

logger = structlog.get_logger()  # 本模块日志器


class ConnectionManager:
    """按 userId 维护 WebSocket 集合（一人可多端同时在线，类比 ConcurrentHashMap<userId, Set<Session>>）。"""

    def __init__(self):
        # key=userId，value=该用户所有活跃 WebSocket 连接的 set（≈Map<String, Set<WebSocketSession>>）
        self._connections: dict[str, set[WebSocket]] = {}
        # 单用户最大在线连接数（防单账号海量建连 DoS）
        self._max_per_user = 8

    async def connect(self, user_id: str, ws: WebSocket) -> None:
        """接受握手并把连接登记到对应用户下（超上限拒绝新连接）。"""

        await ws.accept()  # 完成 WebSocket 握手（HTTP 101 Switching Protocols）
        conns = self._connections.setdefault(user_id, set())
        if len(conns) >= self._max_per_user:
            await ws.close(code=1013)  # Try Again Later：连接数超限
            self._connections.get(user_id, set()).discard(ws)
            return
        conns.add(ws)  # 无 key 则建空 set 再加入；有则直接 add

    def disconnect(self, user_id: str, ws: WebSocket) -> None:
        """连接关闭时从集合移除；若该用户已无连接则删掉 dict 项（释放内存）。"""

        conns = self._connections.get(user_id)  # 可能为 None（用户从未连接或已清理）
        if conns:
            conns.discard(ws)  # set.discard：元素不存在也不报错（类比 Set.remove 的安全版）
            if not conns:  # 该用户最后一个连接也断了
                self._connections.pop(user_id, None)  # 删除空 entry，避免 dict 膨胀

    async def send_json(self, user_id: str, data: dict) -> None:
        """向某用户所有在线连接广播 JSON；发送失败的连接会被清理（多端同步收流式消息）。"""

        conns = self._connections.get(user_id, set())  # 用户不在线则空 set，循环零次
        # 迭代前拷贝：await 期间其他协程可能增删集合，防止 RuntimeError/漏推
        for ws in list(conns):
            try:
                await ws.send_json(data)  # FastAPI 封装：自动 JSON 序列化 dict
            except Exception:
                self.disconnect(user_id, ws)  # 连接已死，立即清理（防僵尸堆积）
        if not conns:
            return
        # 清理后若集合为空则移除 entry
        if not self._connections.get(user_id):
            self._connections.pop(user_id, None)


manager = ConnectionManager()  # 全局单例：全进程共享连接表（类比 @Component 单例 Bean）

_listener_task: asyncio.Task | None = None  # Redis 订阅协程的 Task 句柄，shutdown 时 cancel

from app.utils.ws_token import resolve_ws_token as _resolve_ws_token  # 从 query/cookie/header 解析 token（延迟 import 防循环）

# 允许的 Origin hostname 白名单（精确匹配，防子串绕过；浏览器跨端口同 host 属自家场景）
_ALLOWED_WS_ORIGINS = {"simlect.com", "localhost", "127.0.0.1", "::1"}


def _origin_allowed(origin: str) -> bool:  # Origin 校验 — 精确 hostname 白名单（含 *.simlect.com 子域）
    try:
        hostname = (urlparse(origin).hostname or "").lower()  # 提取 hostname 并统一小写
    except ValueError:
        return False  # 非法 URL 直接拒绝
    if not hostname:  # 无 hostname（如 file:// 之类）— 拒绝
        return False
    return hostname in _ALLOWED_WS_ORIGINS or hostname.endswith(".simlect.com")  # 本站主域或子域


async def _topic_listener(redis_client) -> None:
    """常驻协程：订阅 Agent 推送频道，收到消息后转发到本机持有的 WebSocket（类比 @KafkaListener）。"""

    pubsub = redis_client.pubsub()  # 获取 Redis 发布订阅客户端（与普通 GET/SET 不同接口）
    await pubsub.subscribe(WS_MESSAGE_TOPIC_AGENT)  # 订阅固定频道名，开始接收 publish
    logger.info("ws_topic_subscribed", topic=WS_MESSAGE_TOPIC_AGENT)  # 启动日志，便于排查未收到推送
    try:
        async for message in pubsub.listen():  # 阻塞式异步迭代每条 pub/sub 事件（含 subscribe 确认、业务 message）
            if message["type"] != "message":  # 忽略 subscribe/unsubscribe/psubscribe 等系统消息
                continue  # 只处理 type=message 的真实业务 payload
            try:
                data = json.loads(message["data"])  # 频道 payload 应为 JSON 字符串 → dict
            except (json.JSONDecodeError, TypeError):
                continue  # 脏数据直接丢弃，不影响 listener 继续运行
            user_id = data.get("userId")  # stream_service 发布的 DTO 里必须带 userId 路由到连接
            if user_id:
                await manager.send_json(user_id, data)  # 推给该用户所有在线 WS 端
    except asyncio.CancelledError:
        # 应用 shutdown 时 cancel 本 task，需优雅 unsubscribe 释放 Redis 订阅
        await pubsub.unsubscribe(WS_MESSAGE_TOPIC_AGENT)
        raise  # 重新抛出 CancelledError，让 asyncio 正确标记 task 已取消


async def stop_ws_listener() -> None:
    """停止 Redis 订阅协程（lifespan 关闭阶段调用）。"""

    global _listener_task  # 修改模块级 task 引用
    if _listener_task and not _listener_task.done():  # task 存在且仍在运行
        _listener_task.cancel()  # 向协程注入 CancelledError
        try:
            await _listener_task  # 等待 task 真正结束（在 _topic_listener 里 unsubscribe）
        except asyncio.CancelledError:
            pass  # cancel 后的预期异常，吞掉即可
    _listener_task = None  # 清空句柄，下次 start 可重新 create_task


async def start_ws_listener(redis_client) -> None:
    """启动 Redis 订阅协程（lifespan 启动阶段调用；会先 stop 再 start 防重复）。"""

    global _listener_task
    await stop_ws_listener()  # 防止热重载或重复调用产生多个 listener 重复消费
    _listener_task = asyncio.create_task(_topic_listener(redis_client))  # 后台常驻，不阻塞 lifespan


async def _handle_heartbeat(user_id: str, ws: WebSocket, data: str) -> bool:
    """
    处理客户端心跳。
    返回 True 表示已处理（调用方应 continue）；False 表示非 ping 消息。
    """

    if data.lower() != "ping":  # 大小写不敏感匹配心跳包
        return False  # 不是 ping，交给上层其他逻辑（当前版本无其他上行）
    await redis_service.save_user_heartbeat(user_id)  # 记录用户最近在线时间（供运营/风控统计）
    await ws.send_text("pong")  # 明文回复，前端据此判断连接存活、重置超时计时器
    return True  # 已处理，主循环应 continue 等待下一条


async def websocket_endpoint(ws: WebSocket, query_token: str | None = None) -> None:
    """
    单连接处理：鉴权 → 登记 → 循环收心跳。
    下行业务数据不走这个循环，而走 Redis 订阅推送。
    """

    # CSWSH 防护：校验 Origin 同源（仅允许本站域名或未携带 Origin 的客户端）
    # 精确 hostname 白名单（防子串绕过：evilsimlect.com / localhost.evil.com）
    origin = ws.headers.get("origin")
    if origin and not _origin_allowed(origin):
        logger.warning("ws_rejected", reason="origin_not_allowed", origin=origin)
        await ws.close(code=1008, reason="origin not allowed")
        return

    # 安全：拒绝 query token（会进日志/Referer），仅允许 Cookie/Header
    token = _resolve_ws_token(
        None,  # query_token 不再信任（CSWSH/日志泄露面）
        ws.cookies.get("token"),  # Cookie 里的 token（与 HTTP 登录态一致）
        ws.headers.get("token"),  # 自定义 Header token（Gateway 可能注入）
    )
    if not token:
        logger.warning("ws_rejected", reason="token_required")  # 审计日志
        await ws.close(code=1008, reason="token required")  # 1008=Policy Violation，拒绝握手后连接
        return  # 结束协程，不再进入消息循环
    user = await get_user_by_token(token)  # 调认证服务或本地解析 JWT，得到 TokenUserInfo
    if not user or not user.user_id:
        logger.warning("ws_rejected", reason="invalid_token")
        await ws.close(code=1008, reason="invalid token")
        return
    user_id = user.user_id  # 提取 userId 作为连接表 key
    await manager.connect(user_id, ws)  # 握手 accept + 登记到 ConnectionManager
    logger.info("ws_connected", user_id=user_id)
    try:
        while True:
            data = await ws.receive_text()  # 阻塞等待客户端上行文本（主要是 ping；业务上行可扩展）
            if await _handle_heartbeat(user_id, ws, data):
                continue  # ping 已回复 pong，继续下一轮 receive
            # 当前版本不处理其他上行业务消息；扩展时可在此 elif 分支

    except WebSocketDisconnect:
        # 客户端正常关闭 Tab 或网络断开，FastAPI 抛此异常
        manager.disconnect(user_id, ws)  # 从连接表移除
        logger.info("ws_disconnected", user_id=user_id)
    except Exception:  # 兜底：二进制帧/Redis 心跳异常等 — 不清理则残留僵尸连接
        logger.warning("ws_connection_error", user_id=user_id, exc_info=True)
        try:
            manager.disconnect(user_id, ws)  # 从连接表移除（幂等）
        except Exception:
            pass
