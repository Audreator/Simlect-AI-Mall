"""
================================================================================
文件：graph/nodes.py
角色：LangGraph 每个「工序」的具体实现（类比 Java 状态机里每个 StateHandler / @Node 方法）
================================================================================

【这个文件干什么】
实现智能客服 LangGraph 状态机上的 7 个节点 + 若干私有辅助函数。
builder.py 只负责「谁连谁、边怎么路由」；真正的业务逻辑（意图识别、调 LLM、
调 MCP 工具、推 WebSocket、写会话记忆）全部集中在本文件。

【流程（必须按这个顺序理解）】
entry_guard
  → build_context_node      # 组上下文（记忆 + 意图 + RAG + System 提示）
  → agent_loop_node ⇄ tools_node   # ReAct 循环：模型推理 ↔ 工具执行，最多 N 轮
  → finalize_node           # 收束回复、输出护栏、写 MySQL、推 WS done
  → post_turn_node          # 回合后更新会话记忆（Redis + MySQL）
  → cleanup_node            # 统一出口，标记 finished → END

【怎么读代码（给 Java 初级开发者）】
1. 每个节点签名：`async def xxx(state: AgentGraphState) -> dict`
   - 类比 Spring StateMachine 的 `Action<S, E>` 或某个 `@Step` 方法。
   - 入参 `state` 是 TypedDict（类似 Java 的 Map<String, Object> 但带类型提示）。
   - 返回值 dict 会被 LangGraph **merge** 进全局 state（类似 partial update DTO）。
2. 短路模式：`if state.get("cancelled"): return {"route": "end", ...}`
   - 类比「流程已取消 / 用户点停止 → 直接跳 cleanup，不再做后续副作用」。
3. `route` 字段是路由键：builder.py 读它决定下一跳（类似 switch-case 分支）。
4. 副作用（写库、推 WS）集中在 finalize / agent_runtime，不在 builder。

【节点一览】
| 函数名              | 职责简述                         | 典型下游 route      |
|---------------------|----------------------------------|---------------------|
| entry_guard         | 取消检测 + 解析用户消息          | build_context / end |
| build_context_node  | 组装 llm_messages + 意图         | agent_loop          |
| agent_loop_node     | 流式调 LLM / 工具兜底            | tools / finalize    |
| tools_node          | 执行 MCP 工具                    | agent_loop / finalize |
| finalize_node       | 护栏 + 写库 + 推 done            | post_turn           |
| post_turn_node      | 会话记忆压缩写入                 | cleanup             |
| cleanup_node        | 标记 finished                    | END                 |

【关联模块】
- agent_runtime：流式 LLM、取消标记、finalize_agent_response
- mcp_tool_router：执行 MCP 工具（调 Java 后端 :7060）
- session_memory / context_builder / intent / rag：组提示词
- output_guard：防止虚假完结话术
- 总导读：docs/智能客服-Java开发者导读.md
================================================================================
"""

from __future__ import annotations  # 允许在类型注解里引用尚未定义的类名（类似 Java 前向声明）

import re  # Python 标准库正则，用于从用户评价文本解析星级

import structlog  # 结构化日志库（类比 SLF4J + MDC，key-value 字段便于检索）
from langchain_core.messages import SystemMessage, ToolMessage  # LangChain 消息类型：系统提示 / 工具返回

from app.config.settings import get_settings  # 读取应用配置单例（如 graph_max_react_rounds）
from app.graph.state import AgentGraphState  # 图状态 TypedDict 定义（所有节点共享的 state 结构）
from app.harness.guardrails.output_guard import OutputGuardrail, strip_emojis  # 输出安全校验 + 去除 emoji
from app.memory.context_builder import context_builder  # 组装 LLM 消息列表（System + 历史 + 当前用户）
from app.memory.post_turn import post_turn_service  # 回合结束后记忆压缩与持久化
from app.memory.session_memory_service import session_memory_service  # 从 Redis/MySQL 加载会话记忆
from app.services import agent_runtime as rt  # 智能体运行时：LLM 绑定、流式、取消、finalize
from app.services.message_service import agent_message_service  # 消息表读写（中断保存等）
from app.services.mcp_tool_router import mcp_tool_router
from app.services.sensitive_word_service import sensitive_word_service  # 输出侧敏感词过滤  # MCP 工具路由：转发到 Java 后端
from app.services.product_service import is_similar_or_recommend_request  # 判断用户是否在要「找类似/推荐」
from app.domain.intent.classifier import resolve_intent  # 意图分类主入口（规则 + 模型）
from app.domain.intent.types import IntentKind  # 意图枚举（查订单、退款、商品咨询等）
from app.domain.intent.rules import looks_like_category_switch, looks_like_new_product_search  # 品类切换启发式
from app.rag.retriever import rag_retriever  # RAG FAQ 检索器
from app.utils.product_consult import is_product_consult_turn  # 判断本轮是否「围绕当前商品咨询」
from app.services.product_snapshot_service import product_snapshot_service  # 商品详情快照服务
from app.services.redis_service import redis_service  # Redis：咨询商品绑定、取消标记、messageId 绑定
from app.utils.order_ids import extract_order_id, extract_order_item_id, extract_refund_target_id  # 从文本抽订单号
from app.utils.biz_payload import is_order_cards_json, is_product_cards_json  # 判断 assistant_cards 是否为订单/商品卡片 JSON

logger = structlog.get_logger()  # 本模块专用 logger（类比 private static final Logger log）
output_guard = OutputGuardrail()  # 输出护栏单例：防止模型未调工具却谎称「已退款/已发货」

# ---------------------------------------------------------------------------
# 常量：用户说这些话时，前端必须展示「订单卡片」组件，不能只用 Markdown 表格
# 类比 Java：private static final Set<String> ORDER_LIST_UI_HINTS = Set.of(...)
# ---------------------------------------------------------------------------
_ORDER_LIST_UI_HINTS = (
    "我的订单",      # 直接查全部订单
    "最近订单",      # 时间维度
    "最近的订单",
    "查订单",
    "订单列表",
    "买了什么",      # 口语化问法
    "买过什么",
    "最近买了",
    "最近买的",
    "最近购买",
    "再买一次",      # 复购场景也需要看到历史订单卡片
    "复购",
    "上次买",
)


def _wants_order_list_cards(user_text: str | None) -> bool:
    """
    【辅助函数】判断用户问法是否要求前端渲染订单卡片。

    【功能】扫描 user_text 是否包含 _ORDER_LIST_UI_HINTS 中任一关键词。

    【输入】
    - user_text: 用户本轮纯文本（可能为 None）

    【输出】
    - bool: True 表示必须用订单卡片 UI；False 表示无此硬性要求

    【下游】agent_loop_node 在 LLM 未调 QUERY_ORDERS 时会读此结果做强制兜底。
    """
    t = (user_text or "").strip()  # 去掉首尾空白；None 当空串处理（类比 Optional.orElse("")）
    if not t:  # 无有效文本 → 不可能命中关键词
        return False
    return any(k in t for k in _ORDER_LIST_UI_HINTS)  # 任一关键词子串命中即 True（类比 String.contains）


