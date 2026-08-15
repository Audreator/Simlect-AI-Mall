"""Agent 运行时服务模块：LLM 流式输出、响应最终化、工具协调与错误处理（类似 Java Orchestrator / ApplicationService 编排层）。"""

from __future__ import annotations  # 启用 postponed evaluation of annotations（Python 3.7+，类似 Java 泛型注解延迟解析）

import json  # import：JSON 序列化/反序列化（类似 Java com.fasterxml.jackson.databind）
import re  # import：正则表达式（类似 Java java.util.regex.Pattern / Matcher）

import structlog  # import：结构化日志（类似 Java SLF4J + MDC 键值对日志）
from app.services.llm_factory import create_chat_llm  # import：创建 Chat LLM 实例的工厂方法（类似 @Bean ChatModel）

from app.harness.guardrails.output_guard import strip_emojis  # import：输出护栏——去除 emoji（防止前端渲染异常）
from app.harness.guardrails.product_text_guard import (  # import：商品文本护栏相关工具
    build_consult_product_cards_json,  # 构建咨询商品卡片 JSON
    collect_known_product_names,  # 收集已知商品名称集合
    name_mentioned_in_text,  # 判断文本是否提及某商品名
    should_force_product_cards,  # 是否应强制展示商品卡片
    text_contains_product_info,  # 文本是否包含商品信息
    text_promises_product_cards,  # 文本是否承诺展示商品卡片
)
from app.harness.metrics.runtime_sensors import STREAM_TOKENS  # import：流式 token 计数指标（类似 Micrometer Counter）
from app.mcp.tools import build_mcp_tools  # import：构建 MCP 工具列表（类似 Java 注册 @Tool 方法）
from app.services.message_service import agent_message_service  # import：Agent 消息持久化服务（类似 MessageRepository）
from app.services.pending_action_service import pending_action_service  # import：待确认操作 Redis 服务（act_xxx 两阶段提交）
from app.services.product_service import (  # import：商品搜索与格式化服务
    derive_search_keyword,  # 从用户文本推导搜索关键词
    format_search_tool_message,  # 格式化搜索工具提示消息
    product_service,  # 商品服务单例
)
from app.services.redis_service import redis_service  # import：Redis 缓存/取消标记服务
from app.services.stream_service import stream_service
from app.services.sensitive_word_service import sensitive_word_service  # 流式输出敏感词过滤  # import：SSE/WebSocket 流式推送服务（类似 SseEmitter）
from app.utils.biz_payload import (  # import：业务载荷构建与校验工具
    build_action_confirm_payload,  # 构建操作确认卡片载荷（用户点 confirm 前的展示 JSON）
    build_action_confirm_unavailable_payload,  # 构建不可用的操作确认载荷（token 无效/用户不匹配）
    build_product_payload,  # 构建商品卡片载荷
    build_product_search_message,  # 构建商品搜索消息 JSON（intro + cards）
    collect_act_token_ids,  # 从文本/消息中收集 act_xxx token ID
    compact_product_search_intro,  # 压缩商品搜索引导语
    is_action_confirm_json,  # 判断是否为操作确认 JSON（防 LLM 伪造）
    is_order_cards_json,  # 判断是否为订单卡片 JSON
    is_product_cards_json,  # 判断是否为商品卡片 JSON
    looks_like_aftersales_or_order_text,  # 文本是否像售后/订单场景
    strip_embedded_product_json,  # 剥离嵌入在流中的商品 JSON（防止流式输出夹带 raw JSON）
    trim_assistant,  # 裁剪助手回复文本（去多余空白/前缀）
)
from app.utils.product_consult import is_product_consult_turn, parse_consult_card  # import：商品咨询回合解析

logger = structlog.get_logger()  # 获取本模块结构化 logger（类似 LoggerFactory.getLogger(AgentRuntime.class)）


def chunk_text(content: object) -> str:
    """
    将 LLM 流式 chunk 的 content 统一转为 str。

    Java 类比：LLM 可能返回 String 或多模态 List<Map>，类似统一转成 String 再推送 SSE。
    """
    if isinstance(content, str):  # 若已是字符串，直接返回
        return content
    if isinstance(content, list):  # list ~ List<Object>：多模态内容常为 List 片段
        parts: list[str] = []  # 声明 List<String> 收集文本片段
        for part in content:  # 遍历每个内容片段
            if isinstance(part, dict):  # dict ~ Map<String,Object>：结构化片段含 text 字段
                parts.append(str(part.get("text") or ""))  # 取 Map 中 text，缺省为空串
            elif isinstance(part, str):  # 纯字符串片段直接追加
                parts.append(part)
        return "".join(parts)  # 拼接所有片段（类似 Java String.join("", parts)）
    return ""  # 未知类型返回空串


