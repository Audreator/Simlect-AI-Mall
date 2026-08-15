from app.utils.biz_payload import (
    build_product_search_message,
    compact_product_search_intro,
    extract_act_token_id,
    extract_act_tokens,
    parse_product_search_message,
)

def test_strip_embedded_product_json():
    from app.utils.biz_payload import strip_embedded_product_json

    intro = "为你推荐以下热销商品，请查看下方卡片挑选你感兴趣的："
    blob = (
        '{"type":"PRODUCT_SEARCH_RESULT","products":[{"productId":"AI_data:prod_001",'
        '"name":"智能降噪蓝牙耳机Pro","price":299}]}'
    )
    cleaned = strip_embedded_product_json(f"{intro}{blob}")
    assert cleaned == "为你推荐以下热销商品，请查看下方卡片挑选你感兴趣的"
    assert "PRODUCT_SEARCH_RESULT" not in cleaned
    assert "prod_001" not in cleaned

def test_is_order_cards_json():
    from app.utils.biz_payload import is_order_cards_json, is_product_cards_json

    orders = '[{"orderId":"20260630022030311uxMu1jHu3YIKRzZ","orderStatus":1,"orderItemList":[]}]'
    products = '[{"productId":"1","productName":"可乐"}]'
    assert is_order_cards_json(orders)
    assert not is_product_cards_json(orders)
    assert is_product_cards_json(products)
    assert not is_order_cards_json(products)

def test_should_not_force_product_cards_on_order_query():
    from app.harness.guardrails.product_text_guard import should_force_product_cards

    orders = '[{"orderId":"1","orderStatus":1,"orderItemList":[]}]'
    text = "你共有 13 笔订单，最近 5 笔的订单号如下"
    assert not should_force_product_cards(
        text, text, None, None, orders, tools_called=["QUERY_ORDERS"]
    )

def test_build_product_search_message_keeps_intro():
    cards = '[{"productId":"1","productName":"可乐"}]'
    intro = "暂未找到类似商品，为您另外推荐："
    raw = build_product_search_message(intro, cards)
    parsed_intro, products = parse_product_search_message(raw)
    assert parsed_intro == intro
    assert len(products or []) == 1

def test_compact_product_search_intro_strips_product_lines():
    hint = (
        "【类似商品】暂未找到与「酷态科10号超级车充」类似或同款的商品。\n"
        "【另荐热销】已为您另外推荐热销商品（非同款，请查看下方卡片）。"
    )
    llm = (
        "抱歉，没有找到和酷态科车充同类型的其他车载充电器产品哦～\n"
        "🍪 旺旺雪饼厚烧海苔 385g — 12元，经典零食\n"
        "🧸 名创优品小猪B-BO趴姿公仔 — 33.9元\n"
        "要直接下单吗？"
    )
    intro = compact_product_search_intro(llm, hint)
    assert "暂未找到" in intro
    assert "另荐热销" in intro or "另外推荐" in intro
    assert "旺旺雪饼" not in intro
    assert "12元" not in intro

def test_compact_prefers_miss_hint_over_llm_found_claim():
    hint = (
        "【搜索结果】暂未找到与「我要吃零食」相关的商品。\n"
        "【另荐热销】已为您另外推荐热销商品，请查看下方卡片。"
    )
    llm = "为你找到以下零食，请查看下方卡片。"
    intro = compact_product_search_intro(llm, hint)
    assert "暂未找到" in intro
    assert "另荐热销" in intro or "另外推荐" in intro
    assert "为你找到以下零食" not in intro

def test_compact_product_search_intro_keeps_full_llm_when_present():
    llm = "这款吉他音色均衡，适合初学者弹唱。\n请查看下方推荐卡片。"
    hint = "【搜索结果】找到 3 个商品（请查看下方卡片）。"
    intro = compact_product_search_intro(llm, hint)
    assert "这款吉他音色均衡" in intro
    assert "请查看下方推荐卡片" in intro

def test_build_product_search_message_dedupes_products():
    cards = (
        '[{"productId":"1","productName":"A"},{"productId":"1","productName":"A"},'
        '{"productId":"2","productName":"B"}]'
    )
    raw = build_product_search_message("请看下方推荐", cards)
    _, products = parse_product_search_message(raw)
    assert len(products or []) == 2

def test_collect_act_token_ids_prefers_tool_message():
    from app.utils.biz_payload import collect_act_token_ids

    class _ToolMsg:
        def __init__(self, content: str):
            self.content = content
            self.tool_call_id = "tc1"

    tool_token = "act_" + "b" * 32
    llm_token = "act_" + "c" * 32
    messages = [_ToolMsg(f"请附带【{tool_token}】")]
    ids = collect_act_token_ids(f"正文【{llm_token}】", messages)
    assert ids[0] == tool_token

def test_build_action_confirm_unavailable_payload():
    from app.utils.biz_payload import build_action_confirm_unavailable_payload
    import json

    token = "act_" + "d" * 32
    raw, _ = build_action_confirm_unavailable_payload(token, "已生成提案", reason="wrong_user")
    card = json.loads(raw)
    assert card["type"] == "ACTION_CONFIRM"
    assert card["status"] == 3
    assert card["token"] == token
    assert "无权" in card["summary"]

def test_extract_review_helpers():
    from app.graph.nodes import _extract_review_content, _extract_review_star

    text = "订单 20260612204304352OBbW6OiMj2BUUhY 给个5星 物流很快包装完好"
    assert _extract_review_star(text) == 5
    content = _extract_review_content(text, "20260612204304352OBbW6OiMj2BUUhY")
    assert content and "物流很快" in content

def test_extract_real_act_token():
    token = "act_" + "a" * 32
    text = f"请在下方确认【{token}】"
    assert extract_act_token_id(text) == token
    assert extract_act_tokens(text) == [f"【{token}】"]

def test_reject_fake_act_token():
    assert extract_act_token_id("【act_propose_product_review】") is None
    assert extract_act_tokens("【act_propose_product_review】") == []
