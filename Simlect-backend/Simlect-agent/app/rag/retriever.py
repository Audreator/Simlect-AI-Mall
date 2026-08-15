import json

import httpx
import structlog

from app.config.settings import get_settings
from app.rag.embedding import embed_text
from app.resilience.circuit_breaker import circuit_registry
from app.services.java_internal_client import java_internal_client

logger = structlog.get_logger()

PRODUCT_INDEX = "simlect-index"


def _escape_wildcard(text: str) -> str:
    """转义 ES wildcard 查询中的通配符（防用户输入 * ? 触发前导通配导致性能 DoS / 语义错乱）。"""
    if not text:
        return text
    return text.replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?")


class RagRetriever:

    def __init__(self):

        self._es_hosts = get_settings().es_hosts

    def _es_url(self, path: str) -> str:

        base = self._es_hosts.split(",")[0].rstrip("/")
        return f"{base}{path}"

    async def search_faq(self, query: str, top_k: int | None = None) -> str:

        docs = await self._vector_search(query, "faq", top_k)
        if not docs:
            return ""

        seen: set[str] = set()
        parts = []
        for d in docs:
            text = d.get("content") or d.get("text") or ""
            if text and text not in seen:
                seen.add(text)
                parts.append(text)
        return "\n".join(parts)

    async def search_product_vector_ids(self, query: str, limit: int) -> list[str]:

        docs = await self._vector_search(query, "product", limit, threshold=0.4)
        ids = []
        for d in docs:
            meta = d.get("metadata") or {}

            pid = meta.get("productId") or meta.get("product_id")
            if pid and str(pid) not in ids:
                ids.append(str(pid))
        return ids

    async def search_product_keyword_ids(self, query: str, limit: int) -> list[str]:

        breaker = circuit_registry.get_or_create("es", failure_threshold=3, recovery_timeout=30)
        if not breaker.allow_request() or not query.strip():
            return []
        try:
            body = {
                "size": min(max(limit, 1), 50),
                "query": {
                    "bool": {
                        "should": [
                            {"match": {"productName": {"query": query, "boost": 2}}},
                            # wildcard 特殊字符转义：防用户输入 * ? 触发前导通配导致性能 DoS/语义错乱
                            {"wildcard": {"productName.keyword": f"*{_escape_wildcard(query)}*"}},
                        ]
                    }
                },
                "_source": ["productId"],
            }

            async with httpx.AsyncClient(timeout=15) as client:
                resp = await client.post(self._es_url(f"/{PRODUCT_INDEX}/_search"), json=body)
                resp.raise_for_status()
                hits = resp.json().get("hits", {}).get("hits", [])
            breaker.record_success()
            return [h["_source"]["productId"] for h in hits if h.get("_source", {}).get("productId")]
        except Exception as e:
            breaker.record_failure()
            logger.error("es_keyword_search_failed", error=str(e))

            return await self._product_search_fallback(query, limit)

    async def _vector_search(
        self,
        query: str,
        data_type: str,
        top_k: int | None = None,
        threshold: float | None = None,
    ) -> list[dict]:

        settings = get_settings()
        k = top_k or settings.rag_top_k

        th = threshold if threshold is not None else settings.rag_score_threshold
        breaker = circuit_registry.get_or_create("es")
        if not breaker.allow_request():
            return []
        vector = await embed_text(query)
        if not vector:
            return []
        try:
            body = {
                "size": k,
                "knn": {
                    "field": "content_vector",
                    "query_vector": vector,
                    "k": k,
                    "num_candidates": k * 2,
                    "filter": {
                        "bool": {
                            "must": [{"term": {"metadata.dataType": data_type}}]
                        }
                    },
                },
                "_source": ["content", "metadata", "text"],
            }
            async with httpx.AsyncClient(timeout=20) as client:
                resp = await client.post(
                    self._es_url(f"/{settings.es_index}/_search"),
                    json=body,
                )
                if resp.status_code == 404:

                    body["knn"]["field"] = "vector"
                    resp = await client.post(
                        self._es_url(f"/{settings.es_index}/_search"),
                        json=body,
                    )
                resp.raise_for_status()
                hits = resp.json().get("hits", {}).get("hits", [])
            breaker.record_success()
            results = []
            for h in hits:
                score = h.get("_score", 0)
                if score < th:
                    continue
                src = h.get("_source", {})
                results.append({
                    "content": src.get("content") or src.get("text", ""),
                    "metadata": src.get("metadata", {}),
                    "score": score,
                })
            return results
        except Exception as e:
            breaker.record_failure()
            logger.error("vector_search_failed", data_type=data_type, error=str(e))
            return []

    async def _product_search_fallback(self, query: str, limit: int) -> list[str]:

        try:
            rows = await java_internal_client.search_on_sale(keyword=query, limit=limit)
            ids = []
            for r in rows:
                pid = r.get("product_id") or r.get("productId")
                if pid and str(pid) not in ids:
                    ids.append(str(pid))
            return ids
        except Exception as e:
            logger.error("product_search_fallback_failed", error=str(e))
            return []

rag_retriever = RagRetriever()