async def is_cancelled(user_id: str, message_id: int) -> bool:
    """
    检查用户是否已取消当前消息的 LLM 生成。

    Java 类比：类似 Redis GET cancel:{userId}:{messageId}，流式循环中轮询。
    async/await 相当于 CompletableFuture<Boolean>。
    """
    return await redis_service.is_cancelled(user_id, message_id)  # await 等待 Redis 查询结果


async def resolve_action_confirm(
    full_text: str, messages: list, user_id: str
) -> tuple[str, str, str] | None:
    """
    解析 LLM 回复中的 act_xxx 确认 token，构建操作确认卡片载荷。

    Java 类比：两阶段提交的「展示确认页」阶段——
    mcp_tools_service 的 PROPOSE_* 已在 Redis 写入 pending；
    本函数根据 token 查 Redis，生成前端 action_confirm 卡片 JSON。
    返回 (assistant, bizData, bizType) 或 None（无有效 token）。

    与 Java Internal API 的关系：此处只读 Redis、不调用 Java；
    用户在前端点确认后，另一路径 action_execute 才调 Java 写库。
    """
    token_ids = collect_act_token_ids(full_text, messages)  # 从全文与历史消息收集 act_xxx token
    if not token_ids:  # 无 token 则无需确认流程
        return None  # None ~ null

    pending = None  # 待确认的 Redis 记录，初始为 null
    token_id = None  # 命中的 token ID
    for tid in token_ids:  # 按出现顺序尝试每个 token（类似遍历 List<String>）
        candidate = await pending_action_service.get_by_token(tid)  # await ~ future.get()：查 Redis pending
        if not candidate:  # Redis 无此 token（可能已过期或 LLM 伪造），尝试下一个
            continue
        if candidate.get("userId") != user_id:  # Map.get ~ 用户不匹配则拒绝（水平权限，类似 Java 校验 currentUserId）
            assistant, biz_data = build_action_confirm_unavailable_payload(  # 构建「不可用」确认卡片
                tid, full_text, reason="wrong_user"
            )
            return assistant, biz_data, "action_confirm"  # bizType 固定为 action_confirm
        pending = candidate  # 找到合法 pending 记录
        token_id = tid
        break  # 只处理第一个有效 token

    if pending and token_id:  # 成功匹配 pending 与 token
        assistant, biz_data = build_action_confirm_payload(pending, full_text)  # 构建正常确认载荷（含订单项摘要等）
        return assistant, biz_data, "action_confirm"

    # Token 存在但 Redis 无 pending —— 通常是 LLM 未调 PROPOSE_* 就臆造了【act_xxx】
    # 绝不渲染「已过期」的确认卡，返回 None 让 finalize 走丢弃伪造 token 分支
    logger.warning(  # 记录告警日志（类似 log.warn）
        "action_confirm_token_missing",
        user_id=user_id,
        tokens=token_ids,
    )
    return None  # None ~ null：让后续逻辑走「丢弃伪造 token」分支


async def stream_llm_turn(
    llm,
    messages: list,  # list ~ List<ChatMessage>：对话消息历史
    user_id: str,
    message_id: int,
    user_message: str | None,  # Optional ~ @Nullable：用户原始消息
    chunks: list[str],  # 可变 List，累积已推送的 delta 片段（类似 StringBuilder 分段）
):
    """
    流式调用 LLM 并逐段推送到前端 SSE。

    Java 类比：类似 Flux<String> 或 SseEmitter.send(delta)；
    gathered 聚合完整 AIMessage 供 finalize_agent_response 使用。
    返回 gathered 或 None（用户取消时）。
    """
    gathered = None  # 聚合的 AIMessage chunk，初始 null
    sent_visible = ""  # 已推送给前端的可见文本（用于计算增量 delta）
    async for chunk in llm.astream(messages):  # async for ~ Stream：异步迭代 LLM 流式输出
        if await is_cancelled(user_id, message_id):  # 用户取消则中断流
            return None  # 提前结束，返回 null
        gathered = chunk if gathered is None else gathered + chunk  # 累加 chunk 得到完整 AIMessage（LangChain 消息合并）
        raw_visible = strip_embedded_product_json(strip_emojis(chunk_text(gathered.content)))
        # 流式输出侧敏感词过滤（防 LLM 生成违规内容实时推送）
        visible = await sensitive_word_service.replace(raw_visible)  # 剥离嵌入 JSON 并去 emoji
        delta = visible[len(sent_visible) :]  # 仅取相对上次的新增可见文本（增量推送）
        sent_visible = visible  # 更新已发送可见文本快照
        if not delta:  # 无新增内容则跳过推送
            continue
        chunks.append(delta)  # 写入 chunks 列表供 finalize 拼接 full_text
        STREAM_TOKENS.inc(len(delta))  # 指标：累加 token/字符数（Prometheus Counter）
        await stream_service.push_chunk(user_id, message_id, delta, user_message)  # await 推送 SSE chunk 到前端
    return gathered  # 返回完整聚合消息供后续工具调用解析（若有）


