# 智能客服：商品卡片可被 LLM 感知 + 切换 DeepSeek V4.1 Flash 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让智能客服 LLM 掌握"本轮展示给用户的商品卡片清单（含顺序、商品名、价格、productId）"，使随访消息"第一个商品怎么样"能定位到具体商品；并把 Agent 对话模型从 `deepseek-chat` 切换到 DeepSeek V4.1 Flash（API 模型名 `deepseek-flash`）。

**Architecture:** 不改动 MCP 工具与 Java 交互链路。利用既有"会话记忆 state + 上下文块（build_context_block）"通道：`post_turn` 从本轮展示的卡片 JSON 中提取有序清单，写入会话记忆新字段 `lastSearchResults`（Redis + MySQL 双写，跨轮持久）；`build_context_block` 把该清单以编号列表渲染进 LLM 上下文，并附"第N个商品 → 编号"映射说明。同时把"卡片轮次"的历史浓缩缓存从 JSON 片段改为可读清单，使工作记忆也携带该信息。模型切换只改配置默认值与示例环境变量。

**Tech Stack:** Python 3.11+ / FastAPI / LangGraph / LangChain-OpenAI / pytest + pytest-asyncio / DeepSeek OpenAI 兼容 API（`https://api.deepseek.com`，模型 `deepseek-flash`）。

---

## 范围与关键设计决策（先读）

1. **仅改 `Simlect-backend/Simlect-agent`（Python 侧）**。Java 微服务、前端 `Simlect-front` 不动。前端按数组顺序渲染商品卡片（`AgentProductList.vue` 对 `products` 数组 `v-for`），所以"卡片数组顺序 = 用户看到的顺序"，持久化该顺序即可可靠解析"第一个商品"。
2. **本地仓库 `D:\Simlect-AI-Mall` 不是 git 仓库**（无 `.git`）。因此本计划**省略 `git commit` 步骤**，每个任务以"测试通过 + 变更文件清单"作为完成标记；代码由用户自行同步 GitHub / 服务器（按既有"本地代码直接更新服务器"流程）。
3. **不展示 `lastToolResults` 原始搜索名**：现有测试 `test_build_context_block_hides_last_search_names` 断言"上轮搜索过的商品名不进上下文（防止模型复用过期信息）"——这是**刻意设计**，保留不动。新增独立字段 `lastSearchResults` 表示"**已展示给用户的卡片**"，语义不同：它是用户亲眼看到的内容，用于指代解析是正确行为；渲染时附"需要最新详情请调 GET_PRODUCT_DETAIL"。
4. **不修改 prompt 文件**：指代映射说明内嵌在上下文块文本中（数据即说明），不依赖 `prompts/*.txt` 或 Redis `mall:prompt:*` 覆盖（生产环境 prompt 可能被 Redis 覆盖，改文件可能不生效）。
5. **模型名依据**：DeepSeek 官方 Chat Completions API 当前取值 `deepseek-flash`（对应 DeepSeek-V4.1-Flash）；旧名 `deepseek-v4-flash` 为 legacy。Base URL 仍为 `https://api.deepseek.com`。`MEMORY_LLM_MODEL` 为空时自动回落主模型，无需单独配置。Java `search` 服务与 `deploy/env.production.example` 中的 `LLM_MODEL` 属另一进程，本次不改。

### 现状数据流（为什么要这样改）

```
SEARCH_PRODUCTS 执行 → ToolInvokeResult.assistant_cards（卡片 JSON，含 productId/名称/价格）
  → tools_node 检测到卡片 → 直接 route=finalize（不再让 LLM 总结，LLM 看不到商品清单）
  → finalize → WS 推卡片 + 落库
下一轮（用户："第一个商品怎么样"）：
  build_context → load_turns_for_memory → 卡片轮次被过滤：
    - assistant_for_history 现在拿到的是「被截断的 JSON 片段 / LLM 压缩的 JSON」
    - lastToolResults 的 searchedProductNames 被故意隐藏
  → LLM 完全不知道用户看到的卡片内容 → 无法解析"第一个商品"
```

### 修复后的数据流

