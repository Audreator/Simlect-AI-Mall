from pathlib import Path

PROMPT_PATH = Path(__file__).resolve().parents[1] / "prompts" / "agent.txt"

def _load_prompt() -> str:
    return PROMPT_PATH.read_text(encoding="utf-8")

def test_prompt_has_intent_routing_section():
    text = _load_prompt()
    assert "意图与工具选择" in text
    assert "**不先** QUERY_ORDERS" in text  # 示例5 实际文案：→ **不先** QUERY_ORDERS

def test_prompt_few_shot_review_examples():
    text = _load_prompt()
    assert "Few-shot 示例" in text
    assert "写评价-信息不足" in text
    assert "PROPOSE_PRODUCT_REVIEW" in text
    assert "QUERY_COMMENT" in text
    assert "不调用** QUERY_ORDERS" in text or "不调用 QUERY_ORDERS" in text

def test_prompt_distinguish_read_write_tools():
    text = _load_prompt()
    assert "QUERY_ORDERS：**仅**查询订单" in text
    assert "QUERY_COMMENT：**仅**查看已提交的评价" in text
    assert "PROPOSE_PRODUCT_REVIEW：**提交**评价提案" in text

def test_prompt_has_forbidden_section():
    text = _load_prompt()
    assert "=== 【禁止】" in text  # 实际标题：=== 【禁止】（安全底线，不是流程教条）===
    assert text.count("【禁止】") >= 9  # 当前文案共 10 条
    assert "优先于句中出现的「订单」字样" in text  # 规则6 实际文案
    assert "用 QUERY_COMMENT 代替写评价" in text  # 实际文案：用 QUERY_COMMENT 代替写评价

def test_prompt_write_ops_no_query_orders_fallback():
    text = _load_prompt()
    for phrase in ("确认收货", "退款", "追评"):
        assert phrase in text
