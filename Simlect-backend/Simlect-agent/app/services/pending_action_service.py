"""
================================================================================
文件：services/pending_action_service.py
角色：人机确认（Human-in-the-loop）——写操作的「待办单」存在 Redis
================================================================================

【这个文件干什么】
LLM/MCP 的 PROPOSE_* 不会改订单，只 create_pending：
生成 act_xxx token、摘要、确认文案，TTL 写入 Redis。
用户点确认 → confirm（分布式锁防双击）→ 把 pending 交给 executor（通常是 action_execute）。
用户点取消 → cancel 删掉 pending。

【流程】
PROPOSE_REFUND → create_pending → 前端确认卡
POST /confirmAction → confirm → ActionExecuteService.execute → Java

【如何使用】
业务工具里：pending = await pending_action_service.create_pending(...)
路由里：await pending_action_service.confirm(user_id, token, executor)

【关联】
redis_service、action_execute_service、utils.biz_payload.ACTION_LABELS、mcp_tools_service
================================================================================
"""

import json  # import：JSON 序列化/反序列化 — 把 params dict 存为字符串写入 Redis
import time  # import：获取 Unix 时间戳 — time.time() 返回秒，乘 1000 得毫秒
import uuid  # import：生成全局唯一 ID — uuid4().hex 类似 Java UUID.randomUUID()

import structlog  # import：结构化日志库 — 审计确认删除失败的告警

from app.services.redis_service import redis_service  # import：Redis 读写封装 — save/get/delete/lock pending

from app.utils.biz_payload import ACTION_LABELS  # import：操作类型 → (标签, 确认文案, 风险提示) 的静态映射表

logger = structlog.get_logger()  # 获取本模块 logger 实例


