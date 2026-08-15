"""
Agent 消息服务模块（Message Service）。

职责：用户/助手消息的持久化、历史分页加载、记忆系统轮次加载、
助手回复压缩调度，以及管理员侧 CRUD 查询。
类似 Java @Service AgentMessageService + MyBatis/JdbcTemplate 操作 agent_message 表。

核心表：agent_message（message_id 自增主键，status 表示 NORMAL/COMPLETE/INTERRUPTED/CANCEL）
与 memory 模块配合：load_turns_for_memory 供 ContextBuilder 构建工作记忆。
"""

from datetime import datetime  # 日期时间，类似 java.time.LocalDateTime

import json  # JSON 解析，类似 Jackson

from app.config.settings import get_settings  # history_message_limit 等配置

from app.constants import MSG_STATUS_CANCEL, MSG_STATUS_COMPLETE, MSG_STATUS_INTERRUPTED, MSG_STATUS_NORMAL  # 消息状态常量，类似 enum

from app.db.pool import acquire  # MySQL 异步连接池，with 类似 try-with-resources

from app.memory.assistant_condense import (  # 助手回复 LLM 压缩与同步截断
    schedule_assistant_condense,
    truncate_assistant_for_history,
)
from app.services.redis_service import redis_service  # Redis：历史压缩缓存
from app.utils.biz_payload import trim_assistant  # 入库前截断过长助手回复


