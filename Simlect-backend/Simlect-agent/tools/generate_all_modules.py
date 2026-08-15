#!/usr/bin/env python3
"""扫描全仓库，为所有缺失的模块目录生成带调用链的 MODULE.md。"""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]  # EShop

SKIP_DIRS = {
    "node_modules", ".venv", "target", "dist", "build", "__pycache__",
    ".git", ".idea", "site-packages", "resources", "scripts", "tests",
    "test", "deploy", "docs",
}

# Java 包名 → (职责, 调用链模板, mermaid, 依赖)
JAVA_PKG_META: dict[str, tuple[str, list[str], str, list[str]]] = {
    "controller": (
        "REST 控制器层，接收 HTTP 请求、参数校验、调用 Service、返回 ResponseVO。类比 Spring `@RestController`。",
        [
            "客户端/前端 → Gateway（可选）→ `@RequestMapping` Controller 方法",
            "Controller → `@Resource` Service / Mapper",
            "Service 返回 VO/DTO → ResponseVO.success(data) → JSON 响应",
        ],
        "flowchart LR\n  Client --> Gateway\n  Gateway --> Controller\n  Controller --> Service\n  Service --> Mapper",
        ["Spring Web", "ResponseVO", "Jakarta Validation"],
    ),
    "controller.internal": (
        "内部 Agent 专用接口（`/internal/**`），供 Simlect-agent Python 通过 java_internal_client 调用，不对外暴露。",
        [
            "Simlect-agent MCP → mcp_tools_service → java_internal_client.post_json",
            "→ 本包 `*AgentInternalController` → Mapper → MySQL",
            "→ toXxxMap/toAgentCard → ResponseVO → Agent 卡片 JSON",
        ],
        "flowchart LR\n  AgentPython --> InternalController\n  InternalController --> Mapper\n  Mapper --> MySQL",
        ["Simlect-agent", "MySQL"],
    ),
    "biz": (
        "业务服务层（接口 + impl），封装领域逻辑、事务、MQ 发送。类比 Spring `@Service`。",
        [
            "Controller → XxxService 接口",
            "XxxServiceImpl → Mapper 读写 → 业务校验 → 返回实体/VO",
            "写操作 → `@Transactional` → MQ / Redis 副作用",
        ],
        "flowchart LR\n  Controller --> Service\n  Service --> Mapper\n  Service --> MQ",
        ["MyBatis", "RabbitMQ", "Redis"],
    ),
    "mappers": (
        "MyBatis Mapper 数据访问层，SQL 映射。类比 MyBatis `@Mapper` / JPA Repository。",
        [
            "ServiceImpl → XxxMapper.select/insert/update",
            "Mapper XML / 注解 SQL → MySQL 表",
        ],
        "flowchart LR\n  Service --> Mapper --> MySQL",
        ["MySQL", "MyBatis-Plus"],
    ),
    "entity": (
        "实体与查询对象：PO 持久化对象、Query 查询条件、VO 视图对象。",
        [
            "Mapper 结果映射 → PO",
            "Controller 入参 → Query/DTO → Service",
            "Service 组装 → VO → ResponseVO",
        ],
        "flowchart LR\n  Mapper --> PO\n  Service --> VO",
        [],
    ),
    "component": (
        "可复用 Spring 组件：Redis、MQ 发送、远程补偿记录等。",
        [
            "Service → `@Resource` Component",
            "Component → Redis / RabbitMQ / HTTP 客户端",
        ],
        "flowchart LR\n  Service --> Component --> Redis",
        ["Redis", "RabbitMQ"],
    ),
    "cloud": (
        "Spring Boot 启动类与云原生配置入口。",
        [
            "`main` → SpringApplication.run",
            "扫描 `@ComponentScan` → 加载 Controller/Service/Mapper Bean",
        ],
        "flowchart LR\n  main --> SpringBoot --> Beans",
        ["Spring Boot"],
    ),
    "config": (
        "Spring 配置类：`@Configuration`、`@Bean`、拦截器、Sa-Token 等。",
        [
            "应用启动 → 加载 `@Configuration`",
            "注册 Bean / 拦截器 / 过滤器链",
        ],
        "flowchart LR\n  Startup --> Config --> Beans",
        ["Spring"],
    ),
    "task": (
        "定时任务（`@Scheduled`），对账、清理、初始化等后台作业。",
        [
            "Scheduler → `@Scheduled` 方法",
            "Task → Service / Mapper 批处理",
        ],
        "flowchart LR\n  Scheduler --> Task --> Service",
        ["Spring Scheduling"],
    ),
    "utils": (
        "静态工具类，字符串/日期/订单号等纯函数。",
        ["Service/Controller → Utils 静态方法 → 返回值"],
        "flowchart LR\n  Service --> Utils",
        [],
    ),
    "constants": (
        "全局常量：MQ 队列名、Redis Key、业务枚举值。",
        ["各模块 import Constants → 避免魔法字符串"],
        "flowchart LR\n  Modules --> Constants",
        [],
    ),
    "exception": (
        "业务异常与全局异常处理。",
        ["Service throw BusinessException → `@ControllerAdvice` → ResponseVO.error"],
        "flowchart LR\n  Service --> Exception --> Handler",
        [],
    ),
    "interceptor": (
        "MVC 拦截器：登录校验、日志、限流。",
        ["HTTP → Interceptor.preHandle → Controller"],
        "flowchart LR\n  HTTP --> Interceptor --> Controller",
        [],
    ),
    "websocket": (
        "WebSocket 端点与消息推送（若本模块启用）。",
        ["WS 连接 → Handler → Service → 推送客户端"],
        "flowchart LR\n  Client --> WS --> Handler",
        ["WebSocket"],
    ),
    "service": (
        "业务服务接口与实现（Ragent 命名习惯）。",
        [
            "Controller → Service 接口",
            "ServiceImpl → DAO/Mapper → 外部 LLM/向量库",
        ],
        "flowchart LR\n  Controller --> Service --> DAO",
        ["Spring", "PostgreSQL"],
    ),
    "dao": (
        "数据访问对象（Mapper + DO 实体）。",
        ["ServiceImpl → Mapper → 数据库"],
        "flowchart LR\n  Service --> Mapper --> DB",
        ["MyBatis"],
    ),
}

