# Simlect 智能客服 · 给 Java 初级开发者的导读

> 本文帮你用 Spring 思维读 Python Agent。代码细节以各文件顶部中文注释为准；本文讲**全貌、流程、怎么跑、和谁关联**。

---

## 1. 它是什么？在架构里站哪？

智能客服是一个**独立微服务进程**（不是 Java 里的某个 `@Service`）：

| 进程 | 默认端口 | 类比 |
|------|----------|------|
| Agent（FastAPI） | `7050` | 一个 Spring Boot 应用：接 HTTP/WS、跑业务编排 |
| MCP Server | `7060` | 另一个「工具微服务」：专门执行搜商品/查订单/写提案 |
| Java Gateway | `8080` | 统一入口；把 `/api/agent/**`、`/ws` 转到 Agent |
| Java 业务微服务 | 订单/商品/券… | Agent **只读**走 `/internal/**`；**写操作**走用户态 `/api/order/**` |

**原则（面试/读码都要记住）：**

1. LLM **不能直接改库**。写操作 = `PROPOSE_*` 写 Redis 待确认 → 用户点确认 → Python 带用户 token 调 Java。
2. HTTP `sendMessage` **立刻返回** `messageId`；真正推理在后台异步跑，结果经 **Redis Pub/Sub → WebSocket** 推给前端（类似 MQ 推送）。

---

## 2. 一次对话的完整流程（跟代码）

```
前端 Vue
  │ POST /api/agent/sendMessage（经 Gateway）
  ▼
api/routes/agent.py          ← 类比 @RestController
  ▼
services/agent_service.py    ← 类比编排 @Service：限流、护栏、落库、起异步任务
  │ 立即返回 messageId
  ▼ (asyncio.create_task)
services/agent_engine.py     ← 薄封装
  ▼
graph/runner.py              ← 启动 LangGraph（类比工作流引擎）
  ▼
graph 节点链（nodes.py）:
  entry_guard        取消检查、解析商品卡片
  build_context      记忆 + 意图 + RAG + 拼 LLM 消息
  agent_loop ⇄ tools ReAct：模型想调工具 ↔ MCP 执行
  finalize           护栏清洗、落库助手回复、WS 推 done
  post_turn          更新会话记忆
  cleanup            收尾
```

**写操作另一条线（用户点确认按钮）：**

```
POST /confirmAction
  → pending_action_service.confirm（Redis 锁防双击）
  → action_execute_service → JavaBridge
  → Gateway /api/order/refundOrder | confirmOrder | comment/...
```

---

## 3. 目录怎么对应 Java 项目？

```
Simlect-agent/app/
├── main.py                 # SpringBootApplication + 挂路由
├── api/routes/agent.py     # Controller
├── api/websocket.py        # WS 连接管理 + 订阅 Redis
├── services/               # Service 层（编排、MCP、Java 客户端、流式）
├── graph/                  # 工作流：State / Builder / Nodes / Runner
├── mcp/tools.py            # 给 LLM 看的「工具说明书」(bind_tools)
├── mcp_server/server.py    # 真正执行工具的独立进程入口
├── memory/                 # 会话记忆（类似会话级缓存 + 摘要）
├── domain/intent/          # 意图分类
├── harness/guardrails/     # 输入/输出/工具护栏
├── rag/                    # FAQ 检索
└── config/settings.py      # application.yml 同类配置
```

各子目录还有 `MODULE.md`，可与本文对照。

---

## 4. 本地如何启动 / 如何联调？

1. 先起中间件：MySQL、Redis、Java Gateway + 业务服务（按仓库 `deploy/` 文档）。
2. 起 MCP：`start-mcp.bat` 或 `python -m app.mcp_server`（`:7060`）。
3. 起 Agent：`start.bat` 或 `uvicorn app.main:app --port 7050`。
4. 前端连 Gateway；发消息看 Network 的 `sendMessage`，看 WS 帧里的流式 `OUTPUTTING` / `DONE`。
5. 配环境变量 / `.env`：`JAVA_WEB_URL`、`INTERNAL_TOKEN`、LLM Key、MCP 地址等（见 `config/settings.py`）。

**读码建议顺序：**  
`docs/本文件` → `main.py` → `api/routes/agent.py` → `agent_service.py` → `graph/builder.py` + `state.py` → `nodes.py` → `mcp_tool_router.py` → `mcp_server/server.py` → `java_internal_client.py` → `pending_action` + `action_execute`。

---

## 5. 和 Java 的关联（对照表）

| Python | Java 侧 |
|--------|---------|
| `JavaInternalClient` + `X-Internal-Token` | Gateway `InternalTokenGlobalFilter` + `*AgentInternalController` |
| `JavaBridge` + 用户 `token` | Gateway 登录鉴权 + `OrderController` 等 |
| Gateway 路由 `agent-http` / `agent-ws` | `Simlect-gateway` 的 `application.yml` |
| Outbox/业务锁 | **不在 Agent**；交易正确性仍在 Java 订单域 |

---

## 6. 注释说明（已做逐行中文注释的文件）

主链路源码已按「几乎每一行 + Java 类比」补中文注释，打开即可边读边学：
`main.py`、`api/routes/agent.py`、`api/websocket.py`、`agent_service.py`、`agent_engine.py`、
`graph/{builder,state,runner,nodes}.py`、`mcp/tools.py`、`mcp_server/server.py`、
`mcp_tool_router.py`、`mcp_tools_service.py`、`agent_runtime.py`、
`java_internal_client.py`、`pending_action_service.py`、`action_execute_service.py`、`stream_service.py`，
以及 memory / intent / harness / message_service 等配套文件。

## 7. 关键文件索引（点进去看文件头注释）

| 文件 | 你要搞懂的一句话 |
|------|------------------|
| `main.py` | 进程入口，挂 HTTP/WS |
| `api/routes/agent.py` | 对外 REST 接口清单 |
| `services/agent_service.py` | 发消息编排与异步起图 |
| `graph/builder.py` | 状态机怎么连线 |
| `graph/state.py` | 图上传递的「上下文对象」字段 |
| `graph/nodes.py` | 每个节点具体干什么 |
| `graph/runner.py` | 如何 invoke / 断点续跑 |
| `services/mcp_tool_router.py` | 工具白名单 + 调 MCP |
| `mcp_server/server.py` | 工具注册表（7060） |
| `services/mcp_tools_service.py` | 工具业务实现（查 Java / 建提案） |
| `services/java_internal_client.py` | 读接口 Feign 式客户端 |
| `services/pending_action_service.py` | Redis 待确认 |
| `services/action_execute_service.py` | 确认后调 Java 写接口 |
| `services/stream_service.py` | 发布流式事件到 Redis |
| `api/websocket.py` | 订阅 Redis → 推前端 |

---

## 8. Python ↔ Java 语法速记

| Python | Java |
|--------|------|
| `async def` / `await` | `CompletableFuture` / 响应式非阻塞调用 |
| `dict` | `Map<String, Object>` |
| `list` | `List` |
| `None` | `null` |
| 模块级 `xxx_service = Xxx()` | Spring 单例 Bean |
| `TypedDict` 状态 | 工作流 Context / DTO |
| LangGraph 条件边 | `if` 路由到下一状态节点 |