async def entry_guard(state: AgentGraphState) -> dict:
    """
    【节点 1/7】入口守卫 —— 图的第一个节点，所有请求必经。

    【功能】
    1) 检查 Redis 取消标记：用户点「停止生成」则短路到 cleanup。
    2) 从 agent_msg 解析商品咨询卡片 + 用户纯文本 user_text。

    【输入 state 关键字段】
    - user_id, message_id, agent_msg

    【输出 dict 关键字段】
    - cancelled: bool — 是否已取消
    - card / message_card: 商品卡片 dict 或 None
    - user_text: str — 用户输入纯文本
    - route: "end"（取消时）或省略（正常时由 builder 默认连 build_context）

    【下游】
    - cancelled=True → cleanup_node
    - 否则 → build_context_node
    """
    user_id = state["user_id"]  # 从初始 state 取用户 ID（类比 Long userId = state.getUserId()）
    message_id = state["message_id"]  # 本轮 WebSocket 消息 ID，用于取消检测与写库
    if await rt.is_cancelled(user_id, message_id):  # 查 Redis：用户是否已点「停止生成」
        return {"cancelled": True, "finished": True, "route": "end"}  # 短路：标记取消并路由到结束
    card, user_text = rt.parse_agent_message(state["agent_msg"])  # 解析原始消息 → (商品卡片, 纯文本)
    return {"card": card, "message_card": card, "user_text": user_text, "cancelled": False}  # 正常进入 build_context


async def build_context_node(state: AgentGraphState) -> dict:
    """
    【节点 2/7】组装本轮给大模型的完整上下文。

    【功能概要】
    - 加载会话记忆（Redis/MySQL 中的摘要 + consultProduct 等状态）
    - 解析/刷新「正在咨询的商品」卡片，处理品类切换
    - 意图分类 resolve_intent（订单/退款/导购/闲聊等）
    - 可选 RAG 检索 FAQ 片段
    - context_builder 拼出 llm_messages（System + 历史 + 当前用户）
    - 按意图追加 SystemMessage 提示（查订单必须先调工具等）

    【输入 state 关键字段】
    - user_id, message_id, user_text, card, from_product

    【输出 dict 关键字段】
    - llm_messages: 给 LLM 的完整消息列表
    - intent, intent_data: 意图及附带数据（如订单号）
    - working_turns, working_oldest_id: 工作记忆窗口
    - react_round=0, pending_tool_calls=[]: 重置 ReAct 状态
    - route="agent_loop"

    【下游】agent_loop_node

    【类比 Java】Controller 调一堆 Service/Helper 拼好 ChatCompletionRequest，
    但此节点还不调用模型，只准备 Prompt。
    """
    if state.get("cancelled"):  # 上游 entry_guard 或别处已标记取消 → 不再组装上下文
        return {"route": "end", "finished": True}

    user_id = state["user_id"]  # 当前用户 ID
    message_id = state["message_id"]  # 本轮消息 ID（日志与 Redis 绑定用）
    user_text = state["user_text"]  # 用户输入的纯文本
    card = state.get("card")  # 本轮消息附带的商品卡片（可能 None，类比 Optional<ProductCard>）
    from_product = state.get("from_product", False)  # 是否从商品详情页入口进入（影响咨询判定）

    memory = await session_memory_service.load(user_id, redis_service.client)  # 加载会话记忆（摘要 + state 字典）
    consult_card = await rt.resolve_consult_card(  # 解析「当前正在咨询的商品」卡片
        user_id, card, memory.state, from_product=from_product
    )

    consult_name = (consult_card or {}).get("productName") or (consult_card or {}).get("product_name")  # 咨询商品名（兼容两种字段名）
    switching_away = (  # 用户是否在「切换品类/换商品」而离开当前咨询品
        consult_card  # 前提：之前有绑定的咨询商品
        and not (card and card.get("productId"))  # 且本轮消息没再带新商品卡片
        and (
            looks_like_category_switch(user_text, consult_name)  # 文本像「换品类/不要这个了」
            or looks_like_new_product_search(user_text)  # 或像全新商品搜索
        )
        and not is_similar_or_recommend_request(user_text)  # 「找类似/推荐」不算切换，仍围绕原品
    )
    category_switch_search = switching_away or (  # 品类切换搜索标记（供 agent_loop 兜底用）
        looks_like_new_product_search(user_text)  # 像新品搜索
        and not is_product_consult_turn(  # 且不是围绕当前商品的咨询
            user_text, card, consult_card, from_product=from_product
        )
    )
    if switching_away:  # 确认用户已切换品类 → 清 Redis 里的咨询商品绑定
        await redis_service.clear_consult(user_id)  # 删 Redis key
        memory.state.pop("consultProduct", None)  # 内存中的会话 state 也清（类比 map.remove）
        consult_card = None  # 本轮不再绑定旧咨询品

    snapshot = None  # 商品详情快照文本，后续塞入 System 提示
    if consult_card and consult_card.get("productId"):  # 有咨询商品 ID 则拉详情快照
        snapshot = await product_snapshot_service.resolve_active_snapshot(user_id, consult_card)

    if card and card.get("productId"):  # 用户本轮带了商品卡片 → 写入记忆 state 供下轮使用
        memory.state["consultProduct"] = {
            "productId": str(card["productId"]),  # 统一转字符串存
            "productName": card.get("productName"),
            "minPrice": card.get("minPrice"),
            "cover": card.get("cover"),
            "categoryId": card.get("categoryId"),
        }

    intent, intent_source, intent_data = await resolve_intent(  # 意图分类（规则 + 可选 LLM）
        user_id,
        user_text,
        from_product=from_product,
        consult_card=consult_card,
        message_card=card,
    )

    # 以下意图即使用户文本像在搜新品，也不强行改成 PRODUCT_SEARCH（订单/退款等业务优先）
    _keep_intent = {
        IntentKind.QUERY_ORDER,       # 查订单
        IntentKind.QUERY_LOGISTICS,   # 查物流
        IntentKind.QUERY_COMMENT,     # 查评价
        IntentKind.QUERY_COUPON,      # 查优惠券
        IntentKind.PRODUCT_REVIEW,    # 商品评价
        IntentKind.RECOMMENT,         # 追评
        IntentKind.REFUND,            # 退款
        IntentKind.CONFIRM_RECEIPT,   # 确认收货
        IntentKind.CANCEL_ORDER,      # 取消订单
    }
    if (switching_away or category_switch_search) and intent not in _keep_intent:
        intent = IntentKind.PRODUCT_SEARCH  # 覆盖为商品搜索意图
        intent_source = "category_switch"  # 标记来源便于日志追踪
    faq_text = ""  # RAG 检索到的 FAQ 片段
    knowledge_text = ""  # 闲聊场景注入的通用知识
    if intent in (IntentKind.PRODUCT_CONSULT, IntentKind.CHAT):  # 商品咨询 / 闲聊才检索 FAQ
        faq_text = await rag_retriever.search_faq(user_text)
    if intent == IntentKind.CHAT:  # 纯闲聊把 FAQ 当 knowledge 注入 System
        knowledge_text = faq_text

    messages, working_turns, working_oldest_id = await context_builder.build_agent_messages(  # 拼 LLM 消息列表
        user_id,
        user_text,
        memory,
        intent=intent,
        product_snapshot=snapshot,
        faq_text=faq_text,
        knowledge_text=knowledge_text,
    )

    logger.info(  # 结构化日志：记录意图解析结果，便于排查
        "agent_intent_resolved",
        user_id=user_id,
        message_id=message_id,
        intent=intent.value,
        source=intent_source,
        intent_data=intent_data or None,
    )

    if card and card.get("productId") and snapshot and intent != IntentKind.PRODUCT_CONSULT:
        # 非纯商品咨询意图但用户带了卡片：额外塞一条商品详情 System 提示
        messages.append(
            SystemMessage(content=f"## 当前咨询商品详情\n{snapshot}")
        )

    if (
        consult_card
        and consult_card.get("productId")
        and is_similar_or_recommend_request(user_text)  # 用户要类似/推荐商品
        and not is_product_consult_turn(
            user_text, card, consult_card, from_product=from_product
        )
    ):
        messages.append(  # 提示模型必须调 SEARCH_PRODUCTS，禁止编造商品名价格
            SystemMessage(
                content=(
                    "【系统提示】用户可能在找类似/推荐商品。"
                    "若需要真实商品列表，请调用 SEARCH_PRODUCTS 后再回复；"
                    "不要编造商品名或价格；有结果时引导查看下方卡片。"
                )
            )
        )

    if category_switch_search:  # 品类切换场景：提示按新意图搜，别死磕旧咨询品
        messages.append(
            SystemMessage(
                content=(
                    "【系统提示】用户可能已切换品类或发起新的商品搜索。"
                    "请按最新意图作答；需要商品列表时调用 SEARCH_PRODUCTS"
                    "（keyword 用品类/品牌/特征），不要强行围绕旧咨询商品拒绝切换。"
                )
            )
        )

    await redis_service.bind_message_id(user_id, message_id)  # 绑定当前生成中的 messageId（供取消检测）
    if intent == IntentKind.QUERY_ORDER:  # 查订单意图：必须先调 QUERY_ORDERS 工具
        messages.append(
            SystemMessage(
                content=(
                    "【系统提示】本轮更像查订单。"
                    "若要陈述用户订单事实，请先调用 QUERY_ORDERS；"
                    "政策/如何查看订单类问题可直接说明入口。"
                )
            )
        )
    elif intent == IntentKind.QUERY_LOGISTICS:  # 查物流意图：必须先调 QUERY_LOGISTICS
        messages.append(
            SystemMessage(
                content=(
                    "【系统提示】本轮更像查物流。"
                    "若要陈述物流轨迹，请先调用 QUERY_LOGISTICS；"
                    "缺订单号时先追问，不要编造轨迹。"
                )
            )
        )

    return {  # merge 进 state，供 agent_loop_node 消费
        "llm_messages": messages,           # 完整 LLM 输入
        "working_turns": working_turns,     # 工作记忆轮次
        "working_oldest_id": working_oldest_id,  # 工作记忆最老消息 ID
        "card": consult_card or card,       # 优先咨询卡片，否则用消息卡片
        "message_card": card,               # 保留原始消息卡片
        "category_switch_search": category_switch_search,  # 品类切换标记
        "intent": intent.value,             # 意图存字符串值（Enum.value）
        "intent_data": intent_data or None, # 意图附带数据（如订单号）
        "react_round": 0,                   # 重置 ReAct 轮次计数
        "pending_tool_calls": [],           # 清空待执行工具队列
        "route": "agent_loop",              # 下一跳：Agent 主循环
    }