FRONTEND_META: dict[str, tuple[str, list[str], str, list[str]]] = {
    "views": (
        "页面级 Vue 组件，由 vue-router 路由加载。",
        [
            "router/index.ts 路由表 → path → View 组件",
            "View → composables/stores → api/* → Gateway 后端",
            "响应 → 渲染模板 / 跳转",
        ],
        "flowchart LR\n  Router --> View --> Store\n  View --> API --> Backend",
        ["Vue Router", "Pinia", "Axios"],
    ),
    "components": (
        "可复用 UI 组件，被 views 或其他 components 引用。",
        [
            "父 View/Component → import 子组件 → props/emits 通信",
        ],
        "flowchart LR\n  View --> Component",
        [],
    ),
    "stores": (
        "Pinia 全局状态：auth、cart、agentMessage 等。",
        [
            "View/composable → store.action()",
            "store → api 请求 → 更新 state → 视图响应式刷新",
        ],
        "flowchart LR\n  View --> PiniaStore --> API",
        ["Pinia"],
    ),
    "composables": (
        "Vue 组合式函数（Composition API），封装可复用逻辑。",
        [
            "View setup() → useXxx() composable",
            "composable → ref/computed/watch → 副作用 api 调用",
        ],
        "flowchart LR\n  View --> Composable --> API",
        [],
    ),
    "api": (
        "HTTP API 封装模块，统一 axios 实例与后端路径。",
        [
            "store/view → api/modules.ts 或 http.ts",
            "http 拦截器附加 token → Gateway → 微服务",
        ],
        "flowchart LR\n  Caller --> http.ts --> Gateway",
        ["Axios"],
    ),
    "router": (
        "前端路由配置：路径、懒加载、导航守卫。",
        [
            "main.ts → createRouter(routes)",
            "beforeEach 守卫 → 鉴权 → next()",
        ],
        "flowchart LR\n  main --> router --> View",
        ["Vue Router"],
    ),
    "layouts": (
        "页面布局壳：MainLayout、PcMainLayout 等，包裹 router-view。",
        [
            "Route meta.layout → Layout 组件 → slot/router-view",
        ],
        "flowchart LR\n  Router --> Layout --> View",
        [],
    ),
    "utils": (
        "前端工具函数：格式化、WebSocket、缓存、Agent 消息渲染等。",
        [
            "View/Component → utils/xxx.ts 纯函数",
            "Agent: agentMessageRender → 解析 biz_type → 卡片组件",
        ],
        "flowchart LR\n  Component --> Utils",
        [],
    ),
    "constants": (
        "前端常量：枚举映射、Tab 页配置、校验规则等。",
        ["modules import constants → 避免硬编码"],
        "flowchart LR\n  Modules --> Constants",
        [],
    ),
    "integrations": (
        "第三方或跨模块集成注册表。",
        ["featureRegistry → 动态加载能力模块"],
        "flowchart LR\n  App --> integrations",
        [],
    ),
    "pages": (
        "React 页面组件（Ragent frontend）。",
        [
            "router.tsx → Route path → Page 组件",
            "Page → hooks/services → ragent-python API",
        ],
        "flowchart LR\n  Router --> Page --> Service --> API",
        ["React Router"],
    ),
    "hooks": (
        "React 自定义 Hooks。",
        ["Page → useChat/useStreamResponse → chatService SSE"],
        "flowchart LR\n  Page --> Hook --> Service",
        [],
    ),
    "services": (
        "React API 服务层，封装 fetch/EventSource。",
        ["Page/Hook → services/chatService.ts → /api/ragent"],
        "flowchart LR\n  Hook --> Service --> Backend",
        [],
    ),
}

