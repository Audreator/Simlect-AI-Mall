"""单体直连库适配（INTERNAL_READ_BACKEND=db）测试：SQL 构建与行数据整形。"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.services.java_internal_client_db import JavaInternalClientDb


class _FakeCursor:
    """记录已执行 SQL 的假游标：fetchone/fetchall 返回预设行。"""

    def __init__(self, one=None, many=None):
        self._one = one
        self._many = many or []
        self.executed = []  # [(sql, params)]

    async def execute(self, sql, params=None):
        self.executed.append((sql, params))

    async def fetchone(self):
        return self._one

    async def fetchall(self):
        return list(self._many)


def _patch_acquire(one=None, many=None):
    cur = _FakeCursor(one, many)
    cm = MagicMock()
    cm.__aenter__ = AsyncMock(return_value=cur)
    cm.__aexit__ = AsyncMock(return_value=None)
    patcher = patch.multiple(
        "app.services.java_internal_client_db",
        acquire=MagicMock(return_value=cm),  # acquire() 返回上下文管理器 cm
        _ensure_db_pool=AsyncMock(),  # 连接池初始化在单测中一并打桩
    )
    return patcher, cur


@pytest.mark.asyncio
async def test_list_orders_builds_filters_and_excludes_deleted():
    patcher, cur = _patch_acquire(many=[{"order_id": "o1", "order_status": 1}])
    with patcher:
        rows = await JavaInternalClientDb().list_orders(
            "u1",
            order_id="o1",
            time_start="2026-01-01 00:00:00",
            time_end="2026-02-01 00:00:00",
        )
    assert rows[0]["order_id"] == "o1"
    sql, params = cur.executed[0]
    assert "FROM order_info" in sql
    assert "user_id=%s" in sql
    assert "order_status != %s" in sql  # 排除已删除订单
    assert "order_id=%s" in sql and "order_time >= %s" in sql and "order_time <= %s" in sql
    assert list(params)[:3] == ["u1", -1, "o1"]


@pytest.mark.asyncio
async def test_search_on_sale_keyword_like_and_hot_ordering():
    patcher, cur = _patch_acquire(many=[{"product_id": "p1"}])
    with patcher:
        rows = await JavaInternalClientDb().search_on_sale(keyword="零食", limit=5, hot_sale=True)
    sql, params = cur.executed[0]
    assert "product_name LIKE %s" in sql
    assert "%零食%" in list(params)
    assert "commend_type DESC" in sql and "total_sale DESC" in sql
    assert rows[0]["product_id"] == "p1"


@pytest.mark.asyncio
async def test_search_on_sale_category_filter():
    patcher, cur = _patch_acquire(many=[])
    with patcher:
        await JavaInternalClientDb().search_on_sale(keyword="", limit=8, category_id="10")
    sql, params = cur.executed[0]
    assert "category_id=%s" in sql
    assert "10" in list(params)


@pytest.mark.asyncio
async def test_get_logistics_merges_records_and_maps_status():
    logistics_row = {
        "order_id": "o1",
        "logistics_no": "SF1",
        "logistics_company": "顺丰",
        "logistics_status": 2,  # 单体语意：已送达
    }
    records = [{"record_time": "2026-01-01 10:00:00", "record_address": "上海"}]
    patcher, cur = _patch_acquire(one=logistics_row, many=records)
    with patcher:
        result = await JavaInternalClientDb().get_logistics("u1", "o1")
    assert result["logistics_status"] == 4  # 微服务语意：已签收
    assert result["record_list"] == records
    sqls = [s for s, _ in cur.executed]
    assert any("order_logistics_info WHERE order_id=%s AND user_id=%s" in s for s in sqls)
    assert any("order_logistics_info_record" in s for s in sqls)


@pytest.mark.asyncio
async def test_get_order_item_and_comment_sql():
    patcher, cur = _patch_acquire(one={"order_item_id": "o1_1"})
    with patcher:
        row = await JavaInternalClientDb().get_order_item("o1_1")
    assert row["order_item_id"] == "o1_1"
    assert "FROM order_item WHERE order_item_id=%s" in cur.executed[0][0]

    patcher, cur = _patch_acquire(one={"order_id": "o1", "comment_content": "很好"})
    with patcher:
        row = await JavaInternalClientDb().get_comment("u1", "o1")
    sql, params = cur.executed[0]
    assert "FROM order_comment" in sql and "user_id=%s" in sql
    assert list(params) == ["o1", "u1"]


@pytest.mark.asyncio
async def test_list_user_coupons_join_and_excludes_voided():
    patcher, cur = _patch_acquire(many=[{"user_coupon_id": "uc1"}])
    with patcher:
        rows = await JavaInternalClientDb().list_user_coupons("u1")
    sql, _ = cur.executed[0]
    assert "JOIN discount_coupon" in sql and "uc.status != 3" in sql
    assert rows[0]["user_coupon_id"] == "uc1"


@pytest.mark.asyncio
async def test_snapshot_batch_runs_three_queries():
    patcher, cur = _patch_acquire(many=[{"product_id": "p1"}])
    with patcher:
        batch = await JavaInternalClientDb().snapshot_batch(["p1", "p2"])
    assert set(batch.keys()) == {"products", "property_values", "skus"}
    sqls = [s for s, _ in cur.executed]
    assert any("FROM product_info" in s for s in sqls)
    assert any("FROM product_property_value" in s for s in sqls)
    assert any("FROM product_sku" in s for s in sqls)
    assert any(list(params) == ["p1", "p2"] for _, params in cur.executed)


@pytest.mark.asyncio
async def test_get_product_detail_attaches_property_and_sku_rows():
    product = {"product_id": "p1", "product_name": "零食", "status": 1}
    patcher, cur = _patch_acquire(one=product, many=[{"marker": 1}])
    with patcher:
        detail = await JavaInternalClientDb().get_product_detail("p1")
    assert detail["product_name"] == "零食"
    assert detail["property_values"] == [{"marker": 1}]
    assert detail["sku_list"] == [{"marker": 1}]


@pytest.mark.asyncio
async def test_latest_browse_product_id_returns_str():
    patcher, cur = _patch_acquire(one={"product_id": "p9"})
    with patcher:
        pid = await JavaInternalClientDb().latest_browse_product_id("u1")
    assert pid == "p9"
    sql, _ = cur.executed[0]
    assert "user_browse_history" in sql and "ORDER BY browse_time DESC" in sql


@pytest.mark.asyncio
async def test_get_order_and_list_order_items_sql():
    patcher, cur = _patch_acquire(one={"order_id": "o1", "user_id": "u1"})
    with patcher:
        row = await JavaInternalClientDb().get_order("o1")
    assert row["order_id"] == "o1"
    assert "FROM order_info WHERE order_id=%s" in cur.executed[0][0]

    patcher, cur = _patch_acquire(many=[{"order_item_id": "o1_1"}])
    with patcher:
        rows = await JavaInternalClientDb().list_order_items("o1")
    assert rows[0]["order_item_id"] == "o1_1"
    assert "FROM order_item WHERE order_id=%s" in cur.executed[0][0]


@pytest.mark.asyncio
async def test_latest_browse_product_id_empty_returns_none():
    patcher, _ = _patch_acquire(one=None)
    with patcher:
        assert await JavaInternalClientDb().latest_browse_product_id("u1") is None


@pytest.mark.asyncio
async def test_adapter_method_ensures_db_pool():
    """每次查询前应确保连接池可用（MCP 进程等宿主不会主动 init_pool）。"""
    ensure = AsyncMock()
    cur = _FakeCursor(many=[])
    cm = MagicMock()
    cm.__aenter__ = AsyncMock(return_value=cur)
    cm.__aexit__ = AsyncMock(return_value=None)
    with patch.multiple(
        "app.services.java_internal_client_db",
        acquire=MagicMock(return_value=cm),
        _ensure_db_pool=ensure,
    ):
        await JavaInternalClientDb().search_on_sale("", 3)
    ensure.assert_awaited_once()


@pytest.mark.asyncio
async def test_ensure_db_pool_idempotent(monkeypatch):
    """空池时仅初始化一次；池已存在时不再初始化。"""
    from app.services import java_internal_client_db as mod

    async def fake_init():
        mod._pool_module._pool = object()  # 模拟建池成功后池存在

    init_mock = AsyncMock(side_effect=fake_init)
    monkeypatch.setattr("app.services.java_internal_client_db.init_pool", init_mock)
    monkeypatch.setattr(mod._pool_module, "_pool", None)
    await mod._ensure_db_pool()
    await mod._ensure_db_pool()
    init_mock.assert_awaited_once()


@pytest.mark.asyncio
async def test_snapshot_batch_empty_ids_returns_none():
    assert await JavaInternalClientDb().snapshot_batch([]) is None