# ---------------------------------------------------------------------------
# 评价星级解析：从用户文本提取 1-5 星
# 正则覆盖多种中文表达：「评价5星」「打4分」「星级：3」等
# ---------------------------------------------------------------------------
_STAR_RE = re.compile(
    r"(?:评[价分]|打)\s*([1-5])\s*星|([1-5])\s*星|星级\s*[：:]*\s*([1-5])|给.{0,8}([1-5])\s*分",
    re.I,  # 忽略大小写（虽然中文场景影响不大，保持一致性）
)
_POSITIVE_STAR_HINTS = ("好评", "很好", "不错", "可以", "满意", "推荐", "赞", "棒", "给力", "喜欢")  # 正面词 → 推断 5 星
_NEGATIVE_STAR_HINTS = ("差评", "很差", "太差", "失望", "糟糕", "垃圾", "坑")  # 负面词 → 推断 1 星
_NEUTRAL_STAR_HINTS = ("一般", "还行", "凑合", "普通")  # 中性词 → 推断 3 星

# 这些意图在 LLM 首轮未调工具时，需要服务端「强制 MCP」兜底（配置 force_mcp_on_llm_skip 开启时）
# 类比 Java：private static final Set<String> TOOL_REQUIRED_INTENTS = Set.of(...)
_TOOL_REQUIRED_INTENTS = frozenset(
    {
        IntentKind.QUERY_ORDER.value,       # 查订单
        IntentKind.QUERY_LOGISTICS.value,   # 查物流
        IntentKind.QUERY_COMMENT.value,     # 查评价
        IntentKind.QUERY_COUPON.value,      # 查优惠券
        IntentKind.REFUND.value,            # 退款
        IntentKind.CONFIRM_RECEIPT.value,   # 确认收货
        IntentKind.PRODUCT_REVIEW.value,    # 商品评价
        IntentKind.RECOMMENT.value,         # 追评
    }
)


def _extract_order_id(*texts: str | None) -> str | None:
    """
    【辅助函数】从多段文本中提取订单号。

    【输入】*texts — 可变参数，多段待扫描文本（user_text、intent_data 等）

    【输出】str | None — 提取到的订单号，无则 None

    【下游】_required_tool_for_intent、agent_loop_node 强制 QUERY_ORDERS 等。
    """
    return extract_order_id(*texts)  # 委托 utils/order_ids 模块，避免重复实现


