"""
================================================================================
文件：graph/checkpoint/redis_saver.py
角色：LangGraph「断点续跑」存储 —— 内存快照 + Redis 持久化
================================================================================

【这个文件干什么】
智能客服跑 LangGraph 时，每执行完一个节点，框架会把当前状态存成 checkpoint。
本类把 checkpoint 先放在进程内 InMemorySaver，再 pickle 序列化写到 Redis（带 TTL）。
这样进程崩溃 / 重启后，runner 仍可能从 Redis 拉回状态继续跑（见 runner._should_resume）。

【类比 Java】
- BaseCheckpointSaver ≈ 工作流引擎的「流程实例持久化 SPI」
- InMemorySaver ≈ 本地 ConcurrentHashMap 缓存
- Redis setex ≈ 把流程实例序列化进 Redis，过期自动删
- thread_id ≈ 流程实例 ID（本项目是 userId:messageId）

【读写时机】
- aput / aput_writes：图执行中异步写入 → 内存 + Redis
- aget_tuple / hydrate_thread：续跑前从 Redis 灌回内存
- adelete_thread：一轮跑完后清理（runner 的 finally）

【关联】
- runner.run_agent_graph / _should_resume
- builder._resolve_checkpointer
- settings.graph_checkpoint_prefix / graph_checkpoint_ttl
- checkpoint/__init__.py 对外导出本模块 API
================================================================================
"""

from __future__ import annotations  # 允许类型注解里写尚未定义的类名（类似 Java 前向引用）

import hashlib  # HMAC 签名校验（防 Redis 注入恶意 pickle 载荷）
import hmac
import os
import pickle  # Python 对象序列化/反序列化（类似 Java Serializable + ObjectOutputStream）
from collections.abc import AsyncIterator, Iterator, Sequence  # 类型：异步迭代器 / 迭代器 / 序列
from typing import Any  # 任意类型，类似 Java Object

import structlog  # 结构化日志（key=value，便于检索）
from langchain_core.runnables import RunnableConfig  # LangGraph 配置对象，里含 thread_id 等
from langgraph.checkpoint.base import (  # LangGraph 检查点抽象基类与数据结构
    BaseCheckpointSaver,  # 检查点存储器基类（我们要实现的接口）
    ChannelVersions,  # 通道版本号类型（图内部状态版本）
    Checkpoint,  # 某一时刻的完整检查点快照
    CheckpointMetadata,  # 检查点元数据（步数、来源等）
    CheckpointTuple,  # (config, checkpoint, metadata, parent_config, pending_writes) 元组
    get_checkpoint_id,  # 工具函数：从 config 取 checkpoint id（基类可能用到）
    get_checkpoint_metadata,  # 工具函数：组装元数据（基类可能用到）
)
from langgraph.checkpoint.memory import InMemorySaver  # 官方内存实现：真正的读写委托给它

from app.config.settings import get_settings  # 读 Redis key 前缀、TTL 等配置

logger = structlog.get_logger()  # 本模块日志器（类比 private static final Logger）

# ---------- HMAC 签名（安全：pickle 不可信反序列化防护） ----------
# checkpoint 载荷包含任意 Python 对象，只能 pickle 序列化；
# 为防止攻击者向共享 Redis 写入恶意 pickle 载荷导致 RCE，
# 写入时附加 HMAC-SHA256 签名，读取时校验签名不符即拒绝。
# 签名定长 32 字节前缀（不依赖分隔符查找，避免载荷含分隔符导致的误判/误丢弃）
_SIG_LEN = hashlib.sha256().digest_size
_checkpoint_hmac_key: bytes | None = None  # 进程级缓存密钥（懒初始化）


def _get_hmac_key() -> bytes:
    global _checkpoint_hmac_key
    if _checkpoint_hmac_key is None:
        secret = get_settings().graph_checkpoint_hmac_secret
        if secret:
            _checkpoint_hmac_key = secret.encode("utf-8")
        else:
            # 未配置：进程随机密钥（重启后旧断点校验失败被丢弃，仅影响断点续跑）
            _checkpoint_hmac_key = os.urandom(32)
            logger.warning(
                "graph_checkpoint_hmac_secret 未配置，使用进程随机密钥"
                "（重启后历史 checkpoint 将无法续跑）"
            )
    return _checkpoint_hmac_key


def _sign_payload(payload: bytes) -> bytes:
    return hmac.new(_get_hmac_key(), payload, hashlib.sha256).digest()


