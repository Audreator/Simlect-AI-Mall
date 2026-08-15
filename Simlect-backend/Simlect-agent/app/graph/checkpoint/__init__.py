"""
================================================================================
文件：graph/checkpoint/__init__.py
角色：checkpoint 包的「对外出口」（类比 Java 模块的 package-info / 门面导出）
================================================================================

【这个文件干什么】
Python 里目录要变成「包」，通常需要 __init__.py。
本文件把 redis_saver 里真正有用的类/函数再导出一层，这样外部可以写：

    from app.graph.checkpoint import get_checkpointer

而不必写很长的：

    from app.graph.checkpoint.redis_saver import get_checkpointer

【谁在用】
- builder.py / runner.py / main.py 的 close_checkpointer
- 本包 MODULE.md 文档索引

【和 redis_saver.py 的关系】
__init__.py = 橱窗（只做 import 转发）
redis_saver.py = 仓库（真正实现断点存 Redis）
================================================================================
"""

# 从同包下的 redis_saver 模块导入三个公开 API，供外部 from app.graph.checkpoint import ...
from app.graph.checkpoint.redis_saver import (
    RedisCheckpointSaver,  # 检查点存储器类（一般不直接 new，走 get_checkpointer）
    close_checkpointer,  # 应用关闭时释放单例 + 清图缓存
    get_checkpointer,  # 懒加载获取 RedisCheckpointSaver 单例
)

# __all__：规定「from app.graph.checkpoint import *」时能导出哪些名字（白名单）
# 类比 Java：明确模块对外 public API，避免把私有 _xxx 泄漏出去
__all__ = [
    "RedisCheckpointSaver",  # 类名字符串，必须与上面 import 的符号一致
    "close_checkpointer",  # 关闭函数
    "get_checkpointer",  # 获取单例函数
]