def _strip_emojis_from_assistant(assistant: str) -> str:
    """
    对最终 assistant 文本去 emoji；若为商品搜索 JSON 则只处理 intro 字段。

    Java 类比：类似 ResponseFilter 在写出前清理 emoji。
    """
    text = (assistant or "").strip()  # None ~ null 安全转空串并 trim
    if not text:  # 空文本直接返回
        return ""
    if text.startswith("{"):  # 可能是 JSON 结构化消息（商品搜索等）
        try:  # try-catch：JSON 解析失败则走纯文本分支
            obj = json.loads(text)  # 反序列化为 dict ~ Map<String,Object>
            if isinstance(obj, dict):  # 确认是 Map 结构
                from app.utils.biz_payload import PRODUCT_SEARCH_RESULT_TYPE  # 延迟 import 避免循环依赖

                if obj.get("type") == PRODUCT_SEARCH_RESULT_TYPE:  # 商品搜索结果 JSON 类型
                    if obj.get("intro"):  # 若有 intro 字段
                        obj["intro"] = strip_emojis(str(obj["intro"]))  # 对 intro 去 emoji
                    return json.dumps(obj, ensure_ascii=False)  # 序列化回 JSON 字符串（保留中文）
                if obj.get("intro"):  # 其他含 intro 的 JSON 结构
                    obj["intro"] = strip_emojis(str(obj["intro"]))
                    return json.dumps(obj, ensure_ascii=False)
        except json.JSONDecodeError:  # JSON 非法则 fall through 到下方纯文本处理
            pass  # 不做处理，走下方纯文本分支
    return strip_emojis(text)  # 普通文本直接 strip_emojis


async def _resolve_product_cards_json(
    assistant_cards: str | None,  # LLM 输出的卡片 JSON 或 null
    tool_biz: dict | None,  # 工具返回的业务 Map 或 null
    tools_called: list[str] | None,  # 已调用工具名 List 或 null
    search_tool_hint: str | None = None,  # 搜索工具提示（可选）
) -> tuple[str | None, str | None]:
    """
    解析应展示的商品卡片 JSON 及 forced_biz_type。

    Java 类比：从 ToolInvokeResult.assistantCards 或 bizData.productIds 组装前端卡片。
    仅在实际调用 SEARCH_PRODUCTS 或已有 productIds 时标记 product_search。
    返回 (cards_json, forced_biz_type)。
    """
    called = list(tools_called or [])  # 复制工具列表，null 时用空 List
    # 仅当 SEARCH_PRODUCTS 实际执行时才视为商品搜索；不能单凭 search_tool_hint 推断（其他工具曾误用该字段）

    if assistant_cards and is_product_cards_json(assistant_cards):  # LLM/工具已给出合法商品卡片 JSON
        return assistant_cards, "product_search"

    ids = (tool_biz or {}).get("productIds") or []  # 从 tool_biz Map 取 productIds，缺省空 List
    if ids:  # 工具有商品 ID 列表但无卡片 JSON——按 ID 补全
        products = await product_service._load_products_by_ids([str(i) for i in ids])  # await 按 ID 加载商品详情
        cards_json, _ = build_product_payload(products)  # 构建卡片 JSON（忽略第二返回值）
        if cards_json and cards_json != "[]":  # 非空卡片才采用
            return cards_json, "product_search"

    if "SEARCH_PRODUCTS" in called:  # 调用了搜索但无卡片——仍标记 bizType 为 product_search
        return None, "product_search"
    return None, None  # 非商品搜索场景，无 forced bizType


# 不可变 Set：非商品类 MCP 工具名（物流/订单/优惠券/写提案等）
_NON_PRODUCT_TOOLS = frozenset({
    "QUERY_ORDERS",
    "QUERY_LOGISTICS",
    "QUERY_COMMENT",
    "QUERY_USER_COUPONS",
    "PROPOSE_REFUND",
    "PROPOSE_CONFIRM_RECEIPT",
    "PROPOSE_PRODUCT_REVIEW",
    "PROPOSE_RECOMMENT",
    "GET_PRODUCT_DETAIL",
})

