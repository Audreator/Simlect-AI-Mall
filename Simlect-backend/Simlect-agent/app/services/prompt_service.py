"""Prompt 服务模块：加载全局/意图 Prompt 模板，组装 Agent 系统提示词并注入 ReAct 执行说明。"""

from __future__ import annotations  # import statement — 延迟类型注解

from pathlib import Path  # import statement — 路径操作，类似 java.nio.file.Path

from app.constants import REDIS_PROMPT  # import statement — Redis Prompt 键前缀
from app.domain.intent.types import INTENT_PROMPT_KEY, IntentKind  # import statement — 意图枚举与 Prompt 映射
from app.services.redis_service import redis_service  # import statement — Redis 缓存读取
from app.utils.prompt_boundary import append_untrusted_rule  # import statement — 追加不可信输入边界规则

PROMPT_DIR = Path(__file__).resolve().parents[2] / "prompts"  # Prompt 文件目录 — 相对 app/prompts

PROMPT_FILE_MAP: dict[str, str] = {  # dict 类似 Map — Prompt 键到文件名
    "agent": "agent.txt",
    "compress": "compress.txt",
    "global": "global.txt",
    "user_intent": "user_intent.txt",
    "chat": "chat.txt",
    "product_consult": "product_consult.txt",
    "product_search": "product_search.txt",
    "query_order": "query_order.txt",
    "query_logistics": "query_logistics.txt",
    "query_coupon": "query_coupon.txt",
    "query_comment": "query_comment.txt",
    "product_review": "product_review.txt",
    "recomment": "recomment.txt",
    "refund": "refund.txt",
    "confirm_receipt": "confirm_receipt.txt",
    "cancel_order": "cancel_order.txt",
}

_REACT_SUPPLEMENT = """
=== ReAct 执行说明（优先级高于上文冲突条目）===
你是自主规划的工具 Agent：自己判断下一步是追问、直接回答，还是调用哪个 MCP 工具。
- 政策/如何操作/能力边界类问题（含优惠券怎么用、如何取消订单）：直接回答，不要为了「走流程」强行查单或查券。
- 需要真实业务数据时再调工具：搜商品 SEARCH_PRODUCTS；查订单 QUERY_ORDERS；物流 QUERY_LOGISTICS；查评价 QUERY_COMMENT；查券列表 QUERY_USER_COUPONS；写评价 PROPOSE_PRODUCT_REVIEW；追评 PROPOSE_RECOMMENT；退款 PROPOSE_REFUND；确认收货 PROPOSE_CONFIRM_RECEIPT。
- 取消订单：无取消写工具；说明用户去「我的订单」操作；仅当用户要核对某笔订单状态时再 QUERY_ORDERS。
- 写操作必须走 PROPOSE_*；禁止编造【act_xxx】；禁止未调工具就声称业务已完成。
- 商品/订单卡片由系统渲染，回复中禁止输出 JSON 数组。
""".strip()  # ReAct 工具调用补充说明 — 多行字符串

_REACT_ADAPTED_INTENTS = frozenset(  # frozenset 类似不可变 Set — 需 ReAct 说明的意图
    {
        IntentKind.PRODUCT_SEARCH,
        IntentKind.QUERY_ORDER,
        IntentKind.QUERY_LOGISTICS,
        IntentKind.QUERY_COUPON,
        IntentKind.QUERY_COMMENT,
        IntentKind.PRODUCT_REVIEW,
        IntentKind.RECOMMENT,
        IntentKind.REFUND,
        IntentKind.CONFIRM_RECEIPT,
        IntentKind.CANCEL_ORDER,
    }
)

async def load_prompt(prompt_key: str) -> str:  # 加载 Prompt — Redis 优先，文件兜底
    cached = await redis_service.client.get(f"{REDIS_PROMPT}{prompt_key}")  # 读 Redis 缓存
    if cached:
        return cached
    filename = PROMPT_FILE_MAP.get(prompt_key, f"{prompt_key}.txt")  # 默认 {key}.txt
    file_path = PROMPT_DIR / filename
    if file_path.exists():
        return file_path.read_text(encoding="utf-8")  # 读本地文件
    return ""

