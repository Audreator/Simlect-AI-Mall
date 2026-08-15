"""
用户意图分类模块（Intent Classifier）。

职责：根据用户输入、商品卡片、来源通道等，判定用户意图类型（IntentKind）。
采用多层策略：结构规则 → 高置信规则 → LLM → 普通规则 → 默认 CHAT。
类似 Java 责任链模式（Chain of Responsibility）或 Strategy 组合。

输出用于：选择不同的 Agent 提示词、工具权限、FAQ/知识库检索等。
"""

from __future__ import annotations

import json  # 解析 LLM 返回的 JSON 意图

import structlog  # 结构化日志
from langchain_core.messages import HumanMessage, SystemMessage  # LLM 消息类型

from app.config.settings import get_settings  # intent_use_llm、intent_rule_fallback 等开关
from app.domain.intent.types import IntentKind  # 意图枚举，类似 public enum IntentKind
from app.harness.metrics.runtime_sensors import INTENT_TOTAL  # Prometheus 计数器
from app.services.llm_factory import create_memory_llm  # 创建轻量 LLM 客户端
from app.services.prompt_service import load_user_intent_classifier_prompt  # 意图分类提示模板
from app.utils.order_ids import extract_order_id  # 从文本提取订单号
from app.utils.product_consult import is_product_consult_turn, normalize_consult_card  # 商品咨询判定

logger = structlog.get_logger()


def _structural_intent(
    user_text: str,
    *,
    from_product: bool = False,
    consult_card: dict | None = None,
    message_card: dict | None = None,
) -> IntentKind | None:
    """
    结构型意图：根据是否来自商品页、是否带商品卡片等「硬信号」判定。

    最高优先级之一，不依赖关键词。

    参数:
        user_text: 用户输入
        from_product: 是否从商品详情页进入
        consult_card: 咨询商品卡片
        message_card: 消息附带的商品卡片

    返回:
        PRODUCT_CONSULT 或 None
    """
    if is_product_consult_turn(
        user_text, message_card, consult_card, from_product=from_product
    ):
        return IntentKind.PRODUCT_CONSULT
    return None


def classify_intent_by_rules(
    user_text: str,
    *,
    from_product: bool = False,
    consult_card: dict | None = None,
    message_card: dict | None = None,
) -> IntentKind | None:
    """
    基于关键词/规则的意图分类（兜底策略）。

    按固定顺序匹配中文关键词，命中即返回对应 IntentKind。
    类似 Java 里一长串 if-else contains() 规则引擎。

    参数:
        user_text: 用户输入
        from_product: 是否来自商品页
        consult_card: 咨询卡片
        message_card: 消息卡片

    返回:
        匹配的 IntentKind；无匹配返回 None
    """
    # 延迟 import 减轻模块加载负担
    from app.domain.intent.rules import (
        looks_like_category_switch,
        looks_like_new_product_search,
    )
    from app.services.product_service import is_similar_or_recommend_request

    t = (user_text or "").strip()  # 规范化文本
    if not t:  # 空输入当闲聊
        return IntentKind.CHAT

    # 先走结构型规则
    structural = _structural_intent(
        user_text,
        from_product=from_product,
        consult_card=consult_card,
        message_card=message_card,
    )
    if structural:
        return structural

    # 「如何/怎么 + 订单/优惠券/退款…」→ 操作说明，走 CHAT 让 Agent 直接答
    _howto = any(
        k in t
        for k in (
            "如何",
            "怎么",
            "怎样",
            "步骤",
            "方法",
            "教程",
            "在哪用",
            "哪里用",
            "怎么用",
            "如何用",
            "怎么使用",
            "如何使用",
            "在哪使用",
            "在哪里用",
            "在哪看",
            "哪里看",
        )
    )
    # 操作说明类：交给 Agent 直接答，勿强行查库
    if _howto and any(
        k in t
        for k in (
            "取消",
            "优惠券",
            "优惠卷",
            "用券",
            "退款",
            "退货",
            "评价",
            "收货",
            "物流",
            "快递",
            "订单",
        )
    ):
        return IntentKind.CHAT

    # 以下按业务优先级依次匹配
    if any(k in t for k in ("追评", "再评", "二次评价")):
        return IntentKind.RECOMMENT
    if any(k in t for k in ("退款", "退货", "退钱")):
        return IntentKind.REFUND
    if any(k in t for k in ("确认收货", "已收到", "收货确认")):
        return IntentKind.CONFIRM_RECEIPT
    if any(k in t for k in ("取消这个订单", "不要这个订单", "帮我取消", "给我取消")) or (
        "取消订单" in t
    ):
        return IntentKind.CANCEL_ORDER
    if any(k in t for k in ("物流", "快递", "到哪了", "运单", "包裹")):
        return IntentKind.QUERY_LOGISTICS
    if any(k in t for k in ("查看评价", "评价内容", "写了什么评价", "我的评价")):
        return IntentKind.QUERY_COMMENT
    if any(k in t for k in ("评价", "好评", "差评", "打分", "评星", "星级")) and any(
        k in t for k in ("订单", "给", "写", "提交")
    ):
        return IntentKind.PRODUCT_REVIEW
    if any(k in t for k in ("我的优惠券", "查优惠券", "有哪些券", "还有几张券", "可用券", "未使用券")):
        return IntentKind.QUERY_COUPON
    if any(k in t for k in ("优惠券", "优惠卷")) and any(
        k in t for k in ("查", "看看", "有没有", "还有", "几张", "列表")
    ):
        return IntentKind.QUERY_COUPON
    # 「取消订单」含「订单」字样，勿误判为查单
    if "取消" not in t and any(
        k in t
        for k in (
            "我的订单",
            "查订单",
            "买了什么",
            "买过什么",
            "最近买",
            "最近购买",
            "最近订单",
            "订单列表",
        )
    ):
        return IntentKind.QUERY_ORDER
    # bare 订单 太宽，留给 LLM；规则不抢

    if is_similar_or_recommend_request(t) or looks_like_new_product_search(t):
        return IntentKind.PRODUCT_SEARCH

    consult_name = (consult_card or {}).get("productName") or (consult_card or {}).get("product_name")
    if consult_card and looks_like_category_switch(t, consult_name):
        return IntentKind.PRODUCT_SEARCH

    if any(k in t for k in ("搜索", "找", "买", "推荐", "热销", "爆款")) and len(t) <= 40:
        return IntentKind.PRODUCT_SEARCH
    return None  # 规则未命中


