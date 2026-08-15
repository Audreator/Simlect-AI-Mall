"""
================================================================================
文件：services/agent_service.py
角色：发消息编排门面（类比 Java 应用服务 / Facade）
================================================================================

【这个文件干什么】
在真正跑 LangGraph 之前，把「网关入口」该做的事做完：
限流、体验次数、注入检测、敏感词、商品咨询态、用户消息落库，
然后 asyncio.create_task 丢给引擎——所以 HTTP 能马上返回 messageId。

【流程】
routes.send_message → AgentOrchestrator.send_message
  → 校验与落库
  → create_task(_run_agent) → agent_engine → graph
队列满时直接 push_error「客服繁忙」，不再起任务。

【如何使用】
外部只应通过 agent_orchestrator 单例；不要在路由里散落限流逻辑。

【关联】input_guard、message_service、stream_service、agent_engine
导读：docs/智能客服-Java开发者导读.md ；细节 app/services/MODULE.md
================================================================================
"""

import asyncio  # 标准库：协程、create_task、Lock、CancelledError（类比 CompletableFuture + ReentrantLock）
import structlog  # 第三方：结构化日志，输出 key=value 便于 ELK 检索（类比 SLF4J + MDC）

from app.config.settings import get_settings  # 读取 task_queue_max、ai_chat_limit 等配置（类比 @ConfigurationProperties）
from app.harness.guardrails.input_guard import InputGuardrail  # Prompt 注入检测护栏（类比 Servlet Filter 前置校验）
from app.services.agent_engine import agent_engine  # LangGraph 引擎门面（类比下游 DomainService 入口）
from app.services.message_service import agent_message_service  # 消息 MySQL 持久化（类比 MessageRepository）
from app.services.product_snapshot_service import product_snapshot_service  # 商品快照预加载到 Redis（类比缓存预热）
from app.services.rate_limit_service import rate_limit_service  # Redis 滑动窗口限流（类比 Guava RateLimiter / Bucket4j）
from app.services.redis_service import redis_service  # 咨询态、取消标记等 Redis 操作（类比 RedisTemplate）
from app.services.sensitive_word_service import sensitive_word_service  # 敏感词替换（类比内容审核组件）
from app.services.stream_service import stream_service  # 流式/错误推送到前端（类比 WebSocket 推送服务）
from app.utils.product_consult import build_consult_card_message, parse_consult_card  # 商品咨询卡片 JSON 解析与重建

logger = structlog.get_logger()  # 获取本模块绑定了文件名的 Logger 实例（类比 LoggerFactory.getLogger）
input_guard = InputGuardrail()  # 输入护栏单例：无状态工具类，模块级复用即可（类比 static final 工具）
AGENT_BUSY_MESSAGE = "客服繁忙，请稍后再试"  # 任务队列满时推给用户的固定文案（类比常量池字符串）

_active_tasks = 0  # 当前正在运行的 Agent 后台任务数（模块级，类似 AtomicInteger 计数器）
_active_lock = asyncio.Lock()  # 保护 _active_tasks 的异步互斥锁（类比 synchronized / ReentrantLock）