```
post_turn（卡片轮次）：
  assistant_cards/assistant_text 中的卡片 JSON →_extract_search_result_cards（有序：id/名称/价格）
  → memory.state["lastSearchResults"]（持久化，最多 12 条）
  → 历史浓缩缓存写入 _cards_history_summary（可读清单，替代 JSON 片段）
build_context（下一轮）：
  build_context_block 渲染编号清单 + 映射说明 → LLM 知道"第一个商品"= 清单第 1 项
  → LLM 用清单里的 productId 调 GET_PRODUCT_DETAIL 或直接引用卡片信息作答
```

---

## Task 1: 环境准备与测试基线

**Files:**
- 无代码变更（只创建本地 `.venv`，它已在 `.gitignore` 中）

- [ ] **Step 1: 创建虚拟环境并安装运行依赖**

在 `D:\Simlect-AI-Mall\Simlect-backend\Simlect-agent` 目录执行：

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -q -r requirements-runtime.txt
```

预期：无报错，`.venv\Scripts\python.exe` 可用。

- [ ] **Step 2: 安装测试工具（runtime requirements 不含 pytest）**

```powershell
.\.venv\Scripts\python.exe -m pip install -q pytest pytest-asyncio
```

- [ ] **Step 3: 运行全量测试基线**

注意：`get_settings()` 有 FailFast 校验（内部令牌未配置或为 `your-token`/`changeme` 时抛异常），测试必须带一个强令牌环境变量；`chat/llm` 构造还需非空 key。所有 pytest 命令统一使用下面前缀：

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests -q
```

预期：绝大多数通过；**唯一已知失败**是 `tests/test_agent_prompt.py` 的 4 处断言——该文件与当前 `prompts/agent.txt` 内容漂移（已核实：文件里 `【禁止】` 出现 10 次、无 `=== 【禁止】===` 精确串、示例 5 是 `**不先** QUERY_ORDERS`）。Task 2 修复。
若出现上述之外的失败，先停下记录现象并与用户确认（不在本计划范围内）。

- [ ] **Step 4: 记录基线**

把 pytest 汇总行（passed/failed 数）记录到执行记录中，作为后续"无新增失败"的比较基准。

---

## Task 2: 修复与当前 prompt 漂移的过期测试断言

**Files:**
- Modify: `tests/test_agent_prompt.py`（4 行断言）

说明：这不是功能改动，而是让测试基线与仓库现状一致，否则后续任务无法用"全量测试通过"作为验收门槛。修改是**让断言匹配现有 prompts/agent.txt 的实际文案**（不改 prompt 文件本身）。

- [ ] **Step 1: 修改 4 处断言**

```python
# tests/test_agent_prompt.py 第 11 行，原：
    assert "禁止**先 QUERY_ORDERS" in text or "禁止**先 QUERY_ORDERS" in text.replace(" ", "")
# 改为：
    assert "**不先** QUERY_ORDERS" in text  # 示例5 实际文案：→ **不先** QUERY_ORDERS

# 第 29 行，原：
    assert "=== 【禁止】===" in text
# 改为：
    assert "=== 【禁止】" in text  # 实际标题：=== 【禁止】（安全底线，不是流程教条）===

# 第 30 行，原：
    assert text.count("【禁止】") >= 15
# 改为：
    assert text.count("【禁止】") >= 9  # 当前文案共 10 条

# 第 31 行，原：
    assert "【禁止】句中出现「订单」二字就默认调用 QUERY_ORDERS" in text
# 改为：
    assert "优先于句中出现的「订单」字样" in text  # 规则6 实际文案
```

- [ ] **Step 2: 运行该文件验证通过**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests/test_agent_prompt.py -q
```

预期：`6 passed`（该文件共 5 个测试函数、0 failed）。

- [ ] **Step 3: 全量测试确认基线转绿**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests -q
```

预期：全部通过（0 failed）。若仍有失败，先记录并停下确认。

---

## Task 3: 新增共享价格格式化工具 `format_price_text`

**Files:**
- Modify: `app/utils/biz_payload.py`（在 `_to_number` 函数后新增）
- Test: `tests/test_biz_payload_act.py`（追加测试）

- [ ] **Step 1: 先写失败测试**

追加到 `tests/test_biz_payload_act.py` 末尾：

