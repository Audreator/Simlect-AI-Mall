"""
================================================================================
文件：services/mcp_streamable_client.py
角色：MCP Streamable HTTP 客户端 — Agent 进程远程调用 :7060 MCP 服务
================================================================================

【这个文件干什么】
封装 MCP 官方 Python SDK，通过 Streamable HTTP 协议：
- list_tools()：拉取 MCP 服务器注册的工具清单
- call_tool(name, args)：远程执行指定工具并返回文本

【流程】
1. _session() 建立 streamablehttp_client 连接 → ClientSession.initialize()
2. session.list_tools() / session.call_tool() 发 JSON-RPC 请求
3. call_tool 把 content 块拼成字符串返回给调用方

【关联】
- 调用方：mcp/tools.py（LLM bind_tools）、mcp_tool_router.py（图节点路由）
- 服务端：mcp_server/server.py（FastMCP @7060）
- 配置：settings.mcp_server_url

【类比 Java】
类似 RestTemplate / WebClient 封装的 Feign Client，每次调用短连接会话。
================================================================================
"""

from __future__ import annotations  # import：延迟类型注解，允许 str | None 等写法

import asyncio  # import：超时保护（asyncio.timeout / asyncio.TimeoutError）
import json  # import：JSON 序列化，用于非 text 类型的 content 块
from contextlib import asynccontextmanager  # import：@asynccontextmanager 装饰器，类似 try-with-resources
from typing import Any, AsyncIterator  # import：Any ~ Object；AsyncIterator ~ Stream<T>

import structlog  # import：结构化日志库，类似 SLF4J + MDC

from app.config.settings import get_settings  # import：读取 mcp_server_url 等配置

logger = structlog.get_logger()  # 获取本模块 logger 实例


class McpStreamableClient:  # MCP 流式 HTTP 客户端类 — 类似 Java @Component RestClient
    """Calls Simlect MCP server over Streamable HTTP (tools/list + tools/call)."""

    def __init__(self, base_url: str | None = None):  # 构造器 — base_url 可选，默认读配置
        settings = get_settings()  # 加载应用配置单例
        self._url = (base_url or settings.mcp_server_url).rstrip("/")  # 存 MCP 基础 URL，去掉尾部 /

    @asynccontextmanager  # 装饰器：yield 前初始化，with 块结束自动清理 — 类似 @AutoCloseable
    async def _session(self) -> AsyncIterator[Any]:  # 私有方法：创建并 yield 一个 MCP ClientSession
        from mcp import ClientSession  # 延迟 import：MCP 客户端会话类，避免模块加载时强依赖
        from mcp.client.streamable_http import streamablehttp_client  # Streamable HTTP 底层传输

        settings = get_settings()  # 加载配置 — 取内部令牌注入请求头（服务端中间件校验）
        endpoint = self._url if self._url.endswith("/mcp") else f"{self._url}/mcp"  # 确保 endpoint 以 /mcp 结尾
        # 每个请求携带 X-Internal-Token：MCP 服务端统一鉴权，防 IDOR
        async with streamablehttp_client(endpoint, headers={"X-Internal-Token": settings.internal_token}) as (read, write, _get_session_id):  # 建立 HTTP 双工流
            async with ClientSession(read, write) as session:  # 在读写流上创建 MCP 会话
                await session.initialize()  # MCP 握手：交换协议版本与能力
                yield session  # 产出 session 给 with 块使用

    async def list_tools(self) -> list[dict[str, Any]]:  # 列出 MCP 服务器上注册的所有工具
        timeout = float(getattr(get_settings(), "mcp_client_timeout", 30) or 30)  # 与 call_tool 同一超时配置
        try:
            async with asyncio.timeout(timeout):  # 防 MCP 进程 hang 导致 bind_agent_llm 卡死
                async with self._session() as session:  # with 自动管理 session 生命周期
                    result = await session.list_tools()  # 发 tools/list JSON-RPC 请求
                    tools = getattr(result, "tools", None) or []  # getattr 安全取 tools 属性，None 则空列表
                    out = []  # 输出 List<Map>，每个 Map 含 name 和 description
                    for t in tools:  # 遍历每个 Tool 对象
                        out.append(  # 追加到结果列表
                            {
                                "name": getattr(t, "name", None),  # 工具名
                                "description": getattr(t, "description", None),  # 工具描述
                            }
                        )
                    return out  # 返回工具清单
        except (TimeoutError, asyncio.TimeoutError):  # 超时 — MCP 进程无响应
            logger.warning("mcp_list_tools_timeout")
            return []  # 返回空清单（调用方降级为无工具可用）
        except Exception:  # 连接层异常（MCP 进程未启动/网络中断）
            logger.warning("mcp_list_tools_failed", exc_info=True)
            return []

    async def call_tool(self, name: str, arguments: dict[str, Any] | None = None) -> str:  # 远程调用指定 MCP 工具
        # 超时保护：慢工具不挂死 Agent（默认 30s，可经 settings.mcp_client_timeout 覆盖）
        timeout = float(getattr(get_settings(), "mcp_client_timeout", 30) or 30)
        try:
            async with asyncio.timeout(timeout):
                async with self._session() as session:  # 每次 call 新建短连接 session
                    result = await session.call_tool(name, arguments or {})  # 发 tools/call 请求，arguments 默认 {}
                    parts: list[str] = []  # 收集所有文本块的 List<String>
                    content = getattr(result, "content", None) or []  # MCP 返回的 content 数组
                    for block in content:  # 遍历每个 content 块（可能是 TextContent 等）
                        text = getattr(block, "text", None)  # 尝试取 text 字段
                        if text:  # 有 text 则直接追加
                            parts.append(text)
                        else:  # 非文本块则 JSON 序列化
                            parts.append(json.dumps(block.model_dump() if hasattr(block, "model_dump") else str(block)))
                    if getattr(result, "isError", False):  # MCP 协议标记的错误响应
                        logger.warning("mcp_tool_error", tool=name, content=parts)  # 打 warning 日志（内部细节只进日志）
                        # 错误细节不回传 LLM/用户（防内部信息泄露），给通用失败文案
                        return "【工具执行失败】该操作未成功，请稍后重试或联系客服"
                    return "\n".join(parts) if parts else ""  # 多段文本用换行拼接；无内容返回空串
        except (TimeoutError, asyncio.TimeoutError):
            logger.warning("mcp_tool_timeout", tool=name)
            return f"【工具调用超时】工具 {name} 响应超时，请稍后重试"
        except Exception:  # 连接层异常 — MCP 进程未启动、EOF、握手失败等
            logger.warning("mcp_tool_call_failed", tool=name, exc_info=True)
            return "【工具调用失败】服务暂时不可用，请稍后重试"


mcp_streamable_client = McpStreamableClient()  # 模块级单例 — 全局共享一个客户端实例
