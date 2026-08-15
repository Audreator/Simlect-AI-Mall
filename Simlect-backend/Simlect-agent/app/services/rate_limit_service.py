"""限流服务模块：基于 Redis 计数器实现滑动窗口内的用户操作频率限制。"""

from app.services.redis_service import redis_service  # import statement — Redis INCR/EXPIRE 操作

_PREFIX = "mall:agent:ratelimit:"  # Redis 键前缀 — 类似 Java 常量 KEY_PREFIX

class RateLimitService:  # 限流服务 — 类似 Java @Service RateLimitService

    async def allow(self, user_id: str, action: str, window_seconds: int, max_count: int) -> bool:  # 判断是否允许操作

        if not user_id:  # 无 userId 时不限流
            return True
        key = f"{_PREFIX}{action}:{user_id}"  # 构造 Redis 键 — action + userId 维度

        count = await redis_service.client.incr(key)  # async/await — INCR 原子递增，类似 RedisTemplate.opsForValue().increment
        if count == 1:  # 窗口内首次请求

            await redis_service.client.expire(key, window_seconds)  # 设置 TTL 开启滑动窗口
        return count <= max_count  # 未超过上限则允许

rate_limit_service = RateLimitService()  # 模块级单例