# 不可变 Set：写操作 PROPOSE 类工具（只 create_pending，不直接调 Java 写库）
_WRITE_PROPOSE_TOOLS = frozenset({
    "PROPOSE_REFUND",
    "PROPOSE_CONFIRM_RECEIPT",
    "PROPOSE_PRODUCT_REVIEW",
    "PROPOSE_RECOMMENT",
})

# tuple：用户意图「查订单列表」的关键词 hint（用于无 QUERY_ORDERS 工具调用时也展示订单 UI）
_ORDER_LIST_UI_HINTS = (
    "我的订单",
    "最近订单",
    "最近的订单",
    "查订单",
    "订单列表",
    "买了什么",
    "买过什么",
    "最近买了",
    "最近买的",
    "最近购买",
    "再买一次",
    "复购",
    "上次买",
)


def _wants_order_list_ui(user_text: str | None) -> bool:
    """
    判断用户输入是否表达「想看订单列表」意图。

    Java 类比：简单关键词意图识别，类似 IntentMatcher.containsAny(hints)。
    """
    t = (user_text or "").strip()  # null 安全 trim
    if not t:  # 空输入
        return False
    return any(k in t for k in _ORDER_LIST_UI_HINTS)  # 任一 hint 子串命中即 true（类似 String.contains）


async def _recover_order_cards(
    user_id: str,
    assistant_cards: str | None,
    tool_biz: dict | None,
) -> str | None:
    """
    补救 QUERY_ORDERS 缺失的订单卡片 JSON，确保前端始终有可渲染的订单卡。

    Java 类比：工具返回了 orderIds 但 assistantCards 丢失时，本地重调 tool_query_orders 兜底。
    """
    if is_order_cards_json(assistant_cards):  # 已有合法订单卡 JSON
        return assistant_cards
    if (assistant_cards or "").strip() == "[]":  # 明确空列表
        return "[]"
    from app.services.mcp_tools_service import tool_query_orders  # 延迟 import 避免循环依赖

    order_ids = (tool_biz or {}).get("orderIds") or []  # 从 tool_biz 取 orderIds
    oid = str(order_ids[0]) if len(order_ids) == 1 else None  # 仅一个 ID 时作为单笔查询参数
    try:  # try-catch：本地重查订单
        local = await tool_query_orders(user_id, oid)  # await 调用 mcp_tools_service 订单查询
    except Exception as e:  # 捕获任意异常
        logger.warning("order_cards_recover_failed", error=str(e))  # 记录失败原因
        return None  # null：无法恢复
    if is_order_cards_json(local.assistant_cards):  # 本地结果含合法卡片
        logger.info("order_cards_recovered", count=len(local.order_ids or []))  # 记录恢复成功
        return local.assistant_cards
    if (local.assistant_cards or "").strip() == "[]":  # 本地结果为空列表
        return "[]"
    return None  # 仍无法恢复


async def _ensure_product_search_cards(
    user_id: str,
    user_text: str | None,
    full_text: str | None,
    cards_json: str | None,
    called: list[str],
    consult_card: dict | None,
    from_product: bool,
) -> tuple[str | None, str | None, str | None, str | None]:
    """
    必要时回填商品搜索卡片（LLM 承诺展示商品但未调工具或 SEARCH_PRODUCTS 无卡时）。

    Java 类比：编排层补偿逻辑，类似 FallbackSearchService.backfillCards()。
    返回 (cards, biz_data, hint, forced_biz)；全 None 表示无需回填。
    注意：不劫持售后/订单流程为商品搜索。
    """
    from app.domain.intent.rules import looks_like_new_product_search  # 延迟 import 意图规则

    has_cards = bool(cards_json and cards_json != "[]" and is_product_cards_json(cards_json))  # 是否已有有效商品卡
    if has_cards:  # 无需回填
        return None, None, None, None  # 四元组全 null 表示无操作

    # 绝不把售后/订单流程劫持为商品搜索回填
    if any(t in _NON_PRODUCT_TOOLS for t in called):  # 本回合调用了物流/订单/PROPOSE 等非商品工具
        return None, None, None, None
    if looks_like_aftersales_or_order_text(user_text) or looks_like_aftersales_or_order_text(full_text):  # 文本像售后/订单
        return None, None, None, None

    should_search = False  # 是否应触发商品搜索回填
    if looks_like_new_product_search(user_text or ""):  # 用户意图像新商品搜索
        should_search = True
    elif text_promises_product_cards(full_text) and "SEARCH_PRODUCTS" not in called:  # LLM 承诺卡片但未调 SEARCH_PRODUCTS
        should_search = True
    elif "SEARCH_PRODUCTS" in called:  # 已调用搜索工具但可能无卡片
        should_search = True

    if not should_search:  # 不满足搜索条件
        return None, None, None, None

    keyword = (user_text or "").strip() or (full_text or "").strip()[:40]  # 关键词：优先用户句，否则全文前 40 字
    consult = consult_card if from_product else None  # 来自商品详情页时才带咨询卡上下文
    cards_json, biz_data, _, products, source = await product_service.search_products(  # await 执行商品搜索
        user_id,
        keyword,
        user_text=user_text or "",
        consult_product=consult,
    )
    hint = format_search_tool_message(keyword or "", consult, products, source)  # 格式化搜索 hint 给 intro
    return cards_json, biz_data, hint, "product_search"  # 返回卡片、bizData、hint、强制 bizType


