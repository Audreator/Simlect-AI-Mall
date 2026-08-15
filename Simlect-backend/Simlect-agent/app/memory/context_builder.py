"""
上下文构建模块（Context Builder）。

职责：把「会话记忆 + 历史轮次 + 当前用户输入」组装成 LangChain 消息列表，
供大模型 Agent 调用。类似 Java 里把 Session、历史 Message 和 System Prompt
拼成 List<ChatMessage> 再交给 LLM 客户端。

核心概念：
- 工作记忆（working memory）：在 token 预算内选取的最近几轮完整对话
- 上下文块（context block）：摘要、商品咨询状态、待确认操作等结构化文本
"""

from __future__ import annotations  # 启用延迟类型注解，类似 Java 泛型前向引用

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage  # LangChain 消息类型，类似 ChatMessage 子类

from app.config.settings import get_settings  # 读取配置，类似 @ConfigurationProperties
from app.memory.models import SessionMemory  # 会话记忆数据模型，类似 Session DTO
from app.memory.token_estimator import estimate_text_tokens  # 估算文本 token 数
from app.services.message_service import agent_message_service  # 消息服务单例，类似 @Autowired MessageService
from app.domain.intent.types import IntentKind  # 意图枚举，类似 enum IntentKind
from app.services.prompt_service import build_agent_system_prompt  # 构建系统提示词
from app.utils.prompt_boundary import isolate_user_message  # 隔离用户消息，防 prompt 注入


def _assistant_for_context(turn: dict) -> str:
    """
    从一轮对话 dict 中取出「供上下文使用的助手回复」。

    优先用已压缩的历史版本 assistant_for_history；否则用原始 assistant_message，
    但需通过 should_include_in_working_memory 过滤（排除纯 JSON 卡片等）。

    参数:
        turn: 单轮对话 dict，含 user_message、assistant_message 等键

    返回:
        可用于上下文的助手文本；无效则返回空字符串
    """
    # 优先取 Redis/压缩后的精简版，strip() 去首尾空白，类似 String.trim()
    condensed = (turn.get("assistant_for_history") or "").strip()
    if condensed:  # 有压缩版就直接用
        return condensed
    # 否则取原始助手回复
    assistant = (turn.get("assistant_message") or "").strip()

    # 只有「适合进工作记忆」的纯文本才纳入上下文
    if assistant and agent_message_service.should_include_in_working_memory(assistant):
        return assistant
    return ""  # 卡片/空内容不进上下文


def is_complete_turn_for_context(turn: dict) -> bool:
    """
    判断一轮对话是否「完整」，可纳入 LLM 上下文。

    完整 = 用户消息非空 + 助手侧有可展示内容。

    参数:
        turn: 单轮对话 dict

    返回:
        True 表示该轮可加入 working memory
    """
    user = (turn.get("user_message") or "").strip()  # 用户侧必须有内容

    # bool() 把非空字符串转为 True，类似 Java !user.isEmpty()
    return bool(user and _assistant_for_context(turn))


def estimate_turn_tokens(turn: dict) -> int:
    """
    估算单轮对话占用的 token 数（用户 + 助手 + 固定开销）。

    参数:
        turn: 单轮对话 dict

    返回:
        token 估算值；不完整轮次返回 0
    """
    if not is_complete_turn_for_context(turn):  # 不完整不计入预算
        return 0
    user_text = turn.get("user_message") or ""
    assistant_text = _assistant_for_context(turn)

    # +8 为 role/格式等固定开销的粗略估计
    return estimate_text_tokens(user_text) + estimate_text_tokens(assistant_text) + 8


