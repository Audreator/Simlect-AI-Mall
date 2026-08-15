"""
================================================================================
文件：graph/state.py
角色：LangGraph「流程上下文」定义（类比 Java 工作流里的 ProcessContext / 状态 DTO）
================================================================================

【这个文件干什么】
定义一次智能客服对话在状态机里传递的所有字段（AgentGraphState），
以及如何用用户消息初始化状态（initial_state）、如何生成 checkpoint 线程号。

【在整体流程中的位置】
runner.run_agent_graph → initial_state(...) 得到初始 dict
  → 各 nodes.py 节点返回「要合并进状态的局部 dict」
  → builder 里的条件边读取 state["route"] / state["cancelled"] 决定下一跳

【怎么用】
- 节点函数签名：async def xxx(state: AgentGraphState) -> dict
  返回值只需包含「本节点改过的字段」，LangGraph 会 merge 进总状态。
- chunks / tools_called 使用 Annotated[..., operator.add]：多次返回会「追加列表」，不是覆盖。

【关联】
- builder.py：StateGraph(AgentGraphState)
- nodes.py：读写本状态
- runner.py：initial_state / thread_id_for
- 导读：docs/智能客服-Java开发者导读.md

【Java 对照】
AgentGraphState ≈ 一个大的可变 Context Bean；route ≈ 枚举下一状态；
thread_id ≈ 工作流实例 ID（userId:messageId）。
================================================================================
"""

import operator  # 标准库：提供 operator.add 函数，用于 Annotated 列表「追加合并」语义
from typing import Annotated, Any, Literal  # Annotated=带元数据的类型；Literal=字面量联合类型（类似 enum 常量）

from langchain_core.messages import BaseMessage  # LangChain 消息基类（System/Human/AI/Tool），类比 ChatMessage 层次
from typing_extensions import TypedDict  # 结构化 dict 类型（类似 Java 的 Map 接口 + @JsonProperty 字段约定）

# 条件路由取值：告诉 builder 条件边「下一步去哪个节点」（类比工作流 transition 目标状态名）
RouteKind = Literal["agent_loop", "tools", "finalize", "post_turn", "end"]


class AgentGraphState(TypedDict, total=False):
    """
    一次对话在图上的全部上下文（字段均可选，因节点分批写入）。
    total=False 表示不是所有 key 都必须存在，类似 Java DTO 字段可为 null。
    """

    # ---------- 请求身份 / 原始入参（entry 前后就有）----------
    agent_msg: dict  # 原始 Agent 消息行（含 userId、messageId、userMessage 等，类似 DB 行 Map）
    user_id: str  # 用户 ID，从 agent_msg 抽出便于节点直接读
    message_id: int  # 本轮助手消息 ID（先插库再异步生成，WS 推送与 checkpoint 都用它）
    user_message: str  # 原始用户消息（可能含商品卡片 JSON 字符串）
    user_text: str  # 解析卡片后的纯文本，给意图识别 / LLM 用
    from_product: bool  # 是否从商品详情页点进客服（影响 prompt 与工具选择）
    card: dict | None  # 本轮解析出的商品咨询卡片（≈Map，含 productId 等）
    message_card: dict | None  # 与 card 同源，兼容旧字段名（历史节点可能读此 key）

    # ---------- 控制流 ----------
    cancelled: bool  # 用户点了「停止生成」，节点应尽早检查并 route 到 end
    finished: bool  # 本轮是否已结束（finalize 后置 true）
    route: RouteKind  # 条件边依据：去 agent_loop / tools / finalize / post_turn / end

    # ---------- 给 LLM 的输入 ----------
    llm_messages: list[BaseMessage]  # System + 历史 + 当前用户，类比 List<ChatMessage>
    working_turns: list[dict]  # 本轮工作记忆里用到的对话片段（压缩后的近期上下文）
    working_oldest_id: int | None  # 工作窗口最老消息 ID（用于记忆压缩边界）

    # ---------- ReAct 循环 ----------
    # Annotated[list, operator.add]：多轮节点返回的 list 会拼接，而不是整表覆盖（LangGraph reducer）
    chunks: Annotated[list[str], operator.add]  # 流式文本碎片列表（可观测 / 兜底拼接完整回复）
    react_round: int  # 已跑的「模型↔工具」轮数（有上限，防工具死循环）
    tools_called: Annotated[list[str], operator.add]  # 已调用过的工具名列表（审计、去重）
    pending_tool_calls: list[dict]  # 本轮模型请求的 tool_calls JSON，交给 tools_node 执行

    # ---------- 工具/业务卡片回传前端 ----------
    tool_biz: dict | None  # 工具结构化结果缓存（节点间传递，避免重复解析）
    biz_type: str | None  # 前端渲染类型：商品卡 / 订单卡 / 确认卡等（类比 ViewType 枚举字符串）
    biz_data: str | None  # 业务 JSON 字符串（前端 parse 后渲染组件）
    assistant_cards: str | None  # 助手侧卡片载荷（可能与 biz_data 分工不同场景）
    search_tool_hint: str | None  # 搜索相关提示文案（引导 LLM 或前端展示）
    search_fallback_done: bool  # 是否已做过搜索兜底，避免重复调用搜索工具
    category_switch_search: bool  # 品类切换触发的搜索标记（特殊 prompt 分支）

    # ---------- 意图 ----------
    intent: str | None  # 如 query_order / refund / product_consult（build_context 解析）
    intent_data: str | None  # 意图附带结构化数据（JSON 字符串，如订单号）


