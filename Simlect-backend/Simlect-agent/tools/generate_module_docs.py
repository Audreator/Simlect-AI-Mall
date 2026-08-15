#!/usr/bin/env python3
"""为 Simlect-agent / ragent-python / Java 微服务生成带调用链的 MODULE.md。"""

from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]  # EShop (tools→Simlect-agent→Simlect-backend→Simlect→EShop)

def md(title: str, purpose: str, keys: list[str], chains: list[str], deps: list[str], mermaid: str, extra: str = "") -> str:
    chain_block = "\n".join(f"1. `{c}`" for c in chains)
    keys_block = "\n".join(f"- `{k}`" for k in keys)
    deps_block = "\n".join(f"- {d}" for d in deps)
    return f"""# {title}

## 模块职责

{purpose}

## 核心类 / 文件

{keys_block}

## 调用链（自上而下）

{chain_block}

## 调用链图

```mermaid
{mermaid}
```

## 外部依赖

{deps_block}

{extra}
"""

SIMLECT_AGENT = {
    "app/api": (
        "HTTP / WebSocket 接口层",
        "对外暴露 Agent REST 与 WS；鉴权、限流、统一 ResponseVO。类比 Spring `@RestController` + `@ControllerAdvice`。",
        ["routes/agent.py — sendMessage / loadHistory / confirmAction", "websocket.py — ConnectionManager + Redis Pub/Sub 下行", "deps.py — require_login", "exception_handlers.py"],
        [
            "main.py → POST /api/agent/sendMessage → agent.py::send_message",
            "agent.py → deps.require_login → agent_service.AgentOrchestrator.send_message",
            "agent.py → message_service.save_user_message (MySQL)",
            "agent.py → asyncio.create_task(_run_agent) → agent_engine.assistant_answer",
            "stream_service.push_* → redis_service.publish_ws → websocket._topic_listener → WS 客户端",
            "POST /api/agent/confirmAction → pending_action_service.confirm → action_execute_service → Java /api/order/*",
        ],
        "graph LR\n  Client -->|HTTP/WS| main\n  main --> agent_routes\n  agent_routes --> agent_service\n  agent_service --> agent_engine\n  agent_engine --> graph_runner\n  stream_service --> redis_pubsub --> websocket",
        ["Redis Pub/Sub", "MySQL (message_service)", "Java Web (confirm 写操作)", "app.auth.token_service"],
    ),
    "app/graph": (
        "LangGraph Agent 状态机",
        "编排一次对话的完整生命周期：入口守卫 → 上下文 → ReAct 循环 → 工具 → 收尾 → 记忆。类比 Spring StateMachine / Camunda BPMN。",
        ["builder.py — build_agent_graph / 条件边", "runner.py — run_agent_graph", "state.py — AgentGraphState", "nodes.py — 7 个节点", "checkpoint/redis_saver.py"],
        [
            "agent_engine.assistant_answer → runner.run_agent_graph → builder.get_compiled_graph().ainvoke",
            "entry_guard → build_context_node → agent_loop_node",
            "agent_loop_node ⇄ tools_node (ReAct 循环)",
            "agent_loop_node → finalize_node → post_turn_node → cleanup_node → END",
            "entry_guard: agent_runtime.is_cancelled + parse_agent_message",
            "build_context: session_memory + resolve_intent + rag + context_builder",
            "agent_loop: bind_agent_llm + stream_llm_turn + mcp_tool_router 兜底",
            "tools: mcp_tool_router.invoke → mcp_streamable_client",
            "finalize: output_guard + agent_runtime.finalize_agent_response",
            "post_turn: post_turn_service.run → session_memory.save",
        ],
        "flowchart TD\n  entry[entry_guard] --> ctx[build_context]\n  ctx --> loop[agent_loop]\n  loop -->|tool_calls| tools[tools_node]\n  tools --> loop\n  loop --> fin[finalize]\n  fin --> mem[post_turn]\n  mem --> clean[cleanup]",
        ["Redis Checkpoint", "LLM", "MCP", "MySQL (resume 检查)"],
    ),
    "app/services": (
        "业务服务层",
        "Agent 编排、LLM 运行时、消息持久化、Redis、MCP 路由、Java 内部 API。类比 Spring `@Service` 层。",
        ["agent_service — 编排入口", "agent_engine — 调 Graph", "agent_runtime — 流式/最终化", "mcp_tool_router — 工具分发", "java_internal_client — 读 Java", "action_execute_service — 写 Java", "product_service — 商品搜索", "pending_action_service — 确认 token"],
        [
            "send_message: agent_service → message_service + redis + product_snapshot → agent_engine",
            "LLM: agent_runtime.bind_agent_llm → llm_factory + mcp/tools.build_mcp_tools",
            "流式: stream_llm_turn → stream_service.push_chunk → redis publish → WS",
            "工具: mcp_tool_router.invoke → mcp_streamable_client → MCP 进程 → mcp_tools_service → java_internal_client",
            "确认: pending_action_service (Redis NX) → confirm → action_execute_service → httpx POST java",
            "商品兜底: product_service.search_products → java_internal + ES hybrid",
        ],
        "flowchart LR\n  agent_service --> agent_engine\n  agent_runtime --> stream_service\n  agent_runtime --> message_service\n  mcp_tool_router --> mcp_client\n  mcp_client --> mcp_server\n  mcp_tools_service --> java_internal",
        ["Redis", "MySQL", "LLM API", "MCP :7060", "Java Gateway"],
    ),
    "app/domain": (
        "领域层 — 意图识别",
        "规则 + LLM 将用户输入映射为 PRODUCT_SEARCH / QUERY_ORDER / REFUND 等。类比 Java 领域服务 + 规则引擎。",
        ["intent/types.py — IntentKind", "intent/rules.py — 关键词规则", "intent/classifier.py — resolve_intent"],
        [
            "graph/nodes.build_context_node → classifier.resolve_intent",
            "resolve_intent → rules 结构匹配 (优先)",
            "resolve_intent → llm_factory.create_memory_llm (兜底分类)",
            "结果写入 state.intent / intent_data → 影响 prompt 与工具强制策略",
        ],
        "flowchart TD\n  user_text --> resolve_intent\n  resolve_intent --> rules\n  resolve_intent --> llm_classify\n  rules --> IntentResult\n  llm_classify --> IntentResult",
        ["memory LLM", "utils/order_ids", "utils/product_consult"],
    ),
    "app/memory": (
        "会话记忆",
        "Redis 热缓存 + MySQL 持久化；超 token 阈值压缩摘要。类比 Redis Cache + JPA Repository + 定时压缩任务。",
        ["session_memory_service", "context_builder.build_agent_messages", "post_turn_service", "compress_service", "token_estimator"],
        [
            "build_context: session_memory.load → context_builder.build_agent_messages → prompt_service",
            "post_turn: post_turn_service.run → session_memory.save",
            "compress: token_estimator 超阈值 → compress_service (LLM 摘要) → assistant_condense",
            "startup: main.lifespan → session_memory.ensure_table",
        ],
        "flowchart LR\n  load --> context_builder --> llm_messages\n  post_turn --> save --> compress",
        ["Redis", "MySQL agent_session_memory", "memory LLM"],
    ),
    "app/mcp": (
        "MCP 工具 Schema（Agent 进程内）",
        "为 LangChain 提供 StructuredTool 定义；实际执行转发到独立 MCP 进程。类比 Feign Client 接口定义。",
        ["tools.py — build_mcp_tools / Pydantic Args"],
        [
            "agent_runtime.bind_agent_llm → build_mcp_tools → llm.bind_tools",
            "LLM 产出 tool_call → mcp_streamable_client.call_tool (非本地 func)",
        ],
        "flowchart LR\n  bind_agent_llm --> build_mcp_tools --> LLM\n  tool_call --> mcp_streamable_client",
        ["MCP Server 独立进程"],
    ),
    "app/mcp_server": (
        "MCP 工具服务（独立进程 :7060）",
        "FastMCP 注册 SEARCH_PRODUCTS / QUERY_ORDERS / PROPOSE_* 等；底层调 mcp_tools_service + Java。",
        ["server.py — @mcp.tool handlers", "__main__.py — python -m app.mcp_server"],
        [
            "mcp_streamable_client.call_tool → server.py handler",
            "handler → mcp_tools_service.tool_* → java_internal_client (/internal/**)",
            "PROPOSE_* → pending_action_service (Redis 存 act token)",
        ],
        "flowchart LR\n  Agent -->|HTTP| mcp_server\n  mcp_server --> mcp_tools_service\n  mcp_tools_service --> java_internal",
        ["Redis", "Java Internal API"],
    ),
    "app/rag": (
        "RAG 检索增强",
        "FAQ/知识库向量检索 + Java 搜索 API + RRF 融合。",
        ["retriever.search_faq", "embedding.embed_text", "rrf 融合"],
        [
            "build_context_node → rag_retriever.search_faq",
            "embed_text → DashScope Embedding (circuit_breaker 保护)",
            "hybrid → java_internal + ES vector + rrf",
        ],
        "flowchart LR\n  build_context --> retriever --> embedding\n  retriever --> ES\n  retriever --> java_internal",
        ["Embedding API", "Elasticsearch", "circuit_breaker"],
    ),
    "app/harness": (
        "护栏与指标",
        "输入/输出/工具/商品文本校验 + Prometheus 指标。类比 Spring AOP + Micrometer。",
        ["input_guard", "output_guard", "tool_guard", "product_text_guard", "runtime_sensors"],
        [
            "send_message → input_guard.detect_injection",
            "finalize_node → output_guard.validate_no_false_completion",
            "mcp_tool_router → tool_guard.is_allowed",
            "finalize_agent_response → product_text_guard",
            "classifier/router/runtime → runtime_sensors (Prometheus)",
        ],
        "flowchart TD\n  input_guard --> graph\n  graph --> output_guard\n  tool_guard --> mcp_router",
        ["Prometheus /metrics", "settings 开关"],
    ),
    "app/utils": (
        "无状态工具库",
        "卡片 JSON、订单号抽取、Prompt 边界、WS Token。类比 Java util / helper 包。",
        ["product_consult", "biz_payload", "order_ids", "prompt_boundary", "ws_token"],
        [
            "agent_service → product_consult.parse_consult_card",
            "context_builder → prompt_boundary.isolate_user_message",
            "nodes/finalize → biz_payload / order_ids",
            "websocket → ws_token.resolve_ws_token",
        ],
        "flowchart LR\n  agent_service --> product_consult\n  finalize --> biz_payload",
        [],
    ),
}

