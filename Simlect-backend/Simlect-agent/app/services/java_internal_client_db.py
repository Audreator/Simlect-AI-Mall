"""
================================================================================
文件：services/java_internal_client_db.py
角色：JavaInternalClient 的「单体直连数据库」实现 —— 供单体版服务器部署使用
================================================================================

【这个文件干什么】
微服务版部署中，Agent 读数据走 Gateway `/internal/**`（见 java_internal_client.py）；
而部分服务器上跑的是单体版 Java（没有 /internal 接口）。此时通过环境变量
`INTERNAL_READ_BACKEND=db` 切换到本实现：方法签名与 HTTP 版完全一致，
直接查单体业务库（默认 eshop，复用 MYSQL_* 配置），使上层 order_service /
product_service / search_recommend_service / mcp_tools_service 等零改动。

【如何启用】
服务器 .env：INTERNAL_READ_BACKEND=db
（写操作不受影响 —— 仍由 action_execute_service 走 Java 用户接口 /api/order/*）。

【SQL 依据】
语句以该服务器上已验证可用的旧版实现为蓝本（order_info / order_item /
order_logistics_info(_record) / order_comment / user_coupon / discount_coupon /
product_info / product_property_value / product_sku / user_browse_history）。

【关联】
- java_internal_client.build_internal_client（按配置装配本实现）
- 消费方：order_service、product_service、product_snapshot_service、
  search_recommend_service、rag.retriever、mcp_tools_service
================================================================================
"""

from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager
from typing import Any

from app.constants import ORDER_STATUS_DELETE, PRODUCT_STATUS_ON_SALE
from app.db import pool as _pool_module
from app.db.pool import acquire, init_pool

# 单体物流状态码 → 微服务状态码
# 上层展示按微服务语意映射（0待揽收 1已揽收 2运输中 3派送中 4已签收 5异常）
# 单体库语意：0待发货 1运输中 2已送达 3订单取消
_MONOLITH_LOGISTICS_STATUS_MAP = {0: 0, 1: 2, 2: 4, 3: 5}

_pool_init_lock = asyncio.Lock()


async def _ensure_db_pool() -> None:
    """进程内首次使用时初始化 MySQL 连接池（MCP 进程等宿主不会主动调用 init_pool）。"""
    if _pool_module._pool is None:
        async with _pool_init_lock:
            if _pool_module._pool is None:  # 双重检查：并发首调只建一次池
                await init_pool()


@asynccontextmanager
async def _db_cur():
    """统一取游标：先确保连接池可用，再从池中借出 DictCursor。"""
    await _ensure_db_pool()
    async with acquire() as cur:
        yield cur


def _rows(rows: list[dict] | None) -> list[dict]:
    """复制查询行，避免调用方修改游标缓存行（防御性拷贝）。"""
    return [dict(r) for r in rows or []]


