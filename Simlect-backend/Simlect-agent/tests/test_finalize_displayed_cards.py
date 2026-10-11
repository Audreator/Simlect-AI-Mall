"""finalize：把实际展示的商品卡片回写 state（供 post_turn 记录「用户看到的清单」）测试。"""

import json
from unittest.mock import AsyncMock, patch

import pytest

from app.graph.nodes import finalize_node
from app.services.agent_runtime import finalize_agent_response

CARDS = '[{"productId":"p1","productName":"旺旺雪饼","minPrice":12.0}]'


@pytest.mark.asyncio
async def test_finalize_agent_response_returns_displayed_cards():
    agent_msg = {"userId": "u1", "messageId": 1, "userMessage": "找零食"}
    with patch("app.services.agent_runtime.stream_service") as stream, \
         patch("app.services.agent_runtime.agent_message_service") as message:
        stream.push_done = AsyncMock()
        message.complete_message = AsyncMock()
        result = await finalize_agent_response(
            agent_msg,
            [],
            [],
            assistant_cards=CARDS,
            tools_called=["SEARCH_PRODUCTS"],
            user_text="找零食",
        )
    assert result == CARDS
    pushed_assistant = stream.push_done.await_args.args[2]
    payload = json.loads(pushed_assistant)
    assert payload["type"] == "PRODUCT_SEARCH_RESULT"
    assert payload["products"][0]["productId"] == "p1"
    message.complete_message.assert_awaited_once()


@pytest.mark.asyncio
async def test_finalize_agent_response_returns_none_without_product_cards():
    agent_msg = {"userId": "u1", "messageId": 2, "userMessage": "你好"}
    with patch("app.services.agent_runtime.stream_service") as stream, \
         patch("app.services.agent_runtime.agent_message_service") as message:
        stream.push_done = AsyncMock()
        message.complete_message = AsyncMock()
        result = await finalize_agent_response(
            agent_msg, ["您好，有什么可以帮您？"], [], user_text="你好"
        )
    assert result is None


@pytest.mark.asyncio
async def test_finalize_agent_response_returns_backfilled_cards():
    """LLM 未调工具、由回填链路产出卡片时，finalize 也应返回实际展示的卡片。"""
    agent_msg = {"userId": "u1", "messageId": 5, "userMessage": "买点零食"}
    backfilled = '[{"productId":"p9","productName":"卫龙辣条","minPrice":3.5}]'
    fake_products = [{"product_id": "p9", "product_name": "卫龙辣条"}]
    with patch("app.services.agent_runtime.stream_service") as stream, \
         patch("app.services.agent_runtime.agent_message_service") as message, \
         patch("app.services.agent_runtime.product_service") as ps:
        stream.push_done = AsyncMock()
        message.complete_message = AsyncMock()
        ps.search_products = AsyncMock(
            return_value=(backfilled, '["p9"]', "product_search", fake_products, "hybrid")
        )
        result = await finalize_agent_response(
            agent_msg, [], [], tools_called=None, user_text="买点零食"
        )
    assert result == backfilled
    ps.search_products.assert_awaited_once()


@pytest.mark.asyncio
async def test_finalize_node_merges_resolved_cards_into_state():
    state = {
        "agent_msg": {"userId": "u1", "messageId": 3, "userMessage": "找零食"},
        "user_id": "u1",
        "message_id": 3,
        "chunks": [],
        "llm_messages": [],
        "tools_called": ["SEARCH_PRODUCTS"],
        "assistant_cards": None,
    }
    with patch("app.graph.nodes.rt") as rt, \
         patch("app.graph.nodes.redis_service") as redis, \
         patch("app.graph.nodes.sensitive_word_service") as sensitive:
        rt.finalize_agent_response = AsyncMock(return_value=CARDS)
        rt.push_chat_error = AsyncMock()
        redis.clear_bound_message_id = AsyncMock()
        sensitive.replace = AsyncMock(side_effect=lambda text: text)
        result = await finalize_node(state)
    assert result["route"] == "post_turn"
    assert result["assistant_cards"] == CARDS


@pytest.mark.asyncio
async def test_finalize_node_keeps_state_cards_when_nothing_resolved():
    state = {
        "agent_msg": {"userId": "u1", "messageId": 4, "userMessage": "你好"},
        "user_id": "u1",
        "message_id": 4,
        "chunks": ["您好"],
        "llm_messages": [],
        "tools_called": [],
    }
    with patch("app.graph.nodes.rt") as rt, \
         patch("app.graph.nodes.redis_service") as redis, \
         patch("app.graph.nodes.sensitive_word_service") as sensitive:
        rt.finalize_agent_response = AsyncMock(return_value=None)
        rt.push_chat_error = AsyncMock()
        redis.clear_bound_message_id = AsyncMock()
        sensitive.replace = AsyncMock(side_effect=lambda text: text)
        result = await finalize_node(state)
    assert result["route"] == "post_turn"
    assert "assistant_cards" not in result