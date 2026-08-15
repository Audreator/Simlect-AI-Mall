"""
================================================================================
文件：services/java_internal_client.py
角色：调 Java「内部只读 API」的 HTTP 客户端（类比 Feign + Internal Token）
================================================================================

【这个文件干什么】
Agent/MCP 查订单、商品、券等，统一 POST Gateway 的 /internal/**，
请求头带 X-Internal-Token；把 Java 驼峰 JSON 转成 Python 侧 snake_case。

【流程】
post_json(path, body)
  → httpx POST java_web_url + path
  → 解析 ResponseVO { status, code, data, info }
  → normalize_keys 递归 camelCase → snake_case

【和 JavaBridge 的区别（极易混）】
| 本类 JavaInternalClient | action_execute 里的 JavaBridge |
|-------------------------|--------------------------------|
| /internal/**            | /api/order/** 等用户接口       |
| 服务间令牌                | 用户登录 token                 |
| 读为主                   | 确认后的写操作                  |

【如何使用】
java_internal_client.list_orders(user_id, ...) 等；由 mcp_tools_service 调用。
配置：settings.java_web_url、internal_token（须与 Gateway 一致）。

【关联】
Gateway InternalTokenGlobalFilter、*AgentInternalController、mcp_tools_service
================================================================================
"""

from typing import Any  # import：泛型 Any — 表示任意类型，类似 Java Object

import httpx  # import：异步 HTTP 客户端库，类比 OkHttp / Spring WebClient 的 async 版
import structlog  # import：结构化日志 — key-value 格式，便于 ELK 检索

from app.config.settings import get_settings  # import：读取 application 配置 — java_web_url、internal_token 等

logger = structlog.get_logger()  # 获取本模块 logger 实例


def _camel_to_snake(name: str) -> str:  # 私有工具函数：单个 camelCase 标识符 → snake_case
    out: list[str] = []  # 字符缓冲 List — 类似 Java StringBuilder 逐字符拼接
    for i, ch in enumerate(name):  # enumerate 同时返回索引 i 和字符 ch — 类似 for(int i=0; ...)
        # 在大写字母前插入下划线：首字符除外；连续大写如 "HTTPClient" 只在 HTTP 与 C 之间加 _
        if ch.isupper() and i > 0 and (not name[i - 1].isupper() or (i + 1 < len(name) and name[i + 1].islower())):
            out.append("_")  # 追加分隔下划线
        out.append(ch.lower())  # 当前字符转小写后追加
    return "".join(out)  # List 拼接为完整 snake_case 字符串 — 如 orderItemId → order_item_id


def normalize_keys(obj: Any) -> Any:  # 递归规范化 JSON 对象所有键名为 snake_case — 供 Python 侧统一访问

    if isinstance(obj, list):  # isinstance ~ instanceof：当前节点是 List/数组
        return [normalize_keys(x) for x in obj]  # 列表推导 — 对每个元素递归处理，返回新 List

    if isinstance(obj, dict):  # 当前节点是 Map/dict
        # 字典推导：键经 _camel_to_snake 转换，值递归 normalize_keys
        return {_camel_to_snake(str(k)): normalize_keys(v) for k, v in obj.items()}

    return obj  # 基本类型（str/int/float/bool/None）— 无需转换，原样返回