RAGENT_PYTHON = {
    "app/api": (
        "HTTP / SSE 接口层",
        "RAG 聊天 SSE、知识库 CRUD、摄取、意图树、认证。类比 Spring MVC + SseEmitter。",
        ["routes/chat.py", "routes/knowledge*.py", "routes/ingestion.py", "exception_handlers.py"],
        [
            "main.create_app → include_router(api/*)",
            "GET /rag/v3/chat → chat.py::chat_sse → rag_chat_service.stream_chat",
            "POST stop → task_manager.cancel",
            "各 CRUD 路由 → knowledge/ingestion services + require_login",
        ],
        "flowchart LR\n  chat_sse --> rag_chat_service\n  rag_chat_service --> sse_handler\n  knowledge_routes --> document_service",
        ["FastAPI", "SQLAlchemy AsyncSession"],
    ),
    "app/graph": (
        "RAG Multi-Agent 编排",
        "LangGraph 或 Supervisor 模式：memory → rewrite → intent → guidance → system/retrieve → writer。",
        ["runner.py", "builder.py", "nodes.py", "agents/*.py", "state.ChatGraphState"],
        [
            "rag_chat_service → runner.run_chat_pipeline",
            "[graph] builder.build_chat_graph → ainvoke: memory→rewrite→intent→guidance→system→retrieve→writer",
            "[supervisor] RagSupervisor.execute 顺序等价",
            "guidance route=guidance → 短路 END",
            "retrieve route=empty → END; route=stream_rag → writer → LLM stream",
        ],
        "flowchart TD\n  memory --> rewrite --> intent --> guidance\n  guidance -->|continue| system\n  system --> retrieve --> writer",
        ["langgraph", "LLM"],
    ),
    "app/services": (
        "业务服务",
        "RAG 聊天主编排、会话、认证、反馈、设置。",
        ["rag_chat_service", "conversation_service", "auth_service", "settings_service"],
        [
            "stream_chat: id_generator → chat_queue.enqueue → StreamChatEventHandler → trace_runner → run_chat_pipeline",
            "finish_success: conversation_memory.append → summary_service.compress_if_needed",
        ],
        "flowchart LR\n  rag_chat_service --> chat_queue --> graph_runner\n  finish --> conversation_memory",
        ["asyncio", "PostgreSQL"],
    ),
    "app/sse": (
        "SSE 流式推送",
        "meta/message/finish/done/error 事件；任务注册与取消。",
        ["stream_chat_handler", "task_manager", "emitter"],
        [
            "handler.initialize → task_manager.register",
            "LLM delta → emitter.format_sse(message) → StreamingResponse",
            "stop_chat → task_manager.cancel → Redis pub/sub",
            "finish_success → conversation_memory.append + touch",
        ],
        "flowchart LR\n  handler --> emitter --> client\n  task_manager --> redis_cancel",
        ["Redis"],
    ),
    "app/retrieve": (
        "多通道检索",
        "intent_directed / milvus / keyword / es / vector 并行 → dedup → fusion → rerank。",
        ["engine.py", "multi_channel.py", "channels/*", "postprocessors/*"],
        [
            "retrieval_agent.run → engine.retrieve",
            "per sub_question → filters.kb/.mcp → multi_channel.retrieve_knowledge_channels",
            "channels 并行 → dedup → fusion → rerank",
            "MCP 分支 → registry + client.call_tool",
        ],
        "flowchart TD\n  engine --> multi_channel\n  multi_channel --> channels\n  channels --> postprocessors",
        ["pgvector", "Milvus", "ES", "rerank_service", "MCP"],
    ),
    "app/ingestion": (
        "文档摄取 DAG",
        "可配置 pipeline 节点链 + 条件分支。",
        ["engine.py", "nodes_impl", "pipeline_service", "task_service"],
        [
            "api/ingestion → task_service 触发",
            "engine.execute → validate → find_start → execute_chain",
            "condition_evaluator 分支 → NODE_REGISTRY[type].execute",
        ],
        "flowchart LR\n  task --> engine --> nodes_impl",
        ["SQLAlchemy"],
    ),
    "app/knowledge": (
        "知识库管理",
        "文档上传、分块、向量化、定时同步。",
        ["document_service", "chunk_service", "schedule/job"],
        [
            "upload → file_storage → mq producer TOPIC_DOC_CHUNK",
            "consumer → chunk_document → parser → chunk → embedding → pg_store.index",
            "scheduler → remote_file_fetcher → 重分块",
        ],
        "flowchart LR\n  upload --> mq --> chunk --> vector",
        ["MQ Redis Stream", "APScheduler", "core/parser", "vector/pg_store"],
    ),
    "app/memory": (
        "对话记忆",
        "历史加载、持久化、摘要压缩、标题生成。",
        ["conversation_memory", "summary_service", "title_service"],
        [
            "memory_agent → load_and_append_user",
            "finish_success → append assistant → compress_if_needed",
            "reject 排队超时 → title_service.maybe_generate",
        ],
        "flowchart LR\n  memory_agent --> conversation_memory\n  summary_service --> LLM",
        ["PostgreSQL", "Redis 锁", "LLM"],
    ),
    "app/intent": (
        "意图树",
        "LLM 匹配意图节点；歧义引导；KB/MCP 过滤。",
        ["resolver", "tree_cache", "guidance", "filters"],
        [
            "intent_agent → resolver.resolve → tree_cache + llm_service.chat",
            "guidance_agent → detect_ambiguity → route guidance|continue",
            "retrieve → filters.kb / filters.mcp",
        ],
        "flowchart TD\n  resolver --> tree_cache\n  resolver --> LLM\n  guidance --> route",
        ["LLM", "IntentNode 表"],
    ),
    "app/infra": (
        "模型基础设施",
        "LLM/Embedding 统一调用、路由、健康检查、Rerank。",
        ["llm_service", "model_selector", "model_health", "rerank_service"],
        [
            "各模块 → llm_service.chat|stream_chat|embed",
            "model_selector 选模型 → ChatOpenAI streaming",
            "失败 → model_health 切换候选",
        ],
        "flowchart LR\n  caller --> llm_service --> model_selector --> API",
        ["langchain-openai", "OpenAI 兼容 API"],
    ),
}