class JavaInternalClientDb:
    """
    单体直连库版「内部读客户端」—— 方法签名与 JavaInternalClient 完全一致。

    所有查询均参数化（防 SQL 注入）；行为与旧版 Agent 的直连库实现对齐。
    """

    async def list_orders(
        self,
        user_id: str,
        order_id: str | None = None,
        time_start: str | None = None,
        time_end: str | None = None,
        limit: int | None = 30,
    ) -> list[dict]:
        """按用户/订单号/时间窗查订单（排除已删除），按创建时间倒序。"""
        sql = (
            "SELECT order_id, user_id, order_status, amount, pay_scene FROM order_info "
            "WHERE user_id=%s AND order_status != %s"
        )
        params: list[Any] = [user_id, ORDER_STATUS_DELETE]
        if order_id:
            sql += " AND order_id=%s"
            params.append(order_id)
        if time_start:
            sql += " AND order_time >= %s"
            params.append(time_start)
        if time_end:
            sql += " AND order_time <= %s"
            params.append(time_end)
        sql += " ORDER BY order_time DESC LIMIT %s"
        params.append(max(1, min(int(limit or 30), 100)))
        async with _db_cur() as cur:
            await cur.execute(sql, params)
            return _rows(await cur.fetchall())

    async def get_order(self, order_id: str) -> dict | None:
        """按订单号查整行（写操作提案的归属/状态校验用）。"""
        async with _db_cur() as cur:
            await cur.execute("SELECT * FROM order_info WHERE order_id=%s", (order_id,))
            row = await cur.fetchone()
        return dict(row) if row else None

    async def get_order_item(self, order_item_id: str) -> dict | None:
        """按订单项 ID 查整行（退款提案用）。"""
        async with _db_cur() as cur:
            await cur.execute("SELECT * FROM order_item WHERE order_item_id=%s", (order_item_id,))
            row = await cur.fetchone()
        return dict(row) if row else None

    async def list_order_items(self, order_id: str) -> list[dict]:
        """列出订单下全部订单项。"""
        async with _db_cur() as cur:
            await cur.execute(
                "SELECT * FROM order_item WHERE order_id=%s ORDER BY order_item_id",
                (order_id,),
            )
            return _rows(await cur.fetchall())

    async def get_logistics(self, user_id: str, order_id: str) -> dict | None:
        """查物流主信息 + 轨迹（同时约束 user_id 防越权）；状态码转换为微服务语意。"""
        async with _db_cur() as cur:
            await cur.execute(
                "SELECT * FROM order_logistics_info WHERE order_id=%s AND user_id=%s",
                (order_id, user_id),
            )
            logistics = await cur.fetchone()
            if not logistics:
                return None
            await cur.execute(
                "SELECT record_id, record_time, record_address FROM order_logistics_info_record "
                "WHERE order_id=%s ORDER BY record_id DESC",
                (order_id,),
            )
            records = await cur.fetchall()
        result = dict(logistics)
        raw_status = int(result.get("logistics_status") or 0)
        result["logistics_status"] = _MONOLITH_LOGISTICS_STATUS_MAP.get(raw_status, raw_status)
        result["record_list"] = _rows(records)
        return result

    async def get_comment(self, user_id: str, order_id: str) -> dict | None:
        """查订单已提交评价（同时约束 user_id 防越权）。"""
        async with _db_cur() as cur:
            await cur.execute(
                "SELECT * FROM order_comment WHERE order_id=%s AND user_id=%s",
                (order_id, user_id),
            )
            row = await cur.fetchone()
        return dict(row) if row else None

    async def snapshot_batch(self, product_ids: list[str]) -> dict | None:
        """批量商品快照：商品主信息 + 属性值 + SKU（三个 IN 查询）。"""
        if not product_ids:
            return None
        placeholders = ",".join(["%s"] * len(product_ids))
        params = [str(pid) for pid in product_ids]
        async with _db_cur() as cur:
            await cur.execute(
                "SELECT product_id, product_name, cover, min_price, max_price, status, "
                f"total_sale, category_id FROM product_info WHERE product_id IN ({placeholders})",
                params,
            )
            products = await cur.fetchall()
            await cur.execute(
                "SELECT product_id, property_id, property_name, property_sort, cover_type, "
                "property_value_id, property_value, property_cover, property_remark "
                f"FROM product_property_value WHERE product_id IN ({placeholders})",
                params,
            )
            property_values = await cur.fetchall()
            await cur.execute(
                "SELECT product_id, property_value_ids, price, stock, sort "
                f"FROM product_sku WHERE product_id IN ({placeholders}) ORDER BY sort ASC",
                params,
            )
            skus = await cur.fetchall()
        return {
            "products": _rows(products),
            "property_values": _rows(property_values),
            "skus": _rows(skus),
        }

    async def search_on_sale(
        self,
        keyword: str | None = None,
        limit: int = 20,
        category_id: str | None = None,
        hot_sale: bool = False,
    ) -> list[dict]:
        """在售商品检索：keyword 走 LIKE；hot_sale 时按推荐位+销量排序。"""
        sql = (
            "SELECT product_id, product_name, cover, min_price, category_id "
            "FROM product_info WHERE status=%s"
        )
        params: list[Any] = [PRODUCT_STATUS_ON_SALE]
        kw = (keyword or "").strip()
        if kw:
            sql += " AND product_name LIKE %s"
            params.append(f"%{kw}%")
        if category_id:
            sql += " AND category_id=%s"
            params.append(str(category_id))
        if hot_sale:
            sql += " ORDER BY commend_type DESC, total_sale DESC, create_time DESC"
        else:
            sql += " ORDER BY total_sale DESC, create_time DESC"
        sql += " LIMIT %s"
        params.append(max(1, min(int(limit or 20), 50)))
        async with _db_cur() as cur:
            await cur.execute(sql, params)
            return _rows(await cur.fetchall())

    async def get_product_detail(self, product_id: str) -> dict | None:
        """商品详情：主信息 + 属性值行 + SKU 行（供快照组装与详情文本）。"""
        async with _db_cur() as cur:
            await cur.execute(
                "SELECT product_id, product_name, product_desc, min_price, max_price, "
                "status, total_sale, cover, category_id FROM product_info WHERE product_id=%s",
                (product_id,),
            )
            product = await cur.fetchone()
            if not product:
                return None
            await cur.execute(
                "SELECT product_id, property_id, property_name, property_sort, cover_type, "
                "property_value_id, property_value, property_cover, property_remark "
                "FROM product_property_value WHERE product_id=%s ORDER BY property_sort ASC",
                (product_id,),
            )
            props = await cur.fetchall()
            await cur.execute(
                "SELECT product_id, property_value_ids, price, stock, sort "
                "FROM product_sku WHERE product_id=%s ORDER BY sort ASC",
                (product_id,),
            )
            skus = await cur.fetchall()
        detail = dict(product)
        detail["property_values"] = _rows(props)
        detail["sku_list"] = _rows(skus)
        return detail

    async def list_user_coupons(self, user_id: str) -> list[dict]:
        """用户券列表（排除已作废 status=3；别名 uc_status 供上层动态过期判定）。"""
        async with _db_cur() as cur:
            await cur.execute(
                "SELECT uc.user_coupon_id, uc.coupon_id, uc.status AS uc_status, "
                "dc.coupon_name, dc.coupon_type, dc.threshold_amount, dc.discount_amount, "
                "dc.discount_rate, dc.valid_start_time, dc.valid_end_time "
                "FROM user_coupon uc JOIN discount_coupon dc ON uc.coupon_id=dc.coupon_id "
                "WHERE uc.user_id=%s AND uc.status != 3 "
                "ORDER BY uc.user_coupon_id DESC LIMIT 40",
                (user_id,),
            )
            return _rows(await cur.fetchall())

    async def latest_browse_product_id(self, user_id: str) -> str | None:
        """最近浏览商品 ID（推荐偏好来源）。"""
        async with _db_cur() as cur:
            await cur.execute(
                "SELECT product_id FROM user_browse_history "
                "WHERE user_id=%s ORDER BY browse_time DESC LIMIT 1",
                (user_id,),
            )
            row = await cur.fetchone()
        if not row or not row.get("product_id"):
            return None
        return str(row["product_id"])