PYTHON_SUB_META: dict[str, tuple[str, list[str], str, list[str]]] = {
    "auth": (
        "认证模块：Token 解析、FastAPI Depends 登录注入。",
        ["api Depends → auth/deps → token_service → Redis"],
        "flowchart LR\n  Route --> deps --> Redis",
        ["Redis"],
    ),
    "db": (
        "数据库连接池与 ORM。",
        ["main.lifespan → session/get_redis → services CRUD"],
        "flowchart LR\n  API --> session --> DB",
        ["PostgreSQL/SQLAlchemy 或 MySQL"],
    ),
    "graph/checkpoint": (
        "LangGraph Checkpoint 持久化（Redis）。",
        ["graph/builder → get_checkpointer → RedisCheckpointSaver"],
        "flowchart LR\n  Graph --> RedisSaver --> Redis",
        ["Redis"],
    ),
    "domain/intent": (
        "意图识别子域：规则 + LLM 分类。",
        ["build_context → classifier.resolve_intent → rules/LLM"],
        "flowchart LR\n  nodes --> classifier --> rules",
        ["LLM"],
    ),
    "harness/guardrails": (
        "输入/输出/工具/商品文本护栏。",
        ["send_message/finalize/tools → 各 Guardrail 校验"],
        "flowchart TD\n  input --> graph --> output",
        [],
    ),
    "harness/metrics": (
        "Prometheus 运行时指标。",
        ["classifier/router/runtime → Counter.inc"],
        "flowchart LR\n  Runtime --> Prometheus",
        ["Prometheus"],
    ),
    "graph/agents": (
        "Ragent Multi-Agent 节点实现（memory/rewrite/intent 等）。",
        ["graph/builder 节点 → agents/*.run → 更新 ChatGraphState"],
        "flowchart TD\n  builder --> agents",
        ["LLM"],
    ),
    "retrieve/channels": (
        "检索通道：向量/关键词/ES/Milvus/意图定向。",
        ["multi_channel → channels/*.search 并行"],
        "flowchart LR\n  engine --> channels",
        ["pgvector", "ES", "Milvus"],
    ),
    "retrieve/postprocessors": (
        "检索后处理：去重、融合、重排。",
        ["multi_channel 结果 → dedup → fusion → rerank"],
        "flowchart LR\n  channels --> dedup --> rerank",
        [],
    ),
    "knowledge/services": (
        "知识库文档/分块服务。",
        ["api → document_service/chunk_service → vector"],
        "flowchart LR\n  API --> doc_service --> vector",
        [],
    ),
    "ingestion/services": (
        "摄取任务与 pipeline 服务。",
        ["api/ingestion → task_service → engine.execute"],
        "flowchart LR\n  API --> task --> engine",
        [],
    ),
    "core/parser": (
        "文档解析器选择器。",
        ["chunk_document → parser/selector → 按 MIME 选解析器"],
        "flowchart LR\n  doc --> parser",
        [],
    ),
    "core/chunk": (
        "文本分块与 Embedding。",
        ["strategies.split → embedding_service.embed"],
        "flowchart LR\n  text --> chunk --> embed",
        ["LLM Embedding"],
    ),
    "db/models": (
        "SQLAlchemy ORM 实体定义。",
        ["services → entities.py 模型 → PostgreSQL 表"],
        "flowchart LR\n  Service --> ORM --> DB",
        [],
    ),
    "api/routes": (
        "FastAPI 路由子模块，每个文件一组 REST/SSE 端点。",
        ["main include_router → routes/*.py → services"],
        "flowchart LR\n  main --> routes --> services",
        ["FastAPI"],
    ),
}