def select_working_turns(
    turns: list[dict],
    after_message_id: int,
    token_budget: int,
) -> tuple[list[dict], int | None]:
    """
    在 token 预算内，从摘要水位线之后选取「工作记忆」轮次。

    策略：从最新轮次往前贪心选取，直到再加一轮会超预算则停止。
    类似 Java 从 List 尾部倒序累加，直到 sum(tokens) > budget。

    参数:
        turns: 全部历史轮次（时间正序）
        after_message_id: 摘要已覆盖到的最大 message_id，此 ID 之后的才可选
        token_budget: 工作记忆 token 上限（来自配置）

    返回:
        (选中的轮次列表, 最旧选中轮的 message_id)；无选中时 ([], None)
    """
    # 列表推导：只保留 message_id 更大且完整的轮次，类似 stream().filter()
    eligible = [
        t for t in turns
        if int(t["message_id"]) > after_message_id and is_complete_turn_for_context(t)
    ]
    if not eligible:  # 没有可选轮次

        return [], None

    selected_rev: list[dict] = []  # 倒序收集，最后 reverse 成正序
    used = 0  # 已用 token 累计

    # reversed() 从新到旧遍历，优先保留最近对话
    for turn in reversed(eligible):
        turn_tokens = estimate_turn_tokens(turn)
        if turn_tokens <= 0:  # 异常轮次跳过
            continue

        if turn_tokens > token_budget:  # 单轮就超预算，无法纳入
            continue

        # 已有选中轮且再加会超预算 → 停止（保证不截断中间）
        if selected_rev and used + turn_tokens > token_budget:
            break
        selected_rev.append(turn)
        used += turn_tokens

    selected = list(reversed(selected_rev))  # 恢复时间正序
    oldest_id = int(selected[0]["message_id"]) if selected else None  # 最旧选中 ID，供压缩用
    return selected, oldest_id


def build_context_block(memory: SessionMemory) -> str:
    """
    把 SessionMemory 中的摘要与状态格式化为 Markdown 文本块。

    Agent 会收到第二个 SystemMessage，内容为该块，让模型知晓会话背景。

    参数:
        memory: 当前用户会话记忆对象

    返回:
        多行 Markdown 字符串
    """
    summary = memory.summary  # 长期摘要：narrative + facts
    facts = summary.get("facts") or {}  # 结构化事实，类似 Map<String, Object>
    state = memory.state  # 运行时状态：咨询商品、待确认操作等
    lines = ["## 会话摘要"]  # 逐行拼接，类似 StringBuilder
    narrative = (summary.get("narrative") or "").strip()  # 叙述性摘要
    if narrative:
        lines.append(narrative)
    goal = facts.get("goal")  # 用户目标
    budget = facts.get("budget")  # 预算

    preferences = facts.get("preferences") or []  # 偏好列表
    decisions = facts.get("decisions") or []  # 已做决策
    meta_parts = []  # 元信息片段，最后用 | 连接
    if goal:
        meta_parts.append(f"目标: {goal}")
    if budget:
        meta_parts.append(f"预算: {budget}")
    if preferences:

        meta_parts.append(f"偏好: {', '.join(map(str, preferences))}")  # join 类似 String.join
    if decisions:

        meta_parts.append(f"已决策: {', '.join(map(str, decisions[-8:]))}")  # 只取最近 8 条
    if meta_parts:
        lines.append(" | ".join(meta_parts))

    lines.append("\n## 当前状态")
    consult = state.get("consultProduct")  # 当前咨询商品
    if consult and consult.get("productName"):
        price = consult.get("minPrice")

        price_txt = f" (¥{price})" if price is not None else ""  # 三元表达式，类似 Java ? :
        lines.append(f"咨询商品: {consult.get('productName')}{price_txt}")
    else:
        lines.append("咨询商品: 无")

    pending = state.get("pendingAction")  # 待用户确认的操作（退款/评价等）
    if pending and pending.get("summary"):
        lines.append(f"待确认操作: {pending.get('summary')}")
    else:
        lines.append("待确认操作: 无")

    last = state.get("lastToolResults") or {}  # 上轮工具调用结果摘要
    if last.get("searchedProductNames") or last.get("searchedProducts"):
        lines.append("上轮曾搜索过商品（需重新调用 SEARCH_PRODUCTS 获取最新结果）")
    else:
        lines.append("上轮搜索结果: 无")

    return "\n".join(lines)  # 换行连接，类似 String.join("\n", lines)


