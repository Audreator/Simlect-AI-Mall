"""
================================================================================
文件：mcp/tools.py
角色：给「大模型」看的工具说明书（LangChain StructuredTool），不是真正执行端
================================================================================

【这个文件干什么】
定义每个工具的名称、中文描述、参数 Schema（Pydantic），供 LLM bind_tools。
模型决定「要调哪个工具、传什么参数」后，实际执行走：
  StructuredTool.coroutine → mcp_streamable_client.call_tool → MCP :7060

【流程】
1. agent_runtime.bind_agent_llm() 调用 build_mcp_tools() 构造工具列表
2. LLM 根据 description + args_schema 决定调用哪个工具
3. 每个工具的 coroutine 统一走 _call() → MCP HTTP 客户端远程执行

【和 mcp_server/server.py 的区别】
| 本文件 mcp/tools.py     | mcp_server/server.py        |
|-------------------------|-----------------------------|
| 跑在 Agent 进程         | 跑在 MCP 进程               |
| 面向 LLM 的 schema      | 面向网络的真实执行入口      |
| 描述必须写清楚，否则模型乱调 | 里面调 mcp_tools_service |

【关联】
- 上游：agent_runtime / graph/nodes.py（agent_loop_node）
- 下游：mcp_streamable_client → mcp_server/server.py → mcp_tools_service
================================================================================
"""

from langchain_core.tools import StructuredTool  # import：LangChain 结构化工具类 — 带 name/description/schema，类比 Java @Tool 注解
from pydantic import BaseModel, Field  # import：Pydantic 数据校验 — BaseModel 类似 Java Bean + @Valid + @Schema

from app.services.mcp_streamable_client import mcp_streamable_client  # import：MCP HTTP 客户端单例 — 远程调 :7060 执行工具


class SearchProductsArgs(BaseModel):  # 商品搜索参数 Schema — LLM 输出 JSON 须符合此结构，Pydantic 自动校验
    userId: str = Field(description="用户Id")  # 必填字段 — Field(description=...) 写入 JSON Schema 供 LLM 阅读
    keyword: str = Field(description="搜索关键词（品类/品牌/特征，非用户原话）")  # 必填 — 引导 LLM 提取关键词而非复述
    excludeProductId: str | None = Field(None, description="排除的商品Id，如当前咨询商品")  # 可选 — 默认 None


class QueryOrdersArgs(BaseModel):  # 订单查询参数 Schema
    userId: str = Field(description="用户Id")  # 必填：当前登录用户 ID
    orderId: str | None = Field(None, description="订单号，空则查最近订单")  # 可选 — None 表示查最近订单列表


class ProductDetailArgs(BaseModel):  # 商品详情参数 Schema
    userId: str = Field(description="用户Id")  # 必填
    productId: str = Field(description="商品Id")  # 必填 — 要查询的商品 ID


class UserIdOrderArgs(BaseModel):  # 通用 Schema — 需要 userId + orderId 的工具复用（物流/评价/确认收货等）
    userId: str = Field(description="用户Id")  # 必填
    orderId: str = Field(description="订单Id")  # 必填


class UserIdOrderItemArgs(BaseModel):  # 退款提案参数 Schema — 需要 orderItemId 而非 orderId
    userId: str = Field(description="用户Id")  # 必填
    orderItemId: str = Field(description="订单项Id")  # 必填 — 格式如 订单号_1


class ReviewArgs(BaseModel):  # 商品评价提案参数 Schema
    userId: str  # 必填 — 无 Field 时 Pydantic 仍标记为 required，LLM 从 schema 推断
    orderId: str  # 必填：订单 ID
    commentContent: str  # 必填：评价正文
    star: int = Field(ge=1, le=5)  # 必填：星级 1-5 — ge/le 类似 Java @Min(1) @Max(5) 校验


class RecommentArgs(BaseModel):  # 追评提案参数 Schema
    userId: str  # 必填
    orderId: str  # 必填
    reCommentContent: str  # 必填：追评正文 — 字段名与首次评价的 commentContent 不同


class CouponArgs(BaseModel):  # 优惠券查询参数 Schema
    userId: str  # 必填
    status: int | None = Field(None, description="0未使用 1已使用 2已过期")  # 可选 — 按状态筛选


async def _call(name: str, **kwargs) -> str:  # 内部统一转发 — 所有 StructuredTool 的 coroutine 最终都调此函数
    """统一转发到 MCP 客户端；None 参数不传，避免污染请求体。"""
    args = {k: v for k, v in kwargs.items() if v is not None}  # 字典推导 — 过滤 None 值，避免 MCP schema 收到 null
    return await mcp_streamable_client.call_tool(name, args)  # await 远程 HTTP 调 MCP :7060 — 返回 Wire 文本


