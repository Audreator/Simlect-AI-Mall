"""商品搜索服务模块：混合检索（关键词+向量 RRF）、品类/热销兜底，及搜索工具消息格式化。"""

import re  # import statement — 正则表达式
import structlog  # import statement — 结构化日志

from app.constants import PRODUCT_CANDIDATE_SIZE, PRODUCT_RESULT_SIZE, PRODUCT_STATUS_ON_SALE  # import statement — 检索常量
from app.domain.intent.rules import looks_like_browse_recommend  # import statement — 浏览推荐意图规则
from app.rag.retriever import rag_retriever  # import statement — RAG 检索器
from app.rag.rrf import rrf_merge  # import statement — RRF 融合算法
from app.services.java_internal_client import java_internal_client  # import statement — Java 商品 API
from app.services.product_search_query import (  # import statement — 搜索词规范化与相关性过滤
    filter_products_by_query_relevance,
    normalize_product_search_query,
)
from app.services.search_recommend_service import search_recommend_service  # import statement — 品类/热销推荐
from app.utils.biz_payload import build_product_payload, first_cover  # import statement — 商品卡片构造

logger = structlog.get_logger()

_VAGUE_MARKERS = ("什么", "类似", "同款", "推荐", "有没有", "哪些", "哪个", "吗", "呢", "怎么")  # 模糊搜索标记词

_NOISE_RE = re.compile(r"[\d]+g|[\*×x]\d|装|规格|分享装", re.I)  # 商品名噪声正则 — 规格/包装等

def is_vague_search_keyword(text: str | None) -> bool:  # 判断是否模糊搜索词

    t = (text or "").strip()
    if not t or len(t) < 2:  # 空或过短视为模糊
        return True
    if any(m in t for m in _VAGUE_MARKERS) and len(t) <= 16:  # 含模糊标记且较短
        return True
    return False

_SIMILAR_HINTS = ("类似", "同款", "相近", "同类型", "同款式", "别的款", "其他款")  # 找类似商品提示词

def is_similar_or_recommend_request(text: str | None) -> bool:  # 是否类似/推荐请求

    t = (text or "").strip()
    if not t or len(t) > 40:
        return False
    if any(h in t for h in _SIMILAR_HINTS):
        return True
    if "推荐" in t and len(t) <= 28:
        return True
    return False

def derive_search_keyword(
    keyword: str | None,
    consult_product: dict | None,
) -> str:  # 推导实际搜索关键词 — 模糊词时用咨询商品名/品类

    kw = (keyword or "").strip()
    if consult_product and is_vague_search_keyword(kw):  # 模糊词且有咨询商品

        name = (consult_product.get("productName") or consult_product.get("product_name") or "").strip()
        if name:
            cleaned = _NOISE_RE.sub(" ", name)  # 去除规格噪声
            parts = [p.strip() for p in re.split(r"[\s/|]+", cleaned) if p.strip()]  # 分词
            if parts:
                return " ".join(parts[:4])  # 取前 4 段作为关键词

        category_id = consult_product.get("categoryId") or consult_product.get("category_id")
        if category_id:
            return f"category:{category_id}"  # 降级为品类搜索
    # 「我要吃零食」→「零食」，提升关键词/向量命中率
    return normalize_product_search_query(kw) or kw

