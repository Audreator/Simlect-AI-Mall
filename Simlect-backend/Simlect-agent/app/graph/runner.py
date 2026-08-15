"""
================================================================================
文件：graph/runner.py
角色：LangGraph 执行入口（类比「启动一条 Camunda/StateMachine 流程实例」）
================================================================================

【这个文件干什么】
把「一条已落库的用户消息」交给编译好的状态图跑完：
- 正常：initial_state → graph.ainvoke(state)
- 异常中断后续跑：若 DB 仍显示「生成中」且 Redis 里有 checkpoint，则 resume

【在整体流程中的位置】
agent_service._run_agent
  → agent_engine.assistant_answer
  → run_agent_graph(本文件)          ← 你在这里
  → get_compiled_graph().ainvoke
  → nodes.py 各节点
结束后删掉本 thread 的 checkpoint，避免脏状态残留。

【怎么用】
业务代码只需：await run_agent_graph(agent_msg)
不要在业务里自己拼节点；改流程去 builder.py / nodes.py。

【关联】
- builder.get_compiled_graph
- state.initial_state / thread_id_for
- checkpoint.redis_saver（断点）
- message 表 status / assistant_message（是否允许 resume）

【注意】
resume 条件很严：消息仍是「正常未完成」且还没有助手正文，且 checkpoint 存在。
跑完 finally 一定会尝试删除 checkpoint。
================================================================================
"""

from __future__ import annotations  # 允许在类型注解里引用尚未定义的类型名

import structlog  # 结构化日志库，便于 ELK 检索

from app.constants import MSG_STATUS_NORMAL  # 消息「正常生成中」状态码常量
from app.graph.builder import get_compiled_graph  # 获取已 compile 的 LangGraph 单例
from app.graph.checkpoint.redis_saver import get_checkpointer  # Redis checkpoint 读写
from app.graph.state import initial_state, thread_id_for  # 初始状态构造 + 线程 ID 生成
from app.db.pool import acquire  # 异步 MySQL 连接池上下文管理器
from app.services.agent_runtime import parse_agent_message  # 解析用户消息中的商品卡片
from app.services.redis_service import redis_service  # Redis 客户端

logger = structlog.get_logger()  # 本模块日志记录器


async def _should_resume(user_id: str, message_id: int, thread_id: str) -> bool:
    """
    判断是否应从 checkpoint 续跑（而不是从头 invoke）。

    【类比】JVM 宕机后工作流引擎按实例 ID 恢复。
    【返回 True 的条件】
    1) agent_message 行存在且 status=正常
    2) 还没有 assistant_message（说明上次没 finalize 完）
    3) Redis checkpoint 里确实有该 thread 快照
    """

    async with acquire() as cur:  # 从连接池借一条 MySQL 连接
        await cur.execute(  # 查该消息是否仍在「生成中」且尚无助手回复
            "SELECT status, assistant_message FROM agent_message WHERE message_id=%s AND user_id=%s",
            (message_id, user_id),
        )
        row = await cur.fetchone()  # 取一行结果（dict 形式）
    if not row:  # 消息不存在 → 无法续跑
        return False
    status = row["status"]  # 消息状态字段
    assistant = row["assistant_message"]  # 助手回复正文（空表示尚未 finalize）
    if status != MSG_STATUS_NORMAL:  # 非正常状态（已取消/已完成等）→ 不续跑
        return False
    if assistant:  # 已有助手正文说明上次跑完了 → 不续跑
        return False
    checkpointer = get_checkpointer(redis_service.client)  # 获取 Redis checkpoint 实例

    await checkpointer.hydrate_thread(thread_id)  # 从 Redis 加载该 thread 的快照元数据

    config = {"configurable": {"thread_id": thread_id}}  # LangGraph 要求的 config 格式

    return (await checkpointer.aget_tuple(config)) is not None  # checkpoint 存在才返回 True


async def run_agent_graph(agent_msg: dict) -> None:
    """
    执行（或续跑）一轮智能客服图。

    【参数】agent_msg：至少含 userId、messageId、userMessage（及可选 fromProduct）。
    【副作用】节点内会推 WS、写 MySQL 助手回复、更新会话记忆等。
    """

    user_id = agent_msg["userId"]  # 用户 ID
    message_id = agent_msg["messageId"]  # 消息 ID
    thread_id = thread_id_for(user_id, message_id)  # checkpoint 线程 ID = userId:messageId
    config = {"configurable": {"thread_id": thread_id}}  # 传给 ainvoke 的配置
    graph = get_compiled_graph()  # 编译好的 LangGraph 单例
    checkpointer = get_checkpointer(redis_service.client)  # checkpoint 实例，用于清理

    try:
        if await _should_resume(user_id, message_id, thread_id):  # 判断是否断点续跑
            logger.info("graph_resume", thread_id=thread_id, message_id=message_id)  # 记录续跑日志
            # ainvoke(None) = 从 checkpoint 恢复，不重新灌 initial_state
            await graph.ainvoke(None, config)
            return  # 续跑完成直接返回

        await checkpointer.adelete_thread(thread_id)  # 全新跑：先清旧 checkpoint，避免脏数据
        card, user_text = parse_agent_message(agent_msg)  # 解析商品卡片 + 纯文本
        state = initial_state(agent_msg, card, user_text)  # 构造图初始状态 dict
        logger.info("graph_invoke", thread_id=thread_id, message_id=message_id)  # 记录全新 invoke

        await graph.ainvoke(state, config)  # 异步执行整张图直到 END
    finally:
        # 无论成功/异常/取消，都尝试删除 checkpoint，避免 Redis 堆积
        try:
            await checkpointer.adelete_thread(thread_id)
        except Exception as e:
            logger.warning("graph_checkpoint_cleanup_failed", thread_id=thread_id, error=str(e))
