"""
输出护栏模块（Output Guardrail）。

职责：在 Agent 回复展示/流式输出给用户前做校验与清洗。
防止：虚假完成承诺、虚假确认卡片、越权能力描述、泄露内嵌 JSON、过多 emoji 等。
类似 Java 响应拦截器 ResponseBodyAdvice 或输出 Filter。

与 input_guard 对称：input 管进，output 管出。
"""

import re  # 正则清洗与模式匹配

from app.utils.biz_payload import strip_embedded_product_json  # 剥离内嵌商品 JSON

# 匹配确认操作 token：【act_32位十六进制】
ACT_TOKEN_PATTERN = re.compile(r"【act_[a-f0-9]{32}】", re.I)

# Agent 未真正调用 PROPOSE_* 却声称「已成功」的违禁表述
FALSE_COMPLETION_PATTERNS = [
    r"退款已成功",
    r"已为您确认收货",
    r"评价已提交",
    r"追评已完成",
]

# 未调用 PROPOSE_* 却声称已生成确认卡片
FALSE_CONFIRM_CARD_PATTERNS = [
    r"已生成.{0,12}确认卡片",  # .{0,12} 中间最多 12 任意字符
    r"请在下方.{0,8}确认",
    r"请查看下方.{0,8}卡片",
    r"下方确认卡片",
    r"确认卡片中完成",
]

# Agent 不应承诺的能力（代下单、收集地址等）
FALSE_CAPABILITY_PATTERNS = [
    r"帮你处理下单",
    r"帮你下单",
    r"我来帮你下单",
    r"帮你完成下单",
    r"提供.{0,12}收货地址",
    r"提供.{0,12}联系方式",
    r"收集.{0,8}地址",
    r"处理下单事宜",
    r"收货地址和联系方式",
]

# 检测到越权能力描述时追加的合规提示
_ORDER_CAPABILITY_HINT = "如需购买，请点击商品详情页或购物车自行完成下单。"

# Unicode emoji 范围（多段码点区间），用于 strip_emojis
_EMOJI_PATTERN = re.compile(
    "["
    "\U0001F600-\U0001F64F"  # 表情符号
    "\U0001F300-\U0001F5FF"  # 符号与象形文字
    "\U0001F680-\U0001F6FF"  # 交通与地图
    "\U0001F700-\U0001F77F"
    "\U0001F780-\U0001F7FF"
    "\U0001F800-\U0001F8FF"
    "\U0001F900-\U0001F9FF"
    "\U0001FA00-\U0001FAFF"
    "\U00002600-\U000026FF"  # 杂项符号
    "\U00002700-\U000027BF"
    "]+",
    flags=re.UNICODE,
)

# 连续多个空格/制表符压缩为单个空格
_MULTI_SPACE = re.compile(r"[ \t]{2,}")


def strip_emojis(text: str | None) -> str:
    """
    移除文本中的 emoji 并压缩多余空白。

    参数:
        text: 原始文本，None 当空串处理

    返回:
        清洗后的字符串
    """
    if not text:
        return ""
    cleaned = _EMOJI_PATTERN.sub("", text)  # 删除 emoji
    cleaned = _MULTI_SPACE.sub(" ", cleaned)  # 多空格 → 单空格
    return cleaned.strip()


class OutputGuardrail:
    """
    输出护栏类。

    类似 Java @Component OutputGuardrail，供流式/完整回复校验。
    """

    def extract_action_tokens(self, text: str) -> list[str]:
        """
        从回复中提取所有 act 确认 token。

        参数:
            text: 助手回复

        返回:
            匹配到的 token 字符串列表，类似 Matcher 多次 find
        """
        return ACT_TOKEN_PATTERN.findall(text)

    def validate_no_false_completion(self, text: str, tools_called: list[str]) -> str:
        """
        校验并修正「虚假完成/虚假确认卡片」类回复。

        若本轮未调用任何 PROPOSE_* 写操作工具，却出现成功/卡片话术，
        则替换为合规引导文案。

        参数:
            text: 助手原始回复
            tools_called: 本轮已调用工具名列表

        返回:
            清洗或替换后的文本
        """
        propose_called = any(t.startswith("PROPOSE_") for t in tools_called)  # 是否发起过确认
        cleaned = strip_emojis(text)
        # 未调用 PROPOSE_* 时 strip 捏造的 act token
        if not propose_called:
            cleaned = ACT_TOKEN_PATTERN.sub("", cleaned).strip()  # 去掉【act_...】
            cleaned = re.sub(r"act_[a-f0-9]{32}", "", cleaned, flags=re.I).strip()  # 裸 token 也删
            if any(re.search(p, cleaned) for p in FALSE_CONFIRM_CARD_PATTERNS):
                return (
                    "系统尚未生成确认卡片。请提供订单号；评价还需星级(1-5)和内容，"
                    "退款请提供订单项ID，我再为您正式发起确认。"
                )
        for pattern in FALSE_COMPLETION_PATTERNS:
            if re.search(pattern, cleaned) and not propose_called:
                return (
                    "系统尚未执行该操作，也未生成确认卡片。"
                    "请重新说明订单号与操作需求。"
                )

        # 继续校验虚假能力描述
        return self.validate_no_false_capability(cleaned, tools_called)

    def validate_no_false_capability(self, text: str, tools_called: list[str]) -> str:
        """
        校验「虚假能力」描述（如代下单），必要时追加合规提示。

        参数:
            text: 已部分清洗的回复
            tools_called: 工具列表（本方法暂未按工具细分，预留扩展）

        返回:
            原文或追加提示后的文本
        """
        cleaned = strip_emojis(text)
        if any(re.search(p, cleaned) for p in FALSE_CAPABILITY_PATTERNS):
            if _ORDER_CAPABILITY_HINT not in cleaned:  # 避免重复追加
                return cleaned.rstrip() + f"\n\n（{_ORDER_CAPABILITY_HINT}）"
        return cleaned

    def validate_chunk(self, chunk: str) -> str:
        """
        流式输出单 chunk 的轻量校验（去 emoji + 剥离内嵌商品 JSON）。

        参数:
            chunk: SSE/WebSocket 流式片段

        返回:
            可安全展示给用户的片段
        """
        return strip_embedded_product_json(strip_emojis(chunk))
