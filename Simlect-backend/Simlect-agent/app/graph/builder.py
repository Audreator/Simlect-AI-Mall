"""
================================================================================
文件：graph/builder.py
角色：拼装 LangGraph 拓扑（只连线，不写业务）
================================================================================

【这个文件干什么】
用 StateGraph 注册 7 个节点并加条件边，compile 后带 Redis checkpoint。
业务逻辑全在 nodes.py；改「流程顺序」改本文件，改「某步干什么」改 nodes。

【拓扑（务必记住）】
entry → build_context → agent_loop ⇄ tools → finalize → post_turn → cleanup → END
取消时任意点可短路到 cleanup。

【如何使用】
runner 调用 get_compiled_graph()（lru_cache 单例）。测试改图后可 reset_compiled_graph_cache()。

【关联】nodes.*、state.AgentGraphState、checkpoint.redis_saver
导读：docs/智能客服-Java开发者导读.md
================================================================================
"""

from functools import lru_cache  # 函数结果缓存装饰器，编译好的图只建一次（单例）

from langgraph.checkpoint.memory import InMemorySaver  # Redis 不可用时的内存 checkpoint 兜底
from langgraph.graph import END, StateGraph  # END=图终点常量；StateGraph=状态机图构建器

from app.graph.checkpoint.redis_saver import get_checkpointer  # 获取 Redis 持久化 checkpoint 实例
from app.graph.nodes import (  # 导入 7 个节点函数（每个对应图上的一个工序）
    agent_loop_node,      # ReAct 推理：调 LLM，决定走 tools 还是 finalize
    build_context_node,   # 组装 LLM 上下文（记忆、意图、RAG）
    cleanup_node,         # 图出口前的清理桩
    entry_guard,          # 入口守卫：检查取消、解析消息
    finalize_node,        # 收束回复：写库、推 WS
    post_turn_node,       # 回合后更新会话记忆
    tools_node,           # ReAct 行动：执行 MCP 工具
)
from app.graph.state import AgentGraphState  # 图状态 TypedDict，所有节点共享的「流程上下文」
from app.services.redis_service import redis_service  # Redis 客户端，供 checkpoint 使用


def _after_entry(state: AgentGraphState) -> str:
    """entry 节点执行完后的路由函数：决定下一跳是 build_context 还是 cleanup。"""

    if state.get("cancelled"):  # 用户已点「停止生成」→ 直接跳到清理，不再跑后续节点
        return "cleanup"
    return "build_context"  # 正常流程：进入上下文组装


def _after_agent_loop(state: AgentGraphState) -> str:
    """agent_loop 节点执行完后的路由：tools / finalize / cleanup 三选一。"""

    if state.get("cancelled"):  # 中途被取消 → 短路到 cleanup
        return "cleanup"
    route = state.get("route", "finalize")  # 节点在 state 里写入的下一跳意图，默认 finalize
    if route == "tools":  # LLM 返回了 tool_calls → 去 tools 节点执行工具
        return "tools"
    if route == "end":  # 异常/取消等标记为 end → 走 cleanup 统一出口
        return "cleanup"
    return "finalize"  # 无工具调用或兜底搜索已完成 → 收束回复


def _after_tools(state: AgentGraphState) -> str:
    """tools 节点执行完后的路由：继续 agent_loop 或 finalize。"""

    if state.get("cancelled"):  # 工具执行过程中用户取消
        return "cleanup"
    if state.get("route") == "finalize":  # 已有可直接展示的卡片（订单/商品）→ 不再让 LLM 总结
        return "finalize"
    return "agent_loop"  # 普通读工具：把 ToolMessage 塞回上下文，再让 LLM 组织语言


def _resolve_checkpointer():
    """解析 checkpoint 实现：优先 Redis，失败则降级内存（开发/测试环境）。"""

    try:
        return get_checkpointer(redis_service.client)  # 生产：Redis 存图快照，支持断点续跑
    except RuntimeError:
        return InMemorySaver()  # Redis 未配置或连接失败：进程内内存，重启即丢


def build_agent_graph():
    """装配并返回已 compile 的状态图（含 checkpoint）。

    条件边：entry 取消→cleanup；agent_loop 有 tool_calls→tools 否则 finalize；
    tools 后再回 agent_loop，形成模型↔工具环。
    """

    graph = StateGraph(AgentGraphState)  # 创建状态图，状态类型为 AgentGraphState

    # 注册 7 个节点：名称字符串 → 异步处理函数
    graph.add_node("entry", entry_guard)
    graph.add_node("build_context", build_context_node)
    graph.add_node("agent_loop", agent_loop_node)
    graph.add_node("tools", tools_node)
    graph.add_node("finalize", finalize_node)
    graph.add_node("post_turn", post_turn_node)
    graph.add_node("cleanup", cleanup_node)

    graph.set_entry_point("entry")  # 图从 entry 节点开始执行

    # entry 后的条件边：路由函数返回值 → 目标节点名 的映射
    graph.add_conditional_edges("entry", _after_entry, {"build_context": "build_context", "cleanup": "cleanup"})
    graph.add_edge("build_context", "agent_loop")  # 上下文组装完固定进入 agent 主循环
    graph.add_conditional_edges(
        "agent_loop",
        _after_agent_loop,
        {"tools": "tools", "finalize": "finalize", "cleanup": "cleanup"},
    )
    graph.add_conditional_edges(
        "tools",
        _after_tools,
        {"agent_loop": "agent_loop", "finalize": "finalize", "cleanup": "cleanup"},
    )
    graph.add_edge("finalize", "post_turn")  # 收束后更新记忆
    graph.add_edge("post_turn", "cleanup")   # 记忆更新完进入清理
    graph.add_edge("cleanup", END)           # cleanup 后图执行结束

    checkpointer = _resolve_checkpointer()  # 获取 checkpoint 存储（Redis 或内存）

    return graph.compile(checkpointer=checkpointer)  # 编译图并绑定 checkpoint，返回可 ainvoke 的对象


@lru_cache(maxsize=1)
def get_compiled_graph():
    """获取编译好的图单例；进程内只 build 一次，避免重复注册节点。"""

    return build_agent_graph()


def reset_compiled_graph_cache() -> None:
    """清空 lru_cache，单元测试改拓扑后需调用以重新 compile。"""

    get_compiled_graph.cache_clear()