async def _resolve_cards_when_text_mentions_products(
    user_id: str,
    user_text: str | None,
    full_text: str | None,
    tool_biz: dict | None,
    assistant_cards: str | None,
    consult_card: dict | None,
    *,
    is_consult_turn: bool = False,  # 仅关键字参数：是否商品咨询回合
) -> tuple[str | None, str | None, str | None]:
    """
    当 LLM 文本提及商品但无卡片时，解析或补全商品卡片 JSON。

    Java 类比：文本 NER + 商品搜索补偿，优先单商品咨询卡，否则 keyword 搜索。
    返回 (cards_json, hint, biz_data)。
    """
    if is_consult_turn or is_order_cards_json(assistant_cards):  # 咨询回合或已是订单卡，不处理
        return None, None, None
    if assistant_cards and is_product_cards_json(assistant_cards):  # 已有商品卡
        return assistant_cards, None, None

    text = full_text or ""  # 全文，null 转空串
    consult = consult_card  # 本地咨询卡引用
    if consult and consult.get("productId"):  # 有咨询商品 ID（来自商品详情页）
        name = consult.get("productName") or consult.get("product_name") or ""  # 取商品名（兼容 camel/snake 字段）
        if name_mentioned_in_text(name, text):  # LLM 文本提及该商品名
            cards = build_consult_product_cards_json(consult)  # 构建单商品咨询卡 JSON
            if cards:  # 构建成功
                return cards, None, None

    lines = [ln.strip() for ln in re.split(r"[\n\r]+", text) if ln.strip()]  # 按换行拆行并去空行
    price_lines = sum(1 for ln in lines if re.search(r"(?:¥|[￥])[\d,]+|\d[\d,]*(?:\.\d+)?\s*元", ln))  # 统计含价格行数
    listing = any(re.search(r".{2,80}[—\-–]\s*\d[\d,]*(?:\.\d+)?\s*元", ln) for ln in lines)  # 是否像「商品名—价格」列表
    if price_lines < 2 and not listing and consult and consult.get("productId"):  # 非列表样式但像单品咨询
        cards = build_consult_product_cards_json(consult)
        if cards and text_contains_product_info(text, collect_known_product_names(tool_biz, consult, assistant_cards)):  # 文本含已知商品信息
            return cards, None, None

    if not consult:  # 无传入 consult，尝试 Redis 缓存
        cached = await redis_service.get_consult_product(user_id)  # await 读咨询商品缓存
        if cached and await redis_service.is_consult_active(user_id):  # 缓存存在且咨询会话仍 active
            consult = cached  # 采用缓存 consult
    exclude = str(consult["productId"]) if consult and consult.get("productId") else None  # 搜索时排除当前咨询品（避免重复）
    keyword = derive_search_keyword(user_text or text, consult)  # 从用户句+consult 推导搜索关键词
    cards_json, biz_data, _, products, source = await product_service.search_products(  # await 搜索商品
        user_id,
        keyword,
        user_text=user_text or "",
        consult_product=consult,
        exclude_product_id=exclude,
    )
    if not products:  # 无搜索结果
        return None, None, None
    hint = format_search_tool_message(keyword or "", consult, products, source)  # 格式化 hint
    return cards_json, hint, biz_data  # 返回卡片 JSON、hint、bizData