class JavaInternalClient:  # Java 内部只读 API 客户端 — 类比 @FeignClient(name="internal-api")
    """Calls Java /internal/** exclusively via Gateway (java_web_url)."""
    # ↑ 类 docstring：只调 Gateway 的 /internal/** 路径，不走用户 /api/** 接口

    def __init__(self, timeout: float = 30.0):  # 构造器 — timeout 为 HTTP 请求超时秒数，默认 30s
        self._timeout = timeout  # 私有字段存超时配置 — 每次 post_json 创建 AsyncClient 时使用

    def _headers(self) -> dict[str, str]:  # 构造每次 HTTP 请求的公共 Header Map
        settings = get_settings()  # 读取全局配置单例 — 类似 @Value 注入
        return {
            "X-Internal-Token": settings.internal_token,  # 服务间鉴权令牌 — Gateway InternalTokenGlobalFilter 校验
            "Content-Type": "application/json",  # 请求体格式为 JSON — 对应 Java @RequestBody
        }

    def _base(self) -> str:  # 返回 Gateway 基础 URL — 去掉尾部斜杠避免双斜杠
        return get_settings().java_web_url.rstrip("/")  # 如 http://localhost:8080

    async def post_json(self, path: str, body: dict | None = None) -> Any:  # 通用 POST JSON — 解析 Java ResponseVO 并返回 data

        url = f"{self._base()}/{path.lstrip('/')}"  # 拼接完整 URL — path 去掉Leading / 防止 //
        try:  # try-except 捕获网络层异常 — 超时、连接拒绝、DNS 失败等
            # async with ~ try-with-resources：退出 with 块自动关闭 AsyncClient 连接池
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                resp = await client.post(url, json=body or {}, headers=self._headers())  # POST JSON 请求体 — body 为 None 则发 {}
                resp.raise_for_status()  # HTTP 状态码非 2xx 时抛 httpx.HTTPStatusError — 类似 RestTemplate 抛异常
                payload = resp.json()  # 解析响应体 JSON → Python dict — 对应 Java ResponseVO 结构
        except Exception as e:  # 网络异常或 raise_for_status 抛出的 HTTPStatusError
            logger.error("java_internal_http_failed", url=url, error=str(e))  # 记录失败 URL 和错误信息
            raise  # 重新抛出 — 让上层 mcp_tools_service 决定如何包装为用户文案

        if not isinstance(payload, dict):  # ResponseVO 必须是 Map 结构 — 否则格式非法
            raise ValueError(f"invalid ResponseVO from {url}")  # ValueError ~ IllegalArgumentException

        status = payload.get("status")  # 业务状态字段 — Java 侧通常为 "success" 或 "error"
        code = payload.get("code", 200)  # HTTP 风格业务码 — 默认 200 表示成功

        if status == "error" or (status is not None and status != "success"):  # 业务明确标记失败
            raise ValueError(payload.get("info") or f"internal call failed: {url}")  # info 字段为用户可读错误信息

        if status is None and code not in (200, "200", None):  # 兼容旧接口：仅有 code 无 status 字段
            raise ValueError(payload.get("info") or f"internal call failed: {url}")

        return payload.get("data")  # 成功时返回 data 字段 — 真正的业务数据（List 或 Map）

    async def list_orders(
        self,
        user_id: str,  # 必填：用户 ID — Java 侧做权限校验
        order_id: str | None = None,  # 可选：指定订单号 — 有则查单笔
        time_start: str | None = None,  # 可选：时间范围起始 — ISO 格式字符串
        time_end: str | None = None,  # 可选：时间范围结束
        limit: int | None = 30,  # 可选：返回条数上限 — 默认 30
    ) -> list[dict]:  # 返回订单列表 — 每项为 snake_case 键的 dict
        body: dict[str, Any] = {"userId": user_id}  # 请求体 — Java Controller 期望 camelCase 键名
        if order_id:  # Python Truthy 检查 — 非空非 None 才加入
            body["orderId"] = order_id
        if time_start:
            body["timeStart"] = time_start
        if time_end:
            body["timeEnd"] = time_end
        if limit is not None:  # limit=0 也合法 — 所以用 is not None 而非 if limit
            body["limit"] = limit
        data = await self.post_json("/internal/order/agent/listOrders", body)  # POST 内部订单列表接口
        return normalize_keys(data or [])  # 键名转 snake_case — data 为 null 则返回空列表

    async def get_order(self, order_id: str) -> dict | None:  # 单笔订单详情 — 不存在返回 None
        data = await self.post_json(
            "/internal/order/agent/getOrder",  # 内部单笔订单接口路径
            {"orderId": order_id},  # 请求体 — 只传 orderId
        )
        return normalize_keys(data) if data else None  # 有数据则规范化键名，无数据返回 None

    async def get_order_item(self, order_item_id: str) -> dict | None:  # 单个订单项详情 — 如 订单号_1
        data = await self.post_json(
            "/internal/order/agent/getOrderItem",  # 内部订单项接口
            {"orderItemId": order_item_id},  # 请求体
        )
        return normalize_keys(data) if data else None

    async def list_order_items(self, order_id: str) -> list[dict]:  # 某订单下所有订单项列表
        data = await self.post_json(
            "/internal/order/agent/listOrderItems",  # 内部订单项列表接口
            {"orderId": order_id},
        )
        return normalize_keys(data or [])  # 空则 []

    async def get_logistics(self, user_id: str, order_id: str) -> dict | None:  # 物流轨迹 — 需 userId 做归属校验
        data = await self.post_json(
            "/internal/order/agent/getLogistics",  # 内部物流接口
            {"userId": user_id, "orderId": order_id},  # 同时传 userId + orderId — 防止越权查他人物流
        )
        return normalize_keys(data) if data else None

    async def get_comment(self, user_id: str, order_id: str) -> dict | None:  # 订单已提交的评价内容 — 只读
        data = await self.post_json(
            "/internal/order/agent/getComment",  # 内部评价查询接口
            {"userId": user_id, "orderId": order_id},
        )
        return normalize_keys(data) if data else None

    async def snapshot_batch(self, product_ids: list[str]) -> dict | None:  # 批量商品快照 — 标题/价格/封面等轻量信息
        data = await self.post_json(
            "/internal/product/snapshotBatch",  # 内部商品快照批量接口
            {"productIds": product_ids},  # 商品 ID 数组 — Java List<String>
        )
        return normalize_keys(data) if data else None

    async def search_on_sale(
        self,
        keyword: str | None = None,  # 搜索关键词 — None 或空串表示不限关键词
        limit: int = 20,  # 返回条数上限 — 默认 20
        category_id: str | None = None,  # 可选：按类目 ID 筛选
        hot_sale: bool = False,  # 可选：true 则只看热销商品
    ) -> list[dict]:  # 返回在售商品列表 — snake_case 键
        body: dict[str, Any] = {"keyword": keyword or "", "limit": limit}  # 基础参数 — keyword 默认空串
        if category_id:  # 有类目 ID 则加入筛选条件
            body["categoryId"] = category_id
        if hot_sale:  # True 时才传 hotSale 字段 — 避免传 false 干扰 Java 默认逻辑
            body["hotSale"] = True
        data = await self.post_json(
            "/internal/product/agent/searchOnSale",  # 内部在售商品搜索接口
            body,
        )
        return normalize_keys(data or [])

    async def get_product_detail(self, product_id: str) -> dict | None:  # 单个商品完整详情 — 含 SKU/规格等
        data = await self.post_json(
            "/internal/product/agent/getDetail",  # 内部商品详情接口
            {"productId": product_id},
        )
        return normalize_keys(data) if data else None

    async def list_user_coupons(self, user_id: str) -> list[dict]:  # 用户全部优惠券列表
        data = await self.post_json(
            "/internal/coupon/agent/listUserCoupons",  # 内部优惠券接口
            {"userId": user_id},
        )
        return normalize_keys(data or [])

    async def latest_browse_product_id(self, user_id: str) -> str | None:  # 用户最近浏览的商品 ID — 用于推荐上下文
        data = await self.post_json(
            "/internal/user/agent/latestBrowseProductId",  # 内部用户行为接口
            {"userId": user_id},
        )
        if not data:  # 无浏览记录 — Java 返回 null 或空
            return None
        if isinstance(data, dict):  # Java 可能返回 { "productId": "xxx" } 结构
            pid = data.get("productId") or data.get("product_id")  # 兼容 camelCase 和已规范化的 snake_case
            return str(pid) if pid else None  # 有 ID 则转字符串返回
        return str(data)  # Java 直接返回字符串 ID 的情况 — 如 "prod_123"


java_internal_client = JavaInternalClient()  # 模块级单例 — mcp_tools_service 直接 import 使用，全局共享一个客户端
