import json
import re
from decimal import Decimal
from typing import Any

ASSISTANT_MESSAGE_MAX_LEN = 16000
MAX_PRODUCT_SEARCH_INTRO_LEN = 12000
MAX_ORDER_CARDS = 30
MAX_PRODUCT_CARDS = 20
MAX_ORDER_ITEMS = 5
ACTION_CONFIRM_HINT = "请核对以下信息，确认后将立即执行。"

ACTION_LABELS = {
    "REFUND": ("退款", "确认退款", "退款将原路返回，提交后无法撤销"),
    "CONFIRM_RECEIPT": ("确认收货", "确认收货", "确认后将无法发起退款"),
    "PRODUCT_REVIEW": ("提交评价", "确认提交评价", "评价提交后不可修改"),
    "RECOMMENT": ("提交追评", "确认提交追评", "追评提交后不可修改"),
}

from app.constants import ORDER_STATUS_NAMES

def _json_default(obj: Any) -> Any:

    if isinstance(obj, Decimal):
        return float(obj)
    raise TypeError(f"Object of type {type(obj)} is not JSON serializable")

def _json_dumps(data: Any) -> str:

    return json.dumps(data, ensure_ascii=False, default=_json_default)

def _to_number(value: Any) -> Any:

    if value is None:
        return None
    if isinstance(value, Decimal):
        return float(value)
    return value

def trim_assistant(text: str | None) -> str | None:

    if text is None:
        return None
    return text[:ASSISTANT_MESSAGE_MAX_LEN] if len(text) > ASSISTANT_MESSAGE_MAX_LEN else text

def first_cover(cover: str | None) -> str | None:

    if not cover:
        return cover
    return cover.split(",")[0] if "," in cover else cover

def _dedupe_product_cards(cards: list[dict]) -> list[dict]:

    seen: set[str] = set()
    out: list[dict] = []
    for card in cards:
        pid = card.get("productId")
        if not pid or pid in seen:
            continue
        seen.add(str(pid))
        out.append(card)
    return out

def build_product_payload(products: list[dict]) -> tuple[str, str | None]:

    if not products:
        return "[]", None
    limited = products[:MAX_PRODUCT_CARDS]
    cards, ids = [], []
    for p in limited:
        pid = p.get("product_id") or p.get("productId")
        if not pid:
            continue
        pid = str(pid)
        if pid in ids:
            continue
        ids.append(pid)
        cards.append({
            "productId": pid,
            "productName": p.get("product_name") or p.get("productName", ""),
            "cover": first_cover(p.get("cover")),
            "minPrice": _to_number(p.get("min_price") or p.get("minPrice")),
        })
    cards = _dedupe_product_cards(cards)
    return _json_dumps(cards), _json_dumps(ids) if ids else None

PRODUCT_SEARCH_RESULT_TYPE = "PRODUCT_SEARCH_RESULT"

_PRICE_IN_LINE = re.compile(r"\d+\.?\d*\s*元")
_EMOJI_BULLET_LINE = re.compile(r"^[\s]*[\U0001F300-\U0001FAFF🍪🧸💎🔥✨👇—\-•\*]")
_LISTING_LINE = re.compile(r".{2,80}[—\-–]\s*\d[\d,]*(?:\.\d+)?\s*元")

def _bracket_line_for_intro(line: str) -> str:

    line = line.strip()
    if "：" not in line:
        return line
    head, tail = line.split("：", 1)
    tail = tail.strip()
    if "、" in tail or len(tail) > 24:
        return f"{head}，请查看下方商品卡片。"
    return line

