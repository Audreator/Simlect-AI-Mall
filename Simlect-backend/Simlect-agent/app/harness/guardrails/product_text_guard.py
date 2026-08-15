"""
================================================================================
文件：harness/guardrails/product_text_guard.py
角色：商品文案 / 商品卡片「一致性」护栏（输出侧配套）
================================================================================

【这个文件干什么】
判断助手回复文本里是否「像在介绍/罗列商品」，以及是否应该强制附带商品卡片 JSON。
典型问题：模型口头说「下方有推荐商品」，但 assistant_cards 为空 → 前端空白；
或咨询态下误塞搜索卡片。本模块用启发式规则检测并给出 should_force_product_cards。

【和 output_guard 的区别】
| output_guard              | product_text_guard（本文件）        |
|---------------------------|-------------------------------------|
| 防虚假退款/假确认卡/emoji | 防「说到商品却没卡片」类体验问题   |
| 偏安全与话术真实性         | 偏商品 UI 契约（文案 ↔ 卡片）      |

【谁在调用】
一般在 agent_runtime.finalize_agent_response 收束阶段：
若 should_force_product_cards(...) 为 True，则补建/回填商品卡片。

【类比 Java】
类似一个 ProductCardConsistencyAdvice：在 Response 写出前检查
「正文提到商品 ⇒ 必须带 ProductCardDTO 列表」。

【关联】
biz_payload.parse_product_search_message / is_order_cards_json
output_guard、agent_runtime
================================================================================
"""

from __future__ import annotations  # 允许类型注解前向引用

import json  # 解析 assistant_cards JSON 数组（类似 Jackson ObjectMapper）
import re  # 正则匹配价格行、列表行、括号内后缀

from app.utils.biz_payload import is_order_cards_json, parse_product_search_message  # 订单卡判断 + 搜索结果解析

# 匹配价格片段：¥199 / ￥1,299.00 / 199元 等（用于「这像不像商品介绍」）
_PRODUCT_PRICE = re.compile(r"(?:¥|[￥])[\d,]+(?:\.\d+)?|\d[\d,]*(?:\.\d+)?\s*元")

# 匹配「商品名 — 价格」列表行，如：蓝牙耳机 — 99元
_LISTING_LINE = re.compile(r".{2,80}[—\-–]\s*\d[\d,]*(?:\.\d+)?\s*元")

# 商品名最短长度：太短（如「手机」两字）误报率高，忽略
_MIN_NAME_LEN = 4


def is_product_search_result(raw: str | None) -> bool:
    """
    【功能】判断一段助手原文是否「已经是」结构化商品搜索结果消息。

    【返回】True = parse 出了 products 列表，说明卡片/结构化结果已在文本侧存在，
    一般不必再 force 补卡。

    【参数】raw：助手完整回复字符串，可为 None。
    """
    _, products = parse_product_search_message(raw)  # 拆出文案与商品列表；解析失败则 products=None
    return products is not None  # 有列表即视为搜索结果形态


