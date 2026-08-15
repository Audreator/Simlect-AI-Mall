from app.utils.biz_payload import (
    compact_product_search_intro,
    is_product_cards_json,
    strip_embedded_product_json,
)

def test_strip_bare_product_id_array():
    raw = (
        '为你推荐：\n'
        '[{"productId":"147099495496439"},{"productId":"326818100160840"}]\n'
        "请查看下方卡片"
    )
    cleaned = strip_embedded_product_json(raw)
    assert "productId" not in cleaned
    assert "为你推荐" in cleaned
    assert "请查看下方卡片" in cleaned

def test_id_only_array_is_not_product_cards():
    assert not is_product_cards_json('[{"productId":"1"}]')
    assert is_product_cards_json('[{"productId":"1","productName":"零食"}]')

def test_compact_intro_strips_id_array():
    raw = '看看这些\n[{"productId":"1"},{"productId":"2"}]'
    intro = compact_product_search_intro(raw, None)
    assert "productId" not in intro