class PendingActionService:  # 待确认操作服务 — 类比 Java @Service，管理 Redis 中的 pending 生命周期
    """待确认操作服务（类比 Java @Service）。对外主 API：create_pending / confirm / cancel。"""

    STATUS_PENDING = 0  # 状态常量 — 待用户确认（类似 enum ActionStatus.PENDING）
    STATUS_CONFIRMED = 1  # 状态常量 — 用户已确认并成功执行
    STATUS_CANCELLED = 2  # 状态常量 — 用户主动取消

    async def create_pending(
        self,
        action_type: str,  # 操作类型枚举字符串：REFUND / CONFIRM_RECEIPT / PRODUCT_REVIEW / RECOMMENT
        user_id: str,  # 发起用户 ID — confirm/cancel 时校验归属
        params: dict,  # 业务参数 Map — 确认后 JSON 解析交给 executor 使用
        summary: str,  # 人类可读摘要 — 展示在确认卡片上
    ) -> dict:  # 返回完整 pending 对象 — 含 token、confirmText 等，供 mcp_tools_service 生成 LLM 回复

        await redis_service.ensure_connected()  # 确保 Redis 连接池已建立 — 类似 DataSource 预热
        token = f"act_{uuid.uuid4().hex}"  # 生成唯一令牌 — act_ 前缀标识 + 32 位 hex（无连字符）

        # ACTION_LABELS.get 查 UI 文案三元组 — 未知 action_type 则回退 (action_type, "确认", "")
        label, confirm_text, risk_tip = ACTION_LABELS.get(action_type, (action_type, "确认", ""))

        pending = {  # 构造 pending 记录 Map — 序列化后存入 Redis，TTL 自动过期
            "token": token,  # 唯一标识 — 前端确认/取消时回传此 token
            "userId": user_id,  # 归属用户 — confirm 时校验 pending.userId == 当前用户
            "messageId": await redis_service.get_bound_message_id(user_id),  # 绑定当前对话消息 ID — 前端定位卡片
            "actionType": action_type,  # 操作类型 — execute 时按此分发到 JavaBridge 不同方法
            "paramsJson": json.dumps(params, ensure_ascii=False),  # 参数 JSON 字符串 — ensure_ascii=False 保留中文
            "summary": summary,  # 卡片摘要 — 如「退款订单项 xxx，金额 ¥99」
            "confirmText": confirm_text,  # 确认按钮文案 — 如「确认退款」
            "riskTip": risk_tip,  # 风险提示 — 如「退款后不可撤销」
            "status": self.STATUS_PENDING,  # 初始状态：待确认 — 整数常量
            "createTime": int(time.time() * 1000),  # 创建时间毫秒戳 — 类似 System.currentTimeMillis()
        }
        await redis_service.save_pending_action(token, pending)  # 写入 Redis — 带 TTL，过期自动删除
        return pending  # 返回给 mcp_tools_service — 用于构造 ToolInvokeResult 和确认卡

    async def get_by_token(self, token: str) -> dict | None:  # 按 token 查询 pending — 不存在或 TTL 过期返回 None

        await redis_service.ensure_connected()  # 确保 Redis 连接可用
        return await redis_service.get_pending_action(token)  # 从 Redis 读取并反序列化为 dict

    async def load_owned(self, user_id: str, token: str) -> dict:  # 加载 pending 并校验水平权限 — 非本人抛 ValueError

        pending = await self.get_by_token(token)  # 先从 Redis 查 pending 记录
        if not pending:  # None 表示不存在或 TTL 已过期
            raise ValueError("操作已过期或不存在")  # 业务异常 — API 层转为 HTTP 400
        if pending.get("userId") != user_id:  # 水平权限校验 — 用户 A 不能操作用户 B 的 pending
            raise ValueError("无权操作该请求")
        return pending  # 校验通过 — 返回 pending Map 供后续 confirm/cancel 使用

    async def confirm(
        self,
        user_id: str,  # 当前登录用户 ID
        token: str,  # pending 令牌 — 前端从确认卡获取
        executor,  # 执行回调 — 通常是 action_execute_service.execute 方法引用
    ) -> tuple[str, bool, str]:  # 返回三元组：(actionType, 是否成功, 结果文案)

        await redis_service.ensure_connected()  # 确保 Redis 可用
        # Serialize double-clicks: only one request may hold the lock (owner-token lock).
        # ↑ 防双击 — 同一 token 同时只能有一个 confirm 请求持有锁（带持有者标识，解锁校验防误删）

        lock_owner = await redis_service.try_lock_pending_action(token, ttl_seconds=120)  # 分布式锁 — SET NX EX 120s
        if lock_owner is None:  # 锁已被其他请求占用 — 用户快速双击场景
            raise ValueError("操作处理中，请勿重复点击")

        try:  # try-finally 确保释放锁 — 类似 synchronized { try { ... } finally { unlock(); } }
            pending = await self.load_owned(user_id, token)  # 加载 pending 并校验 userId 归属
            if pending.get("status") != self.STATUS_PENDING:  # 已确认/已取消/状态异常
                raise ValueError("该操作已处理或已过期")

            # 执行前原子置「处理中」标记（防 delete 失败/进程崩溃后重试导致写操作二次执行）
            if not await redis_service.try_mark_pending_executing(token):
                raise ValueError("该操作正在处理中，请勿重复提交")

            # 执行前 CAS 原子落定状态 PENDING→CONFIRMED：状态先行持久化，
            # 此后无论 delete 成功与否，重试都会被 load_owned 状态检查拒绝 —— 双失败窗口归零
            if not await redis_service.cas_pending_status(
                    token, self.STATUS_PENDING, self.STATUS_CONFIRMED):
                raise ValueError("该操作已处理或已过期")

            try:  # 内层 try — 区分业务失败（ValueError）与系统异常（Exception）
                result_message = await executor(pending)  # 调用 executor — 传入 pending，返回用户可读结果文案
                try:
                    await redis_service.delete_pending_action(token)  # 成功后删除 Redis — 防止重复确认
                except Exception:
                    # 删除失败：状态已落为 CONFIRMED（CAS 先行），重试被拒，无需补救
                    logger.warning("pending_confirm_delete_failed", token=token)
                    return pending.get("actionType"), False, "操作已执行成功，结果请稍后刷新查看"
                return pending.get("actionType"), True, result_message  # 三元组：类型、成功、文案
            except ValueError as e:  # 业务校验失败 — 如 Java 返回 info="订单已退款"
                # 业务层明确拒绝（未执行写操作）：回滚状态允许用户修正后重试
                try:
                    await redis_service.cas_pending_status(
                        token, self.STATUS_CONFIRMED, self.STATUS_PENDING)
                except Exception:
                    pass  # 回滚失败：状态保持 CONFIRMED，该操作需重新发起（保守防重复执行）
                return pending.get("actionType"), False, str(e)  # 失败但不删 pending — 用户可修正后重试
            except Exception:  # 系统异常 — 网络超时、Java 500 等
                # executor 可能已部分执行写操作：保持 CONFIRMED 不回滚（重试被状态拒绝），防二次执行
                return pending.get("actionType"), False, "系统处理异常，请稍后重试"  # 通用失败文案
            finally:
                # 无论结果如何释放「处理中」标记（允许后续重试）
                await redis_service.clear_pending_executing(token)
        finally:
            await redis_service.unlock_pending_action(token, lock_owner)  # 无论成功失败都释放分布式锁

    async def cancel(self, user_id: str, token: str) -> None:  # 用户点取消 — 删除 pending，不执行写操作

        await redis_service.ensure_connected()  # 确保 Redis 可用
        lock_owner = await redis_service.try_lock_pending_action(token, ttl_seconds=30)  # 短 TTL 锁 — 防并发取消/确认
        if lock_owner is None:
            raise ValueError("操作处理中，请稍后再试")

        try:  # try-finally 释放锁
            pending = await self.load_owned(user_id, token)  # 加载并校验归属
            if pending.get("status") != self.STATUS_PENDING:  # 非待确认状态 — 已处理或过期
                raise ValueError("该操作已处理或已过期")
            pending["status"] = self.STATUS_CANCELLED  # 内存标记已取消 — 实际靠 delete 清理
            await redis_service.delete_pending_action(token)  # 从 Redis 删除 pending 记录
        finally:
            await redis_service.unlock_pending_action(token, lock_owner)  # 释放锁（持有者校验）


pending_action_service = PendingActionService()  # 模块级单例 — mcp_tools_service / agent 路由直接 import