def collect_known_product_names(
    tool_biz: dict | None,  # 工具回填的业务 dict，可能含 productNames
    consult_card: dict | None,  # 当前咨询中的商品卡片
    assistant_cards: str | None,  # 前端卡片 JSON 字符串（常为数组）
) -> list[str]:
    """
    【功能】从工具结果、咨询卡、assistant_cards 收集「已知商品名」去重列表。

    【用途】后面用 name_mentioned_in_text 看正文是否点名这些商品。
    【类比】从多个 DTO 抽 productName 放进 Set，再转 List。
    """
    names: list[str] = []  # 有序列表，便于调试查看收集顺序
    seen: set[str] = set()  # 去重集合（类似 HashSet）

    def _add(name: str | None) -> None:
        """内部：合法且未见过的名字才加入 names。"""
        n = (name or "").strip()  # None → ""，并去空白
        if len(n) < _MIN_NAME_LEN or n in seen:  # 过短或重复 → 丢弃
            return
        seen.add(n)  # 记入去重集
        names.append(n)  # 追加到结果列表

    for n in (tool_biz or {}).get("productNames") or []:  # 工具显式给出的名称列表
        _add(str(n))  # 统一转 str，防止数字等类型
    if consult_card:  # 咨询态卡片上的商品名（兼容 camel / snake）
        _add(consult_card.get("productName") or consult_card.get("product_name"))
    if assistant_cards and assistant_cards.strip().startswith("["):  # 看起来像 JSON 数组
        try:
            parsed = json.loads(assistant_cards)  # 反序列化
            if isinstance(parsed, list):  # 必须是 list 才遍历
                for item in parsed:
                    if isinstance(item, dict):  # 每个元素应是商品对象 Map
                        _add(item.get("productName") or item.get("product_name"))
        except json.JSONDecodeError:  # 非法 JSON → 忽略，不当崩溃
            pass
    return names  # 返回去重后的商品名列表


def name_mentioned_in_text(name: str, text: str) -> bool:
    """
    【功能】判断商品名（或其核心片段/分词）是否出现在正文中。

    【策略】
    1) 全名直接包含
    2) 去掉括号/书名号后缀后再包含（如「xxx（新款）」→「xxx」）
    3) 按空格/斜杠等切开，前几个长度≥3 的 token 任一命中即可

    【为何放宽】模型常写简称或去掉规格后缀，严格全等会漏检。
    """
    name = (name or "").strip()  # 规范化商品名
    if len(name) < _MIN_NAME_LEN:  # 过短不做匹配，降误报
        return False
    if name in text:  # 全名命中
        return True
    # 去掉 （）【】[] 及其内部内容，得到「核心名」
    core = re.sub(r"[（(【\[].*?[）)】\]]", "", name).strip()
    if len(core) >= _MIN_NAME_LEN and core in text:  # 核心名命中
        return True
    # 再拆成 token：空格、/、|、·、横线等做分隔；只保留长度≥3
    tokens = [tok for tok in re.split(r"[\s/|·\-—]+", core) if len(tok) >= 3]
    return any(tok in text for tok in tokens[:6])  # 最多看前 6 个 token，防过长名拖慢


def text_contains_product_info(text: str | None, known_names: list[str] | None = None) -> bool:
    """
    【功能】启发式：这段文本是否「在讲商品/报价/清单」。

    【命中任一即 True】
    - 至少 2 行带价格
    - 存在「名称—价格」列表行
    - 正文提到 known_names 里某个商品
    - 有价格行且另有较长行（像在描述商品）

    【排除】空串、或以 { 开头（多半是纯 JSON，不当作文案介绍）
    """

    t = (text or "").strip()  # None 当空串
    if not t or t.startswith("{"):  # 空或整段 JSON → 不视为「商品介绍文案」
        return False

    # 按换行拆成非空行
    lines = [ln.strip() for ln in re.split(r"[\n\r]+", t) if ln.strip()]
    # 筛出含价格的行
    price_lines = [ln for ln in lines if _PRODUCT_PRICE.search(ln)]
    if len(price_lines) >= 2:  # 多行报价 → 很像商品列表
        return True
    if any(_LISTING_LINE.search(ln) for ln in lines):  # 「名—价」格式
        return True
    for name in known_names or []:  # 点名已知商品
        if name_mentioned_in_text(name, t):
            return True
    # 有价格 + 另有较长描述行 → 也像在推商品
    if price_lines and any(len(ln) > 10 for ln in lines):
        return True
    return False  # 以上都不像


