# MCP 工具服务（独立进程 :7060）

## 模块职责

FastMCP 注册 SEARCH_PRODUCTS / QUERY_ORDERS / PROPOSE_* 等；底层调 mcp_tools_service + Java。

## 核心类 / 文件

- `server.py — @mcp.tool handlers`
- `__main__.py — python -m app.mcp_server`

## 调用链（自上而下）

1. `mcp_streamable_client.call_tool → server.py handler`
1. `handler → mcp_tools_service.tool_* → java_internal_client (/internal/**)`
1. `PROPOSE_* → pending_action_service (Redis 存 act token)`

## 调用链图

```mermaid
flowchart LR
  Agent -->|HTTP| mcp_server
  mcp_server --> mcp_tools_service
  mcp_tools_service --> java_internal
```

## 外部依赖

- Redis
- Java Internal API


## 说明

Python 模块：async≈CompletableFuture，dict≈Map，本文件描述模块间**调用链**，代码内 `# [zh]` 为逐行注释。