JAVA_SIMLECT = {
    "Simlect-gateway": (
        "API 网关",
        "Spring Cloud Gateway：路由、鉴权、限流入口。所有前端请求第一站。",
        ["GatewayApplication", "路由配置 application.yml"],
        [
            "浏览器/前端 → Gateway :端口",
            "Gateway → 路由规则 → Simlect-user/product/order/pay/coupon/search",
            "Agent Python 直连或经 Gateway 调 Java /internal/**",
        ],
        "flowchart LR\n  Web --> Gateway\n  Gateway --> User\n  Gateway --> Product\n  Gateway --> Order",
        ["Nacos/配置", "下游微服务"],
    ),
    "Simlect-product": (
        "商品服务",
        "商品 CRUD、SKU、分类、库存查询；Agent 内部读接口 ProductAgentInternalController。",
        ["ProductApplication", "ProductAgentInternalController", "ProductInfoServiceImpl", "ProductInternalService"],
        [
            "C 端: Gateway → ProductController → ProductInfoService → Mapper → MySQL",
            "Agent: python java_internal_client → POST /internal/agent/product/* → ProductAgentInternalController",
            "Agent 热销/搜索: search/hotSale → ProductInfoMapper (orderBy total_sale)",
        ],
        "flowchart LR\n  Gateway --> ProductController\n  AgentPython --> ProductAgentInternal\n  ProductAgentInternal --> Mapper",
        ["MySQL", "Redis 缓存", "MQ 同步搜索/RAG"],
    ),
    "Simlect-order": (
        "订单服务",
        "下单、支付回调、物流、评价；Agent 查单与写操作桥接。",
        ["OrderApplication", "OrderAgentInternalController", "OrderInfoServiceImpl"],
        [
            "用户下单: Gateway → OrderController → OrderInfoService → MQ/库存/优惠券",
            "Agent 查单: MCP tool_query_orders → java_internal → OrderAgentInternalController",
            "Agent 确认: confirmAction → action_execute_service → Gateway /api/order/refund 等",
        ],
        "flowchart LR\n  Gateway --> OrderController\n  Agent --> OrderAgentInternal\n  confirm --> action_execute",
        ["MySQL", "RabbitMQ", "Redis"],
    ),
    "Simlect-user": (
        "用户服务",
        "登录注册、会员、收藏浏览、签到；Agent 查优惠券/用户信息。",
        ["UserApplication", "UserAgentInternalController"],
        [
            "Gateway → UserController → 各 Service → Mapper",
            "Agent: QUERY_USER_COUPONS → UserAgentInternalController",
        ],
        "flowchart LR\n  Gateway --> UserController\n  Agent --> UserAgentInternal",
        ["MySQL", "Redis Token"],
    ),
    "Simlect-coupon": (
        "优惠券服务",
        "发券、抢券、核销；Agent 内部查券。",
        ["CouponApplication", "CouponAgentInternalController"],
        [
            "Gateway → CouponController → DiscountCouponService",
            "Agent 内部接口读用户券列表",
        ],
        "flowchart LR\n  Gateway --> Coupon\n  Agent --> CouponAgentInternal",
        ["MySQL", "Redis"],
    ),
    "Simlect-pay": (
        "支付服务",
        "支付宝/微信渠道、支付单、回调通知。",
        ["PayApplication", "PayInfoService"],
        [
            "Checkout → Gateway → PayController → 渠道 SDK",
            "支付回调 → 更新订单状态 → MQ",
        ],
        "flowchart LR\n  Order --> Pay\n  Pay --> 渠道SDK\n  回调 --> Order",
        ["支付宝/微信 SDK", "MySQL"],
    ),
    "Simlect-common": (
        "公共模块",
        "实体、枚举、ResponseVO、MQ 常量、Redis 组件、通用异常。被各微服务依赖。",
        ["entity/po", "entity/vo/ResponseVO", "constants", "component/RedisComponent"],
        [
            "各微服务 pom 依赖 common",
            "Controller 返回 ResponseVO<T>",
            "ReliableMessageSender / TransactionalMqSender 跨服务消息",
        ],
        "flowchart TD\n  common --> product\n  common --> order\n  common --> user",
        [],
    ),
}