def intro_from_search_tool_hint(hint: str | None) -> str:

    if not hint:
        return ""
    lines = [ln.strip() for ln in hint.splitlines() if ln.strip()]
    bracket_lines = [_bracket_line_for_intro(ln) for ln in lines if ln.startswith("【")]
    if not bracket_lines:
        return lines[0][:MAX_PRODUCT_SEARCH_INTRO_LEN] if lines else ""

    if any("【类似商品】" in ln for ln in bracket_lines):
        similar = next((ln for ln in bracket_lines if "【类似商品】" in ln), bracket_lines[0])
        alt = next((ln for ln in bracket_lines if "【另荐热销】" in ln or "【浏览推荐】" in ln), "")
        intro = similar if not alt else f"{similar} {alt}"
        return intro[:MAX_PRODUCT_SEARCH_INTRO_LEN]

    # Search miss + alternative recommend (hot-sale / browse backfill).
    miss = next((ln for ln in bracket_lines if "暂未找到" in ln or "未找到" in ln), "")
    alt = next(
        (ln for ln in bracket_lines if "【另荐热销】" in ln or "【浏览推荐】" in ln or "【热销推荐】" in ln),
        "",
    )
    if miss and alt and miss != alt:
        return f"{miss} {alt}"[:MAX_PRODUCT_SEARCH_INTRO_LEN]
    return " ".join(bracket_lines[:2])[:MAX_PRODUCT_SEARCH_INTRO_LEN]

def _find_balanced_json_span(text: str, start: int) -> tuple[int, int] | None:

    if start >= len(text) or text[start] != "{":
        return None
    depth = 0
    in_string = False
    escape = False
    for i in range(start, len(text)):
        ch = text[i]
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
        elif ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return start, i + 1
    return None

def _find_balanced_json_array_span(text: str, start: int) -> tuple[int, int] | None:

    if start >= len(text) or text[start] != "[":
        return None
    depth = 0
    in_string = False
    escape = False
    for i in range(start, len(text)):
        ch = text[i]
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
        elif ch == "[":
            depth += 1
        elif ch == "]":
            depth -= 1
            if depth == 0:
                return start, i + 1
    return None

def _is_product_id_array(obj: object) -> bool:
    if not isinstance(obj, list) or not obj:
        return False
    for item in obj:
        if not isinstance(item, dict):
            return False
        if not (item.get("productId") or item.get("product_id")):
            return False
    return True

def strip_embedded_product_json(text: str | None) -> str:
    """Remove PRODUCT_SEARCH_RESULT blobs and bare product card JSON arrays from prose."""
    if not text:
        return ""
    s = text
    marker = f'"type":"{PRODUCT_SEARCH_RESULT_TYPE}"'
    marker_spaced = f'"type": "{PRODUCT_SEARCH_RESULT_TYPE}"'
    while True:
        idx = s.find(marker)
        if idx < 0:
            idx = s.find(marker_spaced)
        if idx < 0:
            break
        start = s.rfind("{", 0, idx)
        if start < 0:
            s = s.replace(marker, "").replace(marker_spaced, "")
            break
        span = _find_balanced_json_span(s, start)
        if not span:
            s = s[:start].rstrip()
            break
        a, b = span
        before = s[:a].rstrip()
        after = s[b:].lstrip()
        if before.endswith(":") or before.endswith("："):
            before = before[:-1].rstrip()
        s = f"{before}{after}".strip()

    # Strip bare [{"productId":...}, ...] the model often echoes.
    guard = 0
    while guard < 8:
        guard += 1
        start = s.find("[")
        if start < 0:
            break
        span = _find_balanced_json_array_span(s, start)
        if not span:
            break
        a, b = span
        blob = s[a:b]
        try:
            obj = json.loads(blob)
        except json.JSONDecodeError:
            # Skip past this '[' and continue scanning.
            s = s[:start] + s[start + 1 :]
            continue
        if not _is_product_id_array(obj):
            # Not a product array — leave remaining text as-is.
            break
        before = s[:a].rstrip()
        after = s[b:].lstrip()
        if before.endswith(":") or before.endswith("："):
            before = before[:-1].rstrip()
        s = f"{before}{after}".strip()
    return s.strip()