def build_mcp_tools() -> list[StructuredTool]:  # 构造供 LLM bind_tools 使用的工具列表 — agent_runtime 启动时调用
    """构造供 LLM bind_tools 使用的工具列表（与 server.py 注册名必须一致）。"""
    return [  # 返回 StructuredTool 列表 — LLM 从此列表中选择要调用的工具
        StructuredTool.from_function(  # 从 lambda 异步函数创建 StructuredTool — LangChain 标准工厂方法
            coroutine=lambda userId, keyword, excludeProductId=None: _call(  # lambda 包装 — 参数名须与 args_schema 字段一致
                "SEARCH_PRODUCTS",  # MCP 工具名 — 必须与 server.py @mcp.tool(name=...) 完全一致
                userId=userId,  # 关键字传参 — 对应 MCP handler 的 userId 参数
                keyword=keyword,
                excludeProductId=excludeProductId,
            ),
            name="SEARCH_PRODUCTS",  # LLM 看到的工具名 — tool_calls[].name 字段
            description="[READ] 搜索/推荐商品",  # LLM 选择工具时阅读的说明 — [READ] 表示只读
            args_schema=SearchProductsArgs,  # 参数 JSON Schema — LLM 据此构造 arguments
        ),
        StructuredTool.from_function(
            coroutine=lambda userId, orderId=None: _call(  # orderId 可选 — None 时不传入 args
                "QUERY_ORDERS", userId=userId, orderId=orderId
            ),
            name="QUERY_ORDERS",
            description="[READ] 仅查询订单列表或订单状态；用户要评价/退款/确认收货时不要用本工具",  # 防止 LLM 误用查单代替写操作
            args_schema=QueryOrdersArgs,
        ),
        StructuredTool.from_function(
            coroutine=lambda userId, productId: _call(
                "GET_PRODUCT_DETAIL", userId=userId, productId=productId
            ),
            name="GET_PRODUCT_DETAIL",
            description="[READ] 查询商品详情",
            args_schema=ProductDetailArgs,
        ),
        StructuredTool.from_function(
            coroutine=lambda userId, orderId: _call(
                "QUERY_LOGISTICS", userId=userId, orderId=orderId
            ),
            name="QUERY_LOGISTICS",
            description="[READ] 查询订单物流轨迹（不是查订单列表）",  # 与 QUERY_ORDERS 明确区分
            args_schema=UserIdOrderArgs,
        ),
        StructuredTool.from_function(
            coroutine=lambda userId, orderId: _call(
                "QUERY_COMMENT", userId=userId, orderId=orderId
            ),
            name="QUERY_COMMENT",
            description="[READ] 查看订单已提交的评价内容（不是写评价）",  # 只读 — 写评价走 PROPOSE_PRODUCT_REVIEW
            args_schema=UserIdOrderArgs,
        ),
        StructuredTool.from_function(
            coroutine=lambda userId, status=None: _call(  # status 可选 — None 表示查全部状态
                "QUERY_USER_COUPONS", userId=userId, status=status
            ),
            name="QUERY_USER_COUPONS",
            description="[READ] 查询用户优惠券",
            args_schema=CouponArgs,
        ),
        StructuredTool.from_function(
            coroutine=lambda userId, orderId: _call(  # 确认收货提案 — [WRITE] 需用户二次确认
                "PROPOSE_CONFIRM_RECEIPT", userId=userId, orderId=orderId
            ),
            name="PROPOSE_CONFIRM_RECEIPT",
            description="[WRITE] 确认收货提案；用户说确认收货时直接调用，不要先 QUERY_ORDERS",  # 引导 LLM 直接提案
            args_schema=UserIdOrderArgs,
        ),
        StructuredTool.from_function(
            coroutine=lambda userId, orderItemId: _call(
                "PROPOSE_REFUND", userId=userId, orderItemId=orderItemId
            ),
            name="PROPOSE_REFUND",
            description="[WRITE] 退款提案；用户要退款时直接调用，不要先 QUERY_ORDERS",
            args_schema=UserIdOrderItemArgs,
        ),
        StructuredTool.from_function(
            coroutine=lambda userId, orderId, commentContent, star: _call(  # 评价提案 — 四个必填参数
                "PROPOSE_PRODUCT_REVIEW",
                userId=userId,
                orderId=orderId,
                commentContent=commentContent,
                star=star,
            ),
            name="PROPOSE_PRODUCT_REVIEW",
            description="[WRITE] 提交评价提案；用户要写评价/打分时用；缺星级或内容时先追问用户",
            args_schema=ReviewArgs,
        ),
        StructuredTool.from_function(
            coroutine=lambda userId, orderId, reCommentContent: _call(  # 追评提案
                "PROPOSE_RECOMMENT",
                userId=userId,
                orderId=orderId,
                reCommentContent=reCommentContent,
            ),
            name="PROPOSE_RECOMMENT",
            description="[WRITE] 提交追评提案；不是查评价",  # 与 QUERY_COMMENT 区分
            args_schema=RecommentArgs,
        ),
    ]  # 共 10 个工具 — 6 个 [READ] + 4 个 [WRITE] PROPOSE_*