def classify_high_confidence_order_intent(user_text: str) -> IntentKind | None:
    """
    仅返回高置信意图类型（不含附带 data）。

    兼容旧调用方；内部委托 classify_high_confidence_intent。

    参数:
        user_text: 用户输入

    返回:
        IntentKind 或 None
    """
    intent, _ = classify_high_confidence_intent(user_text)
    return intent


def classify_high_confidence_intent(user_text: str) -> tuple[IntentKind | None, str]:
    """
    高置信、低误判的少量规则（仅防 PRODUCT_SEARCH 等误分类）。

    完整业务策略由 Agent + LLM 决定；此处不编码全部退款/评价规则。
    类似 Java 里只保留「明显查单/查物流」的 fast path。

    参数:
        user_text: 用户输入

    返回:
        (意图, 附带数据如订单号)；无匹配则 (None, "")
    """
    t = (user_text or "").strip()
    if not t:
        return None, ""
    oid = extract_order_id(t) or ""  # 尝试提取订单号

    # 含订单号样式 + 物流关键词 → 查物流
    if oid and any(k in t for k in ("到哪里", "到哪了", "物流", "快递", "运单", "包裹", "轨迹")):
        return IntentKind.QUERY_LOGISTICS, oid
    if any(
        k in t
        for k in (
            "我的订单",
            "最近的订单",
            "最近订单",
            "查订单",
            "订单列表",
            "买了什么",
            "买过什么",
            "最近买了",
            "最近买的",
            "最近购买",
        )
    ):
        return IntentKind.QUERY_ORDER, oid
    return None, ""


def _parse_intent_json(raw: str) -> tuple[IntentKind | None, str]:
    """
    从 LLM 原始输出中解析 JSON 意图。

    LLM 可能带多余文字，故用 find/rfind 截取第一个 { ... } 块。

    参数:
        raw: LLM 返回字符串

    返回:
        (IntentKind, data)；解析失败 (None, partial_data)
    """
    text = (raw or "").strip()
    if not text:
        return None, ""
    start = text.find("{")  # 第一个左花括号
    end = text.rfind("}")  # 最后一个右花括号
    if start < 0 or end <= start:  # 无合法 JSON 块
        return None, ""
    try:
        obj = json.loads(text[start : end + 1])  # 切片解析
    except json.JSONDecodeError:
        return None, ""
    if not isinstance(obj, dict):  # 必须是对象
        return None, ""
    key = (obj.get("intentType") or obj.get("intent_type") or "").strip().upper()
    data = (obj.get("data") or obj.get("keyword") or "").strip()
    try:
        return IntentKind(key), data  # 字符串转枚举，非法则 ValueError
    except ValueError:
        return None, data


def _build_intent_context(
    user_id: str,
    user_text: str,
    *,
    from_product: bool = False,
    consult_card: dict | None = None,
    message_card: dict | None = None,
) -> str:
    """
    构建供 LLM 意图分类使用的「当前上下文」文本块。

    参数:
        user_id: 用户 ID（模板占位用）
        user_text: 用户输入
        from_product: 是否商品页通道
        consult_card: 咨询卡片
        message_card: 消息卡片

    返回:
        多行上下文字符串
    """
    consult = normalize_consult_card(consult_card) or normalize_consult_card(message_card)
    lines = [
        "=== 当前上下文 ===",
        f"商品详情通道(fromProduct)：{'是' if from_product else '否'}",
        f"消息含商品卡片：{'是' if normalize_consult_card(message_card) else '否'}",
    ]
    if consult:
        lines.append(
            f"咨询中商品：{consult.get('productName')}（ID={consult.get('productId')}）"
        )
    else:
        lines.append("咨询中商品：无")
    if extract_order_id(user_text or ""):
        lines.append("用户消息含订单号样式文本：是")
    return "\n".join(lines)