def _verify_signature(raw: bytes) -> bytes | None:
    """校验签名；通过返回纯载荷，不通过返回 None（丢弃不可信数据）。"""
    if raw is None or len(raw) <= _SIG_LEN:
        return None
    sig = raw[:_SIG_LEN]
    payload = raw[_SIG_LEN:]
    expected = _sign_payload(payload)
    if not hmac.compare_digest(sig, expected):
        return None
    return payload


class RedisCheckpointSaver(BaseCheckpointSaver[str]):
    """
    Redis + 内存 双层 Checkpoint 实现。

    【设计要点】
    LangGraph 同步 API（get_tuple/put）走内存；异步 API（aget/aput）在写后刷 Redis。
    续跑前必须先 hydrate_thread，把 Redis 里的字节流还原进 _memory。
    """

    def __init__(self, redis_client, *, key_prefix: str, ttl_seconds: int = 3600):
        """
        构造检查点存储器。

        【参数】
        - redis_client: 已连接的异步 Redis 客户端（与 redis_service.client 同源）
        - key_prefix: Redis key 前缀，如 "simlect:graph:ckpt"（配置项）
        - ttl_seconds: 过期秒数，默认 3600；防止僵尸 thread 永久占 Redis
        """
        super().__init__()  # 调用基类构造，初始化序列化器等内部字段
        self._redis = redis_client  # 保存 Redis 客户端引用（类似注入 RedisTemplate）
        self._prefix = key_prefix.rstrip(":")  # 去掉末尾冒号，后面拼 key 时再加 ":"，避免 "a::b"
        self._ttl = ttl_seconds  # 缓存过期时间（秒）
        self._memory = InMemorySaver()  # 进程内真正存 checkpoint 的 Map 结构

    def _redis_key(self, thread_id: str) -> str:
        """把 thread_id 转成 Redis 完整 key。例：prefix=ckpt, thread=u1:99 → ckpt:u1:99"""
        return f"{self._prefix}:{thread_id}"  # 字符串拼接，类比 String.format("%s:%s", prefix, id)

    async def hydrate_thread(self, thread_id: str) -> bool:
        """
        【功能】从 Redis 把某 thread 的 checkpoint「灌」进内存（续跑前必调）。

        【返回】
        - True：内存已有，或 Redis 加载成功
        - False：Redis 无数据，或反序列化失败

        【类比】把数据库里的流程实例反序列化进 JVM 内存再继续执行。
        """

        if thread_id in self._memory.storage:  # 内存里已有该 thread → 无需再读 Redis
            return True
        raw = await self._redis.get(self._redis_key(thread_id))  # 异步 GET 字节串；没有则 None
        if not raw:  # Redis 无 key → 无法续跑
            return False
        # 兼容 decode_responses=True 的客户端：str 先转回 bytes
        if isinstance(raw, str):
            raw = raw.encode("utf-8")
        # 安全：先校验 HMAC 签名（防共享 Redis 被注入恶意 pickle 载荷 → RCE）
        signed = _verify_signature(raw)
        if signed is None:
            logger.warning(
                "graph_checkpoint_signature_invalid",
                thread_id=thread_id,
                reason="HMAC 校验失败或格式非法，丢弃并清理该 checkpoint",
            )
            # 清理脏 key（防 TTL 前每次续跑稳定失败）
            try:
                await self._redis.delete(self._redis_key(thread_id))
            except Exception:
                pass
            return False
        try:
            payload = pickle.loads(signed)  # 反序列化成 dict：含 storage / writes / blobs
            self._memory.storage[thread_id] = payload["storage"]  # 恢复主存储（各 checkpoint 版本）
            for k, v in payload.get("writes", {}).items():  # 恢复挂起的写入（pending writes）
                self._memory.writes[k] = v  # k 通常是 (thread_id, ...) 元组键
            for k, v in payload.get("blobs", {}).items():  # 恢复大对象 blob 存储
                self._memory.blobs[k] = v
            logger.info("graph_checkpoint_hydrated", thread_id=thread_id)  # 成功灌入日志
            return True  # 告诉调用方：可以 aget_tuple 了
        except Exception as e:  # pickle 损坏、结构不兼容等
            logger.warning("graph_checkpoint_hydrate_failed", thread_id=thread_id, error=str(e))
            # 反序列化失败同样清理脏 key（防 TTL 前每次续跑稳定失败）
            try:
                await self._redis.delete(self._redis_key(thread_id))
            except Exception:
                pass
            return False  # 失败当「没有可用断点」处理

    async def _persist_thread(self, thread_id: str) -> None:
        """
        【功能】把内存里该 thread 的 storage/writes/blobs 打包写入 Redis（SETEX）。

        【何时调用】aput / aput_writes 成功改完内存之后刷盘。
        【注意】若 storage 里还没有该 thread，直接 return（无需写空 key）。
        """
        if thread_id not in self._memory.storage:  # 内存无此 thread → 没什么可持久化
            return
        # 只挑出属于本 thread_id 的 writes（writes 的 key[0] 约定为 thread_id）
        writes = {k: v for k, v in self._memory.writes.items() if k[0] == thread_id}
        # 同理过滤 blobs
        blobs = {k: v for k, v in self._memory.blobs.items() if k[0] == thread_id}
        payload = pickle.dumps(  # 序列化为 bytes，供 Redis 存储
            {
                "storage": self._memory.storage[thread_id],  # 该 thread 的检查点树
                "writes": writes,  # 未完成的通道写入
                "blobs": blobs,  # 大字段旁路存储
            },
            protocol=pickle.HIGHEST_PROTOCOL,  # 用当前最高协议，体积更小、速度更快
        )
        # 附加 HMAC 签名（定长 32 字节前缀，与读取端 _SIG_LEN 对应）
        signed = _sign_payload(payload) + payload
        # SETEX key ttl value：写入并设置过期，类比 redisTemplate.opsForValue().set(k,v,ttl)
        await self._redis.setex(self._redis_key(thread_id), self._ttl, signed)

    def get_tuple(self, config: RunnableConfig) -> CheckpointTuple | None:
        """同步读检查点元组；委托内存实现（调用前应已 hydrate）。"""
        return self._memory.get_tuple(config)  # 直接转发，不碰 Redis

    def list(
        self,
        config: RunnableConfig | None,
        *,
        filter: dict[str, Any] | None = None,  # 按元数据过滤（可选）
        before: RunnableConfig | None = None,  # 列出此检查点之前的版本（可选）
        limit: int | None = None,  # 最多返回条数
    ) -> Iterator[CheckpointTuple]:
        """同步列出检查点历史；委托内存。"""
        return self._memory.list(config, filter=filter, before=before, limit=limit)

    def put(
        self,
        config: RunnableConfig,
        checkpoint: Checkpoint,
        metadata: CheckpointMetadata,
        new_versions: ChannelVersions,
    ) -> RunnableConfig:
        """同步写入检查点到内存（同步路径不刷 Redis；异步请用 aput）。"""
        return self._memory.put(config, checkpoint, metadata, new_versions)

    def put_writes(
        self,
        config: RunnableConfig,
        writes: Sequence[tuple[str, Any]],  # [(channel, value), ...] 通道写入列表
        task_id: str,  # 产生这些写入的任务 ID
        task_path: str = "",  # 任务路径（嵌套图用，默认可空）
    ) -> None:
        """同步写入 pending writes 到内存。"""
        self._memory.put_writes(config, writes, task_id, task_path)

    def delete_thread(self, thread_id: str) -> None:
        """同步删除内存中该 thread 的全部检查点数据（不删 Redis；删 Redis 用 adelete_thread）。"""
        self._memory.delete_thread(thread_id)

    async def aget_tuple(self, config: RunnableConfig) -> CheckpointTuple | None:
        """
        【异步读】续跑时 LangGraph / runner 会调这个。
        先 hydrate（Redis→内存），再 get_tuple。
        """
        thread_id = config["configurable"]["thread_id"]  # 从 config 取出流程实例 ID
        await self.hydrate_thread(thread_id)  # 确保内存有最新快照
        return self._memory.get_tuple(config)  # 读出 CheckpointTuple 或 None

    async def alist(
        self,
        config: RunnableConfig | None,
        *,
        filter: dict[str, Any] | None = None,
        before: RunnableConfig | None = None,
        limit: int | None = None,
    ) -> AsyncIterator[CheckpointTuple]:
        """异步列出：内部仍用同步 list，再逐个 yield（满足 AsyncIterator 接口）。"""
        for item in self.list(config, filter=filter, before=before, limit=limit):
            yield item  # 异步生成器：每次产出一条 CheckpointTuple

    async def aput(
        self,
        config: RunnableConfig,
        checkpoint: Checkpoint,
        metadata: CheckpointMetadata,
        new_versions: ChannelVersions,
    ) -> RunnableConfig:
        """
        【异步写主检查点】图每前进一截会调用。
        1) 写内存  2) 尝试刷 Redis（失败只打日志，不让整图崩溃）
        """
        thread_id = config["configurable"]["thread_id"]  # 当前对话图实例 ID
        result = self._memory.put(config, checkpoint, metadata, new_versions)  # 先落内存
        try:
            await self._persist_thread(thread_id)  # 再持久化到 Redis
        except Exception as e:  # Redis 抖动时降级：内存仍可用，但宕机可能丢断点
            logger.warning("graph_checkpoint_persist_failed", thread_id=thread_id, error=str(e))
        return result  # 返回更新后的 config（含新 checkpoint_id 等）

    async def aput_writes(
        self,
        config: RunnableConfig,
        writes: Sequence[tuple[str, Any]],
        task_id: str,
        task_path: str = "",
    ) -> None:
        """
        【异步写 pending writes】节点产生的通道写入先记内存，再刷 Redis。
        与 aput 成对：保证续跑时未提交的 writes 也不丢。
        """
        self._memory.put_writes(config, writes, task_id, task_path)  # 内存写入
        thread_id = config["configurable"]["thread_id"]  # 取出 thread_id 用于落盘
        try:
            await self._persist_thread(thread_id)  # 刷 Redis
        except Exception as e:
            logger.warning("graph_checkpoint_writes_persist_failed", thread_id=thread_id, error=str(e))

    async def adelete_thread(self, thread_id: str) -> None:
        """
        【异步删除】一轮图跑完后 runner finally 会调：清内存 + 删 Redis key。
        避免大量过期前的僵尸 checkpoint 占空间。
        """
        self.delete_thread(thread_id)  # 先清进程内数据
        try:
            await self._redis.delete(self._redis_key(thread_id))  # 再删 Redis key
        except Exception as e:  # 删失败不影响主流程，只告警
            logger.warning("graph_checkpoint_delete_failed", thread_id=thread_id, error=str(e))


