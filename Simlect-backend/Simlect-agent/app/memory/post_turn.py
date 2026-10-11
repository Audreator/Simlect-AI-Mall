"""
单轮对话结束后处理模块（Post Turn）。

职责：每轮 Agent 回复完成后，同步更新会话记忆中的 state，
并触发记忆压缩、助手回复精简等异步/后续任务。
类似 Java 里 @Transactional 方法在业务完成后更新 Session 并发 MQ 消息。

调用时机：Agent 流式输出结束、消息入库 complete 之后。
"""

from __future__ import annotations

import json  # 解析助手消息中的 <!--BIZ:...--> 业务标记
import re  # 正则匹配 BIZ 注释块

import structlog  # 结构化日志

from app.memory.assistant_condense import schedule_assistant_condense, truncate_assistant_for_history  # 助手回复压缩
from app.utils.biz_payload import format_price_text  # 价格展示文本（12.0→12）
from app.memory.compress_service import compress_service  # 长上下文摘要压缩调度
from app.memory.models import SessionMemory  # 会话记忆模型
from app.memory.session_memory_service import session_memory_service  # 记忆读写服务
from app.services.prompt_service import load_agent_prompt  # 加载 Agent 系统提示（估算 token 用）
from app.services.redis_service import redis_service  # Redis 单例

logger = structlog.get_logger()


def _extract_search_result_cards(
    assistant_cards: str | None,
    assistant_text: str | None,
    tool_biz: dict | None,
) -> list[dict]:
    """
    按展示顺序提取本轮搜索/推荐的商品卡片清单。

    与用户页面上的卡片顺序严格一致（前端按数组顺序渲染）：
    1) 优先解析 assistant_cards / assistant_text 中的商品卡片 JSON 数组；
    2) 回退用 tool_biz 的 productIds + productNames 拼接（无价格）。
    订单卡片（元素含 orderId、无 productId）不视为商品卡片。
    """
    for raw in (assistant_cards, assistant_text):
        text = (raw or "").strip()
        if not text.startswith("["):
            continue
        try:
            parsed = json.loads(text)
        except json.JSONDecodeError:
            continue
        if not isinstance(parsed, list):
            continue
        cards: list[dict] = []
        for item in parsed:
            if not isinstance(item, dict):
                continue
            pid = item.get("productId") or item.get("product_id")
            if not pid:
                continue
            price = item.get("minPrice")
            if price is None:
                price = item.get("min_price")
            cards.append({
                "productId": str(pid),
                "productName": item.get("productName") or item.get("product_name") or "",
                "minPrice": price,
            })
        if cards:
            return cards[:12]

    ids = (tool_biz or {}).get("productIds") or []
    names = (tool_biz or {}).get("productNames") or []
    fallback: list[dict] = []
    for index, pid in enumerate(ids[:12]):
        fallback.append({
            "productId": str(pid),
            "productName": str(names[index]) if index < len(names) and names[index] else "",
            "minPrice": None,
        })
    return fallback


def _cards_history_summary(cards: list[dict]) -> str:
    """把展示给用户的商品卡片清单压成一行可读文本（写入历史浓缩缓存，供后续轮次指代）。"""
    parts: list[str] = []
    for index, card in enumerate(cards[:12], start=1):
        name = (card.get("productName") or "").strip() or "商品"
        price_text = format_price_text(card.get("minPrice"))
        price_suffix = f"（¥{price_text}）" if price_text else ""
        parts.append(f"{index}. {name}{price_suffix}")
    text = "已向用户展示商品卡片（顺序与页面一致）：" + "；".join(parts) + "。"
    return truncate_assistant_for_history(text)


# 匹配助手回复 HTML 注释中的业务 JSON：<!--BIZ:{"productIds":[...]}-->
_BIZ_MARKER = re.compile(r"<!--BIZ:([\s\S]*?)-->", re.I)  # re.I 忽略大小写


