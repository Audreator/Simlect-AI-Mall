"""健康检查服务模块：聚合 Redis、MySQL、Elasticsearch、Java Web 等依赖的可用性探测。"""

import httpx  # import statement — HTTP 客户端库，类似 Java 的 OkHttp/HttpClient
import structlog  # import statement — 结构化日志，类似 Java SLF4J + Logback

from app.config.settings import get_settings  # import statement — 读取应用配置
from app.db.pool import acquire  # import statement — 获取 MySQL 连接池游标
from app.services.redis_service import redis_service  # import statement — Redis 服务单例

logger = structlog.get_logger()  # 获取日志记录器 — 类似 LoggerFactory.getLogger()

class HealthService:  # 健康检查服务类 — 类似 Java @Service HealthService

    async def check_all(self) -> dict:  # 异步方法 — 类似 CompletableFuture<Map<String, Object>>
        checks: dict[str, bool | str] = {  # dict 类似 Map<String, Object>，初始化各组件检查状态
            "redis": False,  # Redis 默认未通过
            "mysql": False,  # MySQL 默认未通过
            "elasticsearch": False,  # ES 默认未通过
            "java_web": False,  # Java 网关默认未通过
            "status": "ok",  # 整体状态初始为 ok
        }

        try:  # try 块 — 探测 Redis
            await redis_service.client.ping()  # async/await — 类似 future.get()，发送 PING
            checks["redis"] = True  # 标记 Redis 可用
        except Exception as e:  # 捕获异常 — Redis 不可用时降级
            logger.warning("health_redis_failed", error=str(e))  # 记录警告日志
            checks["status"] = "degraded"  # 整体状态降为 degraded

        try:  # try 块 — 探测 MySQL
            async with acquire() as cur:  # with 类似 try-with-resources，自动释放连接
                await cur.execute("SELECT 1")  # 执行简单探活 SQL
            checks["mysql"] = True  # 标记 MySQL 可用
        except Exception as e:  # MySQL 不可用时降级
            logger.warning("health_mysql_failed", error=str(e))
            checks["status"] = "degraded"

        settings = get_settings()  # 读取配置 — 获取 ES 与 Java Web 地址
        try:  # try 块 — 探测 Elasticsearch
            async with httpx.AsyncClient(timeout=3) as client:  # with 类似 try-with-resources
                resp = await client.get(f"{settings.es_hosts.split(',')[0].rstrip('/')}/")  # 请求 ES 根路径
                checks["elasticsearch"] = resp.status_code < 500  # HTTP 5xx 视为不可用
        except Exception as e:  # ES 不可用时降级
            logger.warning("health_es_failed", error=str(e))
            checks["status"] = "degraded"

        try:  # try 块 — 探测 Java Web 网关
            async with httpx.AsyncClient(timeout=3) as client:
                resp = await client.get(f"{settings.java_web_url.rstrip('/')}/api/")  # 请求 Java API 根路径
                checks["java_web"] = resp.status_code < 500  # 非 5xx 视为可用
        except Exception as e:  # Java Web 不可用时降级
            logger.warning("health_java_web_failed", error=str(e))
            checks["status"] = "degraded"

        if not checks["redis"] or not checks["mysql"]:  # 核心依赖 Redis/MySQL 任一失败
            checks["status"] = "unhealthy"  # 整体状态标记为 unhealthy
        return checks  # 返回检查结果 Map

health_service = HealthService()  # 模块级单例 — 类似 Spring @Component 注入实例