def _extract_review_star(text: str) -> int | None:
    """
    【辅助函数】从用户评价文本推断 1-5 星级。

    【功能】
    1) 先用正则匹配显式星级（「5星」「打4分」）
    2) 再用情感词启发式（「很好」→5，「很差」→1）
    3) 提到「评价」但无数字 → 默认 5 星

    【输入】text — 用户原始文本

    【输出】int | None — 1~5 或 None（无法推断）

    【下游】_required_tool_for_intent 构造 PROPOSE_PRODUCT_REVIEW 参数。
    """
    t = text or ""  # 防空（None → 空串）
    m = _STAR_RE.search(t)  # 先尝试正则匹配显式星级表达
    if m:
        for g in m.groups():  # 正则有四个捕获组，任一命中即可
            if g:
                return int(g)  # 转为整数星级返回
    if any(k in t for k in _NEGATIVE_STAR_HINTS):  # 含负面情感词
        return 1
    if any(k in t for k in _NEUTRAL_STAR_HINTS):  # 含中性情感词
        return 3
    if any(k in t for k in _POSITIVE_STAR_HINTS):  # 含正面情感词
        return 5
    if "评价" in t or "打分" in t or "评星" in t:  # 提到评价但没具体星级 → 默认好评
        return 5
    return None  # 完全无法推断星级


def _extract_review_content(text: str, order_id: str | None) -> str | None:
    """
    【辅助函数】从用户文本剥离订单号、星级词后，提取评价正文。

    【输入】
    - text: 用户原始文本
    - order_id: 已知订单号（会从正文中剔除，避免被当评价内容）

    【输出】str | None — 评价正文（最多 200 字），无实质内容则 None

    【下游】_required_tool_for_intent 构造 PROPOSE_PRODUCT_REVIEW / PROPOSE_RECOMMENT。
    """
    raw = (text or "").strip()  # 去首尾空白
    if not raw:  # 空文本无内容可提取
        return None
    cleaned = raw  # 工作副本，逐步清洗
    if order_id:
        cleaned = cleaned.replace(order_id, " ")  # 去掉订单号，避免误当评价正文
    cleaned = _STAR_RE.sub(" ", cleaned)  # 用空格替换星级表达

    sentiment = ""  # 若清洗后无实质内容，用情感关键词兜底
    for k in _POSITIVE_STAR_HINTS + _NEGATIVE_STAR_HINTS + _NEUTRAL_STAR_HINTS:
        if k in cleaned and k not in ("可以",):  # 「可以」歧义大，单独处理
            sentiment = k
            break
    if "可以" in cleaned and not sentiment:  # 单独捕获「可以」
        sentiment = "可以"
    cleaned = re.sub(  # 去掉业务动词残留（退款、确认收货、评价等指令词）
        r"(请?帮我)?(申请)?退款|(确认收货)|评价一下|评价|好评|差评|追评|打分|评星|星级|订单号|订单",
        " ",
        cleaned,
    )
    cleaned = re.sub(r"\s+", " ", cleaned).strip(" ，。、：:;；~～")  # 归一空白和首尾标点
    if len(cleaned) >= 1:  # 清洗后仍有实质文字
        return cleaned[:200]  # 截断至 200 字返回
    if sentiment:  # 无实质文字但捕获到情感词
        return sentiment  # 情感词本身也算评价内容
    return None  # 完全无内容


def _missing_write_args_prompt(intent: str | None, intent_data: str | None, user_text: str) -> str:
    """
    【辅助函数】写操作意图缺参数时，生成追问用户的提示文案。

    【输入】
    - intent: 意图字符串值（如 product_review）
    - intent_data: 意图附带数据（可能含订单号）
    - user_text: 用户原文（备用抽订单号）

    【输出】str — 可直接展示给用户的追问话术

    【下游】目前被 agent_loop 相关兜底逻辑引用（参数不足时引导用户补充）。
    """
    oid = (intent_data or "").strip() or _extract_order_id(user_text) or ""  # 优先 intent_data 里的订单号
    if intent == IntentKind.PRODUCT_REVIEW.value:  # 商品评价缺参
        if not oid:
            return "请提供要评价的订单号，并说明星级（1-5）和评价内容，例如：订单号xxx 5星 物流很快。"
        return (
            f"已识别订单 {oid}。请补充评价星级（1-5星）和评价内容，"
            "例如：「5星 包装完好物流很快」，我再为您生成确认卡片。"
        )
    if intent == IntentKind.RECOMMENT.value:  # 追评缺参
        if not oid:
            return "请提供要追评的订单号和追评内容。"
        return f"已识别订单 {oid}。请补充追评内容，我再为您生成确认卡片。"
    if intent == IntentKind.REFUND.value:  # 退款缺参
        return "请提供要退款的订单号或订单项ID，我再为您生成退款确认卡片。"
    if intent == IntentKind.CONFIRM_RECEIPT.value:  # 确认收货缺参
        return "请提供要确认收货的订单号。"
    return "请补充订单相关信息后重试。"  # 通用兜底话术