class AgentMessageService:
    """
    Agent 消息 CRUD 服务类。

    类似 Java @Service public class AgentMessageService。
    模块末尾 agent_message_service 为单例 bean。
    """

    async def save_user_message(self, user_id: str, message: str) -> dict:
        """
        保存用户发送的一条消息（status=NORMAL，待 Agent 回复）。

        参数:
            user_id: 用户 ID
            message: 用户文本

        返回:
            含 messageId、userId、userMessage、status、sendTime 的 dict（camelCase）
        """
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")  # 格式化为 SQL 时间字符串

        async with acquire() as cur:  # 从池取连接，退出自动 release
            await cur.execute(
                "INSERT INTO agent_message (user_message, send_time, user_id, status) VALUES (%s, %s, %s, %s)",
                (message, now, user_id, MSG_STATUS_NORMAL),  # 占位符防 SQL 注入
            )
            message_id = cur.lastrowid  # 自增主键，类似 JDBC getGeneratedKeys().getInt(1)

        return {  # 返回 Map 结构给上层，字段 camelCase 对接前端
            "messageId": message_id,
            "userId": user_id,
            "userMessage": message,
            "status": MSG_STATUS_NORMAL,
            "sendTime": now,
        }

    async def complete_message(
        self,
        message_id: int,
        assistant_message: str,
        biz_type: str | None = None,
        biz_data: str | None = None,
    ) -> None:
        """
        Agent 回复完成后更新消息：写入 assistant_message，status→COMPLETE。

        优先 UPDATE ... WHERE status=NORMAL，防止并发重复完成；失败则无条件更新。

        参数:
            message_id: 消息主键
            assistant_message: 助手完整回复
            biz_type: 业务类型（如商品搜索卡片）
            biz_data: 业务 JSON 数据
        """
        trimmed = trim_assistant(assistant_message)  # 过长内容截断再入库
        async with acquire() as cur:

            rows = await cur.execute(  # 返回受影响行数
                """UPDATE agent_message SET assistant_message=%s, biz_type=%s, biz_data=%s, status=%s
                   WHERE message_id=%s AND status=%s""",
                (trimmed, biz_type, biz_data, MSG_STATUS_COMPLETE, message_id, MSG_STATUS_NORMAL),
            )
            if rows == 0:  # 状态已不是 NORMAL（可能已 interrupt / cancel / 已完成）
                # 仅中断状态可降级补写完成内容；CANCEL/COMPLETE 等终态不被覆盖
                await cur.execute(
                    """UPDATE agent_message SET assistant_message=%s, biz_type=%s, biz_data=%s, status=%s
                       WHERE message_id=%s AND status=%s""",
                    (trimmed, biz_type, biz_data, MSG_STATUS_COMPLETE, message_id, MSG_STATUS_INTERRUPTED),
                )

    async def cancel_message(self, user_id: str, message_id: int) -> None:
        """
        取消消息（用户主动取消或空回复中断时）。

        参数:
            user_id: 用户 ID（校验归属）
            message_id: 消息 ID
        """
        async with acquire() as cur:
            # 仅可取消「待处理」消息，不覆盖已完成/已中断等终态
            await cur.execute(
                "UPDATE agent_message SET status=%s WHERE user_id=%s AND message_id=%s AND status=%s",
                (MSG_STATUS_CANCEL, user_id, message_id, MSG_STATUS_NORMAL),
            )

    async def interrupt_message(
        self,
        user_id: str,
        message_id: int,
        partial_message: str,
        biz_type: str | None = None,
    ) -> None:
        """
        中断流式输出：保存已生成的部分助手回复，status→INTERRUPTED。

        若部分内容为空则改为 cancel_message。

        参数:
            user_id: 用户 ID
            message_id: 消息 ID
            partial_message: 已流式输出的片段拼接
            biz_type: 可选业务类型
        """
        trimmed = trim_assistant(partial_message)
        if not trimmed:  # 无任何有效助手内容
            await self.cancel_message(user_id, message_id)
            return
        async with acquire() as cur:
            await cur.execute(
                """UPDATE agent_message SET assistant_message=%s, biz_type=%s, status=%s
                   WHERE message_id=%s AND user_id=%s AND status=%s""",
                (trimmed, biz_type, MSG_STATUS_INTERRUPTED, message_id, user_id, MSG_STATUS_NORMAL),
            )

    async def load_history(
        self,
        user_id: str,
        page_no: int = 1,
        max_message_id: int | None = None,
        page_size: int = 15,
    ) -> dict:
        """
        分页加载聊天历史（前端展示用，含 COMPLETE/INTERRUPTED 等）。

        支持 max_message_id 游标：只查 message_id 更小的记录（上拉加载更多）。

        参数:
            user_id: 用户 ID
            page_no: 页码，从 1 开始
            max_message_id: 可选游标上限
            page_size: 每页条数

        返回:
            totalCount、pageSize、pageNo、pageTotal、list
        """
        offset = (max(page_no, 1) - 1) * page_size  # 分页 OFFSET
        where = "user_id=%s"
        params: list = [user_id]  # 动态 SQL 参数列表，类似 List<Object>
        if max_message_id:  # 游标分页条件
            where += " AND message_id < %s"
            params.append(max_message_id)
        async with acquire() as cur:
            await cur.execute(f"SELECT COUNT(*) AS cnt FROM agent_message WHERE {where}", params)
            count_row = await cur.fetchone()
            total = count_row["cnt"] if count_row else 0
            await cur.execute(
                f"""SELECT message_id, assistant_message, user_message, send_time, user_id, status, biz_type, biz_data
                    FROM agent_message WHERE {where} ORDER BY message_id DESC LIMIT %s OFFSET %s""",
                params + [page_size, offset],  # 列表拼接，类似 Java List.addAll
            )
            rows = await cur.fetchall()
        page_total = (total + page_size - 1) // page_size if page_size else 0  # ceil 总页数
        return {
            "totalCount": total,
            "pageSize": page_size,
            "pageNo": page_no,
            "pageTotal": page_total,
            "list": [_row_to_dict(r) for r in rows],  # 行 → 前端 DTO
        }

    async def load_recent_history(self, user_id: str, limit: int = 15) -> list[dict]:
        """
        加载最近 N 条完整/中断消息，转为 role/content 格式供 LLM 使用。

        参数:
            user_id: 用户 ID
            limit: 最多条数

        返回:
            [{"role":"user"|"assistant","content":"..."}, ...] 时间正序
        """
        async with acquire() as cur:
            await cur.execute(
                """SELECT user_message, assistant_message, biz_type FROM agent_message
                   WHERE user_id=%s AND status IN (%s, %s) ORDER BY message_id DESC LIMIT %s""",
                (user_id, MSG_STATUS_COMPLETE, MSG_STATUS_INTERRUPTED, limit),
            )
            rows = list(await cur.fetchall())
        rows.reverse()  # DB 降序 → 反转为时间正序（旧在前）
        history = []  # 构建 LangChain/OpenAI 风格消息列表
        for r in rows:
            if r.get("user_message"):
                history.append({"role": "user", "content": r["user_message"]})
            assistant = r.get("assistant_message")
            if _should_include_assistant_in_history(assistant):  # 过滤 JSON 卡片

                history.append({"role": "assistant", "content": assistant[:500]})  # 单条最多 500 字
        return history

    async def load_turns_for_memory(self, user_id: str) -> list[dict]:
        """
        为记忆/上下文系统加载历史「轮次」列表。

        每轮含 message_id、user_message、assistant_message、assistant_for_history（压缩版）等。
        窗口大小约为 history_message_limit * 4，至少 60 条。

        参数:
            user_id: 用户 ID

        返回:
            时间正序的 turn dict 列表
        """
        settings = get_settings()
        limit = max(settings.history_message_limit * 4, 60)  # 记忆窗口比展示历史更大
        async with acquire() as cur:
            await cur.execute(
                """SELECT message_id, user_message, assistant_message, biz_type, biz_data
                   FROM agent_message
                   WHERE user_id=%s AND status IN (%s, %s)
                   ORDER BY message_id DESC LIMIT %s""",
                (user_id, MSG_STATUS_COMPLETE, MSG_STATUS_INTERRUPTED, limit),
            )
            rows = list(await cur.fetchall())
        rows.reverse()  # 正序
        turns: list[dict] = []
        for row in rows:
            assistant = row.get("assistant_message")

            condensed = await redis_service.get_history_condensed(  # 优先读 Redis 压缩结果
                user_id, int(row["message_id"])
            )
            if not condensed and assistant:

                from app.utils.biz_payload import parse_product_search_message  # 延迟 import 防循环依赖

                intro, _ = parse_product_search_message(assistant)  # 从搜索卡片提取 intro 文本
                if intro:
                    condensed = intro
            if not condensed and assistant and self.should_include_in_working_memory(assistant):

                condensed = truncate_assistant_for_history(assistant)  # 同步规则截断
                schedule_assistant_condense(user_id, int(row["message_id"]), assistant)  # 异步 LLM 精压缩
            turns.append(
                {
                    "message_id": int(row["message_id"]),
                    "user_message": row.get("user_message") or "",
                    "assistant_message": assistant or "",
                    "assistant_for_history": condensed,  # ContextBuilder 优先使用
                    "biz_type": row.get("biz_type"),
                    "biz_data": row.get("biz_data"),
                }
            )
        return turns

    @staticmethod  # 静态方法，类似 Java static method，无需 self
    def should_include_in_working_memory(assistant_message: str | None) -> bool:
        """
        判断助手回复是否应纳入工作记忆（供 ContextBuilder 调用）。

        参数:
            assistant_message: 助手回复文本

        返回:
            True 表示纯文本可纳入；JSON 卡片等返回 False
        """
        return _should_include_assistant_in_history(assistant_message)

    async def count_user_messages(self, user_id: str) -> int:
        """
        统计用户消息总数，供 InputGuardrail.check_chat_limit 使用。

        参数:
            user_id: 用户 ID

        返回:
            消息条数
        """
        async with acquire() as cur:
            await cur.execute("SELECT COUNT(*) AS cnt FROM agent_message WHERE user_id=%s", (user_id,))
            row = await cur.fetchone()
        return row["cnt"] if row else 0

    async def admin_load_messages(
        self,
        page_no: int = 1,
        page_size: int = 15,
        user_id: str | None = None,
    ) -> dict:
        """
        管理员分页查询全部/指定用户消息。

        参数:
            page_no: 页码
            page_size: 每页大小，限制 1~100
            user_id: 可选，按用户过滤

        返回:
            分页结果 dict
        """
        page_no = max(int(page_no or 1), 1)
        page_size = max(1, min(int(page_size or 15), 100))  # clamp 到 [1,100]
        offset = (page_no - 1) * page_size
        where = "1=1"  # 恒真，便于拼接 AND
        params: list = []
        if user_id:
            where += " AND user_id=%s"
            params.append(user_id)
        async with acquire() as cur:
            await cur.execute(f"SELECT COUNT(*) AS cnt FROM agent_message WHERE {where}", params)
            count_row = await cur.fetchone()
            total = count_row["cnt"] if count_row else 0
            await cur.execute(
                f"""SELECT message_id, assistant_message, user_message, send_time, user_id, status, biz_type, biz_data
                    FROM agent_message WHERE {where}
                    ORDER BY message_id DESC LIMIT %s OFFSET %s""",
                params + [page_size, offset],
            )
            rows = await cur.fetchall()
        page_total = (total + page_size - 1) // page_size if page_size else 0
        return {
            "totalCount": total,
            "pageSize": page_size,
            "pageNo": page_no,
            "pageTotal": page_total,
            "list": [_row_to_dict(r) for r in rows],
        }

    async def admin_get_message(self, message_id: int) -> dict | None:
        """
        管理员按 message_id 查询单条消息。

        参数:
            message_id: 主键

        返回:
            DTO dict；不存在返回 None，类似 Optional.empty()
        """
        async with acquire() as cur:
            await cur.execute(
                """SELECT message_id, assistant_message, user_message, send_time, user_id, status, biz_type, biz_data
                   FROM agent_message WHERE message_id=%s""",
                (message_id,),
            )
            row = await cur.fetchone()
        return _row_to_dict(row) if row else None

    async def admin_delete_message(self, message_id: int) -> bool:
        """
        管理员删除一条消息。

        参数:
            message_id: 主键

        返回:
            True 表示删除了至少一行
        """
        async with acquire() as cur:
            rows = await cur.execute(
                "DELETE FROM agent_message WHERE message_id=%s",
                (message_id,),
            )
        return bool(rows)  # 受影响行数 > 0