async def finalize_agent_response(
    agent_msg: dict,  # dict ~ Map：当前 agent 消息元数据（userId、messageId 等）
    chunks: list[str],  # 流式累积的文本片段 List
    messages: list,  # LLM 对话历史（含 tool 消息，供 collect_act_token_ids）
    biz_type: str | None = None,  # Optional ~ @Nullable：业务类型（product_search / query_order / action_confirm 等）
    biz_data: str | None = None,  # Optional：业务数据 JSON 串
    assistant_cards: str | None = None,  # LLM/工具产出的卡片 JSON
    tools_called: list[str] | None = None,  # 本回合调用的 MCP 工具名 List
    tool_biz: dict | None = None,  # 工具返回的业务 Map（orderIds、productIds 等）
    search_tool_hint: str | None = None,  # 商品搜索 hint 文本
    user_text: str | None = None,  # 用户输入原文
    consult_card: dict | None = None,  # 咨询商品卡 Map
    message_card: dict | None = None,  # 消息附带卡片 Map
) -> None:
    """
    流式 LLM 回合结束后的核心编排：解析 confirm token、选择 UI 类型、落库并 push_done。

    Java 类比：类似 ChatOrchestrator.finalizeTurn()，根据 toolsCalled 分支：
    - resolve_action_confirm → action_confirm 卡片（关联 Redis pending，后续 confirm 调 Java）
    - PROPOSE_* → 纯文本 + token，勿落入商品 UI
    - QUERY_ORDERS → 订单卡片 JSON
    - SEARCH_PRODUCTS → 商品搜索 JSON
    最后 agent_message_service.complete_message 持久化（类似 MessageRepository.save）。
    """
    user_id = agent_msg["userId"]  # 从 agent_msg Map 取 userId
    message_id = agent_msg["messageId"]  # 取 messageId
    full_text = "".join(chunks)  # 拼接流式 chunk 为完整 LLM 文本（类似 StringBuilder.toString()）
    is_consult_turn = is_product_consult_turn(  # 判断是否商品咨询回合（来自商品页 + 特定 messageCard）
        user_text,
        message_card,
        consult_card,
        from_product=bool(agent_msg.get("fromProduct")),  # 是否来自商品详情页入口
    )

    resolved = await resolve_action_confirm(full_text, messages, user_id)  # await 解析操作确认 act_xxx token
    if resolved:  # 命中 action_confirm 分支：展示确认卡，等待用户点 confirm 后 Java 写库
        assistant, biz_data, biz_type = resolved  # 解构三元组 (assistantJson, bizData, bizType)
    else:  # 非操作确认，走通用 finalize 分支逻辑
        # 丢弃 LLM 伪造的 act token，避免前端展示「已过期」确认卡
        full_text = re.sub(r"【act_[a-f0-9]{32}】", "", full_text or "", flags=re.I).strip()  # 去除【act_xxx】形式
        full_text = re.sub(r"act_[a-f0-9]{32}", "", full_text, flags=re.I).strip()  # 去除裸 act_xxx（无括号）
        # LLM 有时会输出伪造的 ACTION_CONFIRM JSON —— 绝不渲染
        if is_action_confirm_json(full_text) or is_action_confirm_json(assistant_cards):  # 检测到伪造确认 JSON
            logger.warning("fabricated_action_confirm_json_dropped", user_id=user_id)  # 告警日志
            full_text = "操作确认卡片无效，请重新说明退款/评价/收货需求后重试。"  # 替换为用户友好文案
            assistant_cards = None  # 清空卡片，防止前端误渲染

        called = list(tools_called or [])  # 本回合 MCP 工具调用 List 副本
        wants_orders = "QUERY_ORDERS" in called or _wants_order_list_ui(user_text)  # 是否需要订单列表 UI
        if any(t in _WRITE_PROPOSE_TOOLS for t in called):  # 调用了 PROPOSE 写操作工具（mcp_tools_service 已 create_pending）
            # PROPOSE 已执行但 Redis token 缺失/无效 —— 勿落入商品搜索 UI
            assistant = trim_assistant(full_text) or (  # 裁剪 LLM 全文或使用默认提示
                "未能生成有效确认卡片。请确认订单号/订单项、评价星级与内容后重试。"
            )
            if is_action_confirm_json(assistant):  # 裁剪后仍是伪造确认 JSON
                assistant = "未能生成有效确认卡片。请确认订单号/订单项、评价星级与内容后重试。"  # 强制替换
            biz_type = biz_type or "agent"  # 默认 bizType 为 agent（纯文本助手回复）
        elif wants_orders:  # 订单列表/单笔查询分支
            # 始终渲染订单卡片 JSON —— 绝不用 LLM 输出的 markdown/HTML 表格当 UI
            cards = await _recover_order_cards(user_id, assistant_cards, tool_biz)  # await 补救/重查订单卡
            if is_order_cards_json(cards):  # 恢复出合法订单 JSON
                assistant = cards or "[]"  # assistant 字段即为卡片 JSON 字符串
                biz_type = "query_order"
            elif (cards or "").strip() == "[]":  # 明确无订单
                assistant = "[]"
                biz_type = "query_order"
            else:  # 无法生成订单卡，降级为纯文本
                logger.warning("query_orders_without_cards", user_id=user_id)  # 告警
                assistant = trim_assistant(full_text) or "未查询到相关订单。"  # 回退 LLM 文本
                assistant = re.sub(r"【act_[a-f0-9]{32}】", "", assistant, flags=re.I).strip()  # 再次清除 act token
                biz_type = biz_type or "query_order"
        elif any(t in called for t in _NON_PRODUCT_TOOLS) and "SEARCH_PRODUCTS" not in called:  # 物流/优惠券/详情等非商品工具且未搜索
            # 物流/优惠券/写提案等：保留工具返回文本，绝不改写成商品搜索 UI
            assistant = trim_assistant(full_text) or ""  # 裁剪助手文本
            assistant = re.sub(r"【act_[a-f0-9]{32}】", "", assistant, flags=re.I).strip()  # 清除合法 act token 残留
            assistant = re.sub(r"【act_(?![a-f0-9]{32})[^】]*】", "", assistant, flags=re.I).strip()  # 清除非法 act 标记
            if is_action_confirm_json(assistant):  # 仍是伪造确认 JSON
                assistant = "操作未能完成，请稍后重试或补充订单信息。"
            if not biz_type:  # 若尚未设定 bizType，按调用的只读工具推断
                if "QUERY_LOGISTICS" in called:
                    biz_type = "query_logistics"
                elif "QUERY_COMMENT" in called:
                    biz_type = "query_comment"
                elif "QUERY_USER_COUPONS" in called:
                    biz_type = "query_coupon"
                else:
                    biz_type = "agent"
        else:  # 默认分支：可能含商品搜索/商品卡片
            # 优先使用 Agent 文本；明确售后/订单场景时阻止商品搜索「劫持」
            if looks_like_aftersales_or_order_text(user_text) and "SEARCH_PRODUCTS" not in called:  # 售后/订单且未调搜索
                assistant = trim_assistant(full_text) or ""
                if is_action_confirm_json(assistant):
                    assistant = "请补充必要信息后重试，或说明你想办理的具体操作。"
                biz_type = biz_type or "agent"
            else:  # 商品搜索/卡片解析主路径
                cards_json, forced_biz = await _resolve_product_cards_json(  # await 解析已有商品卡
                    assistant_cards, tool_biz, tools_called, search_tool_hint
                )
                backfill_cards, backfill_biz, backfill_hint, backfill_forced = await _ensure_product_search_cards(  # await 必要时回填搜索
                    user_id,
                    user_text,
                    full_text,
                    cards_json,
                    called,
                    consult_card,
                    bool(agent_msg.get("fromProduct")),
                )
                if backfill_forced:  # 触发了商品搜索回填
                    cards_json = backfill_cards
                    forced_biz = backfill_forced
                    if backfill_hint:  # 有 hint 则更新 search_tool_hint
                        search_tool_hint = backfill_hint
                    if backfill_biz:  # 有 bizData 则更新
                        biz_data = backfill_biz
                if not cards_json and should_force_product_cards(  # 仍无卡片但规则认为应强制展示商品卡
                    full_text,
                    None,
                    tool_biz,
                    consult_card,
                    assistant_cards,
                    is_consult_turn=is_consult_turn,
                    tools_called=called,
                ):
                    extra_cards, extra_hint, extra_biz = await _resolve_cards_when_text_mentions_products(  # await 按文本提及补卡
                        user_id,
                        user_text,
                        full_text,
                        tool_biz,
                        assistant_cards,
                        consult_card,
                        is_consult_turn=is_consult_turn,
                    )
                    if extra_cards:  # 补全成功
                        cards_json = extra_cards
                        forced_biz = "product_search"
                        if extra_hint:
                            search_tool_hint = extra_hint
                        if extra_biz:
                            biz_data = extra_biz
                if cards_json and is_product_cards_json(cards_json):  # 有合法商品卡片 JSON
                    intro = compact_product_search_intro(full_text, search_tool_hint)  # 压缩搜索引导语作为 intro
                    if text_promises_product_cards(intro) and cards_json == "[]":  # LLM 承诺有卡但结果空
                        intro = "未找到相关商品，请换个关键词试试。"
                    assistant = build_product_search_message(intro, cards_json)  # 组装 {type, intro, cards} JSON
                    biz_type = biz_type or forced_biz
                elif assistant_cards and assistant_cards.strip().startswith("{"):  # assistant_cards 像 JSON 但非标准商品卡
                    if is_action_confirm_json(assistant_cards):  # 伪造操作确认 JSON
                        assistant = "操作确认卡片无效，请重新发起。"
                        biz_type = biz_type or "agent"
                    else:  # 其他 JSON 卡片原样使用
                        assistant = assistant_cards
                        biz_type = biz_type or "product_search"
                elif forced_biz == "product_search" and "QUERY_ORDERS" not in called:  # 强制商品搜索但无卡
                    intro = compact_product_search_intro(full_text, search_tool_hint)
                    if text_promises_product_cards(intro):
                        intro = "未找到相关商品，请换个关键词试试。"
                    assistant = build_product_search_message(intro or "未找到相关商品，请换个关键词试试。", "[]")  # 空结果消息
                    biz_type = biz_type or forced_biz
                else:  # 纯文本 assistant 路径（无结构化卡片）
                    assistant = trim_assistant(full_text) or ""
                    assistant = re.sub(r"【act_[a-f0-9]{32}】", "", assistant, flags=re.I).strip()
                    assistant = re.sub(r"【act_(?![a-f0-9]{32})[^】]*】", "", assistant, flags=re.I).strip()
                    if is_action_confirm_json(assistant):
                        assistant = "操作确认卡片无效，请重新发起。"
                    biz_type = biz_type or "agent"
                    if should_force_product_cards(  # 最后再尝试一次强制商品卡（纯文本路径）
                        full_text,
                        assistant,
                        tool_biz,
                        consult_card,
                        assistant_cards,
                        is_consult_turn=is_consult_turn,
                        tools_called=called,
                    ):
                        extra_cards, extra_hint, extra_biz = await _resolve_cards_when_text_mentions_products(
                            user_id,
                            user_text,
                            full_text,
                            tool_biz,
                            assistant_cards,
                            consult_card,
                            is_consult_turn=is_consult_turn,
                        )
                        if extra_cards:  # 补卡成功则覆盖 assistant 为商品搜索 JSON
                            intro = compact_product_search_intro(full_text, extra_hint or search_tool_hint)
                            assistant = build_product_search_message(intro, extra_cards)
                            biz_type = "product_search"
                            if extra_biz:
                                biz_data = extra_biz

    assistant = _strip_emojis_from_assistant(assistant)  # 最终回复统一去 emoji

    await stream_service.push_done(  # await 推送流结束事件（含最终 assistant、bizType）
        user_id, message_id, assistant, biz_type, agent_msg.get("userMessage")
    )
    await agent_message_service.complete_message(message_id, assistant, biz_type, biz_data)  # await 持久化完成消息到 DB


