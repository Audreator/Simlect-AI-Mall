"""会话记忆与上下文测试。"""

from app.memory.context_builder import (
    build_context_block,
    estimate_turn_tokens,
    is_complete_turn_for_context,
    select_working_turns,
)
from app.memory.models import SessionMemory
from app.memory.token_estimator import estimate_text_tokens
from app.services.product_service import (
    _products_match_consult_category,
    derive_search_keyword,
    format_search_tool_message,
    is_similar_or_recommend_request,
    is_vague_search_keyword,
)

def test_estimate_chinese_tokens():
    assert estimate_text_tokens("你好") == 4
    assert estimate_text_tokens("hello") >= 1

def test_select_working_turns_respects_boundary():
    turns = [
        {"message_id": 1, "user_message": "a", "assistant_message": "b"},
        {"message_id": 2, "user_message": "c", "assistant_message": "d"},
        {"message_id": 3, "user_message": "e", "assistant_message": "f"},
    ]
    selected, oldest = select_working_turns(turns, after_message_id=1, token_budget=100_000)
    assert len(selected) == 2
    assert oldest == 2
    assert all(int(t["message_id"]) > 1 for t in selected)

def test_select_working_turns_drops_oldest_pairs_when_over_budget():
    turns = [
        {
            "message_id": 2,
            "user_message": "旧问题",
            "assistant_message": "旧回答" * 200,
            "assistant_for_history": "旧回答" * 200,
        },
        {
            "message_id": 3,
            "user_message": "新问题",
            "assistant_message": "新回答",
            "assistant_for_history": "新回答",
        },
    ]
    budget = estimate_turn_tokens(turns[1]) + 10
    selected, oldest = select_working_turns(turns, after_message_id=1, token_budget=budget)
    assert len(selected) == 1
    assert selected[0]["message_id"] == 3
    assert oldest == 3

def test_select_working_turns_skips_incomplete_pairs():
    turns = [
        {
            "message_id": 2,
            "user_message": "搜商品",
            "assistant_message": '[{"productId":"1"}]',
            "assistant_for_history": None,
        },
        {
            "message_id": 3,
            "user_message": "你好",
            "assistant_message": "您好，有什么可以帮您？",
            "assistant_for_history": "您好，有什么可以帮您？",
        },
    ]
    assert not is_complete_turn_for_context(turns[0])
    assert is_complete_turn_for_context(turns[1])
    selected, oldest = select_working_turns(turns, after_message_id=1, token_budget=100_000)
    assert len(selected) == 1
    assert selected[0]["message_id"] == 3

def test_select_working_turns_skips_oversized_pair_instead_of_truncating():
    huge = "超长回复" * 8000
    turns = [
        {
            "message_id": 2,
            "user_message": "问题",
            "assistant_message": huge,
            "assistant_for_history": huge,
        }
    ]
    selected, oldest = select_working_turns(turns, after_message_id=1, token_budget=100)
    assert selected == []
    assert oldest is None

def test_build_context_block():
    mem = SessionMemory(user_id="u1")
    mem.summary["narrative"] = "用户想买零食"
    mem.summary["facts"]["goal"] = "买礼物"
    mem.state["consultProduct"] = {"productName": "旺旺雪饼", "minPrice": 12.0}
    block = build_context_block(mem)
    assert "旺旺雪饼" in block
    assert "买礼物" in block

def test_build_context_block_hides_last_search_names():
    mem = SessionMemory(user_id="u1")
    mem.state["lastToolResults"] = {
        "searchedProductNames": ["旺旺雪饼", "项链"],
        "searchedProducts": ["1", "2"],
    }
    block = build_context_block(mem)
    assert "旺旺雪饼" not in block
    assert "SEARCH_PRODUCTS" in block

def test_is_vague_similar_intent():
    assert is_vague_search_keyword("有没有类似的")

def test_is_similar_or_recommend_request():
    assert is_similar_or_recommend_request("有什么类似的推荐吗")
    assert is_similar_or_recommend_request("有没有同款")
    assert not is_similar_or_recommend_request("你是谁")
    assert not is_similar_or_recommend_request("怎么查看会员等级")

def test_derive_search_keyword_from_consult():
    consult = {"productName": "旺旺 雪饼 厚烧海苔 原味 385g 零食"}
    assert is_vague_search_keyword("有什么类似的吗")
    kw = derive_search_keyword("有什么类似的吗", consult)
    assert "旺旺" in kw or "雪饼" in kw

def test_format_search_tool_message_alternative():
    consult = {"productName": "酷态科10号超级车充"}
    products = [{"product_name": "项链"}, {"product_name": "可乐"}]
    msg = format_search_tool_message("有没有类似的", consult, products, "hot_sale")
    assert "暂未找到" in msg
    assert "另荐热销" in msg
    assert "下方卡片" in msg
    assert "项链" not in msg

def test_format_search_tool_message_keyword_hot_sale_fallback():
    products = [{"product_name": "公仔"}, {"product_name": "点卡"}]
    msg = format_search_tool_message("我要吃零食", None, products, "hot_sale")
    assert "暂未找到" in msg
    assert "零食" in msg
    assert "另荐热销" in msg
    assert "找到 2 个商品" not in msg
    assert "公仔" not in msg

def test_format_search_tool_message_no_product_names_in_hint():
    consult = {"productName": "雅马哈FG800"}
    products = [{"product_name": "项链"}]
    msg = format_search_tool_message("有没有类似的推荐", consult, products, "category")
    assert "项链" not in msg
    assert "下方卡片" in msg

def test_products_match_consult_category():
    consult = {"categoryId": "10"}
    products = [{"category_id": "10"}, {"category_id": "20"}]
    assert _products_match_consult_category(products, consult)
    assert not _products_match_consult_category([{"category_id": "20"}], consult)