JAVA_RAGENT = {
    "bootstrap": (
        "Ragent Java 主应用",
        "Spring Boot 启动；RAG StreamChat Pipeline；与 ragent-python 功能对齐的 Java 版入口。",
        ["RagentApplication", "StreamChatPipeline", "StreamChatContext"],
        [
            "HTTP SSE 聊天 → StreamChatPipeline.execute",
            "Pipeline → 各 Java Agent/Retriever 组件",
            "可与 ragent-python 二选一部署",
        ],
        "flowchart LR\n  Client --> StreamChatPipeline\n  Pipeline --> Retriever\n  Pipeline --> LLM",
        ["PostgreSQL", "Milvus", "LLM"],
    ),
    "infra-ai": (
        "AI 基础设施",
        "LLM/Embedding/Rerank/VLM 多模型路由与健康检查。",
        ["LLMService", "RoutingLLMService", "EmbeddingService", "RerankService", "ModelSelector"],
        [
            "业务层 → RoutingLLMService.chat/stream",
            "ModelSelector 选目标 → ModelRoutingExecutor",
            "失败 → ModelHealthStore 标记降级",
        ],
        "flowchart LR\n  Biz --> RoutingLLM\n  RoutingLLM --> ModelSelector\n  ModelSelector --> Provider",
        ["OpenAI 兼容 API", "Ollama", "SiliconFlow", "百炼"],
    ),
    "mcp-server": (
        "Ragent MCP 服务",
        "独立 MCP Server：天气/工单/销售等示例 Executor。",
        ["McpServerApplication", "WeatherMcpExecutor", "TicketMcpExecutor", "SalesMcpExecutor"],
        [
            "MCP Client → McpServerConfig 注册工具",
            "call_tool → 对应 McpExecutor.execute",
        ],
        "flowchart LR\n  MCPClient --> McpServer\n  McpServer --> Executor",
        ["HTTP MCP 协议"],
    ),
}

