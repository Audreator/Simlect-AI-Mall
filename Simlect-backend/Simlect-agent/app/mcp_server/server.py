"""
================================================================================
文件：mcp_server/server.py
角色：MCP 服务端入口 — 注册 10 个工具（Agent :7050 连，默认 :7060）。
================================================================================

【这个文件干什么】
用 FastMCP 把「工具」注册成 Python 进程内的可调用接口。
Agent 图内 tools_node 不直接 import 业务逻辑（避免 HTTP 请求流程），
而是由本文件转成 mcp_tools_service，最后调 Java / 写 Redis 提案卡。

【流程】
1. 启动时 _mcp_lifespan 连 Redis；PROPOSE_* 写提案卡到 Redis。
2. FastMCP 注册 @mcp.tool 装饰的异步函数。
3. Agent 经 mcp_streamable_client 的 tools/call 调本文件对应 handler。
4. handler 调 mcp_tools_service → 返回 ToolInvokeResult.to_wire() 字符串。

【为什么这么做？】
- 与 Agent 解耦：工具调用不占 Agent 的 HTTP 对话连接。
- 协议统一：LLM 直接 / 经路由都走 MCP Streamable HTTP。
- 提案机制：写操作 PROPOSE_* 只写 Redis，待用户确认才落库。

【如何使用】
1. 独立终端：python -m app.mcp_server（或 start-mcp.bat）
2. 看日志/确认监听 7060，路径一律是 /mcp
3. Agent 侧把 MCP URL 指向本进程

【工具分类】
- [READ] SEARCH_PRODUCTS / QUERY_ORDERS / GET_PRODUCT_DETAIL / QUERY_LOGISTICS /
         QUERY_COMMENT / QUERY_USER_COUPONS  → 调 Java /internal
- [WRITE] PROPOSE_*  → 只写 Redis 确认卡，不直接改数据

【安全】
- 所有 HTTP 请求必须携带 X-Internal-Token（与 Agent 内部令牌一致），
  否则中间件直接 403 —— 防端口可达者以任意 userId 越权调用工具（IDOR）。
- 信任边界：token 校验通过后，userId 由已鉴权的 Agent 上下文传入；
  写操作另有 pending 记录归属校验（load_owned），读操作是 Agent 代理查询。

【关联】
- 客户端：mcp_streamable_client（Agent 进程）
- 业务层：mcp_tools_service → java_internal_client / pending_action_service
- 结果 DTO：tool_invoke_result.ToolInvokeResult
================================================================================
"""

from __future__ import annotations  # import：延迟类型注解 — 函数参数/返回可用 str | None 等写法

import hmac  # import：常量时间比较 — 校验 X-Internal-Token 防时序侧信道
import os  # import：读取环境变量 FASTMCP_HOST / FASTMCP_PORT 及监听地址
from collections.abc import AsyncIterator  # import：异步生成器类型注解 — lifespan yield 的返回类型
from contextlib import asynccontextmanager  # import：装饰器 — 将 async generator 包装为 async context manager
from typing import Any  # import：ASGI 中间件签名用 — scope/receive/send

import structlog  # import：结构化日志库 — 审计 token 拒绝事件
from mcp.server.fastmcp import FastMCP  # import：MCP 官方 FastMCP 框架 — 类比 Spring Boot 的 MCP HTTP 服务端

from app.services import mcp_tools_service as tools  # import：业务工具实现模块 — 所有 tools 层 handler 逻辑
from app.services.redis_service import redis_service  # import：Redis 连接服务 — PROPOSE_* 写提案卡需要
from app.services.tool_invoke_result import ToolInvokeResult  # import：工具结果 DTO — to_wire() 序列化
from app.config.settings import get_settings  # import：配置 — 启动时校验内部令牌（FailFast）

logger = structlog.get_logger()  # 获取本模块 logger 实例


# 监听地址：默认仅本机回环（Agent 与 MCP 同机部署；防端口暴露后任意客户端直调工具越权）
# 需要跨机部署时显式设置 FASTMCP_HOST 并保证网络隔离/网关鉴权
_MCP_HOST = os.getenv("FASTMCP_HOST", "127.0.0.1")  # 监听地址 — 默认仅本机
_MCP_PORT_RAW = os.getenv("FASTMCP_PORT", "7060")
if not _MCP_PORT_RAW.isdigit() or not (0 < int(_MCP_PORT_RAW) < 65536):
    raise ValueError(f"FASTMCP_PORT 非法: {_MCP_PORT_RAW}")
_MCP_PORT = int(_MCP_PORT_RAW)  # 监听端口 — 默认 7060，Agent 侧 mcp_streamable_client 连此端口


