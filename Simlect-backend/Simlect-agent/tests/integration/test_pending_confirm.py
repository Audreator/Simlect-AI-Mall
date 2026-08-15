"""待确认操作 confirm 行为对齐 Java（含执行前 CAS 状态落定闭环）。"""

import json
from unittest.mock import AsyncMock, patch

import pytest

from app.services.pending_action_service import PendingActionService


@pytest.fixture
def service():
    return PendingActionService()


def _redis_stub(success_cas: bool = True):
    """构造 redis_service 桩：连接/锁/exec 标记可用，CAS 默认成功。"""
    redis = AsyncMock()
    redis.ensure_connected = AsyncMock()
    redis.try_lock_pending_action.return_value = "owner1"
    redis.unlock_pending_action = AsyncMock()
    redis.try_mark_pending_executing.return_value = True
    redis.cas_pending_status.return_value = success_cas
    redis.delete_pending_action = AsyncMock()
    redis.clear_pending_executing = AsyncMock()
    return redis


@pytest.mark.asyncio
async def test_confirm_success_deletes_pending(service):
    pending = {
        "token": "act_test",
        "userId": "u1",
        "actionType": "CONFIRM_RECEIPT",
        "paramsJson": json.dumps({"orderId": "o1"}),
        "status": 0,
    }

    async def executor(p):
        assert p["token"] == "act_test"
        return "订单 o1 已确认收货"

    with patch.object(service, "load_owned", AsyncMock(return_value=pending)):
        with patch("app.services.pending_action_service.redis_service") as redis:
            redis = _redis_stub()
            action_type, ok, msg = await service.confirm("u1", "act_test", executor)

    assert action_type == "CONFIRM_RECEIPT"
    assert ok is True
    assert "确认收货" in msg
    # CAS 先落 CONFIRMED，成功后再删除
    redis.cas_pending_status.assert_awaited_with("act_test", 0, 1)
    redis.delete_pending_action.assert_awaited_once_with("act_test")
    redis.clear_pending_executing.assert_awaited_once_with("act_test")


@pytest.mark.asyncio
async def test_confirm_delete_failed_status_already_confirmed(service):
    pending = {"token": "act_del", "userId": "u1", "actionType": "REFUND", "status": 0}

    async def executor(_):
        return "已提交退款"

    with patch.object(service, "load_owned", AsyncMock(return_value=pending)):
        with patch("app.services.pending_action_service.redis_service") as redis:
            redis = _redis_stub()
            redis.delete_pending_action = AsyncMock(side_effect=RuntimeError("redis down"))
            action_type, ok, msg = await service.confirm("u1", "act_del", executor)

    assert ok is False
    assert "刷新" in msg
    # 双失败窗口消除：状态已在执行前 CAS 落定为 CONFIRMED，无需补写
    redis.cas_pending_status.assert_called_once()


@pytest.mark.asyncio
async def test_confirm_failure_rolls_back_status(service):
    pending = {
        "token": "act_fail",
        "userId": "u1",
        "actionType": "REFUND",
        "paramsJson": "{}",
        "status": 0,
    }

    async def executor(_):
        raise ValueError("退款失败：订单状态不允许")

    with patch.object(service, "load_owned", AsyncMock(return_value=pending)):
        with patch("app.services.pending_action_service.redis_service") as redis:
            redis = _redis_stub()
            action_type, ok, msg = await service.confirm("u1", "act_fail", executor)

    assert action_type == "REFUND"
    assert ok is False
    assert "退款失败" in msg
    # 业务失败：回滚 CONFIRMED→PENDING（允许用户修正后重试），不删除 pending
    redis.cas_pending_status.assert_any_await("act_fail", 1, 0)
    redis.delete_pending_action.assert_not_awaited()


@pytest.mark.asyncio
async def test_confirm_system_error_keeps_confirmed(service):
    pending = {"token": "act_sys", "userId": "u1", "actionType": "REFUND", "status": 0}

    async def executor(_):
        raise RuntimeError("java 500")

    with patch.object(service, "load_owned", AsyncMock(return_value=pending)):
        with patch("app.services.pending_action_service.redis_service") as redis:
            redis = _redis_stub()
            action_type, ok, msg = await service.confirm("u1", "act_sys", executor)

    assert ok is False
    assert "系统处理异常" in msg
    # 系统异常不回滚（executor 可能已执行写操作）：重试被 CONFIRMED 状态拒绝，防二次执行
    redis.cas_pending_status.assert_called_once()
    redis.delete_pending_action.assert_not_awaited()


@pytest.mark.asyncio
async def test_confirm_cas_rejected(service):
    pending = {"token": "act_cas", "userId": "u1", "actionType": "REFUND", "status": 0}

    async def executor(_):
        raise AssertionError("不应执行 executor")

    with patch.object(service, "load_owned", AsyncMock(return_value=pending)):
        with patch("app.services.pending_action_service.redis_service") as redis:
            redis = _redis_stub(success_cas=False)  # 并发其他请求已落定
            with pytest.raises(ValueError, match="已处理|已过期"):
                await service.confirm("u1", "act_cas", executor)


@pytest.mark.asyncio
async def test_confirm_idempotent_on_processed(service):
    pending = {"token": "act_done", "userId": "u1", "status": 1}

    with patch.object(service, "load_owned", AsyncMock(return_value=pending)):
        with patch("app.services.pending_action_service.redis_service") as redis:
            redis = _redis_stub()
            with pytest.raises(ValueError, match="已处理"):
                await service.confirm("u1", "act_done", AsyncMock())
            redis.try_mark_pending_executing.assert_not_awaited()