def write_modules(base: Path, modules: dict, prefix_note: str = "") -> int:
    count = 0
    for rel, (title, purpose, keys, chains, mermaid, deps) in modules.items():
        path = base / rel / "MODULE.md"
        path.parent.mkdir(parents=True, exist_ok=True)
        extra = f"\n## 说明\n\n{prefix_note}\n" if prefix_note else ""
        path.write_text(md(title, purpose, keys, chains, deps, mermaid, extra), encoding="utf-8")
        count += 1
    return count

RAGENT_PYTHON.update({
    "app/auth": (
        "认证与鉴权",
        "Token 签发/校验；FastAPI Depends 注入。类比 Spring Security + Sa-Token。",
        ["deps.py — require_login / require_admin", "token_service.py"],
        [
            "api/routes Depends(require_login) → deps.require_login",
            "token_service.resolve_token → Redis token→userId",
            "framework/context.set_user",
        ],
        "flowchart LR\n  route --> deps --> token_service --> redis",
        ["Redis"],
    ),
    "app/db": (
        "数据访问",
        "PostgreSQL 异步连接池 + ORM 实体 + Redis 客户端。",
        ["session.py", "redis_client.py", "models/entities.py"],
        [
            "main.lifespan → get_redis / close_db",
            "api → get_db / get_session_factory",
            "services → AsyncSession CRUD",
        ],
        "flowchart LR\n  api --> session --> postgres\n  sse --> redis_client",
        ["PostgreSQL asyncpg", "Redis"],
    ),
    "app/mq": (
        "消息队列",
        "Redis Stream 异步任务：文档分块、KB 清理、反馈。",
        ["producer.py", "consumers.py"],
        [
            "document_service.upload → producer.send(TOPIC_DOC_CHUNK)",
            "consumer → chunk_document / delete_collection / feedback",
        ],
        "flowchart LR\n  producer --> redis_stream --> consumer",
        ["Redis Stream"],
    ),
    "app/mcp": (
        "MCP 工具客户端",
        "工具注册表 + 远程 call_tool。",
        ["registry.py", "client.py"],
        [
            "lifespan → registry.refresh",
            "retrieve/engine → registry.get → client.call_tool",
        ],
        "flowchart LR\n  retrieve --> registry --> mcp_client",
        ["MCP HTTP Server"],
    ),
    "app/prompt": (
        "Prompt 模板",
        "加载 .st 模板、格式化 KB/MCP 上下文。",
        ["service.py", "template_loader.py", "context_formatter.py"],
        [
            "writer_agent → prompt/service.build_structured_messages",
            "retrieve → context_formatter.format_kb_context",
        ],
        "flowchart LR\n  writer --> prompt_service --> template_loader",
        [],
    ),
    "app/trace": (
        "RAG 链路追踪",
        "Run/Node 持久化、TTFT、装饰器埋点。",
        ["stream_chat_trace_runner.py", "service.py", "decorator.py"],
        [
            "rag_chat_service → trace_runner.run → start_run",
            "decorator.rag_trace_node 包裹各 Agent 节点",
            "finish_run → PostgreSQL RagTraceRun",
        ],
        "flowchart LR\n  chat --> trace_runner --> trace_service --> DB",
        ["PostgreSQL"],
    ),
    "app/vector": (
        "向量存储",
        "pgvector 索引与检索；可选 Milvus。",
        ["pg_store.py", "milvus_store.py"],
        [
            "chunk_document → pg_store.index_document_chunks",
            "pg_retriever → similarity_search",
        ],
        "flowchart LR\n  chunk --> pg_store\n  retrieve --> pg_store",
        ["pgvector", "Milvus 可选"],
    ),
    "app/rewrite": (
        "Query 改写",
        "术语归一化 + LLM 拆分子问题。",
        ["service.py — rewrite_with_split"],
        [
            "rewrite_agent.run → rewrite/service.rewrite_with_split",
            "normalize(QueryTermMapping) → llm JSON → RewriteResult",
            "intent_agent 消费 sub_questions",
        ],
        "flowchart LR\n  rewrite_agent --> rewrite_service --> LLM",
        ["LLM", "QueryTermMapping 表"],
    ),
    "app/ratelimit": (
        "限流排队",
        "Redis Lua 公平限流 + 聊天队列。",
        ["fair_limiter.py", "chat_queue.py"],
        [
            "stream_chat → chat_queue.enqueue → fair_limiter.acquire",
            "超时 → reject SSE + title_service",
        ],
        "flowchart LR\n  chat_queue --> fair_limiter --> redis_lua",
        ["Redis Lua"],
    ),
    "app/admin": (
        "管理仪表盘",
        "RAG 指标 SQL 聚合。",
        ["dashboard.py"],
        [
            "rag_ops (require_admin) → dashboard_service SQL 统计",
        ],
        "flowchart LR\n  rag_ops --> dashboard --> SQL",
        ["PostgreSQL"],
    ),
    "app/core": (
        "文档解析分块",
        "入库专用：parser → chunk → embedding。",
        ["parser/selector.py", "chunk/strategies.py", "chunk/embedding_service.py"],
        [
            "document_service.chunk_document → parser → strategies.split → embedding_service",
        ],
        "flowchart LR\n  document --> parser --> chunk --> embedding",
        ["LLM Embedding"],
    ),
    "app/framework": (
        "公共框架",
        "Result、异常、分页、ID、请求上下文。",
        ["result.py", "exceptions.py", "id_generator.py", "context.py"],
        [
            "routes → result.success|fail",
            "rag_chat_service → id_generator.next_id",
        ],
        "flowchart LR\n  api --> framework_result",
        [],
    ),
    "app/config": (
        "配置中心",
        "Pydantic Settings 绑定 .env。类比 @ConfigurationProperties。",
        ["settings.py — get_settings()"],
        ["全模块 → get_settings() → database_url / redis / llm / rag_pipeline_mode"],
        "flowchart LR\n  modules --> settings --> env",
        [".env"],
    ),
    "app/audit": (
        "业务变更审计",
        "BizChangeLog 记录。",
        ["service.py — BizChangeLogService"],
        ["knowledge/ingestion 变更 → audit/service 写日志"],
        "flowchart LR\n  biz --> audit_service --> DB",
        ["PostgreSQL"],
    ),
})