async def _required_tool_for_intent(
    intent: str | None,
    intent_data: str | None,
    user_text: str,
    user_id: str,
) -> tuple[str, dict] | None:
    """
    【辅助函数】根据意图返回必须执行的工具 (tool_name, args)。

    【功能】为各业务意图构造 MCP 工具名与参数字典；
    参数不足（如缺订单号）则返回 None，由上层决定追问用户。

    【输入】
    - intent: 意图字符串值
    - intent_data: 意图附带数据
    - user_text: 用户原文
    - user_id: 用户 ID（退款场景查可退项用）

    【输出】
    - tuple[str, dict] | None — (工具名, 参数字典) 或 None

    【下游】agent_loop_node 在 tool_required_first_turn 兜底时调用。
    """
    if intent == IntentKind.QUERY_ORDER.value:  # 查订单：有单号查单笔，无单号查列表
        args: dict = {}  # 空 dict 表示查全部/最近订单
        oid = (intent_data or "").strip() or _extract_order_id(user_text)
        if oid:
            args["orderId"] = oid  # 有单号则按单查
        return "QUERY_ORDERS", args
    if intent == IntentKind.QUERY_LOGISTICS.value:  # 查物流：必须有订单号
        oid = (intent_data or "").strip() or _extract_order_id(user_text)
        if not oid:
            return None  # 缺订单号 → 无法调工具
        return "QUERY_LOGISTICS", {"orderId": oid}
    if intent == IntentKind.QUERY_COMMENT.value:  # 查评价：必须有订单号
        oid = (intent_data or "").strip() or _extract_order_id(user_text)
        if not oid:
            return None
        return "QUERY_COMMENT", {"orderId": oid}
    if intent == IntentKind.QUERY_COUPON.value:  # 查优惠券：无需额外参数
        return "QUERY_USER_COUPONS", {}
    if intent == IntentKind.CANCEL_ORDER.value:  # 取消订单：先查订单展示（客服不直接取消）
        oid = (intent_data or "").strip() or _extract_order_id(user_text)
        if not oid:
            return None
        return "QUERY_ORDERS", {"orderId": oid}  # 复用 QUERY_ORDERS 展示订单
    if intent == IntentKind.CONFIRM_RECEIPT.value:  # 确认收货：生成提案卡片
        oid = (intent_data or "").strip() or _extract_order_id(user_text)
        if not oid:
            return None
        return "PROPOSE_CONFIRM_RECEIPT", {"orderId": oid}
    if intent == IntentKind.REFUND.value:  # 退款：需解析 orderItemId
        from app.services.order_service import order_service  # 延迟导入，避免模块循环依赖（类比 @Lazy）

        raw_id = (
            extract_order_item_id(user_text, intent_data)  # 先从文本/intent_data 抽 orderItemId
            or extract_refund_target_id(intent_data, user_text)  # 再尝试退款目标 ID
            or ""
        ).strip()
        if not raw_id:
            return None  # 无法识别退款目标
        item = await order_service.get_order_item(raw_id)  # 先假设 raw_id 是 orderItemId 查库
        if item and item.get("order_item_id"):
            return "PROPOSE_REFUND", {"orderItemId": str(item["order_item_id"])}  # 命中 → 直接提案退款
        order_id = extract_order_id(raw_id) or raw_id  # 否则把 raw_id 当 orderId
        refundable = await order_service.list_refundable_items(user_id, order_id)  # 查该订单可退项列表
        if len(refundable) == 1 and refundable[0].get("order_item_id"):  # 仅一项可退 → 直接提案
            return "PROPOSE_REFUND", {"orderItemId": str(refundable[0]["order_item_id"])}
        if len(refundable) > 1:  # 多项可退 → 先展示订单让用户选择
            return "QUERY_ORDERS", {"orderId": order_id}
        return "PROPOSE_REFUND", {"orderItemId": raw_id}  # 兜底：直接用 raw_id 提案
    if intent == IntentKind.PRODUCT_REVIEW.value:  # 商品评价提案：需订单号 + 星级 + 内容三要素
        oid = (intent_data or "").strip() or _extract_order_id(user_text)
        star = _extract_review_star(user_text)  # 解析星级
        content = _extract_review_content(user_text, oid)  # 解析评价正文
        if not oid or star is None or not content:  # 三要素缺一不行
            return None
        return "PROPOSE_PRODUCT_REVIEW", {
            "orderId": oid,
            "commentContent": content,
            "star": star,
        }
    if intent == IntentKind.RECOMMENT.value:  # 追评提案：需订单号 + 内容
        oid = (intent_data or "").strip() or _extract_order_id(user_text)
        content = _extract_review_content(user_text, oid)
        if not oid or not content:
            return None
        return "PROPOSE_RECOMMENT", {"orderId": oid, "reCommentContent": content}
    return None  # 该意图无对应的强制工具


