from app.services.product_search_query import (
    filter_products_by_query_relevance,
    match_terms_for_query,
    normalize_product_search_query,
)
from app.services.product_service import derive_search_keyword, format_search_tool_message

def test_normalize_snack_utterance():
    assert normalize_product_search_query("我要吃零食") == "零食"
    assert derive_search_keyword("我要吃零食", None) == "零食"

def test_filter_drops_unrelated_hybrid_hits():
    products = [
        {"product_name": "名创优品小猪B-BO趴姿公仔"},
        {"product_name": "任天堂港服NS充值卡"},
        {"product_name": "旺旺雪饼厚烧海苔零食大礼包"},
    ]
    kept = filter_products_by_query_relevance(products, "零食")
    assert len(kept) == 1
    assert "雪饼" in kept[0]["product_name"]

def test_filter_all_miss_means_empty():
    products = [
        {"product_name": "名创优品小猪B-BO趴姿公仔"},
        {"product_name": "WPS Office超级会员"},
    ]
    assert filter_products_by_query_relevance(products, "我要吃零食") == []
    assert "零食" in match_terms_for_query("我要吃零食")

def test_hot_sale_fallback_message_not_found():
    products = [{"product_name": "公仔"}, {"product_name": "点卡"}]
    msg = format_search_tool_message("我要吃零食", None, products, "hot_sale")
    assert "暂未找到" in msg
    assert "零食" in msg
    assert "另荐热销" in msg
    assert "找到 2 个商品" not in msg