```python
def test_format_price_text():
    from app.utils.biz_payload import format_price_text

    assert format_price_text(12.0) == "12"
    assert format_price_text(12.5) == "12.5"
    assert format_price_text("199") == "199"
    assert format_price_text(None) == ""
    assert format_price_text("不是价格") == ""
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests/test_biz_payload_act.py::test_format_price_text -q
```

预期：FAIL，`ImportError: cannot import name 'format_price_text'`。

- [ ] **Step 3: 实现最小代码**

在 `app/utils/biz_payload.py` 的 `_to_number` 函数之后插入：

```python
def format_price_text(value: Any) -> str:
    """价格展示文本：12.0→"12"、12.5→"12.5"；None/非法值返回空串。"""
    if value is None:
        return ""
    try:
        price = float(value)
    except (TypeError, ValueError):
        return ""
    return str(int(price)) if price.is_integer() else str(price)
```

- [ ] **Step 4: 运行测试确认通过**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests/test_biz_payload_act.py -q
```

预期：全部 passed。

---

## Task 4: 会话记忆新增 `lastSearchResults`（post_turn 提取 + 持久化 + 历史浓缩可读化）

**Files:**
- Modify: `app/memory/models.py`（`empty_state()` 增加默认字段）
- Modify: `app/memory/post_turn.py`（新增 2 个模块级辅助函数；`run()` 增加参数与写入逻辑；替换步骤 6/7）
- Modify: `app/graph/nodes.py`（`post_turn_node` 透传 `assistant_cards`）
- Test: `tests/test_post_turn_search_cards.py`（新建）

- [ ] **Step 1: 先写失败测试（完整文件）**

新建 `tests/test_post_turn_search_cards.py`：

```python
"""post_turn：商品卡片记忆（lastSearchResults）与历史浓缩可读清单测试。"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.memory.models import SessionMemory
from app.memory.post_turn import (
    _cards_history_summary,
    _extract_search_result_cards,
    post_turn_service,
)


def test_extract_search_cards_prefers_card_json_order_and_price():
    cards_json = (
        '[{"productId":"p1","productName":"旺旺雪饼","minPrice":12.0},'
        '{"productId":"p2","productName":"乐事薯片","minPrice":6.5}]'
    )
    cards = _extract_search_result_cards(cards_json, None, None)
    assert [c["productId"] for c in cards] == ["p1", "p2"]
    assert cards[0]["productName"] == "旺旺雪饼"
    assert cards[1]["minPrice"] == 6.5


def test_extract_search_cards_prefers_assistant_cards_source():
    cards = _extract_search_result_cards(
        '[{"productId":"p1","productName":"卡片来源"}]',
        '[{"productId":"p2","productName":"正文来源"}]',
        None,
    )
    assert cards[0]["productId"] == "p1"


def test_extract_search_cards_fallback_from_tool_biz():
    cards = _extract_search_result_cards(
        None, "一些正文", {"productIds": ["p9"], "productNames": ["卫龙辣条"]}
    )
    assert cards == [{"productId": "p9", "productName": "卫龙辣条", "minPrice": None}]


def test_extract_search_cards_ignores_order_cards():
    order_cards = '[{"orderId":"o1","orderStatus":2,"orderItemList":[]}]'
    assert _extract_search_result_cards(order_cards, order_cards, None) == []


def test_cards_history_summary_uses_page_order():
    summary = _cards_history_summary([
        {"productId": "p1", "productName": "旺旺雪饼", "minPrice": 12.0},
        {"productId": "p2", "productName": "乐事薯片", "minPrice": None},
    ])
    assert summary.startswith("已向用户展示商品卡片")
    assert "1. 旺旺雪饼（¥12）" in summary
    assert "2. 乐事薯片" in summary


def _run_mocks(memory: SessionMemory):
    sms = MagicMock()
    sms.load = AsyncMock(return_value=memory)
    sms.save = AsyncMock()
    redis = MagicMock()
    redis.client = object()
    redis.get_consult_product = AsyncMock(return_value=None)
    redis.save_history_condensed = AsyncMock()
    compress = MagicMock()
    compress.maybe_schedule_compress = AsyncMock()
    return sms, redis, compress


@pytest.mark.asyncio
async def test_run_stores_search_cards_and_readable_history():
    memory = SessionMemory(user_id="u1")
    sms, redis, compress = _run_mocks(memory)
    cards_json = '[{"productId":"p1","productName":"旺旺雪饼","minPrice":12.0}]'
    with patch("app.memory.post_turn.session_memory_service", sms), \
         patch("app.memory.post_turn.redis_service", redis), \
         patch("app.memory.post_turn.compress_service", compress), \
         patch("app.memory.post_turn.load_agent_prompt", AsyncMock(return_value="")), \
         patch("app.memory.post_turn.schedule_assistant_condense") as condense:
        await post_turn_service.run(
            user_id="u1",
            message_id=9,
            user_text="帮我找零食",
            assistant_text=cards_json,
            tools_called=["SEARCH_PRODUCTS"],
            tool_biz={"productIds": ["p1"], "productNames": ["旺旺雪饼"]},
            card=None,
            assistant_cards=cards_json,
            working_turns=[],
            working_oldest_id=None,
        )
    assert memory.state["lastSearchResults"] == [
        {"productId": "p1", "productName": "旺旺雪饼", "minPrice": 12.0}
    ]
    saved_text = redis.save_history_condensed.await_args.args[2]
    assert saved_text.startswith("已向用户展示商品卡片")
    assert "1. 旺旺雪饼（¥12）" in saved_text
    condense.assert_not_called()


@pytest.mark.asyncio
async def test_run_clears_search_cards_when_search_without_cards():
    memory = SessionMemory(user_id="u1")
    memory.state["lastSearchResults"] = [
        {"productId": "old", "productName": "旧商品", "minPrice": 1.0}
    ]
    sms, redis, compress = _run_mocks(memory)
    with patch("app.memory.post_turn.session_memory_service", sms), \
         patch("app.memory.post_turn.redis_service", redis), \
         patch("app.memory.post_turn.compress_service", compress), \
         patch("app.memory.post_turn.load_agent_prompt", AsyncMock(return_value="")), \
         patch("app.memory.post_turn.schedule_assistant_condense"):
        await post_turn_service.run(
            user_id="u1",
            message_id=10,
            user_text="帮我找不存在的东西",
            assistant_text="【搜索结果】未找到相关商品。",
            tools_called=["SEARCH_PRODUCTS"],
            tool_biz=None,
            card=None,
            assistant_cards=None,
            working_turns=[],
            working_oldest_id=None,
        )
    assert memory.state["lastSearchResults"] == []


@pytest.mark.asyncio
async def test_run_keeps_text_history_for_normal_turns():
    memory = SessionMemory(user_id="u1")
    sms, redis, compress = _run_mocks(memory)
    with patch("app.memory.post_turn.session_memory_service", sms), \
         patch("app.memory.post_turn.redis_service", redis), \
         patch("app.memory.post_turn.compress_service", compress), \
         patch("app.memory.post_turn.load_agent_prompt", AsyncMock(return_value="")), \
         patch("app.memory.post_turn.schedule_assistant_condense") as condense:
        await post_turn_service.run(
            user_id="u1",
            message_id=11,
            user_text="你好",
            assistant_text="您好，有什么可以帮您？",
            tools_called=[],
            tool_biz=None,
            card=None,
            assistant_cards=None,
            working_turns=[],
            working_oldest_id=None,
        )
    assert memory.state["lastSearchResults"] == []
    saved_text = redis.save_history_condensed.await_args.args[2]
    assert saved_text == "您好，有什么可以帮您？"
    condense.assert_called_once()
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests/test_post_turn_search_cards.py -q
```

预期：收集报错/失败，`ImportError: cannot import name '_extract_search_result_cards'`（以及 `run()` 不接受 `assistant_cards`）。

- [ ] **Step 3: 修改 `app/memory/models.py` 的 `empty_state()`**

```python
def empty_state() -> dict[str, Any]:
    return {
        "consultProduct": None,
        "pendingAction": None,
        "lastToolResults": {
            "searchedProducts": [],
            "searchedProductNames": [],
            "queriedOrders": [],
            "viewedProductIds": [],
        },
        "lastSearchResults": [],
        "turnCount": 0,
        "summaryLastMessageId": 0,
        "estimatedTokens": 0,
    }
```

（`lastSearchResults` 为新增行：最近一次展示给用户的商品卡片清单，元素形如 `{"productId": str, "productName": str, "minPrice": float|None}`；兼容旧数据：历史用户 state JSON 无该键，读取处一律用 `.get(...) or []`。）

- [ ] **Step 4: 修改 `app/memory/post_turn.py`**

4a. 顶部 import 区新增：

```python
from app.utils.biz_payload import format_price_text  # 价格展示文本（12.0→12）
```

4b. 在 `logger = structlog.get_logger()` 之后、`_BIZ_MARKER` 定义之前，新增两个模块级辅助函数：

```python
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
```

4c. `run()` 签名新增参数（放在 `card` 之后）：

```python
    async def run(
        self,
        user_id: str,
        message_id: int,
        user_text: str,
        assistant_text: str,
        tools_called: list[str],
        tool_biz: dict | None,
        card: dict | None,
        assistant_cards: str | None = None,
        working_turns: list[dict],
        working_oldest_id: int | None,
    ) -> None:
```

同时把 docstring 的参数区补一行：

```
            assistant_cards: 本轮助手卡片 JSON（商品/订单/确认卡，可为空）
```

4d. 在 `self._sync_tool_results(memory, tools_called, tool_biz, assistant_text)` 之后新增：

```python
        # 3.5 记录"已展示给用户的商品卡片"（有序），供后续轮次解析"第一个商品"指代
        search_cards = _extract_search_result_cards(assistant_cards, assistant_text, tool_biz)
        if search_cards:
            memory.state["lastSearchResults"] = search_cards[:12]
        elif "SEARCH_PRODUCTS" in (tools_called or []):
            # 本轮确实搜过但无卡片 → 显式清空（与"未调用"区分，防旧清单误指代）
            memory.state["lastSearchResults"] = []
```

4e. 替换原步骤 6/7（从 `# 6. 助手回复写入「历史精简」缓存（同步截断版）` 到 `schedule_assistant_condense(user_id, message_id, assistant_text)`）：

```python
        # 6/7. 卡片轮次写「可读商品清单」，替代被截断的 JSON 片段；
        #      普通轮次保持原"同步截断 + 异步 LLM 压缩"链路。
        if search_cards:
            await redis_service.save_history_condensed(
                user_id, message_id, _cards_history_summary(search_cards)
            )
        else:
            immediate = truncate_assistant_for_history(assistant_text)
            if immediate:
                await redis_service.save_history_condensed(user_id, message_id, immediate)
            schedule_assistant_condense(user_id, message_id, assistant_text)
```

- [ ] **Step 5: 修改 `app/graph/nodes.py` 的 `post_turn_node`**

```python
            assistant_text=assistant_text,
            tools_called=state.get("tools_called") or [],
            tool_biz=state.get("tool_biz"),
            card=card,
            assistant_cards=state.get("assistant_cards"),
```

（即在 `card=card,` 后新增一行 `assistant_cards=state.get("assistant_cards"),`。）

- [ ] **Step 6: 运行新测试确认通过**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests/test_post_turn_search_cards.py -q
```

预期：`8 passed`（5 个同步 + 3 个异步）。

- [ ] **Step 7: 运行全量测试（回归）**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests -q
```

预期：全部通过，无新增失败。

---

## Task 5: 上下文块渲染编号清单 + 指代映射说明

**Files:**
- Modify: `app/memory/context_builder.py`（`build_context_block` 尾部 + import）
- Test: `tests/test_memory_context.py`（追加测试）

- [ ] **Step 1: 先写失败测试**

追加到 `tests/test_memory_context.py` 末尾（`build_context_block`、`SessionMemory` 已在该文件导入）：

```python
def test_build_context_block_lists_displayed_cards_with_order():
    mem = SessionMemory(user_id="u1")
    mem.state["lastSearchResults"] = [
        {"productId": "p1", "productName": "旺旺雪饼", "minPrice": 12.0},
        {"productId": "p2", "productName": "乐事薯片", "minPrice": 6.5},
    ]
    block = build_context_block(mem)
    assert "1. 旺旺雪饼（¥12，ID: p1）" in block
    assert "2. 乐事薯片（¥6.5，ID: p2）" in block
    assert "第一个" in block
    # 旧行为（隐藏 lastToolResults 原始搜索名）不受影响
    assert "需重新调用 SEARCH_PRODUCTS" not in block
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests/test_memory_context.py::test_build_context_block_lists_displayed_cards_with_order -q
```

预期：FAIL（块中没有编号清单）。

- [ ] **Step 3: 实现**

3a. `app/memory/context_builder.py` 顶部新增 import：

```python
from app.utils.biz_payload import format_price_text  # 价格展示文本（12.0→12）
```

3b. 把 `build_context_block` 尾部（从 `last = state.get("lastToolResults") or {}` 到 `return "\n".join(lines)`）替换为：

```python
    search_cards = state.get("lastSearchResults") or []  # 最近一次展示给用户的商品卡片（顺序与页面一致）
    if search_cards:
        lines.append("### 最近展示的商品卡片（编号与用户页面卡片顺序一致）")
        for index, card in enumerate(search_cards[:12], start=1):
            name = (card.get("productName") or "").strip() or "商品"
            meta: list[str] = []  # 括号内元信息：价格 / 商品 ID
            price_text = format_price_text(card.get("minPrice"))
            if price_text:
                meta.append(f"¥{price_text}")
            pid = str(card.get("productId") or "").strip()
            if pid:
                meta.append(f"ID: {pid}")
            suffix = f"（{'，'.join(meta)}）" if meta else ""
            lines.append(f"{index}. {name}{suffix}")
        lines.append(
            "用户说「第一个商品／第N个商品」时按上述编号对应；"
            "需要规格、库存等最新详情请调用 GET_PRODUCT_DETAIL（商品 ID 见上）。"
        )
    else:
        last = state.get("lastToolResults") or {}  # 上轮工具调用结果摘要（不展示具体商品名，防过期信息复用）
        if last.get("searchedProductNames") or last.get("searchedProducts"):
            lines.append("上轮曾搜索过商品（需重新调用 SEARCH_PRODUCTS 获取最新结果）")
        else:
            lines.append("上轮搜索结果: 无")

    return "\n".join(lines)  # 换行连接，类似 String.join("\n", lines)
```

（保留原 `lastToolResults` 分支行为与 `test_build_context_block_hides_last_search_names` 不变。）

- [ ] **Step 4: 运行测试确认通过（含旧测试）**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests/test_memory_context.py -q
```

预期：全部 passed（含新的编号清单测试与旧的隐藏搜索名测试）。

- [ ] **Step 5: 全量测试（回归）**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests -q
```

预期：全部通过。

---

## Task 6: 切换模型为 DeepSeek V4.1 Flash（`deepseek-flash`）

**Files:**
- Modify: `app/config/settings.py`（默认模型名 2 行）
- Modify: `.env.example`（2 行）
- Test: `tests/test_llm_factory.py`（追加默认值测试）

- [ ] **Step 1: 先写失败测试**

追加到 `tests/test_llm_factory.py`：

```python
def test_default_chat_model_is_deepseek_flash():
    # 默认值应与 DeepSeek 现行模型名一致（DeepSeek-V4.1-Flash 的 API 模型 ID 为 deepseek-flash）
    assert Settings.model_fields["llm_model"].default == "deepseek-flash"
    assert Settings.model_fields["llm_fallback_model"].default == "deepseek-flash"
```

（用 `Settings.model_fields` 校验声明默认值，不受运行环境变量污染。）

- [ ] **Step 2: 运行测试确认失败**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests/test_llm_factory.py::test_default_chat_model_is_deepseek_flash -q
```

预期：FAIL（当前默认 `deepseek-chat`）。

- [ ] **Step 3: 修改默认值**

`app/config/settings.py`：

```python
    llm_model: str = "deepseek-flash"
    llm_fallback_model: str = "deepseek-flash"
```

`.env.example`：

```ini
LLM_MODEL=deepseek-flash
LLM_FALLBACK_MODEL=deepseek-flash
```

（`LLM_BASE_URL=https://api.deepseek.com`、`MEMORY_LLM_*` 留空回落主模型，均不变；`llm_fallback_model` 当前代码无运行时引用，同步改是为了配置一致性。）

- [ ] **Step 4: 运行测试确认通过**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests/test_llm_factory.py -q
```

预期：`4 passed`。

---

## Task 7: 全量回归、语法校验与真机验收

**Files:**
- 无新代码变更（验收与交付核对）

- [ ] **Step 1: 全量测试 + 全量语法编译校验**

```powershell
$env:SIMLECT_INTERNAL_TOKEN='local-test-token'; $env:LLM_API_KEY='test-key'; .\.venv\Scripts\python.exe -m pytest tests -q
.\.venv\Scripts\python.exe -m compileall -q app
```

预期：pytest 全绿；compileall 退出码 0、无输出。

- [ ] **Step 2: 核对变更文件清单（与计划一致性）**

```text
app/config/settings.py                    # 模型默认值
.env.example                              # 模型示例值
app/utils/biz_payload.py                  # +format_price_text
app/memory/models.py                      # +lastSearchResults 默认字段
app/memory/post_turn.py                   # +提取/清单辅助函数、run 写入与历史浓缩改造
app/memory/context_builder.py             # 上下文块编号清单 + import
app/graph/nodes.py                        # post_turn_node 透传 assistant_cards
tests/test_biz_payload_act.py             # +format_price_text 测试
tests/test_memory_context.py              # +编号清单测试
tests/test_post_turn_search_cards.py      # 新建
tests/test_llm_factory.py                 # +默认模型测试
tests/test_agent_prompt.py                # 对齐过期断言（Task 2）
```

预期：无清单外的文件被修改。

- [ ] **Step 3: 真机验收（发布后，C 端对话）**

1. 先搜商品：发送「帮我找零食」→ 出现商品卡片若干。
2. 发送「第一个商品怎么样」→ 回复必须针对**卡片第 1 个**商品（名称与卡片首项一致）；可通过追问「第二个呢」再次验证序号切换。
3. 发送「你好」（普通闲聊）→ 正常回复，确认历史浓缩链路未破坏。
4. 检查 Agent 日志无新增 `llm_turn_failed` / `post_turn_failed` / `mcp_tool_failed`。

- [ ] **Step 4: 发布说明（服务器侧，按用户既有"本地→服务器"流程执行）**

- 服务器 Agent 的 `.env`：将 `LLM_MODEL` 显式配置为 `deepseek-flash`（若原值未配置则随默认值生效）；无需改 `LLM_BASE_URL`。
- 仅需重启 **Agent 进程（:7050）**；本次未改 MCP 工具实现，**MCP（:7060）无需重启**（MCP 无热重载，但本次不涉及）。
- 真机验证见 Step 3；`GET /health` 应为 `status: ok`。
- 服务器上 Agent 的部署路径与进程管理方式在实际发布时与用户确认后执行（本计划不覆盖服务器文件替换细节）。

---

## Self-Review 记录（计划自检）

- **Spec coverage:** (1) "LLM 知道卡片内容" → Task 3/4/5（有序清单持久化 + 上下文渲染 + 历史浓缩可读化）；(2) "第一个商品 → 具体商品" → Task 5 编号映射 + Task 7 验收；(3) "模型换成 DeepSeek V4.1 Flash" → Task 6（`deepseek-flash`）+ Task 7 发布步骤。
- **Placeholder scan:** 所有代码步骤均为完整可粘贴代码；命令含预期输出；无 TBD/TODO。
- **Type consistency:** `lastSearchResults: list[{"productId": str, "productName": str, "minPrice": float|None}]` 在 post_turn/context_builder/测试中一致；`_extract_search_result_cards(assistant_cards, assistant_text, tool_biz)` 定义与调用一致；`format_price_text(value) -> str` 一致；`run(..., assistant_cards=..., ...)` 与 nodes 调用、测试一致。
- **兼容性:** 旧会话 state 无 `lastSearchResults` 键 → 全部读取处 `.get(...) or []`；`test_build_context_block_hides_last_search_names` 旧断言保留通过。