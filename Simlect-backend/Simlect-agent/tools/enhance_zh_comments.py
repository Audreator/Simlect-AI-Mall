#!/usr/bin/env python3
"""第二遍：将通用

from __future__ import annotations

import re
from pathlib import Path

MARKER = "# [zh]"

# 下一行代码 -> 详细注释（精确匹配 strip 后）
LINE_HINTS: dict[str, str] = {
    # --- AgentGraphState 字段 ---
    "agent_msg: dict": "原始 Agent 消息体 Map（类比 Java Map<String,Object> DTO）",
    "user_id: str": "当前用户 ID（类比 Java String userId）",
    "message_id: int": "当前消息 ID（类比 Java Long messageId）",
    "user_message: str": "用户原始消息文本（未解析卡片）",
    "user_text: str": "解析后的纯文本用户输入",
    "from_product: bool": "是否来自商品详情页入口（类比 boolean fromProduct）",
    "card: dict | None": "当前咨询商品卡片 Map，可空（类比 Optional<ProductCard>）",
    "message_card: dict | None": "本条消息附带的商品卡片，可空",
    "cancelled: bool": "是否已被用户取消（类比 AtomicBoolean cancelled）",
    "finished: bool": "图执行是否已结束",
    "route: RouteKind": "下一跳路由：agent_loop/tools/finalize/post_turn/end",
    "llm_messages: list[BaseMessage]": "送入 LLM 的消息列表（类比 List<ChatMessage>）",
    "working_turns: list[dict]": "工作记忆回合列表（类比 List<TurnDTO>）",
    "working_oldest_id: int | None": "工作记忆最旧消息 ID，用于压缩边界",
    "chunks: Annotated[list[str], operator.add]": "流式输出片段，LangGraph 用 operator.add 合并（类比 List 累加）",
    "react_round: int": "ReAct 循环轮次计数",
    "tools_called: Annotated[list[str], operator.add]": "已调用工具名列表，图状态累加",
    "pending_tool_calls: list[dict]": "待执行的 tool_call 列表（类比 List<ToolCall>）",
    "tool_biz: dict | None": "工具返回的业务 Map 聚合",
    "biz_type: str | None": "业务类型：query_order/product_search/action_confirm 等",
    "biz_data: str | None": "业务数据 JSON 或 ID 字符串",
    "assistant_cards: str | None": "助手卡片 JSON 字符串（订单/商品卡片）",
    "search_tool_hint: str | None": "SEARCH_PRODUCTS 工具返回的提示文本",
    "search_fallback_done: bool": "是否已执行搜索兜底（LLM 未调工具时）",
    "category_switch_search: bool": "是否检测到品类切换/新搜索意图",
    "intent: str | None": "解析后的意图枚举值字符串",
    "intent_data: str | None": "意图附带数据：订单号/关键词等",
    # initial_state dict keys
    '"agent_msg": agent_msg,': "写入原始消息 Map",
    '"user_id": agent_msg["userId"],': "从消息体提取 userId",
    '"message_id": agent_msg["messageId"],': "从消息体提取 messageId",
    '"user_message": agent_msg.get("userMessage") or "",': "用户消息，缺省为空串",
    '"user_text": user_text,': "解析后的用户文本",
    '"from_product": bool(agent_msg.get("fromProduct")),': "是否商品详情入口",
    '"card": card,': "咨询卡片",
    '"message_card": card,': "消息卡片副本",
    '"cancelled": False,': "初始未取消",
    '"finished": False,': "初始未完成",
    '"route": "agent_loop",': "默认进入 agent_loop 节点",
    '"llm_messages": [],': "空 LLM 消息列表",
    '"working_turns": [],': "空工作记忆",
    '"working_oldest_id": None,': "尚无最旧 ID",
    '"chunks": [],': "空流式片段",
    '"react_round": 0,': "ReAct 从第 0 轮开始",
    '"tools_called": [],': "尚未调用工具",
    '"pending_tool_calls": [],': "无待执行工具",
    '"tool_biz": None,': "无工具业务数据",
    '"biz_type": None,': "无业务类型",
    '"biz_data": None,': "无业务数据",
    '"assistant_cards": None,': "无助手卡片",
    '"search_tool_hint": None,': "无搜索提示",
    '"search_fallback_done": False,': "尚未搜索兜底",
    '"category_switch_search": False,': "初始非品类切换",
    '"intent": None,': "意图待 build_context 填充",
    '"intent_data": None,': "意图数据待填充",
    # constants
    "CONSULT_PRODUCT_TTL = 24 * 60 * 60": "咨询商品 Redis TTL：24 小时（秒）",
    "CONSULT_ACTIVE_TTL = 30 * 60": "活跃咨询会话 TTL：30 分钟",
    "PENDING_MSG_TTL = 5 * 60": "待处理消息 TTL：5 分钟",
    "REDIS_HEARTBEAT_TTL = 6": "WebSocket 心跳 Redis TTL：6 秒",
    "ORDER_STATUS_DELETE = -1": "订单状态：已删除（-1）",
    "PRODUCT_STATUS_ON_SALE = 1": "商品状态：在售（1）",
    "ORDER_STATUS_WAIT_PAYMENT = 0": "订单状态：待付款（0）",
    "ORDER_STATUS_PAID = 1": "订单状态：已付款待发货（1）",
    "ORDER_STATUS_SHIPPED = 2": "订单状态：已发货（2）",
    "ORDER_STATUS_COMPLETED = 3": "订单状态：已完成（3）",
    "ORDER_STATUS_CANCELLED = 4": "订单状态：交易取消（4）",
    "ORDER_STATUS_CLOSED = 5": "订单状态：交易关闭（5）",
    "ORDER_STATUS_REFUNDED = 6": "订单状态：已退款关闭（6）",
    "ORDER_STATUS_PARTIALLY_REFUNDED = 7": "订单状态：部分退款（7）",
    "ORDER_STATUS_WAIT_COMMENT = 8": "订单状态：待评价（8）",
    "ORDER_ITEM_STATUS_REFUND = 0": "订单项状态：已退款（0）",
    "ORDER_ITEM_STATUS_NORMAL = 1": "订单项状态：正常（1）",
    "REFUNDABLE_ORDER_STATUSES = frozenset({": "可退款订单状态集合（不可变 Set，类比 EnumSet）",
    "CONFIRM_RECEIPT_ORDER_STATUSES = frozenset({": "可确认收货的订单状态集合",
    "REVIEWABLE_ORDER_STATUSES = frozenset({": "可评价的订单状态集合",
    "ORDER_STATUS_NAMES = {": "订单状态码 -> 中文名 Map（类比 Map<Integer,String>）",
    "ORDER_STATUS_PAID,": "包含：已付款待发货",
    "ORDER_STATUS_SHIPPED,": "包含：已发货",
    "ORDER_STATUS_PARTIALLY_REFUNDED,": "包含：部分退款",
    "ORDER_STATUS_COMPLETED,": "包含：已完成",
    '-1: "已删除",': "状态 -1 显示文案",
    '0: "待付款",': "状态 0 显示文案",
    '1: "已付款,待发货",': "状态 1 显示文案",
    '2: "已发货",': "状态 2 显示文案",
    '3: "已完成",': "状态 3 显示文案",
    '4: "交易取消",': "状态 4 显示文案",
    '5: "交易关闭",': "状态 5 显示文案",
    '6: "已退款,交易关闭",': "状态 6 显示文案",
    '7: "部分退款",': "状态 7 显示文案",
    '8: "待评价",': "状态 8 显示文案",
    # settings fields
    "app_host: str = \"0.0.0.0\"": "FastAPI 监听地址（类比 server.address）",
    "app_port: int = 7050": "FastAPI 监听端口",
    "java_web_url: str = \"http://localhost:8080\"": "Java 商城后端 base URL（内部 HTTP 调用）",
    'default="http://127.0.0.1:7060",': "MCP 工具服务默认地址",
    'default="your-token",': "内部服务鉴权 token 默认值",
    "llm_api_key: str = \"\"": "主 LLM API Key（DeepSeek 等）",
    "llm_base_url: str = \"https://api.deepseek.com\"": "主 LLM OpenAI 兼容 base URL",
    "llm_model: str = \"deepseek-chat\"": "主 LLM 模型名",
    "llm_fallback_model: str = \"deepseek-chat\"": "LLM 降级模型名",
    "llm_timeout: int = 60": "LLM 请求超时秒数",
    "llm_max_retries: int = 3": "LLM 最大重试次数",
    "memory_llm_api_key: str = \"\"": "记忆压缩专用 LLM API Key",
    "memory_llm_base_url: str = \"\"": "记忆 LLM base URL，空则复用主 LLM",
    "memory_llm_model: str = \"\"": "记忆 LLM 模型名",
    "memory_llm_timeout: int | None = None": "记忆 LLM 超时，可空",
    "embedding_api_key: str = \"\"": "向量 Embedding API Key",
    "embedding_base_url: str = \"https://dashscope.aliyuncs.com/compatible-mode/v1\"": "Embedding 服务地址（通义）",
    "embedding_model: str = \"text-embedding-v4\"": "Embedding 模型名",
    "embedding_dimensions: int = 1024": "向量维度，需与 ES 索引一致",
    "mysql_host: str = \"localhost\"": "MySQL 主机",
    "mysql_port: int = 3306": "MySQL 端口",
    "mysql_user: str = \"root\"": "MySQL 用户名",
    "mysql_password: str = \"123456\"": "MySQL 密码",
    "mysql_database: str = \"simlect_agent\"": "MySQL 库名",
    "redis_host: str = \"127.0.0.1\"": "Redis 主机",
    "redis_port: int = 6379": "Redis 端口",
    "redis_db: int = 0": "Redis 逻辑库编号",
    "es_hosts: str = \"http://localhost:9200\"": "Elasticsearch 集群地址",
    "es_index: str = \"simlect_vectorstore\"": "向量索引名",
    "es_vector_dimensions: int = 1024": "ES 向量字段维度",
    "rabbitmq_url: str = \"amqp://guest:guest@localhost:5672/\"": "RabbitMQ 连接 URI",
    "ai_chat_limit: int = 200": "单用户 AI 聊天频率限制",
    "rag_top_k: int = 15": "RAG 检索 top-K 条数",
    "rag_score_threshold: float = 0.5": "RAG 相似度阈值",
    "history_message_limit: int = 15": "加载历史消息条数上限",
    "task_queue_max: int = 300": "异步任务队列最大深度",
    "session_redis_ttl: int = 86400": "会话 Redis 过期秒数（24h）",
    "session_compress_lock_ttl: int = 60": "会话压缩分布式锁 TTL",
    "working_token_budget: int = 100_000": "工作记忆 token 预算上限",
    "compress_token_threshold: int = 100_000": "触发压缩的 token 阈值",
    "assistant_history_max_len: int = 500": "助手历史文本最大字符数",
    "circuit_llm_failure_threshold: int = 5": "LLM 熔断：连续失败次数阈值",
    "circuit_llm_recovery_timeout: int = 60": "LLM 熔断：恢复探测间隔秒",
    "graph_max_react_rounds: int = 5": "LangGraph ReAct 最大轮数",
    "graph_checkpoint_ttl: int = 3600": "图 checkpoint Redis TTL 秒",
    'graph_checkpoint_prefix: str = "mall:agent:graph:ckpt"': "checkpoint Redis key 前缀",
    "intent_use_llm: bool = True": "是否启用 LLM 意图分类",
    "intent_rule_fallback: bool = True": "LLM 失败时是否回退规则引擎",
    "order_query_lookback_days: int = 90": "查订单默认回溯天数",
    "force_mcp_on_llm_skip: bool = False": "LLM 未调工具时是否强制 MCP 兜底",
    "env_file=\".env\",": "从 .env 文件加载配置（类比 application.properties）",
    "env_file_encoding=\"utf-8\",": "env 文件 UTF-8 编码",
    'extra="ignore",': "忽略未声明的环境变量",
    "populate_by_name=True,": "允许字段名与 alias 双向绑定",
    'f"mysql+aiomysql://{self.mysql_user}:{self.mysql_password}"': "DSN 用户名密码段（aiomysql 异步驱动）",
    'f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_database}"': "DSN 主机端口库名段",
    'return f"redis://{self.redis_host}:{self.redis_port}/{self.redis_db}"': "拼接 Redis URL（类比 Lettuce URI）",
    # Redis keys detail
    'REDIS_TOKEN_WEB = f"{REDIS_PREFIX}token:web:"': "Web 端 token -> session 映射 key 前缀",
    'REDIS_TOKEN_USERID_WEB = f"{REDIS_PREFIX}token:web:userId:"': "Web token -> userId 反向索引",
    'REDIS_CANCEL_AGENT = f"{REDIS_PREFIX}cancel:agent:message:userId:"': "用户取消 Agent 生成标志 key",
    'REDIS_PROMPT = f"{REDIS_PREFIX}prompt:"': "Prompt 模板缓存 key 前缀",
    'REDIS_AGENT_CONSULT_PRODUCT = f"{REDIS_PREFIX}agent:consult:product:userId:"': "用户当前咨询商品 key",
    'REDIS_AGENT_CONSULT_ACTIVE = f"{REDIS_PREFIX}agent:consult:active:"': "活跃咨询会话标记 key",
    'REDIS_AGENT_PENDING_ACTION = f"{REDIS_PREFIX}agent:pending:action:"': "待用户确认的操作（退款/评价等）",
    'REDIS_AGENT_PENDING_MSG = f"{REDIS_PREFIX}agent:pending:msg:userId:"': "待关联 messageId 的临时消息",
    'REDIS_AGENT_SESSION = f"{REDIS_PREFIX}agent:session:"': "Agent 会话记忆 JSON key",
    'REDIS_AGENT_SESSION_COMPRESS_LOCK = f"{REDIS_PREFIX}agent:session:compress:lock:"': "会话压缩互斥锁 key",
    'REDIS_AGENT_HISTORY_CONDENSED = f"{REDIS_PREFIX}agent:history:condensed:userId:"': "已压缩历史摘要 key",
    'REDIS_SENSITIVE_WORD_PAYLOAD = f"{REDIS_PREFIX}sensitive:word:payload"': "敏感词库 payload 缓存 key",
    'REDIS_WS_USER_HEARTBEAT = f"{REDIS_PREFIX}user:heartBeat:"': "WebSocket 用户心跳 key 前缀",
    'RouteKind = Literal["agent_loop", "tools", "finalize", "post_turn", "end"]': "图路由字面量类型（类比 Java sealed enum）",
}

GENERIC_PATTERNS = ("可执行语句", "赋值语句", "赋值 `", "复合语句块起始")

def enhance_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    changed = False
    out: list[str] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.strip().startswith(MARKER) and i + 1 < len(lines):
            comment_body = line.strip()[len(MARKER) :].strip()
            next_line = lines[i + 1].strip()
            is_generic = any(p in comment_body for p in GENERIC_PATTERNS)
            if is_generic and next_line in LINE_HINTS:
                indent = re.match(r"^(\s*)", line).group(1)
                out.append(f"{indent}{MARKER} {LINE_HINTS[next_line]}\n")
                changed = True
                i += 1
                continue
        out.append(line)
        i += 1
    if changed:
        path.write_text("".join(out), encoding="utf-8")
    return changed

if __name__ == "__main__":
    import sys
    from pathlib import Path as P

    dirs = [
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\graph"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\domain"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\api"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\utils"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\memory"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\harness"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\mcp"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\rag"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\config"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\models"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\resilience"),
        P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\mcp_server"),
    ]
    files = []
    for d in dirs:
        files.extend(d.rglob("*.py"))
    files.append(P(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\constants.py"))
    n = sum(enhance_file(p) for p in sorted(set(files)))
    print(f"enhanced {n} files")