class ProductService:  # 商品搜索服务 — 类似 Java @Service

    async def search_products(
        self,
        user_id: str,
        keyword: str | None,
        user_text: str = "",
        consult_product: dict | None = None,
        exclude_product_id: str | None = None,
    ) -> tuple[str, str | None, str, list[dict], str]:  # 返回 (卡片JSON, bizData, bizType, 商品List, source)

        query = derive_search_keyword(keyword, consult_product)  # 推导搜索词
        if not query:
            query = (keyword or "").strip()
        if is_vague_search_keyword(query):  # 仍模糊则再次推导
            query = derive_search_keyword(None, consult_product) or query

        biz_type = "product_search"  # 默认业务类型
        product_ids: list[str] = []  # List<String> 融合后的 ID
        source = "none"  # 数据来源标记

        if query.startswith("category:"):  # 品类搜索

            category_id = query.split(":", 1)[1]
            products = await search_recommend_service.load_by_category(category_id, 12)
            source = "category"
        elif query:  # 混合检索

            keyword_ids = await rag_retriever.search_product_keyword_ids(query, PRODUCT_CANDIDATE_SIZE)  # ES 关键词
            vector_ids = await rag_retriever.search_product_vector_ids(query, PRODUCT_CANDIDATE_SIZE)  # 向量
            product_ids = rrf_merge(keyword_ids, vector_ids, PRODUCT_RESULT_SIZE)  # RRF 融合
            logger.info(
                "hybrid_search",
                query=query,
                keyword_hits=len(keyword_ids),
                vector_hits=len(vector_ids),
                merged=len(product_ids),
            )
            products = await self._load_products_by_ids(product_ids)  # 批量加载商品详情
            if products:
                source = "hybrid"
                # Vector/keyword often returns unrelated hot junk — drop non-matching titles.
                relevant = filter_products_by_query_relevance(products, query)  # 标题相关性过滤
                if not relevant:
                    logger.info(
                        "hybrid_relevance_miss",
                        query=query,
                        candidates=len(products),
                    )
                    products = []
                    source = "none"
                elif len(relevant) < len(products):
                    logger.info(
                        "hybrid_relevance_filtered",
                        query=query,
                        before=len(products),
                        after=len(relevant),
                    )
                    products = relevant
        else:
            products = []  # 无 query 则无结果

        if exclude_product_id:  # 排除指定商品 — 如找类似时排除当前咨询商品
            products = [p for p in products if str(p.get("product_id")) != str(exclude_product_id)]

        similar_intent = _similar_intent(keyword, consult_product)  # 是否找类似商品意图
        if products and similar_intent and source == "hybrid":  # 类似意图但品类不匹配
            if not _products_match_consult_category(products, consult_product):
                logger.info(
                    "similar_hybrid_category_mismatch",
                    consult_category=_consult_category_id(consult_product),
                )
                products = []
                source = "none"

        if not products and consult_product and is_vague_search_keyword(keyword or user_text):  # 品类兜底
            category_id = consult_product.get("categoryId") or consult_product.get("category_id")
            if category_id:
                logger.info("category_fallback", category_id=category_id)
                products = await search_recommend_service.load_by_category(str(category_id), 8)
                if exclude_product_id:
                    products = [p for p in products if str(p.get("product_id")) != str(exclude_product_id)]
                if products:
                    source = "category"

        if not products and looks_like_browse_recommend(user_text):  # 浏览推荐兜底
            products = await search_recommend_service.load_recommend_products(user_id, 8)
            if products:
                biz_type = "BROWSE_RECOMMEND"
                source = "browse"

        if not products:  # 热销兜底
            logger.info("hot_sale_fallback", user_id=user_id)
            products = await search_recommend_service.load_hot_sale(8)
            if products:
                biz_type = "product_search"
                source = "hot_sale"

        assistant, biz_data = build_product_payload(products)  # 构造前端卡片 JSON
        return assistant, biz_data, biz_type, products, source

    async def get_product_detail_text(self, product_id: str) -> str:  # 商品详情文本 — 供 GET_PRODUCT_DETAIL 工具

        row = await java_internal_client.get_product_detail(product_id)
        if not row:
            return f"【商品不存在】productId={product_id}"
        if row.get("status") != PRODUCT_STATUS_ON_SALE:
            return f"【商品已下架】{row.get('product_name') or product_id}"
        desc = (row.get("product_desc") or "")[:200]  # 简介截断 200 字
        return (
            f"商品：{row.get('product_name')} | ID：{row.get('product_id')} | "
            f"价格：{row.get('min_price')}~{row.get('max_price')}元 | "
            f"销量：{row.get('total_sale') or row.get('sales') or 0} | 简介：{desc}"
        )

    async def _load_products_by_ids(self, product_ids: list[str]) -> list[dict]:  # 按 ID 列表加载商品 — 保持顺序

        if not product_ids:
            return []
        batch = await java_internal_client.snapshot_batch(product_ids)  # 批量快照
        rows: list[dict] = []
        if batch and isinstance(batch.get("products"), list):
            rows = batch["products"]
        else:  # 批量失败 — 逐个拉详情

            for pid in product_ids:
                detail = await java_internal_client.get_product_detail(pid)
                if detail:
                    rows.append(detail)

        id_map: dict[str, dict] = {}  # Map<productId, product> — 去重与过滤
        for r in rows:
            pid = str(r.get("product_id") or "")
            if not pid:
                continue
            status = r.get("status")
            if status is not None and status != PRODUCT_STATUS_ON_SALE:  # 过滤非在售
                continue
            r["cover"] = first_cover(r.get("cover"))
            id_map[pid] = r

        ordered = []  # 按 RRF 顺序输出
        for pid in product_ids:
            if pid in id_map:
                ordered.append(id_map[pid])
        return ordered

