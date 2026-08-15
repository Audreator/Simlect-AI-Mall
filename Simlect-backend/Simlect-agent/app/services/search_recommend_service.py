"""搜索推荐服务模块：按用户浏览品类、热销榜等策略加载商品卡片，供搜索兜底与浏览推荐。"""

from app.services.java_internal_client import java_internal_client  # import statement — Java 商品/用户 API
from app.utils.biz_payload import first_cover  # import statement — 提取商品封面首图
import structlog  # import statement — 结构化日志

logger = structlog.get_logger()  # 日志记录器

class SearchRecommendService:  # 搜索推荐服务 — 类似 Java @Service

    async def load_recommend_products(self, user_id: str, limit: int = 8) -> list[dict]:  # 综合推荐：浏览品类优先，否则热销

        size = max(1, min(limit, 20))  # 限制 1~20 条
        category_id = await self._resolve_category_from_browse(user_id)  # 从最近浏览推断品类
        products = await self._load_by_category(category_id, size)  # 同品类商品
        if products:
            return products
        return await self._load_hot_sale(size)  # 兜底热销

    async def _resolve_category_from_browse(self, user_id: str) -> str | None:  # 解析用户最近浏览商品的品类 ID

        if not user_id:
            return None
        product_id = await java_internal_client.latest_browse_product_id(user_id)  # 最近浏览商品 ID
        if not product_id:
            return None
        detail = await java_internal_client.get_product_detail(product_id)  # 商品详情
        if not detail:
            return None
        cat = detail.get("category_id")
        return str(cat) if cat else None  # 返回品类 ID 字符串

    async def _load_by_category(self, category_id: str | None, size: int) -> list[dict]:  # 按品类加载在售商品

        if not category_id:
            return []
        rows = await java_internal_client.search_on_sale(
            keyword="",
            limit=size,
            category_id=category_id,
        )
        return self._normalize_cards(rows)  # 规范化封面等字段

    async def _load_hot_sale(self, size: int) -> list[dict]:  # 加载热销商品，失败时降级为最近上架

        try:
            rows = await java_internal_client.search_on_sale(
                keyword="",
                limit=size,
                hot_sale=True,  # 按销量排序
            )
            cards = self._normalize_cards(rows)
            if cards:
                return cards
        except Exception as e:
            # Product service may still run old bytecode (order by sales vs total_sale).
            logger.warning("hot_sale_load_failed_fallback_recent", error=str(e))  # 热销接口异常时降级
        rows = await java_internal_client.search_on_sale(
            keyword="",
            limit=size,
            hot_sale=False,  # 不按热销，取最近商品
        )
        return self._normalize_cards(rows)

    @staticmethod  # static 方法 — 规范化商品卡片字段
    def _normalize_cards(rows: list[dict]) -> list[dict]:
        out = []  # List<Map> 输出
        for r in rows or []:
            item = dict(r)  # 浅拷贝 — 类似 new HashMap<>(r)
            item["cover"] = first_cover(item.get("cover"))  # 统一封面格式
            out.append(item)
        return out

    async def load_hot_sale(self, size: int) -> list[dict]:  # 公开方法 — 加载热销

        return await self._load_hot_sale(size)

    async def load_by_category(self, category_id: str, size: int) -> list[dict]:  # 公开方法 — 按品类加载

        return await self._load_by_category(category_id, size)

search_recommend_service = SearchRecommendService()  # 模块级单例