SIMLECT_AGENT.update({
    "app/config": (
        "配置中心",
        "Pydantic Settings：LLM/Redis/MySQL/ES/Java/MCP/RAG/熔断/限流。",
        ["settings.py — get_settings()"],
        ["全模块 import get_settings → 读 .env"],
        "flowchart LR\n  all --> get_settings --> env",
        [".env"],
    ),
    "app/models": (
        "DTO 模型",
        "ResponseVO、MessageSendDTO。类比 Java VO/DTO。",
        ["response.py", "message_send.py"],
        [
            "api/routes → success/error → ResponseVO",
            "stream_service → MessageSendDTO → redis publish_ws",
        ],
        "flowchart LR\n  api --> ResponseVO\n  stream --> MessageSendDTO",
        [],
    ),
    "app/resilience": (
        "熔断器",
        "Embedding/RAG 外部调用保护。类比 Resilience4j。",
        ["circuit_breaker.py — CircuitRegistry"],
        [
            "rag/embedding.embed_text → circuit_registry.get('embedding')",
            "连续失败 → OPEN → 短路",
        ],
        "flowchart LR\n  embed --> circuit_breaker",
        ["settings 阈值"],
    ),
})

JAVA_SIMLECT["Simlect-search"] = (
    "搜索服务",
    "Elasticsearch 商品/FAQ 索引与检索；Agent RAG 也会调 ES。",
    ["SearchApplication", "搜索 Controller/Service"],
    [
        "商品变更 MQ → 同步 ES 索引",
        "Agent rag/retriever → ES vector + keyword",
        "商城搜索 Portal → Gateway → Search",
    ],
    "flowchart LR\n  MQ --> Search --> ES\n  Agent --> ES",
    ["Elasticsearch"],
)
JAVA_SIMLECT["Simlect-stock"] = (
    "库存服务",
    "SKU 库存扣减/回滚；下单链路依赖。",
    ["StockApplication"],
    [
        "下单 → Order → MQ → Stock 扣减",
        "取消/退款 → 库存回滚",
    ],
    "flowchart LR\n  Order --> MQ --> Stock",
    ["MySQL", "RabbitMQ"],
)

