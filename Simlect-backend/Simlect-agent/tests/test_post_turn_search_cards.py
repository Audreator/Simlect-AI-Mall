"""post_turn：商品卡片记忆（lastSearchResults）与历史浓缩可读清单测试。"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.memory.models import SessionMemory
from app.memory.post_turn import (
    _cards_history_summary,
    _extract_search_result_cards,
    post_turn_service,
)


def test_extract_search_cards_prefers_card_json_order_and_price():
    cards_json = (
        '[{"productId":"p1","productName":"旺旺雪饼","minPrice":12.0},'
        '{"productId":"p2","productName":"乐事薯片","minPrice":6.5}]'
    )
    cards = _extract_search_result_cards(cards_json, None, None)
    assert [c["productId"] for c in cards] == ["p1", "p2"]
    assert cards[0]["productName"] == "旺旺雪饼"
    assert cards[1]["minPrice"] == 6.5


def test_extract_search_cards_prefers_assistant_cards_source():
    cards = _extract_search_result_cards(
        '[{"productId":"p1","productName":"卡片来源"}]',
        '[{"productId":"p2","productName":"正文来源"}]',
        None,
    )
    assert cards[0]["productId"] == "p1"


def test_extract_search_cards_fallback_from_tool_biz():
    cards = _extract_search_result_cards(
        None, "一些正文", {"productIds": ["p9"], "productNames": ["卫龙辣条"]}
    )
    assert cards == [{"productId": "p9", "productName": "卫龙辣条", "minPrice": None}]


def test_extract_search_cards_ignores_order_cards():
    order_cards = '[{"orderId":"o1","orderStatus":2,"orderItemList":[]}]'
    assert _extract_search_result_cards(order_cards, order_cards, None) == []


def test_cards_history_summary_uses_page_order():
    summary = _cards_history_summary([
        {"productId": "p1", "productName": "旺旺雪饼", "minPrice": 12.0},
        {"productId": "p2", "productName": "乐事薯片", "minPrice": None},
    ])
    assert summary.startswith("已向用户展示商品卡片")
    assert "1. 旺旺雪饼（¥12）" in summary
    assert "2. 乐事薯片" in summary


def _run_mocks(memory: SessionMemory):
    sms = MagicMock()
    sms.load = AsyncMock(return_value=memory)
    sms.save = AsyncMock()
    redis = MagicMock()
    redis.client = object()
    redis.get_consult_product = AsyncMock(return_value=None)
    redis.save_history_condensed = AsyncMock()
    compress = MagicMock()
    compress.maybe_schedule_compress = AsyncMock()
    return sms, redis, compress


@pytest.mark.asyncio
async def test_run_stores_search_cards_and_readable_history():
    memory = SessionMemory(user_id="u1")
    sms, redis, compress = _run_mocks(memory)
    cards_json = '[{"productId":"p1","productName":"旺旺雪饼","minPrice":12.0}]'
    with patch("app.memory.post_turn.session_memory_service", sms), \
         patch("app.memory.post_turn.redis_service", redis), \
         patch("app.memory.post_turn.compress_service", compress), \
         patch("app.memory.post_turn.load_agent_prompt", AsyncMock(return_value="")), \
         patch("app.memory.post_turn.schedule_assistant_condense") as condense:
        await post_turn_service.run(
            user_id="u1",
            message_id=9,
            user_text="帮我找零食",
            assistant_text=cards_json,
            tools_called=["SEARCH_PRODUCTS"],
            tool_biz={"productIds": ["p1"], "productNames": ["旺旺雪饼"]},
            card=None,
            assistant_cards=cards_json,
            working_turns=[],
            working_oldest_id=None,
        )
    assert memory.state["lastSearchResults"] == [
        {"productId": "p1", "productName": "旺旺雪饼", "minPrice": 12.0}
    ]
    saved_text = redis.save_history_condensed.await_args.args[2]
    assert saved_text.startswith("已向用户展示商品卡片")
    assert "1. 旺旺雪饼（¥12）" in saved_text
    condense.assert_not_called()


@pytest.mark.asyncio
async def test_run_clears_search_cards_when_search_without_cards():
    memory = SessionMemory(user_id="u1")
    memory.state["lastSearchResults"] = [
        {"productId": "old", "productName": "旧商品", "minPrice": 1.0}
    ]
    sms, redis, compress = _run_mocks(memory)
    with patch("app.memory.post_turn.session_memory_service", sms), \
         patch("app.memory.post_turn.redis_service", redis), \
         patch("app.memory.post_turn.compress_service", compress), \
         patch("app.memory.post_turn.load_agent_prompt", AsyncMock(return_value="")), \
         patch("app.memory.post_turn.schedule_assistant_condense"):
        await post_turn_service.run(
            user_id="u1",
            message_id=10,
            user_text="帮我找不存在的东西",
            assistant_text="【搜索结果】未找到相关商品。",
            tools_called=["SEARCH_PRODUCTS"],
            tool_biz=None,
            card=None,
            assistant_cards=None,
            working_turns=[],
            working_oldest_id=None,
        )
    assert memory.state["lastSearchResults"] == []


@pytest.mark.asyncio
async def test_run_keeps_text_history_for_normal_turns():
    memory = SessionMemory(user_id="u1")
    sms, redis, compress = _run_mocks(memory)
    with patch("app.memory.post_turn.session_memory_service", sms), \
         patch("app.memory.post_turn.redis_service", redis), \
         patch("app.memory.post_turn.compress_service", compress), \
         patch("app.memory.post_turn.load_agent_prompt", AsyncMock(return_value="")), \
         patch("app.memory.post_turn.schedule_assistant_condense") as condense:
        await post_turn_service.run(
            user_id="u1",
            message_id=11,
            user_text="你好",
            assistant_text="您好，有什么可以帮您？",
            tools_called=[],
            tool_biz=None,
            card=None,
            assistant_cards=None,
            working_turns=[],
            working_oldest_id=None,
        )
    assert memory.state["lastSearchResults"] == []
    saved_text = redis.save_history_condensed.await_args.args[2]
    assert saved_text == "您好，有什么可以帮您？"
    condense.assert_called_once()