def _md(title: str, purpose: str, keys: list[str], chains: list[str], deps: list[str], mermaid: str) -> str:
    keys_block = "\n".join(f"- `{k}`" for k in keys) if keys else "- （见目录内源文件）"
    chain_block = "\n".join(f"1. `{c}`" for c in chains)
    deps_block = "\n".join(f"- {d}" for d in deps) if deps else "- 无额外外部依赖"
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
"""


def _list_files(directory: Path, pattern: str) -> list[str]:
    if not directory.is_dir():
        return []
    return sorted({f.name for f in directory.glob(pattern) if f.is_file()})[:15]


def _java_pkg_key(rel_parts: tuple[str, ...]) -> str:
    """匹配 JAVA_PKG_META 的 key，支持 controller.internal。"""
    if not rel_parts:
        return "controller"
    last = rel_parts[-1]
    if last == "internal" and len(rel_parts) >= 2 and rel_parts[-2] == "controller":
        return "controller.internal"
    if last in JAVA_PKG_META:
        return last
    # ragent: user/controller → use controller
    for p in reversed(rel_parts):
        if p in JAVA_PKG_META:
            return p
    return last if last in JAVA_PKG_META else "biz"


def _write_if_missing(path: Path, content: str, force: bool = False) -> bool:
    if path.exists() and not force:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return True


def generate_java_packages(force: bool = False) -> int:
    count = 0
    java_roots = [
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-common" / "src" / "main" / "java" / "com" / "simlect",
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-product" / "app" / "src" / "main" / "java" / "com" / "simlect",
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-order" / "app" / "src" / "main" / "java" / "com" / "simlect",
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-user" / "app" / "src" / "main" / "java" / "com" / "simlect",
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-coupon" / "app" / "src" / "main" / "java" / "com" / "simlect",
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-pay" / "app" / "src" / "main" / "java" / "com" / "simlect",
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-gateway" / "src" / "main" / "java" / "com" / "simlect",
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-search" / "src" / "main" / "java" / "com" / "simlect",
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-stock" / "app" / "src" / "main" / "java" / "com" / "simlect",
        ROOT / "ragent" / "bootstrap" / "src" / "main" / "java" / "com" / "nageoffer" / "ai" / "ragent",
        ROOT / "ragent" / "infra-ai" / "src" / "main" / "java" / "com" / "nageoffer" / "ai" / "ragent" / "infra",
        ROOT / "ragent" / "mcp-server" / "src" / "main" / "java" / "com" / "nageoffer" / "ai" / "ragent" / "mcp",
    ]
    for root in java_roots:
        if not root.exists():
            continue
        for dirpath in sorted(root.rglob("*")):
            if not dirpath.is_dir():
                continue
            if any(p in SKIP_DIRS for p in dirpath.parts):
                continue
            java_files = list(dirpath.glob("*.java"))
            if not java_files:
                continue
            rel = dirpath.relative_to(root).parts
            key = _java_pkg_key(rel)
            meta = JAVA_PKG_META.get(key, JAVA_PKG_META.get("biz"))
            purpose, chains, mermaid, deps = meta
            svc = root.parents[3].name if "Simlect" in str(root) else root.parts[-3]
            title = f"{'/'.join(rel)} — {key}" if rel else key
            files = _list_files(dirpath, "*.java")
            content = _md(f"{svc} / {title}", purpose, files, chains, deps, mermaid)
            if _write_if_missing(dirpath / "MODULE.md", content, force):
                count += 1
    return count


def generate_python_subpackages(force: bool = False) -> int:
    count = 0
    py_roots = [
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-agent" / "app",
        ROOT / "ragent-python" / "app",
        ROOT / "ragent-python" / "mcp_server",
    ]
    for root in py_roots:
        if not root.exists():
            continue
        for dirpath in sorted(root.rglob("*")):
            if not dirpath.is_dir():
                continue
            if any(p in SKIP_DIRS for p in dirpath.parts):
                continue
            py_files = [f for f in dirpath.glob("*.py") if f.name != "__init__.py"]
            if not py_files and not list(dirpath.glob("*.py")):
                continue
            rel = str(dirpath.relative_to(root)).replace("\\", "/")
            if (dirpath / "MODULE.md").exists() and not force:
                continue
            meta = PYTHON_SUB_META.get(rel)
            if not meta:
                # 默认：目录名作为模块
                name = dirpath.name
                meta = (
                    f"Python 子模块 `{rel}`，详见同级 .py 源文件。",
                    [f"上层模块 → `{rel}` 内函数/类 → 下游依赖"],
                    f"flowchart LR\n  Parent --> {name}",
                    [],
                )
            purpose, chains, mermaid, deps = meta
            files = _list_files(dirpath, "*.py")
            title = rel or root.name
            content = _md(title, purpose, files, chains, deps, mermaid)
            if _write_if_missing(dirpath / "MODULE.md", content, force):
                count += 1
    return count


def generate_frontend_modules(force: bool = False) -> int:
    count = 0
    fe_roots = [
        ROOT / "Simlect" / "Simlect-front" / "Simlect-web" / "src",
        ROOT / "Simlect" / "Simlect-front" / "Simlect-admin" / "src",
        ROOT / "ragent" / "frontend" / "src",
    ]
    for root in fe_roots:
        if not root.exists():
            continue
        # 顶层 + 一级子目录 + 重要二级（views/agent, components/agent, pages/admin）
        candidates: set[Path] = set()
        for child in root.iterdir():
            if child.is_dir() and child.name not in SKIP_DIRS:
                candidates.add(child)
                for sub in child.iterdir():
                    if sub.is_dir() and sub.name not in SKIP_DIRS:
                        candidates.add(sub)
        # ragent pages/admin/*
        admin = root / "pages" / "admin"
        if admin.exists():
            for sub in admin.iterdir():
                if sub.is_dir():
                    candidates.add(sub)
        for dirpath in sorted(candidates):
            files = _list_files(dirpath, "*.vue") + _list_files(dirpath, "*.ts") + _list_files(dirpath, "*.tsx")
            if not files:
                continue
            rel_name = dirpath.name
            parent_name = dirpath.parent.name if dirpath.parent != root else ""
            meta_key = rel_name
            if rel_name in ("agent", "admin", "pc") and parent_name in FRONTEND_META:
                meta_key = parent_name
            elif parent_name == "admin":
                meta_key = "pages"
            meta = FRONTEND_META.get(meta_key, FRONTEND_META.get(rel_name, FRONTEND_META.get("components")))
            purpose, chains, mermaid, deps = meta
            rel = str(dirpath.relative_to(root)).replace("\\", "/")
            proj = root.parent.name
            content = _md(f"{proj} / {rel}", purpose, files[:12], chains, deps, mermaid)
            if _write_if_missing(dirpath / "MODULE.md", content, force):
                count += 1
    return count


def cleanup_wrong_tree() -> None:
    wrong = ROOT / "Simlect" / "Simlect"
    if wrong.exists():
        import shutil
        shutil.rmtree(wrong)


def main() -> None:
    import sys
    force = "--force" in sys.argv
    cleanup_wrong_tree()
    j = generate_java_packages(force)
    p = generate_python_subpackages(force)
    f = generate_frontend_modules(force)
    total = j + p + f
    print(f"Generated MODULE.md: Java packages={j}, Python sub={p}, Frontend={f}, total new={total}")
    # 统计全仓库 MODULE.md
    all_mods = list(ROOT.rglob("MODULE.md"))
    all_mods = [m for m in all_mods if not any(s in m.parts for s in SKIP_DIRS | {"Simlect"}) or "Simlect-backend" in str(m)]
    # filter out wrong Simlect/Simlect nested
    all_mods = [m for m in all_mods if "\\Simlect\\Simlect\\" not in str(m) and "/Simlect/Simlect/" not in str(m)]
    print(f"Total MODULE.md in repo: {len(all_mods)}")


if __name__ == "__main__":
    main()
