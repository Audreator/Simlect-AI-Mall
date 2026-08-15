"""
================================================================================
文件：services/stream_service.py
角色：把「正在生成 / 完成 / 错误」发布到 Redis，供 websocket 转给前端
================================================================================

【这个文件干什么】
封装三种推送：push_chunk（打字机效果）、push_done（整轮结束+卡片）、push_error。
真正发到浏览器的是 api/websocket.py 订阅后转发；本类不持有 WS 连接。

【在流程中的位置】
agent_runtime.stream_llm_turn / finalize → stream_service → Redis → websocket → Vue

【如何使用】
业务里注入单例 stream_service，传入 user_id + message_id + 文本即可。
out_put_type：OUTPUTTING / DONE / ERROR（见 constants）。

【关联】MessageSendDTO、redis_service.publish_ws、api/websocket.ConnectionManager
================================================================================
"""

from app.constants import DONE, ERROR, OUTPUTTING, WS_MESSAGE_TYPE_AGENT  # 输出状态枚举 + WS 消息类型常量
from app.models.message_send import MessageSendDTO  # 推送给前端的 DTO（类似 Java MessageSendVO）
from app.services.redis_service import redis_service  # Redis publish 封装


class StreamService:
    """
    流式消息推送服务。
    类比 Java @Service：负责把 Agent 生成过程「广播」出去，但不直接操作 WebSocket。
    """

    async def _publish(self, dto: MessageSendDTO) -> None:
        """内部统一发布：补默认 messageType，再发到 Redis Pub/Sub 频道。"""

        if not dto.message_type:  # 调用方未指定类型时
            dto.message_type = WS_MESSAGE_TYPE_AGENT  # 默认为 Agent 对话消息
        await redis_service.publish_ws(dto.to_ws_dict())  # 转 dict 后 publish，websocket 模块订阅同一频道

    async def push_chunk(
        self,
        user_id: str,
        message_id: int,
        content: str,
        user_message: str | None = None,  # 可选：附带原始用户问题，前端可对齐展示
    ) -> None:
        """
        推送 LLM 流式输出的一小段文本（打字机效果）。
        out_put_type=OUTPUTTING 表示「还在生成中」。
        """

        dto = MessageSendDTO(
            message_id=message_id,
            user_id=user_id,
            user_message=user_message,
            assistant_message=content,  # 当前累积或增量助手回复
            out_put_type=OUTPUTTING,
        )
        await self._publish(dto)

    async def push_done(
        self,
        user_id: str,
        message_id: int,
        assistant_message: str = "",
        biz_type: str | None = None,  # 业务卡片类型，如 CONFIRM_ORDER
        user_message: str | None = None,
    ) -> None:
        """
        推送一轮对话正常结束事件。
        out_put_type=DONE；前端可停止 loading、展示最终内容与卡片。
        """

        dto = MessageSendDTO(
            message_id=message_id,
            user_id=user_id,
            user_message=user_message,
            assistant_message=assistant_message,
            biz_type=biz_type,
            out_put_type=DONE,
        )
        await self._publish(dto)

    async def push_error(
        self,
        user_id: str,
        message_id: int,
        error: str,
        biz_type: str | None = None,
    ) -> None:
        """
        推送错误/异常结束事件（队列满、LLM 失败、用户取消等）。
        assistant_message 字段复用为错误提示文案。
        """

        dto = MessageSendDTO(
            message_id=message_id,
            user_id=user_id,
            assistant_message=error,  # 错误信息展示给用户
            biz_type=biz_type,
            out_put_type=ERROR,
        )
        await self._publish(dto)


stream_service = StreamService()  # 模块级单例，全局 import 使用