async def push_chat_error(agent_msg: dict, prompt_type: str, partial: str = "") -> None:
    """
    推送聊天错误到 SSE 并以异常状态完成消息落库。

    Java 类比：类似 ChatController 捕获异常后 SseEmitter.completeWithError + 保存 partial 回复。
    """
    user_id = agent_msg["userId"]  # 取 userId
    message_id = agent_msg["messageId"]  # 取 messageId
    await stream_service.push_error(user_id, message_id, "服务暂时不可用，请稍后重试", prompt_type)  # await 推送错误 SSE 事件
    await agent_message_service.complete_message(  # await 以异常/降级状态完成消息
        message_id, partial or "服务异常", prompt_type, None  # bizData 为 null
    )


def bind_agent_llm():
    """
    绑定 MCP 工具并返回可流式调用的 LLM 实例。

    Java 类比：类似 @Bean ChatModel chatModel(ToolRegistry tools) {
        return chatModelBuilder().tools(tools).build();
    }
    """
    return create_chat_llm().bind_tools(build_mcp_tools())  # 创建 LLM 并 bind_tools（LangChain 链式 API）


def parse_agent_message(agent_msg: dict) -> tuple[dict | None, str]:
    """
    从 agent 消息的 userMessage 中解析咨询商品卡。

    Java 类比：类似 MessageParser.parseConsultCard(userMessage)。
    返回 (consultCardMap, remainderText)。
    """
    return parse_consult_card(agent_msg.get("userMessage") or "")  # 从 userMessage 解析，null 转空串


async def resolve_consult_card(
    user_id: str,
    message_card: dict | None = None,  # Optional Map：消息附带卡片
    memory_state: dict | None = None,  # Optional Map：会话记忆状态
    from_product: bool | None = None,  # Optional：是否来自商品详情页
) -> dict | None:
    """
    解析/恢复用户当前咨询的商品卡（可能来自 messageCard、Redis 或 memory）。

    Java 类比：委托 product_consult.resolve_consult_card，类似 ConsultContextResolver.resolve()。
    """
    from app.utils.product_consult import resolve_consult_card as _resolve  # 延迟 import 并重命名避免与本函数同名冲突

    return await _resolve(user_id, message_card, memory_state, from_product=from_product)  # await 委托给工具函数
