"""
================================================================================
文件：main.py
角色：Simlect 智能客服 Agent 主进程入口（FastAPI，默认 :7050）
================================================================================

【这个文件干什么】
启动一个独立微服务：连 Redis/MySQL、挂 HTTP 路由与 WebSocket、暴露 /health /metrics。
它不是 Java 进程里的类，而是和 Gateway、订单服务并列的「另一个 Spring Boot」。

【如何启动】
- 开发：python -m uvicorn app.main:app --reload --port 7050
- 或仓库 start.bat；另需先起 MCP :7060（见 mcp_server/server.py）

【调用链 — 启动】
lifespan → redis.connect → MySQL pool → session_memory 建表
        → start_ws_listener → 对外服务

【调用链 — 发消息】（HTTP 立刻返回 messageId，推理在后台）
POST /api/agent/sendMessage → routes/agent → agent_service
  → create_task → agent_engine → graph.runner → nodes...
  → stream_service → Redis → websocket → 前端

【调用链 — 用户确认写操作】
POST /confirmAction → pending_action → action_execute → Java /api/order/*

【推荐阅读】docs/智能客服-Java开发者导读.md（专为 Java 初级）
另见 app/api/MODULE.md、app/graph/MODULE.md、docs/ARCHITECTURE_CALL_CHAINS.md
================================================================================
"""

from contextlib import asynccontextmanager  # 异步上下文管理器，用于 FastAPI 生命周期（类似 @PreDestroy / @PostConstruct）

import structlog  # 结构化日志库，输出 JSON 格式日志（类似 SLF4J + Logstash 编码器）

import uvicorn  # ASGI 服务器，负责真正监听端口、跑 FastAPI 应用（类似内嵌 Tomcat）

from fastapi import FastAPI, WebSocket  # Web 框架入口 + WebSocket 处理（类比 Spring Boot + @ServerEndpoint）
from starlette.requests import Request  # ASGI 请求对象 — 429 限流异常处理器的参数类型

from prometheus_client import make_asgi_app  # 把 Prometheus 指标暴露成 ASGI 子应用（类似 /actuator/prometheus）

from app.api.rate_limit import limiter  # 共享全局限流器单例（防双实例限流失效）
from slowapi.errors import RateLimitExceeded  # 触发限流时抛出的异常类型
from slowapi.middleware import SlowAPIMiddleware  # 限流中间件，在请求链路上检查配额

from app.api.exception_handlers import business_exception_handler  # 业务异常 → HTTP 响应的统一转换
from app.api.routes import agent  # Agent 相关 HTTP 路由模块（@RestController 那层）
from app.exceptions import BusinessException  # 自定义业务异常类（类似 BizException）

from app.api.websocket import start_ws_listener, stop_ws_listener, websocket_endpoint  # WS 订阅启动/停止 + 单连接处理入口

from app.config.settings import get_settings  # 读取 .env / 环境变量配置（类似 @ConfigurationProperties）

from app.db.pool import close_pool, init_pool  # MySQL 连接池初始化与关闭

from app.services.health_service import health_service  # /health 探活逻辑（检查 Redis、MySQL 等）

from app.graph.checkpoint.redis_saver import close_checkpointer  # LangGraph 检查点（Redis）资源释放
from app.services.redis_service import redis_service  # Redis 客户端封装单例

# 配置 structlog：每条日志带 ISO 时间戳，并以 JSON 输出（便于 ELK 采集）
structlog.configure(
    processors=[structlog.processors.TimeStamper(fmt="iso"), structlog.processors.JSONRenderer()]
)

logger = structlog.get_logger()  # 本模块日志记录器


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    FastAPI 应用生命周期钩子（类比 Spring Boot ApplicationListener）。
    yield 之前做启动初始化；yield 之后做优雅关闭。
    """

    await redis_service.connect()  # 建立 Redis 连接
    await init_pool()  # 初始化 MySQL 连接池
    from app.memory.session_memory_service import session_memory_service  # 延迟导入，避免循环依赖

    await session_memory_service.ensure_table()  # 确保会话记忆表存在（不存在则建表）
    await start_ws_listener(redis_service.client)  # 订阅 Redis 频道，把推送转发到 WebSocket
    logger.info("agent_service_started")  # 启动完成日志
    yield  # 此处之后应用开始对外接收请求；shutdown 时从 yield 往下继续执行

    await stop_ws_listener()  # 取消 Redis 订阅协程
    await close_checkpointer()  # 关闭 LangGraph Redis 检查点
    await close_pool()  # 关闭 MySQL 连接池
    await redis_service.close()  # 关闭 Redis 连接
    logger.info("agent_service_stopped")  # 关闭完成日志


# 创建 FastAPI 应用实例；lifespan 绑定上面的启动/关闭逻辑
app = FastAPI(title="EShop Agent Python", version="1.0.0", lifespan=lifespan)

limiter = limiter  # 共享全局限流器单例（见 app.api.rate_limit，防双实例导致限流失效）
app.state.limiter = limiter  # 挂到 app.state，供路由装饰器 @limiter.limit 使用
app.add_middleware(SlowAPIMiddleware)  # 注册限流中间件


async def _rate_limit_exceeded_handler(request: "Request", exc: RateLimitExceeded):
    """限流超限统一响应（429）。"""
    from starlette.responses import JSONResponse

    return JSONResponse(
        status_code=429,
        content={"status": "error", "code": 429, "info": "请求过于频繁，请稍后再试", "data": None},
    )


app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)  # 429 超限响应
app.add_exception_handler(BusinessException, business_exception_handler)  # 业务异常统一格式

app.include_router(agent.router, prefix="/api")  # 挂载 /api/agent/* 等 HTTP 路由

app.mount("/metrics", make_asgi_app())  # Prometheus 指标端点（独立 ASGI 子应用）


@app.get("/health")
async def health():
    """健康检查：供 K8s / 网关探活。"""
    return await health_service.check_all()  # 聚合检查 Redis、MySQL 等依赖


@app.websocket("/ws")
async def ws_route(ws: WebSocket, token: str | None = None):
    """WebSocket 入口（无尾斜杠路径）。"""
    await websocket_endpoint(ws, token)  # 鉴权、登记连接、处理心跳


@app.websocket("/ws/")
async def ws_route_slash(ws: WebSocket, token: str | None = None):
    """WebSocket 入口（带尾斜杠，兼容不同客户端 URL 写法）。"""
    await websocket_endpoint(ws, token)


if __name__ == "__main__":
    # 直接 python app/main.py 启动时走这里（开发常用）
    settings = get_settings()  # 读取 host、port 等配置

    # 字符串 "app.main:app" 表示模块路径 + 应用变量名；reload=False 生产关闭热重载
    uvicorn.run("app.main:app", host=settings.app_host, port=settings.app_port, reload=False)
