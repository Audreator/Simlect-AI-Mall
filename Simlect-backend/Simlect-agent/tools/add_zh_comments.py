#!/usr/bin/env python3
"""为 Python 源码逐行追加详细中文注释（含 Java 类比），不修改逻辑。"""

from __future__ import annotations

import re
import sys
from pathlib import Path

MARKER = "# [zh]"

IMPORT_HINTS = {
    "asyncio": "引入 asyncio 异步 I/O 库（类比 Java CompletableFuture / Virtual Thread 调度器）",
    "operator": "引入 operator 模块，提供 add 等归约函数（类比 Java BinaryOperator）",
    "re": "引入正则模块 re（类比 Java java.util.regex.Pattern）",
    "json": "引入 JSON 编解码（类比 Jackson ObjectMapper read/write）",
    "pickle": "引入 pickle 二进制序列化（类比 Java Serializable + ObjectOutputStream）",
    "structlog": "引入 structlog 结构化日志（类比 SLF4J + MDC 键值对日志）",
    "functools": "引入 functools 高阶函数工具（类比 Java Function.compose / Supplier）",
    "enum": "引入 enum 枚举（类比 Java enum 常量集）",
    "typing": "引入 typing 泛型注解（类比 Java 泛型与 Optional）",
    "typing_extensions": "引入 typing 扩展类型（TypedDict 等，类比 Java record / sealed）",
    "collections": "引入 collections 扩展容器（类比 Guava Multimap 等）",
    "collections.abc": "引入抽象容器协议 Iterator/Sequence（类比 Java Iterable）",
    "pathlib": "引入 Path 路径对象（类比 java.nio.file.Paths）",
    "pydantic": "引入 Pydantic 模型校验（类比 Jakarta Validation + DTO）",
    "pydantic_settings": "引入 Pydantic Settings（类比 Spring @ConfigurationProperties 绑定 .env）",
    "langchain_core": "引入 LangChain 核心消息与 Runnable（类比 Spring AI ChatClient 抽象）",
    "langgraph": "引入 LangGraph 状态图编排（类比 Spring StateMachine / Camunda 流程）",
    "fastapi": "引入 FastAPI Web 框架（类比 Spring Boot @RestController）",
    "starlette": "引入 Starlette ASGI 基础（类比 Servlet 之上的 Web 层）",
    "httpx": "引入 httpx 异步 HTTP 客户端（类比 Java WebClient / OkHttp async）",
    "redis": "引入 Redis 客户端（类比 Jedis / Lettuce）",
    "prometheus_client": "引入 Prometheus 指标（类比 Micrometer Counter/Gauge）",
}

CONSTANT_HINTS = {
    "REDIS_PREFIX": "Redis 全局 key 前缀，避免多服务 key 冲突（类比 Spring cache namespace）",
    "WS_MESSAGE_TOPIC": "WebSocket 广播主题名（类比 JMS Topic / Kafka topic）",
    "WS_MESSAGE_TOPIC_AGENT": "Agent 专用 WebSocket 主题",
    "WS_MESSAGE_TYPE_AGENT": "消息类型标识：agent 通道",
    "TOKEN_COOKIE_NAME": "登录 token 的 Cookie 字段名",
    "TOKEN_HEADER_NAME": "登录 token 的 HTTP Header 字段名",
    "MSG_STATUS_CANCEL": "消息状态：已取消（0）",
    "MSG_STATUS_NORMAL": "消息状态：正常进行中（1）",
    "MSG_STATUS_COMPLETE": "消息状态：已完成（2）",
    "MSG_STATUS_INTERRUPTED": "消息状态：被中断（3）",
    "OUTPUTTING": "输出状态：流式输出中",
    "DONE": "输出状态：已完成",
    "ERROR": "输出状态：出错",
    "RRF_K": "RRF 融合排序常数 k（Reciprocal Rank Fusion 参数）",
    "PRODUCT_RESULT_SIZE": "商品检索最终返回条数",
    "PRODUCT_CANDIDATE_SIZE": "商品检索候选池大小",
    "PENDING_ACTION_TTL": "待确认操作 Redis TTL（秒）",
    "CANCEL_FLAG_TTL": "取消标志 Redis TTL（秒）",
    "ORDER_STATUS": "订单状态码枚举值",
    "INTENT_PROMPT_KEY": "意图 -> Prompt 模板 key 映射（类比 Java Map<Intent, String>）",
}