async def agent_loop_node(state: AgentGraphState) -> dict:
    """
    【节点 3/7】Agent 主循环 —— ReAct 的「推理（Reason）」半步。

    【功能】
    - bind_tools 后流式调用 LLM（边生成边经 stream_service 推前端 WebSocket）
    - 若模型返回 tool_calls → route=tools，把调用列表放进 pending_tool_calls
    - 若无工具调用 → route=finalize
    - 额外兜底：意图强制 MCP、商品搜索兜底（模型忘了调工具时服务端补调）

    【输入 state 关键字段】
    - llm_messages, react_round, intent, card, user_text, chunks 等

    【输出 dict 关键字段】
    - route: "tools" | "finalize" | "end"
    - pending_tool_calls / chunks / assistant_cards 等

    【下游】
    - route=tools → tools_node
    - route=finalize → finalize_node
    - route=end → cleanup_node

    【注意】react_round 超过 graph_max_react_rounds 会强制 finalize，防止工具死循环。

    【类比 Java】while 循环里先「想一步」：需要外部数据就 break 出去调 Feign Client。
    """
    if state.get("cancelled") or state.get("finished"):  # 已取消或已结束 → 不再调 LLM
        return {"route": "end"}

    agent_msg = state["agent_msg"]  # 原始 WebSocket 消息行（含 userMessage 等）
    user_id = state["user_id"]
    message_id = state["message_id"]
    messages = list(state.get("llm_messages") or [])  # 拷贝一份消息列表，避免原地修改 state
    turn_chunks: list[str] = []  # 本轮流式生成的文本碎片（最终 merge 进 state.chunks）

    settings = get_settings()  # 读取配置（含 graph_max_react_rounds）
    if state.get("react_round", 0) >= settings.graph_max_react_rounds:  # 超过最大 ReAct 轮 → 强制收束
        return {"route": "finalize"}

    if await rt.is_cancelled(user_id, message_id):  # 生成过程中用户点了「停止」
        partial = "".join(state.get("chunks") or [])  # 拼接已生成的部分文本
        if partial:
            await agent_message_service.interrupt_message(user_id, message_id, partial, "agent")  # 存中断内容到 MySQL
        await redis_service.clear_bound_message_id(user_id)  # 解绑 Redis 中的 messageId
        return {"cancelled": True, "finished": True, "route": "end"}

    llm = rt.bind_agent_llm()  # 获取绑定了 MCP 工具列表的 LLM 实例
    consult = state.get("card")  # 当前咨询商品卡片
    user_text = state.get("user_text") or ""
    from_product = state.get("from_product", False)
    tools_called = state.get("tools_called") or []  # 历史已调过的工具名列表
    similar_first_turn = (  # 首轮「找类似/推荐」且模型可能不调 SEARCH_PRODUCTS 的兜底条件
        state.get("react_round", 0) == 0  # 必须是第一轮
        and not state.get("search_fallback_done")  # 尚未做过搜索兜底
        and is_similar_or_recommend_request(user_text)  # 用户文本像「找类似/推荐」
        and not is_product_consult_turn(  # 不是围绕当前商品的纯咨询
            user_text, state.get("message_card"), consult, from_product=from_product
        )
        and consult  # 有咨询商品
        and consult.get("productId")
        and "SEARCH_PRODUCTS" not in tools_called  # 尚未调过搜索
    )
    category_switch_first_turn = (  # 首轮品类切换搜索兜底条件
        state.get("react_round", 0) == 0
        and not state.get("search_fallback_done")
        and state.get("category_switch_search")  # build_context 标记的品类切换
        and not is_product_consult_turn(
            user_text, state.get("message_card"), consult, from_product=from_product
        )
        and "SEARCH_PRODUCTS" not in tools_called
    )
    intent_name = state.get("intent")  # 当前意图字符串值
    intent_data = state.get("intent_data")  # 意图附带数据
    tool_required_first_turn = (  # 特定意图首轮强制 MCP 的条件（需配置 force_mcp_on_llm_skip 开启）
        bool(settings.force_mcp_on_llm_skip)
        and state.get("react_round", 0) == 0
        and intent_name in _TOOL_REQUIRED_INTENTS
        and not state.get("search_fallback_done")
    )
    try:
        if similar_first_turn or category_switch_first_turn or tool_required_first_turn:
            # 兜底场景用非流式 ainvoke：避免先推一段错误文本到前端再改正
            response = await llm.ainvoke(messages)
        else:
            # 正常路径：流式生成并通过 WebSocket 实时推给前端
            response = await rt.stream_llm_turn(
                llm,
                messages,
                user_id,
                message_id,
                agent_msg.get("userMessage"),
                turn_chunks,
            )
    except Exception as e:  # LLM 调用失败（网络/限流/模型错误等）
        logger.warning("llm_turn_failed", error=str(e), error_type=type(e).__name__)
        await rt.push_chat_error(agent_msg, "agent", "".join(state.get("chunks") or []))  # 推错误给前端
        await redis_service.clear_bound_message_id(user_id)
        return {"finished": True, "route": "end"}

    if response is None:  # 流式过程中被取消或中断
        partial = "".join((state.get("chunks") or []) + turn_chunks)  # 合并已有 + 本轮碎片
        if partial:
            await agent_message_service.interrupt_message(user_id, message_id, partial, "agent")
        await redis_service.clear_bound_message_id(user_id)
        return {"cancelled": True, "finished": True, "route": "end"}

    tool_calls = getattr(response, "tool_calls", None) or []  # 从 LLM 响应取 tool_calls（可能为空列表）
    if tool_calls:  # 模型请求调工具 → 交给 tools_node 执行
        pending = [  # 规范化 pending 结构
            {"id": tc["id"], "name": tc["name"], "args": tc.get("args") or {}}
            for tc in tool_calls
        ]
        messages.append(response)  # 把 AI 消息（含 tool_calls 字段）加入上下文，供下轮 LLM 读
        return {
            "llm_messages": messages,
            "pending_tool_calls": pending,  # 待执行工具队列
            "react_round": state.get("react_round", 0) + 1,  # 轮次 +1
            "route": "tools",  # 下一跳 tools_node
        }

    if similar_first_turn and not tool_calls:  # 模型没调搜索 → 服务端兜底 SEARCH_PRODUCTS
        llm_body = strip_emojis(rt.chunk_text(getattr(response, "content", "") or ""))  # 取模型已说的文本
        fallback_chunks = [llm_body] if llm_body else []  # 保留模型已输出的一小段（若有）
        search_args: dict = {"keyword": user_text, "excludeProductId": str(consult["productId"])}  # 排除当前咨询品
        result = await mcp_tool_router.invoke("SEARCH_PRODUCTS", search_args, user_id)  # 直接调 MCP
        biz_dict = result.to_biz_dict() or {}
        logger.info(
            "search_fallback_after_llm_skip",
            user_id=user_id,
            product_id=consult.get("productId"),
            has_cards=bool(result.assistant_cards),
        )
        return {
            "llm_messages": messages,
            "tools_called": ["SEARCH_PRODUCTS"],  # 记录已调工具
            "tool_biz": biz_dict or None,
            "biz_type": result.biz_type,
            "biz_data": result.biz_data,
            "assistant_cards": result.assistant_cards,  # 商品卡片 JSON
            "search_tool_hint": result.to_tool_message(),  # 搜索提示给 finalize 用
            "search_fallback_done": True,  # 标记已兜底，避免重复
            "chunks": fallback_chunks,
            "pending_tool_calls": [],
            "route": "finalize",  # 有卡片可直接 finalize，不再回 LLM 总结
        }

    if category_switch_first_turn and not tool_calls:  # 品类切换兜底：强制 SEARCH_PRODUCTS
        llm_body = strip_emojis(rt.chunk_text(getattr(response, "content", "") or ""))
        fallback_chunks = [llm_body] if llm_body else []
        search_args = {"keyword": user_text}  # 新品类关键词，不排除任何商品
        result = await mcp_tool_router.invoke("SEARCH_PRODUCTS", search_args, user_id)
        biz_dict = result.to_biz_dict() or {}
        logger.info(
            "category_switch_search_fallback",
            user_id=user_id,
            keyword=user_text,
            has_cards=bool(result.assistant_cards),
        )
        return {
            "llm_messages": messages,
            "tools_called": ["SEARCH_PRODUCTS"],
            "tool_biz": biz_dict or None,
            "biz_type": result.biz_type,
            "biz_data": result.biz_data,
            "assistant_cards": result.assistant_cards,
            "search_tool_hint": result.to_tool_message(),
            "search_fallback_done": True,
            "chunks": fallback_chunks,
            "pending_tool_calls": [],
            "route": "finalize",
        }

    if tool_required_first_turn and not tool_calls:  # 查订单/退款等意图：LLM 未调工具 → 强制 MCP
        forced = await _required_tool_for_intent(intent_name, intent_data, user_text, user_id)
        if forced:  # 参数齐全，可以强制调工具
            tool_name, tool_args = forced
            result = await mcp_tool_router.invoke(tool_name, tool_args, user_id)
            biz_dict = result.to_biz_dict() or {}
            tool_text = result.to_tool_message() or ""  # 工具返回的可读文本
            messages.append(  # 伪造 ToolMessage 塞回上下文，让后续轮次知道工具已执行
                ToolMessage(content=tool_text or "未查询到相关记录。", tool_call_id="forced_mcp")
            )
            logger.warning(
                "forced_mcp_after_llm_skip",
                user_id=user_id,
                intent=intent_name,
                tool=tool_name,
                has_cards=bool(result.assistant_cards),
                has_act_token="act_" in tool_text.lower(),  # 是否含 action 确认 token
            )
            if result.assistant_cards and result.assistant_cards.strip() not in ("", "[]"):
                chunks_out: list[str] = []  # 有卡片 → 正文留空，前端渲染卡片组件
            else:
                chunks_out = [tool_text or "未查询到相关记录。"]  # 无卡片 → 展示工具返回文本
            if intent_name == IntentKind.CANCEL_ORDER.value:  # 取消订单：额外追加引导话术
                guide = (
                    "客服侧暂不支持直接取消订单，请到「我的订单」页面自行取消。"
                )
                if chunks_out:
                    chunks_out = [guide + "\n" + chunks_out[0]]  # 引导 + 原有文本
                else:
                    chunks_out = [guide]
            biz_type = result.biz_type
            if not biz_type:  # 工具未返回 biz_type 时按工具名推断
                if tool_name == "QUERY_ORDERS":
                    biz_type = "query_order"
                elif tool_name == "QUERY_LOGISTICS":
                    biz_type = "query_logistics"
                elif tool_name == "QUERY_COMMENT":
                    biz_type = "query_comment"
                elif tool_name == "QUERY_USER_COUPONS":
                    biz_type = "query_coupon"
                elif tool_name.startswith("PROPOSE_"):  # 所有提案类工具
                    biz_type = "action_confirm"
            return {
                "llm_messages": messages,
                "tools_called": [tool_name],
                "tool_biz": biz_dict or None,
                "biz_type": biz_type,
                "biz_data": result.biz_data,
                "assistant_cards": result.assistant_cards,
                "search_tool_hint": result.to_tool_message() if tool_name == "SEARCH_PRODUCTS" else None,
                "search_fallback_done": True,
                "chunks": chunks_out,
                "pending_tool_calls": [],
                "route": "finalize",
            }

    if (
        state.get("react_round", 0) == 0  # 首轮
        and not tool_calls  # LLM 未调工具
        and not state.get("search_fallback_done")  # 尚未兜底
        and (
            intent_name == IntentKind.QUERY_ORDER.value  # 查订单意图
            or _wants_order_list_cards(user_text)  # 或 UI 约定必须用订单卡片
        )
    ):
        oid = (intent_data or "").strip() or _extract_order_id(user_text)
        tool_args = {"orderId": oid} if oid else {}  # 有单号查单笔，否则查列表
        result = await mcp_tool_router.invoke("QUERY_ORDERS", tool_args, user_id)
        biz_dict = result.to_biz_dict() or {}
        tool_text = result.to_tool_message() or ""
        messages.append(
            ToolMessage(content=tool_text or "未查询到相关订单。", tool_call_id="forced_orders_ui")
        )
        logger.warning(
            "forced_query_orders_for_cards",
            user_id=user_id,
            intent=intent_name,
            has_cards=bool(result.assistant_cards),
        )
        return {
            "llm_messages": messages,
            "tools_called": ["QUERY_ORDERS"],
            "tool_biz": biz_dict or None,
            "biz_type": result.biz_type or "query_order",
            "biz_data": result.biz_data,
            "assistant_cards": result.assistant_cards,
            "search_tool_hint": None,
            "search_fallback_done": True,
            "chunks": [],  # 正文留空，靠订单卡片展示
            "pending_tool_calls": [],
            "route": "finalize",
        }

    messages.append(response)  # 普通文本回复：把 AI 消息加入上下文（供 post_turn 记忆）
    if not turn_chunks:  # 非流式路径（兜底 ainvoke）可能没有 turn_chunks
        llm_body = strip_emojis(rt.chunk_text(getattr(response, "content", "") or ""))
        if llm_body:
            turn_chunks = [llm_body]  # 从 response.content 补全
    return {
        "llm_messages": messages,
        "chunks": turn_chunks,  # 追加到 state.chunks（LangGraph operator.add 合并）
        "pending_tool_calls": [],
        "route": "finalize",  # 无工具调用 → 进入收束
    }