def build_consult_product_cards_json(consult_card: dict | None) -> str | None:
    """
    【功能】把咨询中的单品卡片编成前端可用的 JSON 数组字符串。

    【返回】None = 没有可用 productId；否则 '[{productId, productName, cover, minPrice}]'
    【用途】咨询态需要展示当前商品卡时的兜底构造。
    """

    if not consult_card or not consult_card.get("productId"):  # 缺 ID 无法建卡
        return None
    card = {
        "productId": str(consult_card["productId"]),  # 统一成字符串，避免前端类型问题
        "productName": consult_card.get("productName") or consult_card.get("product_name") or "",
        "cover": consult_card.get("cover"),  # 封面图 URL，可空
        "minPrice": consult_card.get("minPrice") or consult_card.get("min_price"),  # 兼容两种字段名
    }
    return json.dumps([card], ensure_ascii=False)  # ensure_ascii=False 保留中文，不转 \\uXXXX


def text_promises_product_cards(text: str | None) -> bool:
    """
    【功能】检测文案是否「口头承诺下方有卡片/推荐列表」。

    【规则】同时出现方位词（下方/下面/以下）+ 卡片类词（卡片/推荐商品…）
    【用途】可与缺卡检测配合，抓「说了有卡却没下发」的体验 bug。
    """

    t = (text or "").strip()
    if not t:
        return False
    has_place_ref = any(k in t for k in ("下方", "下面", "以下"))  # 指向 UI 下方
    has_card_word = any(k in t for k in ("卡片", "推荐商品", "推荐结果", "推荐列表"))  # 承诺卡片/列表
    return has_place_ref and has_card_word  # 两个条件都要有


def should_force_product_cards(
    full_text: str | None,  # 流式拼接后的完整助手文案
    assistant: str | None,  # 另一路助手文本（可能含结构化搜索消息）
    tool_biz: dict | None,  # 工具业务数据
    consult_card: dict | None,  # 咨询商品卡
    assistant_cards: str | None,  # 已有卡片 JSON
    *,
    is_consult_turn: bool = False,  # True=本轮是「围绕当前商品咨询」，不要强行塞搜索卡
    tools_called: list[str] | None = None,  # 本轮已调工具名列表
) -> bool:
    """
    【核心决策】是否应在 finalize 时强制补商品卡片。

    【返回 False 的情况（不要强塞）】
    - 咨询回合：用户在问当前商品细节，不是要推荐列表
    - 已调订单/物流/券/写提案/详情等非「搜索推荐」工具
    - 文案像售后/订单话术
    - 已是订单卡片 JSON
    - assistant 已是商品搜索结构化结果

    【返回 True】
    正文像在介绍商品（text_contains_product_info），且上面排除项都不命中
    → 调用方应补 cards，避免「只说话没有卡」。
    """

    if is_consult_turn:  # 咨询态：强制搜索卡会干扰「单品咨询」UI
        return False
    called = tools_called or []  # None → 空列表
    # 若本轮已经走了订单/售后/详情等工具，商品推荐卡不是主展示，不 force
    if any(
        t in called
        for t in (
            "QUERY_ORDERS",
            "QUERY_LOGISTICS",
            "QUERY_COMMENT",
            "QUERY_USER_COUPONS",
            "PROPOSE_REFUND",
            "PROPOSE_CONFIRM_RECEIPT",
            "PROPOSE_PRODUCT_REVIEW",
            "PROPOSE_RECOMMENT",
            "GET_PRODUCT_DETAIL",
        )
    ):
        return False
    # 延迟 import，避免与 biz_payload 潜在循环依赖（类比 Spring @Lazy）
    from app.utils.biz_payload import looks_like_aftersales_or_order_text

    # 售后/订单口吻的文案 → 不应贴商品推荐卡
    if looks_like_aftersales_or_order_text(full_text) or looks_like_aftersales_or_order_text(assistant):
        return False
    if is_order_cards_json(assistant_cards):  # 已经是订单卡 → 不要覆盖成商品卡
        return False
    if is_product_search_result(assistant):  # 已是搜索结果结构 → 无需再 force
        return False
    known = collect_known_product_names(tool_biz, consult_card, assistant_cards)  # 已知商品名
    return text_contains_product_info(full_text, known)  # 文案像推商品 → True，建议补卡
