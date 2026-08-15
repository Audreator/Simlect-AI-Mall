"""
输入护栏模块（Input Guardrail）。

职责：在用户消息进入 Agent 之前做安全检查与限制。
包括：敏感词替换、Prompt 注入检测、对话轮次上限校验。
类似 Java Servlet Filter 或 Spring HandlerInterceptor 的 preHandle。

本模块不抛异常，由调用方根据返回值决定拒绝或继续。
"""

import re  # 正则匹配注入模式

from app.config.settings import get_settings  # ai_chat_limit 等配置


# Prompt 注入常见英文/中文模式列表，编译前为字符串
INJECTION_PATTERNS = [
    r"ignore\s+(all\s+)?previous\s+instructions",  # 忽略之前指令（英文）
    r"忽略.*指令",  # 忽略…指令（中文）
    r"system\s*prompt",  # 试图读取/覆盖 system prompt
]


class InputGuardrail:
    """
    输入护栏类。

    类似 Java @Component InputGuardrail，可注入敏感词列表。
    """

    def __init__(self, sensitive_words: list[str] | None = None):
        """
        构造输入护栏。

        参数:
            sensitive_words: 敏感词列表，命中则替换为等长 *；
                None 表示使用空列表
        """
        self._sensitive_words = sensitive_words or []  # 私有字段，类似 private final List

    def filter_sensitive(self, text: str) -> str:
        """
        将文本中的敏感词替换为等数量的星号 *。

        不改变字符串长度以外的结构，便于前端对齐显示。

        参数:
            text: 原始用户输入

        返回:
            替换后的字符串
        """
        result = text  # 逐词替换，类似循环 replaceAll
        for word in self._sensitive_words:
            if word:  # 跳过空串
                result = result.replace(word, "*" * len(word))  # * len(word) 保持长度
        return result

    def detect_injection(self, text: str) -> bool:
        """
        检测是否疑似 Prompt 注入攻击。

        对 INJECTION_PATTERNS 逐条 re.search，忽略大小写。

        参数:
            text: 用户输入

        返回:
            True 表示命中至少一条注入模式
        """
        for pattern in INJECTION_PATTERNS:
            if re.search(pattern, text, re.IGNORECASE):  # re.I 忽略大小写
                return True
        return False  # 未命中

    def check_chat_limit(self, user_round_count: int) -> bool:
        """
        检查用户是否仍在 AI 对话次数限制内。

        参数:
            user_round_count: 该用户历史消息总数（或轮次）

        返回:
            True 表示允许继续对话；
            ai_chat_limit <= 0 表示不限制，恒为 True
        """
        settings = get_settings()
        if settings.ai_chat_limit <= 0:  # 0 或负数 = 不限次
            return True
        return user_round_count < settings.ai_chat_limit  # 未达上限才允许
