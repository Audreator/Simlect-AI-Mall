# LangGraph Checkpoint（断点续跑）— 给 Java 初级

## 模块职责

把一次对话图的中间状态存进 **内存 + Redis**，进程异常后可尝试续跑。  
类比：工作流引擎把「流程实例」持久化到 Redis。

## 核心文件

| 文件 | 作用 |
|------|------|
| `__init__.py` | 包出口：导出 `get_checkpointer` / `close_checkpointer` / `RedisCheckpointSaver` |
| `redis_saver.py` | 真正实现：`hydrate` 从 Redis 灌内存，`aput` 写完刷 Redis，`adelete` 清理 |

## 调用链

1. `builder` compile 图时绑定 `get_checkpointer(redis)`
2. 图执行中 LangGraph 调 `aput` / `aput_writes` → 内存 + Redis SETEX
3. `runner._should_resume` → `hydrate_thread` + `aget_tuple` 判断能否续跑
4. `runner` finally / `main` shutdown → `adelete_thread` / `close_checkpointer`

## 关键概念

- **thread_id**：`userId:messageId`，一个消息一轮图实例
- **TTL**：`settings.graph_checkpoint_ttl`，过期自动删，防 Redis 堆积
- **pickle**：Python 对象 ↔ bytes（类似 Java 序列化，仅内网自用）

源码已有逐行中文注释，直接打开 `redis_saver.py` / `__init__.py` 阅读即可。