async def load_user_intent_classifier_prompt() -> str:  # 加载意图分类 Prompt
    return await load_prompt("user_intent")

def _safe_format(template: str, *args: str) -> str:  # 安全 % 格式化 — 占位符不匹配时原样返回
    try:
        return template % args  # Python % 格式化 — 类似 String.format
    except TypeError:
        return template

async def _format_intent_prompt(
    intent: IntentKind,
    user_id: str,
    user_text: str,
    *,
    product_snapshot: str | None = None,  # @Nullable 商品快照 JSON
    faq_text: str | None = None,
    knowledge_text: str | None = None,
) -> str:  # 按意图格式化 Prompt 模板
    key = INTENT_PROMPT_KEY.get(intent, "chat")  # 意图 → Prompt 键
    template = await load_prompt(key)
    if not template.strip():
        return ""

    if intent == IntentKind.PRODUCT_CONSULT:  # 商品咨询 — 注入快照与 FAQ
        return _safe_format(
            template,
            product_snapshot or "（暂无商品快照，请先 GET_PRODUCT_DETAIL 或等待用户发送商品卡片）",
            faq_text or "（暂无 FAQ）",
            user_id,
            user_text,
        )
    if intent == IntentKind.CHAT:  # 闲聊 — 注入知识库
        return _safe_format(
            template,
            knowledge_text or "（暂无知识库命中）",
            user_id,
            user_text,
        )
    if intent == IntentKind.PRODUCT_SEARCH:  # 商品搜索 — 禁止编造 productId
        return _safe_format(
            template,
            "（商品数据由 SEARCH_PRODUCTS 工具返回，禁止自行编造 productId）",
            user_text,
        )
    return _safe_format(template, user_id, user_text)  # 默认两占位符

async def build_agent_system_prompt(
    intent: IntentKind,
    user_id: str,
    user_text: str,
    *,
    product_snapshot: str | None = None,
    faq_text: str | None = None,
    knowledge_text: str | None = None,
) -> str:  # 组装完整 Agent 系统 Prompt

    global_part = (await load_prompt("global")).strip()  # 全局规则
    if not global_part:
        global_part = (await load_prompt("agent")).strip()  # 回退 agent.txt

    intent_part = await _format_intent_prompt(  # 意图专用段落
        intent,
        user_id,
        user_text,
        product_snapshot=product_snapshot,
        faq_text=faq_text,
        knowledge_text=knowledge_text,
    )

    parts: list[str] = []  # List<String> 段落列表
    if global_part:
        parts.append(global_part)
    if intent_part:
        parts.append(f"=== 当前意图：{intent.value} ===\n{intent_part}")
    if intent in _REACT_ADAPTED_INTENTS or intent == IntentKind.PRODUCT_CONSULT:  # 需 ReAct 说明
        parts.append(_REACT_SUPPLEMENT)

    body = "\n\n".join(parts).strip()  # 拼接段落
    if not body:
        body = (await load_prompt("agent")).strip()  # 兜底
    return append_untrusted_rule(body)  # 追加不可信输入边界

async def load_agent_prompt(
    intent: IntentKind | None = None,
    user_id: str = "",
    user_text: str = "",
    **kwargs,  # **kwargs 类似 Java Map<String, Object> 可变参数
) -> str:  # 加载 Agent Prompt — 兼容旧接口

    if intent is None:  # 无意图 — 仅全局/agent
        template = await load_prompt("agent")
        if not template.strip():
            template = await load_prompt("global")
        return append_untrusted_rule(template)
    return await build_agent_system_prompt(intent, user_id, user_text, **kwargs)

async def load_compress_prompt() -> str:  # 加载记忆压缩 Prompt
    return await load_prompt("compress")