FRONTEND = {
    "Simlect-front/Simlect-web/src/components/agent": (
        "Simlect 商城 Agent 前端组件",
        "聊天气泡、商品/订单/确认卡片渲染；WebSocket 流式更新。",
        ["AgentChatItem.vue — 主消息渲染", "AgentProductList.vue", "AgentOrderList.vue", "AgentConfirmCard.vue"],
        [
            "AIAssistantView → AgentChatList → AgentChatItem",
            "stores/agentMessage → WebSocket manager → push chunk/done",
            "agentMessageRender.ts 解析 biz_type → 卡片组件",
            "confirmAction → POST /api/agent/confirmAction",
        ],
        "flowchart LR\n  WS --> agentMessage store --> AgentChatItem\n  AgentChatItem --> ProductList\n  AgentChatItem --> OrderList",
        ["Simlect-agent Python API", "WebSocket /ws"],
    ),
    "Simlect-front/Simlect-web/src/utils": (
        "前端工具（Agent 相关）",
        "消息渲染、商品咨询、WebSocket。",
        ["agentMessageRender.ts", "agentProductConsult.ts", "websocket/manager.ts"],
        [
            "AgentChatItem → agentMessageRender 解析 JSON/intro",
            "发送消息 → build consult card → agentProductConsult",
            "websocket/manager 连接 /ws + 心跳",
        ],
        "flowchart LR\n  render --> AgentChatItem\n  ws --> store",
        [],
    ),
    "ragent/frontend/src": (
        "Ragent React 前端",
        "SSE 流式聊天 + 管理后台。",
        ["pages/ChatPage.tsx", "hooks/useStreamResponse.ts", "services/chatService.ts"],
        [
            "ChatPage → useChat → chatService.stream (EventSource /fetch SSE)",
            "useStreamResponse 解析 meta/message/finish/done",
            "AdminLayout → knowledge/ingestion/intent 各 Page → services/*",
        ],
        "flowchart LR\n  ChatPage --> chatService --> SSE\n  Admin --> api",
        ["ragent-python /api/ragent"],
    ),
}