def initial_state(agent_msg: dict, card: dict | None, user_text: str) -> AgentGraphState:
    """
    用「用户刚发来的一条消息」构造图的初始状态。

    【何时调用】runner 在全新 invoke（非断点续跑）时调用。
    【参数】
    - agent_msg: message_service 落库后的消息 Map
    - card / user_text: 一般由 parse_agent_message 拆出
    """

    return {
        "agent_msg": agent_msg,  # 保留原始消息行，后续节点可再解析或写回 DB
        "user_id": agent_msg["userId"],  # 从 DB 行取用户 ID（camelCase 与 Java 前端一致）
        "message_id": agent_msg["messageId"],  # 本轮消息主键，用于 WS 推送和 checkpoint thread_id
        "user_message": agent_msg.get("userMessage") or "",  # 原始用户输入；get 防 key 缺失，or "" 防 None
        "user_text": user_text,  # 解析后的纯文本，可能与 user_message 相同（无卡片时）
        "from_product": bool(agent_msg.get("fromProduct")),  # 转 bool；缺失 key 时 get 返回 None → False
        "card": card,  # 商品咨询卡片（若无卡片则为 None）
        "message_card": card,  # 兼容字段，与 card 相同引用
        "cancelled": False,  # 初始未取消
        "finished": False,  # 初始未完成，finalize 节点会改
        "route": "agent_loop",  # 默认路由：上下文组装后进入 agent 循环节点
        "llm_messages": [],  # LLM 消息列表，由 build_context 节点填充
        "working_turns": [],  # 工作记忆片段，初始空
        "working_oldest_id": None,  # 工作窗口边界，尚无历史
        "chunks": [],  # 流式文本碎片，agent 节点流式写入
        "react_round": 0,  # ReAct 轮次从 0 开始，每轮工具调用后 +1
        "tools_called": [],  # 尚未调用任何工具
        "pending_tool_calls": [],  # 无待执行工具调用
        "tool_biz": None,  # 工具业务结果尚未产生
        "biz_type": None,  # 前端卡片类型未确定
        "biz_data": None,  # 业务 JSON 尚未生成
        "assistant_cards": None,  # 助手卡片载荷空
        "search_tool_hint": None,  # 搜索提示空
        "search_fallback_done": False,  # 尚未做搜索兜底
        "category_switch_search": False,  # 尚未标记品类切换搜索
        "intent": None,  # 意图待 build_context 节点解析
        "intent_data": None,  # 意图附加数据空
    }


def thread_id_for(user_id: str, message_id: int) -> str:
    """
    Checkpoint 线程 ID：同一用户同一 messageId 对应一次图执行实例。
    类比：流程实例号 = userId + ":" + messageId（Activiti processInstanceId 风格）。
    """

    return f"{user_id}:{message_id}"  # LangGraph configurable.thread_id，用于 Redis 存/取图快照（断点续跑）