async def tools_node(state: AgentGraphState) -> dict:
    """
    【节点 4/7】工具执行 —— ReAct 的「行动（Act）」半步。

    【功能】
    - 遍历 pending_tool_calls，经 mcp_tool_router.invoke 调 MCP 服务（Java :7060）
    - 读工具：结果变成 ToolMessage 塞回 llm_messages，通常再回 agent_loop 让模型总结
    - 若已有订单/商品卡片等可直接展示：route=finalize，少一轮 LLM 废话
    - 写工具 PROPOSE_*：只生成确认卡，不直接改 Java 库

    【输入 state 关键字段】
    - pending_tool_calls, llm_messages, user_text, card 等

    【输出 dict 关键字段】
    - llm_messages（含 ToolMessage）
    - tools_called, assistant_cards, biz_type, biz_data
    - route: "agent_loop" | "finalize"

    【下游】
    - 有卡片且未超轮次限制 → finalize_node
    - 普通读工具 → agent_loop_node（让 LLM 总结）
    - 超轮次 → finalize_node

    【关联链路】mcp_tool_router → mcp_streamable_client → mcp_server → JavaInternalClient
    """
    agent_msg = state["agent_msg"]  # 原始消息（本节点主要用 user_id/message_id）
    user_id = state["user_id"]
    message_id = state["message_id"]
    messages = list(state.get("llm_messages") or [])  # 拷贝消息列表，避免原地修改
    called: list[str] = []  # 本轮实际执行的工具名列表
    tool_biz = dict(state.get("tool_biz") or {})  # 累积工具结构化结果（merge 用）
    biz_type = state.get("biz_type")  # 业务类型（query_order / product_search 等）
    biz_data = state.get("biz_data")  # 业务附加数据
    assistant_cards = state.get("assistant_cards")  # 助手卡片 JSON 载荷
    search_tool_hint = state.get("search_tool_hint")  # 搜索工具提示文本

    for tc in state.get("pending_tool_calls") or []:  # 逐个执行模型请求的工具调用
        if await rt.is_cancelled(user_id, message_id):  # 工具执行过程中用户取消
            return {"cancelled": True, "finished": True, "route": "end"}
        if tc["name"] == "SEARCH_PRODUCTS" and is_product_consult_turn(  # 商品咨询场景禁止搜别的品
            state.get("user_text"),
            state.get("message_card"),
            state.get("card"),
            from_product=state.get("from_product", False),
        ):
            messages.append(  # 塞一条「拒绝搜索」的 ToolMessage，不实际调 MCP
                ToolMessage(
                    content="【系统提示】当前为商品咨询，请勿搜索其他商品；围绕当前咨询商品作答。",
                    tool_call_id=tc["id"],
                )
            )
            continue  # 跳过真实 SEARCH_PRODUCTS 调用
        result = await mcp_tool_router.invoke(tc["name"], tc.get("args") or {}, user_id)  # 调 MCP 工具
        called.append(tc["name"])  # 记录已执行工具名
        messages.append(ToolMessage(content=result.to_tool_message(), tool_call_id=tc["id"]))  # 工具结果回上下文

        biz_dict = result.to_biz_dict()  # 工具返回的结构化 biz 字段
        if biz_dict:
            tool_biz.update(biz_dict)  # 累积合并（多工具时后者覆盖同 key）
        if result.assistant_cards:  # 工具有卡片载荷（订单卡/商品卡/确认卡）
            assistant_cards = result.assistant_cards
            biz_type = result.biz_type or biz_type  # 优先用工具返回的 biz_type
            biz_data = result.biz_data or biz_data
        if tc["name"] == "QUERY_ORDERS":  # 查订单特殊处理
            biz_type = result.biz_type or biz_type or "query_order"  # 默认 query_order
            if not result.assistant_cards:
                logger.warning("query_orders_missing_cards_in_tools_node", user_id=user_id)  # 缺卡片告警
        if tc["name"] == "SEARCH_PRODUCTS":
            search_tool_hint = result.to_tool_message()  # 搜索提示留给 finalize 拼接
        if tc["name"] == "GET_PRODUCT_DETAIL":  # 拉商品详情后更新咨询快照
            product_id = (tc.get("args") or {}).get("productId") or (tc.get("args") or {}).get("product_id")
            if product_id:
                await product_snapshot_service.ensure_consult_snapshot(user_id, str(product_id))

    settings = get_settings()
    if is_order_cards_json(assistant_cards) and "QUERY_ORDERS" in called:
        # 已有订单卡片 JSON → 直接 finalize，不再让 LLM 重复描述表格
        logger.info("finalize_after_order_cards", user_id=user_id)
        return {
            "llm_messages": messages,
            "tools_called": called,
            "pending_tool_calls": [],  # 清空待执行队列
            "tool_biz": tool_biz or None,
            "biz_type": biz_type or "query_order",
            "biz_data": biz_data,
            "assistant_cards": assistant_cards,
            "search_tool_hint": search_tool_hint,
            "chunks": [],  # 正文留空，前端渲染卡片
            "route": "finalize",
        }
    if is_product_cards_json(assistant_cards) and "SEARCH_PRODUCTS" in called:
        # 已有商品搜索卡片 JSON → 同样直接 finalize
        return {
            "llm_messages": messages,
            "tools_called": called,
            "pending_tool_calls": [],
            "tool_biz": tool_biz or None,
            "biz_type": biz_type or "product_search",
            "biz_data": biz_data,
            "assistant_cards": assistant_cards,
            "search_tool_hint": search_tool_hint,
            "chunks": [],
            "route": "finalize",
        }

    # 普通读工具：未超 ReAct 轮次上限则回 agent_loop 让模型总结；否则强制 finalize
    next_route = "agent_loop" if state.get("react_round", 0) < settings.graph_max_react_rounds else "finalize"
    return {
        "llm_messages": messages,
        "tools_called": called,
        "pending_tool_calls": [],
        "tool_biz": tool_biz or None,
        "biz_type": biz_type,
        "biz_data": biz_data,
        "assistant_cards": assistant_cards,
        "search_tool_hint": search_tool_hint,
        "route": next_route,  # agent_loop 或 finalize
    }