# ---------- 模块级单例（类比 Spring @Bean 懒加载）----------

_checkpointer: RedisCheckpointSaver | None = None  # 全局唯一实例；None 表示尚未创建


def get_checkpointer(redis_client=None) -> RedisCheckpointSaver:
    """
    【工厂方法】获取 RedisCheckpointSaver 单例。

    首次调用时用 settings 里的前缀和 TTL 构造；之后复用同一对象。
    【类比】Spring 里 getBean(RedisCheckpointSaver.class)，懒汉式单例。

    注意：checkpoint 载荷为二进制 pickle，必须使用 decode_responses=False 的独立客户端
    （共享 redis_service.client 为 decode_responses=True，读取二进制会抛 UnicodeDecodeError）。
    """
    global _checkpointer  # 声明要修改模块级变量
    if _checkpointer is None:  # 双重检查的「懒创建」（单线程 asyncio 下足够）
        settings = get_settings()  # 读取配置（prefix、ttl）
        import redis.asyncio as aioredis  # 局部导入，避免顶层依赖

        # 独立 bytes 客户端（不与共享 str 客户端混用）
        bytes_client = aioredis.from_url(settings.redis_url, decode_responses=False)
        _checkpointer = RedisCheckpointSaver(
            bytes_client,  # bytes 客户端：读写二进制 pickle 安全
            key_prefix=settings.graph_checkpoint_prefix,  # Redis key 前缀
            ttl_seconds=settings.graph_checkpoint_ttl,  # 过期时间
        )
    return _checkpointer  # 返回单例供 builder / runner 使用


async def close_checkpointer() -> None:
    """
    关闭检查点器。建议 shutdown（main.lifespan）时调用。
    同时释放底层 Redis 连接池，避免热重载/重复启动时泄漏连接。
    """
    global _checkpointer  # 要改的模块级变量
    if _checkpointer is not None:
        try:
            await _checkpointer._redis.aclose()  # 关闭 bytes 客户端连接池
        except Exception:
            pass
    _checkpointer = None  # 释放引用，等待 GC；下次 get 会重建
    from app.graph.builder import reset_compiled_graph_cache  # 延迟 import，避免与 builder 循环引用

    reset_compiled_graph_cache()  # 清 lru_cache，下次 get_compiled_graph 会重新 compile