FUNC_HINTS = {
    "get_settings": "获取全局 Settings 单例（类比 Spring ApplicationContext.getBean + @Cacheable）",
    "get_compiled_graph": "获取编译后的 LangGraph 图（类比单例 Bean 懒加载）",
    "build_agent_graph": "构建 Agent 状态图节点与边（类比 StateMachine 配置）",
    "run_agent_graph": "异步执行 Agent 图主入口（类比 CompletableFuture 编排服务）",
    "initial_state": "构造图初始状态 Map（类比 Builder 模式初始 DTO）",
    "thread_id_for": "生成 checkpoint thread_id（userId:messageId）",
    "resolve_intent": "综合规则+LLM 解析用户意图（类比 Intent Router 服务）",
    "classify_intent_by_rules": "基于关键词规则的意图分类（类比 RuleEngine）",
    "classify_intent_by_llm": "调用 LLM 做意图分类（类比 LLM Chain）",
    "entry_guard": "图入口守卫：解析消息并检查取消（类比 Gateway Filter）",
    "build_context_node": "构建 LLM 上下文与意图（类比 ContextAssembler）",
    "agent_loop_node": "ReAct 主循环：流式 LLM + 工具决策（类比 AgentOrchestrator）",
    "tools_node": "执行 pending 工具调用并合并 biz 结果（类比 ToolExecutor）",
    "finalize_node": "收尾：输出校验、持久化、推送 WS（类比 ResponseFinalizer）",
    "post_turn_node": "回合后记忆压缩与写入（类比 @AfterReturning 切面）",
    "cleanup_node": "清理节点：标记 finished（类比 finally 块）",
    "get_checkpointer": "获取 Redis checkpoint 保存器单例",
    "hydrate_thread": "从 Redis 反序列化 checkpoint 到内存（类比 Cache load）",
    "_persist_thread": "将内存 checkpoint 序列化写入 Redis（类比 Cache put）",
    "success": "构造成功 ResponseVO（类比 Result.ok(data)）",
    "error": "构造失败 ResponseVO（类比 Result.fail(code, msg)）",
    "to_ws_dict": "转为 WebSocket 推送用的 Map（camelCase + messageId 字符串化）",
}

JAVA_ANALOGY = {
    r"\bdict\b": "类比 Java Map<K,V>",
    r"\blist\[": "类比 Java List<T>",
    r"\blist\b": "类比 Java List",
    r"\bset\b": "类比 Java Set<T>",
    r"\bfrozenset\b": "类比 Collections.unmodifiableSet",
    r"\btuple\b": "类比不可变 List / record 组件",
    r"\basync def\b": "类比 @Async CompletableFuture 方法",
    r"\bawait\b": "类比 future.get() 等待异步完成",
    r"\b@property\b": "类比 getter",
    r"\bclassmethod\b": "类比 static 工厂",
    r"\bstaticmethod\b": "类比 static 方法",
    r"\bNone\b": "类比 null",
    r"\| None": "可空，类比 Optional",
    r"TypedDict": "类比 Java record / DTO interface",
    r"Annotated\[": "类比 Java 泛型 + 元数据注解",
    r"Generic\[": "类比 Java 泛型类 ResponseVO<T>",
    r"BaseModel": "类比 Lombok @Data DTO",
    r"Enum\)": "类比 Java enum implements String 接口",
}

def _java_analogy(line: str) -> str:
    found = []
    for pat, hint in JAVA_ANALOGY.items():
        if re.search(pat, line):
            found.append(hint)
    return ("；" + "；".join(dict.fromkeys(found))) if found else ""

def _lhs_name(line: str) -> str | None:
    m = re.match(r"^\s*([A-Za-z_][\w]*)\s*=", line.strip())
    return m.group(1) if m else None

def _comment_for_import(line: str) -> str:
    m = re.match(r"^(?:from\s+([\w.]+)\s+import|import\s+([\w.]+))", line.strip())
    full = (m.group(1) or m.group(2) or "") if m else ""
    root = full.split(".")[0]
    detail = IMPORT_HINTS.get(full) or IMPORT_HINTS.get(root) or f"导入依赖 `{full or line.strip()}`"
    return detail + _java_analogy(line)

def _comment_for_def(line: str) -> str:
    m = re.match(r"^\s*(async\s+def|def)\s+(\w+)", line)
    if not m:
        return "定义函数" + _java_analogy(line)
    name = m.group(2)
    extra = FUNC_HINTS.get(name, "")
    kind = "异步方法" if m.group(1).startswith("async") else "方法"
    base = f"定义{kind} `{name}`"
    if extra:
        base += f"：{extra}"
    return base + _java_analogy(line)

def _comment_for_class(line: str) -> str:
    m = re.match(r"^\s*class\s+(\w+)", line)
    if not m:
        return "类定义" + _java_analogy(line)
    name = m.group(1)
    hints = {
        "Settings": "应用配置类，从环境变量/.env 加载（类比 @ConfigurationProperties）",
        "AgentGraphState": "LangGraph 状态 TypedDict（类比 Workflow 上下文 DTO）",
        "IntentKind": "用户意图枚举（类比 Java enum IntentType）",
        "ResponseVO": "统一 API 响应包装（类比 Result<T> / ApiResponse）",
        "MessageSendDTO": "WebSocket 消息 DTO（类比 Java DTO + @JsonProperty）",
        "RedisCheckpointSaver": "Redis 持久化 Checkpoint（类比 RedisCacheWriter）",
        "OutputGuardrail": "输出内容安全/一致性校验（类比 Validator 切面）",
        "InputGuardrail": "输入内容校验（类比 RequestValidator）",
        "ToolGuardrail": "工具调用参数校验",
        "CircuitBreaker": "熔断器（类比 Resilience4j CircuitBreaker）",
    }
    extra = hints.get(name, f"业务类 `{name}`")
    return f"声明类 `{name}`：{extra}" + _java_analogy(line)