async def classify_intent_by_llm(
    user_id: str,
    user_text: str,
    *,
    from_product: bool = False,
    consult_card: dict | None = None,
    message_card: dict | None = None,
) -> tuple[IntentKind | None, str]:
    """
    调用轻量 LLM 进行意图分类。

    参数:
        user_id: 用户 ID
        user_text: 用户输入
        from_product: 是否商品页
        consult_card: 咨询卡片
        message_card: 消息卡片

    返回:
        (意图, data)；失败 (None, "")
    """
    template = await load_user_intent_classifier_prompt()  # 从 DB/文件加载模板
    if not template.strip():  # 未配置模板则跳过 LLM
        return None, ""

    context = _build_intent_context(
        user_id,
        user_text,
        from_product=from_product,
        consult_card=consult_card,
        message_card=message_card,
    )
    try:
        # 用户文本 XML 边界隔离（防 prompt 注入：用户输入视为数据而非指令）
        isolated = f"\n<user_text>\n{user_text}\n</user_text>\n"
        base = template % (user_id, isolated)  # 老式 % 格式化，类似 String.format
    except TypeError:  # 占位符数量不匹配则拼接
        base = f"{template}\n\n用户ID：{user_id}\n用户问题：\n<user_text>\n{user_text}\n</user_text>\n"

    prompt = f"{base}\n\n{context}"

    try:
        llm = create_memory_llm()  # 创建 LLM 实例
        response = await llm.ainvoke(  # 异步调用，类似 CompletableFuture
            [
                SystemMessage(
                    content=(
                        "你是电商客服意图分类器。"
                        "只输出一行 JSON：{\"intentType\":\"类型\",\"data\":\"订单号或搜索关键词或空\"}。"
                        "禁止解释、禁止 markdown。"
                    )
                ),
                HumanMessage(content=prompt),
            ]
        )
        content = response.content if isinstance(response.content, str) else str(response.content or "")
        intent, data = _parse_intent_json(content)
        if intent is None:
            logger.warning("intent_llm_parse_failed", raw=content[:200])  # 只打前 200 字
        return intent, data
    except Exception as e:
        logger.warning("intent_llm_failed", error=str(e))
        return None, ""


async def resolve_intent(
    user_id: str,
    user_text: str,
    *,
    from_product: bool = False,
    consult_card: dict | None = None,
    message_card: dict | None = None,
) -> tuple[IntentKind, str, str]:
    """
    意图解析总入口：按优先级合并结构、高置信规则、LLM、普通规则。

    参数:
        user_id: 用户 ID
        user_text: 用户输入
        from_product: 是否商品页
        consult_card: 咨询卡片
        message_card: 消息卡片

    返回:
        (最终意图, 来源 source, 附带 data)
        source 取值：structural / rule_priority / llm / rule / default
    """
    # 1. 结构型（商品咨询等）
    structural = _structural_intent(
        user_text,
        from_product=from_product,
        consult_card=consult_card,
        message_card=message_card,
    )
    if structural is not None:
        INTENT_TOTAL.labels(intent=structural.value, source="structural").inc()  # 指标 +1
        return structural, "structural", ""

    # 2. 高置信规则（查单/查物流）
    hi_intent, hi_data = classify_high_confidence_intent(user_text)
    if hi_intent is not None:
        INTENT_TOTAL.labels(intent=hi_intent.value, source="rule_priority").inc()
        return hi_intent, "rule_priority", hi_data

    settings = get_settings()
    intent_data = ""
    # 3. LLM 分类（配置开启时）
    if settings.intent_use_llm:
        llm_intent, intent_data = await classify_intent_by_llm(
            user_id,
            user_text,
            from_product=from_product,
            consult_card=consult_card,
            message_card=message_card,
        )
        if llm_intent is not None:
            INTENT_TOTAL.labels(intent=llm_intent.value, source="llm").inc()
            return llm_intent, "llm", intent_data

    # 4. 普通规则兜底
    if settings.intent_rule_fallback:
        ruled = classify_intent_by_rules(
            user_text,
            from_product=from_product,
            consult_card=consult_card,
            message_card=message_card,
        )
        if ruled is not None:
            INTENT_TOTAL.labels(intent=ruled.value, source="rule").inc()
            return ruled, "rule", ""

    # 5. 默认闲聊
    INTENT_TOTAL.labels(intent=IntentKind.CHAT.value, source="default").inc()
    return IntentKind.CHAT, "default", ""
