"""敏感词过滤服务模块：从 Redis 加载词库并对用户输入进行替换脱敏。"""

from app.services.redis_service import redis_service  # import statement — Redis 词库读取

class SensitiveWordService:  # 敏感词服务 — 类似 Java @Service SensitiveWordService

    async def replace(self, text: str) -> str:  # 异步方法 — 替换文本中的敏感词

        words = await redis_service.get_sensitive_words()  # async/await — 从 Redis 获取词库 List<Map>
        if not words or not text:  # 词库为空或文本为空则原样返回
            return text
        result = text  # 可变副本 — 类似 StringBuilder 逐步替换
        for item in words:  # 遍历词库 — 类似 for (Map item : words)

            word = item.get("word") or item.get("Word")  # dict.get 类似 Map.get，兼容大小写键
            replace = item.get("replaceWord") or item.get("replace_word") or "***"  # 默认替换为 ***
            if word and word in result:  # 命中敏感词
                result = result.replace(word, replace)  # 字符串替换 — 类似 String.replace
        return result  # 返回脱敏后的文本

sensitive_word_service = SensitiveWordService()  # 模块级单例
