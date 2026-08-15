"""订单服务模块：封装订单查询、订单项获取及可退款项筛选，对接 Java 内部 API。"""

from datetime import datetime, timedelta  # import statement — 日期时间计算，类似 Java java.time

from app.config.settings import get_settings  # import statement — 订单查询回溯天数配置
from app.constants import ORDER_ITEM_STATUS_NORMAL, REFUNDABLE_ORDER_STATUSES  # import statement — 订单状态常量
from app.services.java_internal_client import java_internal_client  # import statement — Java 内部 HTTP 客户端
from app.utils.biz_payload import build_order_payload  # import statement — 构造订单卡片 JSON

class OrderService:  # 订单服务 — 类似 Java @Service OrderService

    async def query_orders(self, user_id: str, order_id: str | None = None) -> tuple[str, str | None, str]:  # 查询订单并返回卡片

        if order_id:  # 指定订单号
            orders = await self._fetch_orders(user_id, order_id=order_id)
        else:  # 按时间窗口查询最近订单
            # Default window must cover "最近订单/买了什么"; 15d was too short for demo/history data.
            days = max(1, int(get_settings().order_query_lookback_days))  # 回溯天数，至少 1 天
            end = datetime.now()  # 当前时间
            start = end - timedelta(days=days)  # 起始时间 — 类似 LocalDateTime.minusDays
            orders = await self._fetch_orders(
                user_id,
                time_start=start.strftime("%Y-%m-%d 00:00:00"),
                time_end=end.strftime("%Y-%m-%d %H:%M:%S"),
            )
        if not orders:  # 无订单
            return "[]", None, "query_order"  # 返回空卡片
        items_map = self._items_map_from_orders(orders)  # 尝试从订单对象内嵌 items

        if not items_map:  # 内嵌 items 缺失
            order_ids = [str(o["order_id"]) for o in orders if o.get("order_id")]  # list 推导 — 类似 Stream.map
            items_map = await self._fetch_order_items(order_ids)  # 批量拉取订单项
        assistant, biz_data = build_order_payload(orders, items_map)  # 构造前端卡片 JSON
        return assistant, biz_data, "query_order"

    async def _fetch_orders(
        self,
        user_id: str,
        order_id: str | None = None,
        time_start: str | None = None,
        time_end: str | None = None,
    ) -> list[dict]:  # 私有方法 — 调用 Java listOrders API

        return await java_internal_client.list_orders(
            user_id=user_id,
            order_id=order_id,
            time_start=time_start,
            time_end=time_end,
            limit=30,
        )

    @staticmethod  # @staticmethod 类似 Java static method
    def _items_map_from_orders(orders: list[dict]) -> dict[str, list[dict]]:  # 从订单列表提取内嵌 items

        result: dict[str, list[dict]] = {}  # Map<orderId, List<item>>
        for o in orders:
            oid = o.get("order_id")
            items = o.get("items") or o.get("order_item_list") or []  # 兼容多种字段名
            if oid and items:
                result[str(oid)] = items
        return result

    async def _fetch_order_items(self, order_ids: list[str]) -> dict[str, list[dict]]:  # 逐单拉取订单项

        if not order_ids:
            return {}
        result: dict[str, list[dict]] = {}
        for oid in order_ids:
            rows = await java_internal_client.list_order_items(oid)
            if rows:
                result[str(oid)] = rows
        return result

    async def get_order(self, order_id: str) -> dict | None:  # 获取单笔订单 — @Nullable

        return await java_internal_client.get_order(order_id)

    async def get_order_item(self, order_item_id: str) -> dict | None:  # 获取订单项

        return await java_internal_client.get_order_item(order_item_id)

    async def list_order_items(self, order_id: str) -> list[dict]:  # 列出订单下所有订单项

        return await java_internal_client.list_order_items(order_id)

    async def list_refundable_items(self, user_id: str, order_id: str) -> list[dict]:  # 筛选可退款订单项

        order = await self.get_order(order_id)
        if not order or order.get("user_id") != user_id:  # 订单不存在或非本人
            return []
        if order.get("order_status") not in REFUNDABLE_ORDER_STATUSES:  # 订单状态不可退款
            return []
        items = await self.list_order_items(order_id)
        return [i for i in items if i.get("order_item_status") == ORDER_ITEM_STATUS_NORMAL]  # 过滤未退款项

order_service = OrderService()  # 模块级单例