product_service = ProductService()  # 模块级单例

def _similar_intent(keyword: str | None, consult: dict | None) -> bool:  # 模糊词 + 有咨询商品 = 类似意图

    return is_vague_search_keyword(keyword) and bool(consult)

def _consult_category_id(consult: dict | None) -> str:  # 提取咨询商品品类 ID

    if not consult:
        return ""
    return str(consult.get("categoryId") or consult.get("category_id") or "")

def _products_match_consult_category(products: list[dict], consult: dict | None) -> bool:  # 结果是否含同品类商品

    consult_cat = _consult_category_id(consult)
    if not consult_cat or not products:
        return False
    return any(str(p.get("category_id") or "") == consult_cat for p in products)  # any 类似 Stream.anyMatch

def format_search_tool_message(
    keyword: str,
    consult: dict | None,
    products: list[dict],
    source: str,
) -> str:  # 格式化 SEARCH_PRODUCTS 工具返回文案

    from app.domain.intent.rules import looks_like_browse_recommend, looks_like_hot_sale_recommend  # 延迟 import
    from app.services.product_search_query import filter_products_by_query_relevance

    consult_name = (consult or {}).get("productName") or (consult or {}).get("product_name") or "当前商品"
    similar_intent = _similar_intent(keyword, consult)
    alternative_sources = {"browse", "hot_sale"}  # 兜底来源集合
    kw = (keyword or "").strip()
    kw_display = (normalize_product_search_query(kw) or kw)[:24] if kw else "你的需求"

    if similar_intent and source in alternative_sources:  # 找类似但走了兜底
        return (
            f"【类似商品】暂未找到与「{consult_name}」类似或同款的商品。\n"
            f"【另荐热销】已为您另外推荐热销商品（非同款，请查看下方卡片）。"
        )
    if similar_intent and source == "category":  # 同品类推荐
        return f"【同品类推荐】找到 {len(products)} 个同品类商品（请查看下方卡片）。"

    # Even if source claims hybrid, never brand irrelevant titles as「找到」.
    if products and kw and source not in ("category",):  # 混合检索需验证相关性
        relevant = filter_products_by_query_relevance(products, kw)
        intentional_alt = looks_like_hot_sale_recommend(kw) or looks_like_browse_recommend(kw)
        if not relevant and not intentional_alt:  # 无相关且非故意要热销/浏览
            return (
                f"【搜索结果】暂未找到与「{kw_display}」相关的商品。\n"
                f"【另荐热销】已为您另外推荐热销商品，请查看下方卡片。"
            )

    # Keyword search missed → hot-sale / browse backfill: never label as「搜索结果找到」.
    if products and source in alternative_sources:  # 关键词未命中，走了兜底
        intentional_alt = looks_like_hot_sale_recommend(kw) or looks_like_browse_recommend(kw)
        if intentional_alt and source == "hot_sale":
            return f"【热销推荐】为您推荐 {len(products)} 个热销商品（请查看下方卡片）。"
        if intentional_alt and source == "browse":
            return f"【浏览推荐】根据你的浏览为你推荐 {len(products)} 个商品（请查看下方卡片）。"
        alt_label = "【另荐热销】" if source == "hot_sale" else "【浏览推荐】"
        alt_body = (
            "已为您另外推荐热销商品，请查看下方卡片。"
            if source == "hot_sale"
            else "已根据浏览为您另外推荐商品，请查看下方卡片。"
        )
        return (
            f"【搜索结果】暂未找到与「{kw_display}」相关的商品。\n"
            f"{alt_label}{alt_body}"
        )

    if not products:  # 完全无结果
        if kw:
            return f"【搜索结果】暂未找到与「{kw_display}」相关的商品。"
        return "【搜索结果】未找到相关商品。"
    return f"【搜索结果】找到 {len(products)} 个商品（请查看下方卡片）。"  # 正常命中
