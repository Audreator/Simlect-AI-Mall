# harness/guardrails

## 模块职责

输入/输出/工具/商品文本护栏。

## 核心类 / 文件

- `__init__.py`
- `input_guard.py`
- `output_guard.py`
- `product_text_guard.py`
- `tool_guard.py`

## 调用链（自上而下）

1. `send_message/finalize/tools → 各 Guardrail 校验`

## 调用链图

```mermaid
flowchart TD
  input --> graph --> output
```

## 外部依赖

- 无额外外部依赖