class ContextBuilder:
    """
    上下文构建器类。

    类似 Java @Service ContextBuilder，对外提供 build_agent_messages 等方法。
    模块末尾 context_builder 为单例，类似 Spring 容器中的 bean。
    """

    async def build_agent_messages(
        self,
        user_id: str,
        user_text: str,
        memory: SessionMemory,
        *,
        intent: IntentKind = IntentKind.CHAT,
        product_snapshot: str | None = None,
        faq_text: str | None = None,
        knowledge_text: str | None = None,
    ) -> tuple[list, list[dict], int | None]:
        """
        构建发给 Agent LLM 的完整消息列表。

        顺序：系统提示 → 上下文块 → 工作记忆各轮(Human/AI) → 当前用户消息。

        参数:
            user_id: 用户 ID
            user_text: 当前轮用户输入
            memory: 会话记忆
            intent: 意图类型，影响 system prompt 模板
            product_snapshot: 可选商品快照文本
            faq_text: 可选 FAQ 文本
            knowledge_text: 可选知识库文本

        返回:
            (messages, working_turns, working_oldest_id)
            messages 为 LangChain Message 列表；后两者供 post_turn/压缩使用
        """
        settings = get_settings()  # 读取 working_token_budget 等
        # 异步构建系统提示词（可能查库/模板）
        system_prompt = await build_agent_system_prompt(
            intent,
            user_id,
            user_text,
            product_snapshot=product_snapshot,
            faq_text=faq_text,
            knowledge_text=knowledge_text,
        )
        context_block = build_context_block(memory)  # 同步构建状态块

        # 从 DB 加载历史轮次
        turns = await agent_message_service.load_turns_for_memory(user_id)
        working_turns, working_oldest_id = select_working_turns(
            turns,
            memory.summary_last_message_id,  # 摘要水位线
            settings.working_token_budget,
        )

        messages: list = [SystemMessage(content=system_prompt)]  # 第一条：角色与规则
        messages.append(SystemMessage(content=context_block))  # 第二条：会话状态

        # 把工作记忆逐轮转为 HumanMessage + AIMessage
        for turn in working_turns:
            if not is_complete_turn_for_context(turn):
                continue
            user_msg = turn.get("user_message")
            assistant_msg = _assistant_for_context(turn)

            # isolate_user_message 给用户内容加边界标记，降低注入风险
            messages.append(HumanMessage(content=isolate_user_message(user_msg)))
            messages.append(AIMessage(content=assistant_msg))

        # 最后追加当前用户本轮输入
        messages.append(HumanMessage(content=isolate_user_message(user_text)))
        return messages, working_turns, working_oldest_id

    def estimate_context_tokens(
        self,
        memory: SessionMemory,
        working_turns: list[dict],
        user_text: str,
        system_prompt: str,
    ) -> int:
        """
        估算整段上下文（不含 LangChain 包装开销）的总 token 数。

        用于压缩调度：判断是否需要触发 summary 压缩。

        参数:
            memory: 会话记忆
            working_turns: 已选工作记忆轮次
            user_text: 当前用户文本
            system_prompt: 系统提示词全文

        返回:
            估算 token 总数
        """
        total = estimate_text_tokens(system_prompt)
        total += estimate_text_tokens(build_context_block(memory))
        for turn in working_turns:
            if not is_complete_turn_for_context(turn):
                continue
            total += estimate_text_tokens(turn.get("user_message"))
            total += estimate_text_tokens(_assistant_for_context(turn))
        total += estimate_text_tokens(user_text)
        return total


context_builder = ContextBuilder()  # 模块级单例，类似 public static final ContextBuilder INSTANCE