def _clean_llm_intro_body(llm_text: str | None) -> str:

    text = strip_embedded_product_json(llm_text)

    text = re.sub(r"【act_(?![a-f0-9]{32})[^】]*】", "", text, flags=re.I).strip()
    if not text:
        return ""
    kept: list[str] = []
    for line in re.split(r"[\n\r]+", text):
        line = line.strip()
        if not line:
            continue
        if _LISTING_LINE.search(line):
            continue
        if _EMOJI_BULLET_LINE.match(line) and _PRICE_IN_LINE.search(line):
            continue
        kept.append(line)
    return "\n".join(kept).strip()

def compact_product_search_intro(llm_text: str | None, tool_hint: str | None = None) -> str:
    """Prefer tool miss/alt copy so LLM cannot rebrand fallbacks as「搜索结果找到」."""
    hint = tool_hint or ""
    hint_intro = intro_from_search_tool_hint(tool_hint)
    miss_or_alt = any(
        m in hint
        for m in ("暂未找到", "【另荐热销】", "【浏览推荐】", "【热销推荐】")
    )
    if miss_or_alt and hint_intro:
        return hint_intro[:MAX_PRODUCT_SEARCH_INTRO_LEN]

    llm_intro = _clean_llm_intro_body(llm_text)
    if llm_intro:
        return llm_intro[:MAX_PRODUCT_SEARCH_INTRO_LEN]
    return hint_intro[:MAX_PRODUCT_SEARCH_INTRO_LEN] if hint_intro else ""

def build_product_search_message(intro: str | None, cards_json: str) -> str:

    try:
        cards = json.loads(cards_json) if cards_json else []
    except json.JSONDecodeError:
        cards = []
    if not isinstance(cards, list):
        cards = []
    cards = _dedupe_product_cards(cards)[:MAX_PRODUCT_CARDS]
    text = strip_embedded_product_json(intro)
    text = (text or "").strip()
    text = re.sub(r"【act_(?![a-f0-9]{32})[^】]*】", "", text, flags=re.I).strip()
    if len(text) > MAX_PRODUCT_SEARCH_INTRO_LEN:
        text = text[:MAX_PRODUCT_SEARCH_INTRO_LEN]
    payload: dict[str, Any] = {
        "type": PRODUCT_SEARCH_RESULT_TYPE,
        "intro": text or None,
        "products": cards,
    }
    result = _json_dumps(payload)

    if len(result) > ASSISTANT_MESSAGE_MAX_LEN:
        payload["intro"] = (text or "")[:200] or None
        result = _json_dumps(payload)
    return result

def parse_product_search_message(raw: str | None) -> tuple[str | None, list[dict] | None]:

    if not raw or not isinstance(raw, str):
        return None, None
    text = raw.strip()
    if not text.startswith("{"):
        return None, None
    try:
        obj = json.loads(text)
    except json.JSONDecodeError:
        return None, None
    if not isinstance(obj, dict) or obj.get("type") != PRODUCT_SEARCH_RESULT_TYPE:
        return None, None
    intro = obj.get("intro")
    products = obj.get("products")
    if not isinstance(products, list):
        return (str(intro).strip() if intro else None), []
    return (str(intro).strip() if intro else None), products

def is_order_cards_json(raw: str | None) -> bool:

    if not raw or not isinstance(raw, str):
        return False
    text = raw.strip()
    if not text.startswith("["):
        return False
    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        return False
    if not isinstance(parsed, list) or not parsed:
        return False
    first = parsed[0]
    if not isinstance(first, dict):
        return False
    return bool(first.get("orderId") or first.get("order_id"))

def is_product_cards_json(raw: str | None) -> bool:

    if not raw or not isinstance(raw, str):
        return False
    text = raw.strip()
    if not text.startswith("["):
        return False
    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        return False
    if not isinstance(parsed, list) or not parsed:
        return False
    first = parsed[0]
    if not isinstance(first, dict):
        return False
    if first.get("orderId") or first.get("order_id"):
        return False
    # Require a display name — id-only arrays are LLM noise, not cards.
    has_id = bool(first.get("productId") or first.get("product_id"))
    has_name = bool(first.get("productName") or first.get("product_name"))
    return has_id and has_name