class PostTurnService:
    """
    轮次后处理服务。

    类似 Java @Service PostTurnService，run() 为入口方法。
    """

    async def run(
        self,
        user_id: str,
        message_id: int,
        user_text: str,
        assistant_text: str,
        tools_called: list[str],
        tool_biz: dict | None,
        card: dict | None,
        working_turns: list[dict],
        working_oldest_id: int | None,
        assistant_cards: str | None = None,
    ) -> None:
        """
        执行一轮对话结束后的全部后处理逻辑。

        参数:
            user_id: 用户 ID
            message_id: 本轮消息 DB 主键
            user_text: 用户输入
            assistant_text: 助手完整回复
            tools_called: 本轮调用的工具名列表
            tool_biz: 工具返回的结构化业务数据（商品 ID 等）
            card: 前端传来的商品卡片（若有）
            assistant_cards: 本轮助手卡片 JSON（商品/订单/确认卡，可为空）
            working_turns: 构建上下文时选中的工作记忆轮次
            working_oldest_id: 工作记忆最旧 message_id
        """
        # 1. 加载当前会话记忆
        memory = await session_memory_service.load(user_id, redis_service.client)

        # 2. 轮次计数 +1，类似 session.setAttribute("turnCount", n+1)
        memory.state["turnCount"] = int(memory.state.get("turnCount") or 0) + 1

        # 3. 同步各类运行时状态到 memory.state
        await self._sync_consult_product(user_id, memory, card)
        await self._sync_pending_action(user_id, memory, assistant_text)
        self._sync_tool_results(memory, tools_called, tool_biz, assistant_text)

        # 3.5 记录「已展示给用户的商品卡片」（有序），供后续轮次解析「第一个商品」指代
        search_cards = _extract_search_result_cards(assistant_cards, assistant_text, tool_biz)
        if search_cards:
            memory.state["lastSearchResults"] = search_cards[:12]
        elif "SEARCH_PRODUCTS" in (tools_called or []):
            # 本轮确实搜过但无卡片 → 显式清空（与「未调用」区分，防旧清单误指代）
            memory.state["lastSearchResults"] = []

        # 4. 持久化到 Redis + MySQL
        await session_memory_service.save(memory, redis_service.client)

        # 5. 判断是否需调度长上下文压缩（摘要生成）
        system_prompt = await load_agent_prompt()

        await compress_service.maybe_schedule_compress(
            user_id,
            memory,
            working_turns,
            working_oldest_id,
            user_text,
            system_prompt,
        )

        # 6/7. 卡片轮次写「可读商品清单」，替代被截断的 JSON 片段；
        #      普通轮次保持原「同步截断 + 异步 LLM 压缩」链路。
        if search_cards:
            await redis_service.save_history_condensed(
                user_id, message_id, _cards_history_summary(search_cards)
            )
        else:
            immediate = truncate_assistant_for_history(assistant_text)
            if immediate:
                await redis_service.save_history_condensed(user_id, message_id, immediate)
            schedule_assistant_condense(user_id, message_id, assistant_text)

    async def _sync_consult_product(
        self, user_id: str, memory: SessionMemory, card: dict | None
    ) -> None:
        """
        同步「当前咨询商品」到 memory.state.consultProduct。

        优先级：本轮 card > Redis 缓存的 consult_product。

        参数:
            user_id: 用户 ID
            memory: 会话记忆（就地修改 state）
            card: 商品卡片 dict，含 productId 等
        """
        # 本轮有商品卡片则直接写入 state
        if card and card.get("productId"):
            memory.state["consultProduct"] = {
                "productId": str(card["productId"]),  # 统一转字符串 ID
                "productName": card.get("productName"),
                "minPrice": card.get("minPrice"),
                "cover": card.get("cover"),
            }
            return

        # 否则从 Redis 读用户最近咨询商品（跨轮保持）
        cached = await redis_service.get_consult_product(user_id)
        if not cached:
            return
        normalized = cached  # 可能已是规范化结构

        # 兼容 camelCase 与 snake_case 字段名
        memory.state["consultProduct"] = {
            "productId": str(normalized.get("productId") or normalized.get("product_id") or ""),
            "productName": normalized.get("productName") or normalized.get("product_name"),
            "minPrice": normalized.get("minPrice") or normalized.get("min_price"),
            "categoryId": normalized.get("categoryId") or normalized.get("category_id"),
            "cover": normalized.get("cover"),
        }

    async def _sync_pending_action(self, user_id: str, memory: SessionMemory, assistant_text: str) -> None:
        """
        从助手回复中的 act token 同步「待确认操作」到 state.pendingAction。

        act token 格式如 【act_32位hex】，对应 PROPOSE_* 工具生成的待确认项。

        参数:
            user_id: 用户 ID（校验 pending 归属）
            memory: 会话记忆
            assistant_text: 助手回复全文
        """
        # 延迟 import 避免循环依赖，类似 Java 方法内 new 或 @Lazy
        from app.services.pending_action_service import pending_action_service
        from app.utils.biz_payload import extract_act_token_id

        token_id = extract_act_token_id(assistant_text or "")  # 提取 token
        if not token_id:  # 无待确认操作
            memory.state["pendingAction"] = None
            return
        pending = await pending_action_service.get_by_token(token_id)

        # token 无效或不属于当前用户 → 清空
        if not pending or pending.get("userId") != user_id:
            memory.state["pendingAction"] = None
            return
        memory.state["pendingAction"] = {
            "token": pending.get("token"),
            "actionType": pending.get("actionType"),  # 如 REFUND、PRODUCT_REVIEW
            "summary": pending.get("summary"),  # 给用户看的摘要
        }

    def _sync_tool_results(
        self,
        memory: SessionMemory,
        tools_called: list[str],
        tool_biz: dict | None,
        assistant_text: str,
    ) -> None:
        """
        同步上轮工具调用结果摘要到 state.lastToolResults。

        供上下文块提示模型「上轮搜过什么」，避免幻觉复用过期结果。

        参数:
            memory: 会话记忆
            tools_called: 工具名列表
            tool_biz: 工具层直接传来的 biz dict
            assistant_text: 助手回复（可能含 BIZ 注释）
        """
        # setdefault：键不存在则设默认值并返回引用，类似 Map.computeIfAbsent
        last = memory.state.setdefault(
            "lastToolResults",
            {
                "searchedProducts": [],  # 商品 ID 列表
                "searchedProductNames": [],
                "queriedOrders": [],
                "viewedProductIds": [],
            },
        )

        # 优先用工具层结构化数据
        if tool_biz:
            if tool_biz.get("productIds"):
                last["searchedProducts"] = tool_biz["productIds"][:12]  # 最多保留 12 条
            if tool_biz.get("productNames"):
                last["searchedProductNames"] = tool_biz["productNames"][:12]
            if tool_biz.get("orderIds"):
                last["queriedOrders"] = tool_biz["orderIds"][:12]
            return

        # 其次解析助手消息中的 <!--BIZ:json-->
        marker = _BIZ_MARKER.search(assistant_text or "")
        if marker:
            try:
                payload = json.loads(marker.group(1))  # group(1) 为捕获组内容
                if payload.get("productIds"):
                    last["searchedProducts"] = payload["productIds"]
                if payload.get("productNames"):
                    last["searchedProductNames"] = payload["productNames"]
            except json.JSONDecodeError:
                pass  # 解析失败则忽略

        # 调用了搜索/查单但无结果 → 显式置空列表（与「未调用」区分）
        if "SEARCH_PRODUCTS" in tools_called and not last.get("searchedProductNames"):
            last["searchedProductNames"] = []
        if "QUERY_ORDERS" in tools_called and not last.get("queriedOrders"):
            last["queriedOrders"] = []


post_turn_service = PostTurnService()  # 模块单例