class _InternalTokenMiddleware:  # ASGI 中间件 — 类比 Spring Security OncePerRequestFilter
    """MCP HTTP 端点统一鉴权：校验 X-Internal-Token，防 IDOR（任意 userId 越权调工具）。"""

    def __init__(self, app: Any, expected_token: str) -> None:  # 构造器 — 包住内部 ASGI app
        self._app = app  # 被包装的下游 ASGI 应用（FastMCP 的 HTTP app）
        self._expected = expected_token.encode("utf-8")  # 期望令牌 — 预编码避免每次比较解码

    async def __call__(self, scope: dict, receive: Any, send: Any) -> None:  # ASGI 入口
        if scope["type"] in ("http", "websocket"):  # 只拦截 HTTP/WS 请求
            # 运维探活端点放行（无敏感数据，不依赖 FastMCP 内部路由）
            if scope["type"] == "http" and scope.get("path") == "/health" and scope.get("method") == "GET":
                await self._health(send)
                return
            headers = dict(scope.get("headers") or [])  # 原始 header 列表 → dict（bytes 键值）
            token = headers.get(b"x-internal-token", b"")  # 读取内部令牌头（HTTP 头大小写不敏感）
            if not token or not hmac.compare_digest(token, self._expected):  # 恒定时间比较
                logger.warning("mcp_token_rejected", client=str(scope.get("client")))  # 审计日志
                await self._reject(send)  # 返回 403
                return
        await self._app(scope, receive, send)  # 通过 → 放行给 FastMCP

    @staticmethod  # 静态方法 — 不需要实例状态
    async def _health(send: Any) -> None:  # 健康探活响应（不泄漏内部信息）
        body = b'{"status":"ok"}'  # 简洁探活体
        await send({  # 发送响应起始事件
            "type": "http.response.start",  # ASGI 事件类型：响应头
            "status": 200,  # 正常
            "headers": [  # 响应头
                (b"content-type", b"application/json"),  # JSON 内容类型
                (b"content-length", str(len(body)).encode("ascii")),  # 内容长度
            ],
        })
        await send({"type": "http.response.body", "body": body})  # 发送响应体

    @staticmethod  # 静态方法 — 不需要实例状态
    async def _reject(send: Any) -> None:  # 发送 403 响应（不泄漏内部信息）
        body = b'{"status":"error","code":403,"info":"forbidden","data":null}'  # 统一响应信封
        await send({  # 发送响应起始事件
            "type": "http.response.start",  # ASGI 事件类型：响应头
            "status": 403,  # 禁止访问
            "headers": [  # 响应头
                (b"content-type", b"application/json"),  # JSON 内容类型
                (b"content-length", str(len(body)).encode("ascii")),  # 内容长度
            ],
        })
        await send({"type": "http.response.body", "body": body})  # 发送响应体


@asynccontextmanager  # 装饰器：将下面的 async generator 函数包装为 lifespan 上下文管理器
async def _mcp_lifespan(_server: FastMCP) -> AsyncIterator[dict]:  # MCP 进程启动/关闭生命周期钩子 — FastMCP 启动前/后调用
    # 启动 FailFast：内部令牌未配置/弱值直接拒绝启动（与 Agent 一致）
    get_settings()
    # MCP 与 Agent 是两个进程；提案卡依赖 Redis，这里必须先连上。
    await redis_service.ensure_connected()  # 启动时确保 Redis 连接可用 — PROPOSE_* 工具写 pending 需要
    try:
        yield {}  # yield 空 dict — 表示 lifespan 上下文就绪，FastMCP 继续启动 HTTP 服务
    finally:
        # 进程生命周期内保持连接；会话会频繁重连，不必每次关 Redis。
        pass  # 故意不关闭 Redis — 避免 MCP 进程频繁启停时重复建连开销


mcp = FastMCP(  # 创建 FastMCP 应用实例 — 类比 Spring Boot @SpringBootApplication
    "simlect-tools",  # MCP 服务名称 — 客户端 initialize 握手时会看到
    instructions="Simlect mall agent tools. Read tools return text; write tools create confirm cards.",  # 给 MCP 客户端的全局工具说明
    host=_MCP_HOST,  # HTTP 监听地址
    port=_MCP_PORT,  # HTTP 监听端口
    lifespan=_mcp_lifespan,  # 绑定上面定义的生命周期钩子 — 启动前连 Redis
)


def _text(result) -> str:  # 辅助函数：把业务层返回值统一序列化为 MCP 响应字符串
    """把 ToolInvokeResult 序列化成 MCP 线上字符串（含卡片等结构化信息）。"""
    if isinstance(result, ToolInvokeResult):  # isinstance ~ Java instanceof：若是结构化结果 DTO
        return result.to_wire()  # 序列化为 Wire 格式 — 前缀 + JSON 卡片或纯文本
    return str(result)  # 普通 str/int 等 — 直接转字符串返回给 MCP 客户端


@mcp.tool(name="SEARCH_PRODUCTS", description="[READ] 搜索/推荐商品")  # @mcp.tool 注册 MCP 工具 — name 须与 mcp/tools.py 一致
async def search_products(  # 异步 handler — MCP 客户端 tools/call 时执行此函数
    userId: str,  # 用户 ID — camelCase 与 MCP JSON schema 一致
    keyword: str,  # 搜索关键词 — LLM 从用户意图提取
    excludeProductId: str | None = None,  # 可选：排除的商品 ID — 如当前正在咨询的商品
) -> str:  # 返回 Wire 字符串 — MCP 协议要求工具结果为 text
    return _text(await tools.tool_search_products(userId, keyword, excludeProductId))  # 委托给业务层