async def finalize_node(state: AgentGraphState) -> dict:
    """
    【节点 5/7】收束本轮助手回复。

    【功能】
    1) output_guard：防止「没调工具却谎称已退款/已发货」等虚假完结话术
    2) agent_runtime.finalize_agent_response：拼最终文案、写 MySQL、推 WS DONE + 业务卡片
    3) finally 块清理 Redis 上绑定的 messageId

    【输入 state 关键字段】
    - chunks, llm_messages, tools_called, assistant_cards, biz_type 等

    【输出 dict 关键字段】
    - finished=True, route="post_turn"

    【下游】post_turn_node → cleanup_node

    【类比 Java】@Transactional 方法的最后一步：持久化 + 发事件 + 清理临时状态。
    """
    agent_msg = state["agent_msg"]
    user_id = state["user_id"]
    message_id = state["message_id"]

    try:
        if state.get("cancelled"):  # 已取消则跳过 finalize 写库（避免脏数据）
            return {"finished": True, "route": "end"}

        chunks = list(state.get("chunks") or [])  # 流式/兜底产生的文本碎片列表
        messages = list(state.get("llm_messages") or [])  # 完整 LLM 对话上下文
        full_text = "".join(chunks)  # 拼接成完整助手正文
        tools_called = state.get("tools_called") or []  # 本轮实际调过的工具
        guarded = output_guard.validate_no_false_completion(full_text, tools_called)  # 输出护栏校验
        if guarded != full_text:  # 护栏改写了文本（检测到虚假完结话术）
            chunks = [guarded]  # 用改写后的文本替换
            full_text = guarded
        # 输出侧敏感词过滤（防 LLM 生成违规内容落到最终回复/入库）
        filtered = await sensitive_word_service.replace(full_text)
        if filtered != full_text:
            chunks = [filtered]
            full_text = filtered

        await rt.finalize_agent_response(  # 核心：写 DB、推 WS done、附业务卡片
            agent_msg,
            chunks,
            messages,
            biz_type=state.get("biz_type"),
            biz_data=state.get("biz_data"),
            assistant_cards=state.get("assistant_cards"),
            tools_called=tools_called,
            tool_biz=state.get("tool_biz"),
            search_tool_hint=state.get("search_tool_hint"),
            user_text=state.get("user_text"),
            consult_card=state.get("card"),
            message_card=state.get("message_card"),
        )
    except Exception as e:  # finalize 失败不应阻断 cleanup
        logger.exception("graph_finalize_failed", error=str(e))
        await rt.push_chat_error(agent_msg, "agent", "".join(state.get("chunks") or []))  # 推错误给前端
    finally:
        await redis_service.clear_bound_message_id(user_id)  # 无论成败都解绑 messageId

    return {"finished": True, "route": "post_turn"}  # 下一跳：回合后记忆更新


async def post_turn_node(state: AgentGraphState) -> dict:
    """
    【节点 6/7】回合后处理 —— 写入/压缩会话记忆。

    【功能】
    把本轮 user_text + assistant 回复摘要写入 Redis/MySQL，
    供下一轮 build_context_node 加载，避免每次把全部历史塞进 Prompt。

    【输入 state 关键字段】
    - user_id, message_id, user_text, chunks, assistant_cards, tools_called 等

    【输出 dict 关键字段】
    - finished=True

    【下游】cleanup_node

    【类比 Java】异步 @Async 方法更新「用户会话级缓存」；
    失败只打日志，不阻断用户已看到的回复（最终一致性）。
    """
    if state.get("cancelled"):  # 取消的回合不更新记忆（避免脏摘要）
        return {"finished": True}

    user_id = state["user_id"]
    message_id = state["message_id"]
    user_text = state["user_text"]
    card = state.get("card")  # 咨询商品卡片（写入记忆 state）
    assistant_text = "".join(state.get("chunks") or []) or (state.get("assistant_cards") or "")  # 助手侧文本

    try:
        await post_turn_service.run(  # 压缩/写入 Redis + MySQL 会话记忆
            user_id=user_id,
            message_id=message_id,
            user_text=user_text,
            assistant_text=assistant_text,
            tools_called=state.get("tools_called") or [],
            tool_biz=state.get("tool_biz"),
            card=card,
            working_turns=state.get("working_turns") or [],
            working_oldest_id=state.get("working_oldest_id"),
        )
    except Exception as e:
        logger.exception("post_turn_failed", user_id=user_id, error=str(e))  # 记忆失败不影响用户体验

    return {"finished": True}  # 标记完成，builder 会连到 cleanup


async def cleanup_node(state: AgentGraphState) -> dict:
    """
    【节点 7/7】图终点前的清理桩。

    【功能】
    标记 finished=True，随后 builder 边连到 END。
    取消、异常短路也会汇聚到这里，保证状态机有统一出口。

    【输入】任意 state（通常 cancelled 或 finished 已为 True）

    【输出】{"finished": True}

    【下游】LangGraph END（图执行完毕）

    【类比 Java】finally 块或 StateMachine 的 terminal state handler。
    """
    return {"finished": True}  # 仅标记完成，无其他副作用；builder 会连到 END