def is_action_confirm_json(raw: str | None) -> bool:
    """True when text is a full ACTION_CONFIRM card JSON (valid or fabricated)."""
    if not raw or not isinstance(raw, str):
        return False
    text = raw.strip()
    if not text.startswith("{"):
        return False
    try:
        obj = json.loads(text)
    except json.JSONDecodeError:
        return False
    return isinstance(obj, dict) and obj.get("type") == "ACTION_CONFIRM"

_AFTERSALES_HINTS = (
    "退款",
    "退货",
    "退钱",
    "确认收货",
    "追评",
    "取消订单",
    "物流",
    "快递",
    "到哪了",
)

def looks_like_aftersales_or_order_text(text: str | None) -> bool:
    """Used to avoid hijacking aftersales turns into product-search backfill.

    Keep narrow — howto / 优惠券 / bare「评价」不应触发僵硬兜底文案。
    """
    t = (text or "").strip()
    if not t:
        return False
    from app.utils.order_ids import extract_order_id, extract_order_item_id

    if extract_order_item_id(t) or extract_order_id(t):
        return True
    if any(k in t for k in ("如何", "怎么", "怎样", "在哪", "哪里", "方法", "步骤")):
        return False
    return any(k in t for k in _AFTERSALES_HINTS)

def build_order_payload(orders: list[dict], items_map: dict[str, list[dict]]) -> tuple[str, str | None]:

    if not orders:
        return "[]", None
    limited = orders[:MAX_ORDER_CARDS]
    cards, ids = [], []
    for o in limited:
        oid = o.get("order_id") or o.get("orderId")
        if not oid:
            continue
        ids.append(str(oid))
        status = o.get("order_status") or o.get("orderStatus")
        cards.append({
            "orderId": str(oid),
            "orderStatus": status,
            "orderStatusName": ORDER_STATUS_NAMES.get(status, "订单"),
            "amount": _to_number(o.get("amount")),
            "payScene": o.get("pay_scene") or o.get("payScene"),
            "orderItemList": _order_item_cards(items_map.get(str(oid), [])),
        })
    return trim_assistant(_json_dumps(cards)), _json_dumps(ids) if ids else None

def _order_item_cards(items: list[dict]) -> list[dict]:

    result = []
    for item in items[:MAX_ORDER_ITEMS]:
        result.append({
            "orderItemId": item.get("order_item_id") or item.get("orderItemId"),
            "cover": first_cover(item.get("cover")),
            "productName": item.get("product_name") or item.get("productName"),
            "propertyInfo": item.get("property_info") or item.get("propertyInfo"),
        })
    return result

def build_action_confirm_payload(pending: dict, intro: str | None = None) -> tuple[str, str]:

    action_type = pending.get("actionType", "")
    label, confirm_text, risk_tip = ACTION_LABELS.get(action_type, (action_type, "确认", ""))
    params = json.loads(pending.get("paramsJson") or "{}")
    card: dict[str, Any] = {
        "type": "ACTION_CONFIRM",
        "token": pending.get("token"),
        "actionType": action_type,
        "label": label,
        "summary": pending.get("summary"),
        "confirmText": confirm_text,
        "riskTip": risk_tip,
        "intro": _sanitize_intro(intro),
        "details": _build_details(action_type, params, pending.get("summary")),
        "status": pending.get("status", 0),
    }
    if params.get("orderId"):
        card["orderId"] = params["orderId"]
    if params.get("payScene"):
        card["payScene"] = params["payScene"]
    if params.get("orderAmount") is not None:
        card["orderAmount"] = params["orderAmount"]
    items = params.get("orderItems") or []
    if items:
        card["items"] = items[:MAX_ORDER_ITEMS]
    assistant = trim_assistant(_json_dumps(card))
    biz_data = _json_dumps({"token": pending.get("token")})
    return assistant, biz_data