def write_frontend_modules(base: Path, modules: dict) -> int:
    count = 0
    for rel, data in modules.items():
        title, purpose, keys, chains, mermaid, deps = data
        path = base / rel / "MODULE.md"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(md(title, purpose, keys, chains, deps, mermaid), encoding="utf-8")
        count += 1
    return count

def main() -> None:
    n = 0
    agent_base = ROOT / "Simlect" / "Simlect-backend" / "Simlect-agent"
    n += write_modules(
        agent_base,
        SIMLECT_AGENT,
        "Python 模块：async≈CompletableFuture，dict≈Map，本文件描述模块间**调用链**，代码内 `# [zh]` 为逐行注释。",
    )
    ragent_base = ROOT / "ragent-python"
    n += write_modules(
        ragent_base,
        RAGENT_PYTHON,
        "FastAPI≈Spring Boot；SQLAlchemy≈JPA；Pydantic≈DTO。",
    )
    for mod, data in JAVA_SIMLECT.items():
        path = ROOT / "Simlect" / "Simlect-backend" / mod / "MODULE.md"
        path.parent.mkdir(parents=True, exist_ok=True)
        title, purpose, keys, chains, mermaid, deps = data
        path.write_text(md(title, purpose, keys, chains, deps, mermaid), encoding="utf-8")
        n += 1
    for mod, data in JAVA_RAGENT.items():
        path = ROOT / "ragent" / mod / "MODULE.md"
        path.parent.mkdir(parents=True, exist_ok=True)
        title, purpose, keys, chains, mermaid, deps = data
        path.write_text(md(title, purpose, keys, chains, deps, mermaid), encoding="utf-8")
        n += 1
    n += write_frontend_modules(ROOT / "Simlect", {k: v for k, v in FRONTEND.items() if k.startswith("Simlect")})
    n += write_frontend_modules(ROOT, {k: v for k, v in FRONTEND.items() if k.startswith("ragent")})
    # 总览
    overview = agent_base / "docs" / "ARCHITECTURE_CALL_CHAINS.md"
    overview.parent.mkdir(parents=True, exist_ok=True)
    overview.write_text(
        """# Simlect + Ragent 全栈调用链总览

## Simlect 商城 + Agent

```mermaid
flowchart TB
  subgraph frontend [Simlect-web Vue]
    UI[AgentChatItem / WebSocket]
  end
  subgraph agent_py [Simlect-agent Python :7050]
    API[api/routes/agent.py]
    SVC[services/agent_service]
    ENG[services/agent_engine]
    GRP[graph/runner + nodes]
    RT[agent_runtime]
    MCPc[mcp_streamable_client]
  end
  subgraph mcp_proc [MCP Server :7060]
    MS[mcp_server/server.py]
    MT[mcp_tools_service]
  end
  subgraph java [Simlect Java 微服务]
    GW[Gateway]
    PR[product/internal]
    OR[order/internal]
    US[user/internal]
  end
  UI -->|POST sendMessage| API
  UI -->|WS| API
  API --> SVC --> ENG --> GRP
  GRP --> RT
  GRP --> MCPc --> MS --> MT
  MT -->|/internal/**| PR
  MT -->|/internal/**| OR
  MT -->|/internal/**| US
  UI -->|confirmAction| API -->|/api/order/*| GW
```

## Ragent RAG

```mermaid
flowchart TB
  FE[ragent/frontend React]
  CHAT[api/routes/chat.py SSE]
  RCS[rag_chat_service]
  RUN[graph/runner]
  RET[retrieve/engine]
  LLM[infra/llm_service]
  FE --> CHAT --> RCS --> RUN
  RUN --> RET --> LLM
```

## 各模块详细文档

- Simlect-agent: 各 `app/*/MODULE.md`
- ragent-python: 各 `app/*/MODULE.md`
- Simlect Java: 各 `Simlect-*/MODULE.md`
- Ragent Java: `bootstrap/`, `infra-ai/`, `mcp-server/` 下 `MODULE.md`

生成命令: `python Simlect-agent/tools/generate_module_docs.py`
""",
        encoding="utf-8",
    )
    print(f"Generated {n} MODULE.md + 1 overview")

if __name__ == "__main__":
    main()