def _comment_for_assignment(line: str) -> str:
    name = _lhs_name(line)
    if name and name in CONSTANT_HINTS:
        return CONSTANT_HINTS[name] + _java_analogy(line)
    if name and name.startswith("REDIS_"):
        return f"Redis key 模板 `{name}`（类比 CacheKeyGenerator 常量）" + _java_analogy(line)
    if name and name.endswith("_HINTS") or (name and name.endswith("_RE")):
        return f"规则关键词/正则常量 `{name}`（类比 static final 配置表）" + _java_analogy(line)
    if name and name == "__all__":
        return "模块对外导出符号列表（类比 module-info exports）"
    if name:
        return f"赋值 `{name}`" + _java_analogy(line)
    return "赋值语句" + _java_analogy(line)

def _comment_for_control(line: str) -> str:
    s = line.strip()
    if s.startswith("if "):
        return "条件判断 if（类比 Java if）" + _java_analogy(line)
    if s.startswith("elif "):
        return "else-if 分支" + _java_analogy(line)
    if s == "else:":
        return "else 默认分支"
    if s.startswith("for "):
        return "for 循环遍历（类比 Java enhanced-for）" + _java_analogy(line)
    if s.startswith("while "):
        return "while 循环"
    if s.startswith("try:"):
        return "try 块开始（类比 Java try-catch）"
    if s.startswith("except "):
        return "捕获异常 except（类比 catch）"
    if s == "finally:":
        return "finally 清理块"
    if s.startswith("with "):
        return "with 上下文管理器（类比 try-with-resources）" + _java_analogy(line)
    if s.startswith("return "):
        return "return 返回（类比 Java return）" + _java_analogy(line)
    if s.startswith("raise "):
        return "抛出异常（类比 throw）"
    if s.startswith("yield "):
        return "生成器 yield（类比 Stream iterator）"
    if s in ("break", "continue", "pass"):
        return f"控制流关键字 `{s}`"
    if s.startswith("assert "):
        return "断言（类比 Java assert）"
    return "控制流语句" + _java_analogy(line)

def _comment_for_line(line: str, in_docstring: bool) -> str | None:
    stripped = line.strip()
    if not stripped or MARKER in line:
        return None
    if stripped.startswith("#"):
        return None
    if in_docstring:
        if stripped.startswith(('"""', "'''")):
            return "文档字符串结束/开始"
        return "docstring 文档说明文本（类比 Javadoc 内容）"
    if stripped.startswith(('"""', "'''")):
        return "文档字符串/docstring 开始（类比 Javadoc /**）"
    if stripped.startswith(("from ", "import ")):
        return _comment_for_import(line)
    if re.match(r"^\s*@\w+", stripped):
        return f"装饰器 `{stripped.split('(')[0].lstrip('@')}`（类比 Java 注解 @Cacheable 等）"
    if re.match(r"^\s*(async\s+def|def)\s+", line):
        return _comment_for_def(line)
    if re.match(r"^\s*class\s+", line):
        return _comment_for_class(line)
    if re.match(r"^\s*(if |elif |else:|for |while |try:|except |finally:|with |return |raise |yield |break|continue|pass|assert )", stripped):
        return _comment_for_control(line)
    if "=" in stripped and not stripped.endswith(":"):
        return _comment_for_assignment(line)
    if stripped.endswith(":"):
        return "复合语句块起始（dict/TypedDict/class 体等）" + _java_analogy(line)
    return "可执行语句" + _java_analogy(line)

def _docstring_state(line: str, state: str | None) -> str | None:
    stripped = line.strip()
    if state:
        q = state
        if stripped.endswith(q) and len(stripped) >= len(q):
            return None
        return state
    for q in ('"""', "'''"):
        if stripped.startswith(q):
            if stripped.count(q) >= 2 and len(stripped) > len(q):
                return None
            return q
    return None

def annotate_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    if MARKER in text:
        return False
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    doc_state: str | None = None
    for line in lines:
        doc_state = _docstring_state(line, doc_state)
        in_doc = doc_state is not None
        comment = _comment_for_line(line, in_doc)
        if comment:
            indent = re.match(r"^(\s*)", line).group(1)
            out.append(f"{indent}{MARKER} {comment}\n")
        out.append(line)
    new_text = "".join(out)
    if text.endswith("\n") and not new_text.endswith("\n"):
        new_text += "\n"
    path.write_text(new_text, encoding="utf-8")
    return True

def main(argv: list[str]) -> int:
    files = [Path(a) for a in argv[1:]]
    changed = [str(p) for p in files if p.is_file() and annotate_file(p)]
    print(len(changed))
    for c in changed:
        print(c)
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