def _sanitize_intro(intro: str | None) -> str:

    if not intro:
        return ACTION_CONFIRM_HINT
    text = re.sub(r"【act_[^】]+】", "", intro)
    text = re.sub(r"【[^】]*(成功|失败)[^】]*】", "", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text[:80] if text and len(text) <= 80 else (text[:80] if text else ACTION_CONFIRM_HINT)

def _build_details(action_type: str, params: dict, summary: str | None) -> list[dict]:

    details = []
    if action_type == "REFUND":
        _add(details, "退款金额", f"{params.get('refundAmount')} 元" if params.get("refundAmount") else None)
        if not params.get("orderItems"):
            _add(details, "订单项", params.get("orderItemId"))
    elif action_type == "CONFIRM_RECEIPT":
        _add(details, "实付金额", f"{params.get('orderAmount')} 元" if params.get("orderAmount") else None)
        if not params.get("orderItems"):
            _add(details, "订单号", params.get("orderId"))
    elif action_type == "PRODUCT_REVIEW":
        star = params.get("star")
        _add(details, "评价星级", f"{'⭐' * star} {star} 星" if star else None)
        content = params.get("commentContent", "")
        _add(details, "评价内容", content[:60] + "…" if len(content) > 60 else content)
    elif action_type == "RECOMMENT":
        content = params.get("reCommentContent", "")
        _add(details, "追评内容", content[:60] + "…" if len(content) > 60 else content)
    if not details and summary:
        _add(details, "操作摘要", summary)
    return details

def _add(details: list, label: str, value: str | None) -> None:

    if value:
        details.append({"label": label, "value": value})

ACT_TOKEN_ID_PATTERN = re.compile(r"act_[a-f0-9]{32}", re.I)

def extract_act_token_id(text: str) -> str | None:

    if not text:
        return None
    m = ACT_TOKEN_ID_PATTERN.search(text)
    return m.group(0) if m else None

def extract_act_tokens(text: str) -> list[str]:

    token_id = extract_act_token_id(text)
    return [f"【{token_id}】"] if token_id else []

def collect_act_token_ids(full_text: str | None, messages: list | None) -> list[str]:

    seen: set[str] = set()
    ordered: list[str] = []
    for msg in reversed(messages or []):
        content = _tool_message_content(msg)
        if not content:
            continue
        tid = extract_act_token_id(content)
        if tid and tid not in seen:
            seen.add(tid)
            ordered.append(tid)
    tid = extract_act_token_id(full_text or "")
    if tid and tid not in seen:
        ordered.append(tid)
    return ordered

def _tool_message_content(msg) -> str | None:

    if getattr(msg, "tool_call_id", None) is None:
        return None
    content = getattr(msg, "content", "")
    return content if isinstance(content, str) else str(content or "")

ACTION_UNAVAILABLE_MESSAGES = {
    "not_found": "操作提案已过期或不存在，请重新发起。",
    "wrong_user": "无权操作该请求。",
}

def build_action_confirm_unavailable_payload(
    token: str,
    intro: str | None = None,
    *,
    reason: str = "not_found",
) -> tuple[str, str]:

    summary = ACTION_UNAVAILABLE_MESSAGES.get(reason, ACTION_UNAVAILABLE_MESSAGES["not_found"])
    card: dict[str, Any] = {
        "type": "ACTION_CONFIRM",
        "token": token,
        "actionType": "UNKNOWN",
        "label": "操作确认",
        "summary": summary,
        "confirmText": "确认",
        "riskTip": "",
        "intro": _sanitize_intro(intro),
        "status": 3,
        "details": [{"label": "提示", "value": summary}],
    }
    assistant = trim_assistant(_json_dumps(card))
    biz_data = _json_dumps({"token": token})
    return assistant, biz_data