@mcp.tool(  # 注册查询订单工具
    name="QUERY_ORDERS",  # 工具名 — LLM 据此选择调用
    description="[READ] 仅查询订单列表或订单状态；用户要评价/退款/确认收货时不要用本工具",  # 工具语义描述
)
async def query_orders(userId: str, orderId: str | None = None) -> str:  # 查询订单列表（可按订单号过滤）
    return _text(await tools.tool_query_orders(userId, orderId))  # 委托给业务层


@mcp.tool(name="GET_PRODUCT_DETAIL", description="[READ] 查询商品详情")  # 注册商品详情工具
async def get_product_detail(userId: str, productId: str) -> str:  # 商品详情查询 — 咨询上下文用
    return _text(await tools.tool_get_product_detail(userId, productId))  # 委托给业务层


@mcp.tool(name="QUERY_LOGISTICS", description="[READ] 查询订单物流轨迹（不是查订单列表）")  # 注册物流查询工具
async def query_logistics(userId: str, orderId: str) -> str:  # 物流轨迹查询 — 用户问"到哪了"用
    return _text(await tools.query_logistics(userId, orderId))  # 委托给业务层


@mcp.tool(name="QUERY_COMMENT", description="[READ] 查看订单已提交的评价内容（不是写评价）")  # 注册评价查询工具
async def query_comment(userId: str, orderId: str) -> str:  # 已提交评价查询 — 用户问"我评了什么"用
    return _text(await tools.query_comment(userId, orderId))  # 委托给业务层


@mcp.tool(name="QUERY_USER_COUPONS", description="[READ] 查询用户优惠券")  # 注册优惠券查询工具
async def query_user_coupons(userId: str, status: int | None = None) -> str:  # 用户优惠券查询（可按状态过滤）
    return _text(await tools.query_user_coupons(userId, status))  # 委托给业务层


@mcp.tool(  # 注册确认收货提案工具
    name="PROPOSE_CONFIRM_RECEIPT",  # 工具名 — [WRITE] 前缀标识写操作
    description="[WRITE] 确认收货提案；用户说确认收货时直接调用，不要先 QUERY_ORDERS",  # 语义描述 — 引导 LLM 正确路由
)
async def propose_confirm_receipt(userId: str, orderId: str) -> str:  # 确认收货提案 handler — [WRITE]
    return _text(await tools.propose_confirm_receipt(userId, orderId))  # 委托给业务层（只写提案卡）


@mcp.tool(  # 注册退款提案工具
    name="PROPOSE_REFUND",  # 工具名
    description="[WRITE] 退款提案；用户要退款时直接调用，不要先 QUERY_ORDERS",  # 语义描述 — 防止 LLM 先查订单
)
async def propose_refund(userId: str, orderItemId: str) -> str:  # 退款提案 handler — [WRITE]
    return _text(await tools.propose_refund(userId, orderItemId))  # 委托给业务层（只写提案卡）


@mcp.tool(  # 注册商品评价提案工具
    name="PROPOSE_PRODUCT_REVIEW",  # 工具名
    description="[WRITE] 提交评价提案；用户要写评价/打分时用；缺星级或内容时先追问用户",  # 语义描述 — 缺参数先追问
)
async def propose_product_review(  # 评价提案 handler — [WRITE]
    userId: str,  # 用户 ID
    orderId: str,  # 订单 ID
    commentContent: str,  # 评价内容 — LLM 从对话中提取
    star: int,  # 星级 1-5
) -> str:  # 返回提案结果
    return _text(await tools.propose_product_review(userId, orderId, commentContent, star))  # 委托给业务层


@mcp.tool(name="PROPOSE_RECOMMENT", description="[WRITE] 提交追评提案；不是查评价")  # 注册追评提案工具
async def propose_recomment(  # 追评提案 handler — [WRITE]
    userId: str,  # 用户 ID
    orderId: str,  # 订单 ID
    reCommentContent: str,  # 追评内容 — 与首次评价的 commentContent 字段不同
) -> str:
    return _text(await tools.propose_recomment(userId, orderId, reCommentContent))  # 委托给业务层


def main() -> None:  # 进程入口函数 — python -m app.mcp_server 时调用
    # 构造 FastMCP 的 Streamable HTTP ASGI app，外包令牌鉴权中间件后交给 uvicorn
    inner = mcp.streamable_http_app() if hasattr(mcp, "streamable_http_app") else mcp.http_app()  # 兼容新旧 SDK API
    app = _InternalTokenMiddleware(inner, get_settings().internal_token)  # 包上令牌中间件（X-Internal-Token）
    import uvicorn  # 延迟 import — 仅启动进程时需要

    uvicorn.run(app, host=_MCP_HOST, port=_MCP_PORT, log_level="warning")  # 启动 uvicorn — 监听 /mcp 路径


if __name__ == "__main__":  # Python 脚本直接运行时入口判断 — 类比 Java public static void main
    main()  # 调用 main 函数启动 MCP HTTP 服务