class AgentOrchestrator:
    """
    Agent 编排器：HTTP 层与 LangGraph 之间的应用服务。
    类比 Java @Service / Facade，负责校验、落库、异步调度，不含具体 LLM 节点逻辑。
    """

    async def send_message(
        self,
        user_id: str,  # 当前登录用户 ID，来自 token 解析
        message: str,  # 用户输入的原始消息正文（可能含商品卡片 JSON）
        from_product: bool = False,  # 是否从商品详情页入口发起（前端表单 fromProduct）
        consult_product_id: str | None = None,  # 商品页 URL 携带的 productId，可为空
    ) -> dict:
        """
        接收用户消息的主入口。
        同步完成校验与落库后立即返回 agent_msg（含 messageId）；
        实际推理在后台 task 中执行。
        """

        settings = get_settings()  # 每次读取最新配置，开发环境热重载时无需重启进程

        if not await rate_limit_service.allow(user_id, "sendMessage", 1, 1):
            # 滑动窗口：1 秒内最多 1 次 sendMessage；超限则抛业务异常由路由层转 600 码
            raise ValueError("发送消息过于频繁，请稍后再试")

        if settings.ai_chat_limit > 0:
            # 配置了体验次数上限（0 表示不限制，生产环境通常设 0）
            total = await agent_message_service.count_user_messages(user_id)  # 查该用户历史消息总数
            if total >= settings.ai_chat_limit:
                # 已达体验上限，拒绝继续对话
                raise ValueError("AI购物体验已经结束")

        if input_guard.detect_injection(message):
            # 检测 prompt 注入、越狱等异常输入模式（同步 CPU 判断，无需 await）
            raise ValueError("检测到异常输入")

        if from_product and consult_product_id:
            # 从商品页进入且带了 productId → 预拉商品快照到 Redis，供后续 LLM 引用
            await product_snapshot_service.ensure_consult_snapshot(
                user_id, consult_product_id.strip()  # strip 去掉 URL 参数前后空格
            )
        elif from_product:
            # 商品页但未带 ID → 仅标记咨询态 active（沿用 Redis 里已有快照）
            await redis_service.set_consult_active(user_id)
        else:
            # 普通聊天入口 → 暂停商品咨询上下文，避免误用上一次浏览的商品信息
            await redis_service.pause_consult(user_id)

        card, user_text = parse_consult_card(message)  # 尝试从消息 JSON 解析商品卡片；card≈Map，user_text 为纯文本
        if card and card.get("productId"):
            # 用户消息里嵌了商品卡片且含 productId → 解析快照并激活咨询态
            await product_snapshot_service.resolve_active_snapshot(user_id, card)
            await redis_service.set_consult_active(user_id)

        if card:
            # 带卡片的消息：只对用户附加文本做敏感词过滤，再重建完整消息体（保留卡片结构）
            filtered_text = await sensitive_word_service.replace(user_text)
            message = build_consult_card_message(card, filtered_text)  # 把过滤后的文本塞回 JSON 卡片
        else:
            # 纯文本消息：整段做敏感词过滤
            message = await sensitive_word_service.replace(message)

        agent_msg = await agent_message_service.save_user_message(user_id, message)
        # 落库后 agent_msg 是 dict（≈Map），含 messageId、userId、userMessage 等字段

        agent_msg["fromProduct"] = from_product  # 附加来源标记，供图节点判断是否走商品咨询分支

        global _active_tasks  # 声明要修改模块级变量（Python 在函数内赋值前必须 global）
        async with _active_lock:
            # async with ≈ lock.lock(); try/finally unlock；进入临界区检查并发任务上限
            if _active_tasks >= settings.task_queue_max:
                # 队列已满，不再启动新 task，直接通知用户繁忙（背压保护）
                await self._notify_busy(agent_msg)
                return agent_msg  # 仍返回 messageId，前端可展示用户已发送的消息
            _active_tasks += 1  # 占用一个任务槽，防止超额并发压垮 LLM

        asyncio.create_task(self._run_agent(agent_msg))
        # fire-and-forget：不 await，HTTP 立刻返回（类比 executor.submit / CompletableFuture.runAsync）
        return agent_msg  # 返回落库后的消息 Map，含 messageId 供前端轮询/WS 关联

    async def _run_agent(self, agent_msg: dict) -> None:
        """后台协程：运行 LangGraph；异常时推送错误并完成消息；finally 释放任务槽。"""

        global _active_tasks  # 在 finally 里递减计数，需要 global
        try:
            await agent_engine.assistant_answer(agent_msg)  # 进入 graph.runner → 各节点，直到 finalize
        except asyncio.CancelledError:
            # 任务被外部 cancel：标记消息中断（避免前端一直 loading），并重新抛出保持取消语义
            try:
                await agent_message_service.interrupt_message(
                    agent_msg.get("userId"),
                    agent_msg.get("messageId"),
                    agent_msg.get("partialAssistantMessage") or "",
                )
            except Exception:
                logger.warning("agent_cancel_mark_failed", message_id=agent_msg.get("messageId"))
            raise
        except Exception as e:
            # 未预期异常：打结构化日志 + 推 WS 错误 + 消息表标记完成，避免前端一直 loading
            logger.exception(
                "agent_task_failed",  # 日志事件名，便于监控告警
                error=str(e),  # 异常消息字符串
                error_type=type(e).__name__,  # 异常类名，如 RuntimeError
                message_id=agent_msg.get("messageId"),  # 关联的消息 ID
                user_id=agent_msg.get("userId"),  # 关联的用户 ID
            )
            user_id = agent_msg["userId"]  # 从 Map 取用户 ID，用于 WS 推送
            message_id = agent_msg["messageId"]  # 从 Map 取消息 ID

            await stream_service.push_error(
                user_id, message_id, "服务暂时不可用，请稍后重试", None  # None 表示无额外业务数据
            )
            await agent_message_service.complete_message(
                message_id, "服务异常", None, None  # 把助手回复标记为固定文案并置为完成态
            )
        finally:
            async with _active_lock:
                # 无论成功/失败/cancel，都必须释放槽位；max 防止异常路径下减成负数
                _active_tasks = max(0, _active_tasks - 1)

    async def _notify_busy(self, agent_msg: dict) -> None:
        """队列满时的统一处理：WS 推错误文案 + DB 把该条消息标记为已完成（内容为繁忙提示）。"""

        user_id = agent_msg["userId"]  # 推送给哪位用户
        message_id = agent_msg["messageId"]  # 对应哪条消息
        await stream_service.push_error(user_id, message_id, AGENT_BUSY_MESSAGE, None)
        await agent_message_service.complete_message(
            message_id, AGENT_BUSY_MESSAGE, None, None  # DB 里助手侧内容也写成繁忙提示
        )

    async def cancel_message(
        self,
        user_id: str,  # 当前用户，防止越权取消他人消息
        message_id: int,  # 要取消的那条用户消息主键
        partial_assistant_message: str | None = None,  # 前端已收到的部分流式回复（可选）
    ) -> None:
        """
        用户取消正在生成的回复。
        通过 Redis 取消标记让流式循环尽快停止；根据是否已有部分回复选择 interrupt 或 cancel。
        """

        if not await rate_limit_service.allow(user_id, "cancelMessage", 1, 1):
            # 取消操作也限流，防止恶意刷 cancel 接口
            raise ValueError("取消消息过于频繁，请稍后再试")

        await redis_service.set_cancel_flag(user_id, message_id)
        # Agent 流式节点会轮询此 Redis flag，发现后立即中止 LLM 生成

        if partial_assistant_message:
            # 前端已收到部分内容 → 存为「被打断」状态，保留 partial 文本供历史展示
            await agent_message_service.interrupt_message(
                user_id, message_id, partial_assistant_message
            )
        else:
            # 尚无助手内容 → 直接标记消息取消，不保留 assistant 字段
            await agent_message_service.cancel_message(user_id, message_id)

    async def get_consult_context(self, user_id: str) -> dict | None:
        """查询当前商品咨询上下文，供前端展示咨询条；无快照时返回 None。"""

        snapshot = await redis_service.get_consult_product(user_id)  # 从 Redis 取商品快照 Map
        if not snapshot:
            # 用户未在咨询任何商品
            return None
        return {
            "productId": snapshot.get("product_id") or snapshot.get("productId"),  # 兼容 snake/camel 两种 key
            "productName": snapshot.get("product_name") or snapshot.get("productName"),
            "active": await redis_service.is_consult_active(user_id),  # 咨询条是否高亮/active
        }


agent_orchestrator = AgentOrchestrator()  # 模块级单例，routes/agent.py 直接 import 此名（类比 @Autowired 唯一 Bean）