def _row_to_dict(row: dict) -> dict:
    """
    数据库行（snake_case）转为前端 DTO（camelCase）。

    参数:
        row: aiomysql DictCursor 行

    返回:
        前端 JSON 字段名格式的 dict
    """
    return {
        "messageId": row["message_id"],
        "assistantMessage": row.get("assistant_message") or "",
        "userMessage": row.get("user_message") or "",
        "sendTime": row["send_time"].strftime("%Y-%m-%d %H:%M:%S") if row.get("send_time") else None,
        "userId": row["user_id"],
        "status": row["status"],
        "bizType": row.get("biz_type"),
        "bizData": row.get("biz_data"),
    }


agent_message_service = AgentMessageService()  # 模块级单例，全项目 import 此实例


def _should_include_assistant_in_history(assistant_message: str | None) -> bool:
    """
    判断助手回复是否应进入 LLM 历史（排除 JSON 卡片类结构化消息）。

    纯 JSON 数组、ACTION_CONFIRM/PRODUCT_SEARCH_RESULT 等类型不纳入，
    避免模型把卡片 JSON 当自然语言复读。

    参数:
        assistant_message: 助手回复

    返回:
        True 表示可纳入历史
    """
    text = (assistant_message or "").strip()
    if not text:
        return False

    if text.startswith("["):  # JSON 数组开头 → 多为卡片列表
        return False
    if text.startswith("{"):  # JSON 对象
        try:
            obj = json.loads(text)

            if isinstance(obj, dict) and obj.get("type") in (  # 已知卡片类型排除
                "ACTION_CONFIRM",
                "PRODUCT_SEARCH_RESULT",
            ):
                return False
        except json.JSONDecodeError:
            pass  # 非合法 JSON 仍按下面规则处理

        return False  # 其他 { 开头 JSON 也不进历史
    return True  # 普通纯文本可纳入
