# 护栏与指标

## 模块职责

输入/输出/工具/商品文本校验 + Prometheus 指标。类比 Spring AOP + Micrometer。

## 核心类 / 文件

- `input_guard`
- `output_guard`
- `tool_guard`
- `product_text_guard`
- `runtime_sensors`

## 调用链（自上而下）

1. `send_message → input_guard.detect_injection`
1. `finalize_node → output_guard.validate_no_false_completion`
1. `mcp_tool_router → tool_guard.is_allowed`
1. `finalize_agent_response → product_text_guard`
1. `classifier/router/runtime → runtime_sensors (Prometheus)`

## 调用链图

```mermaid
flowchart TD
  input_guard --> graph
  graph --> output_guard
  tool_guard --> mcp_router
```

## 外部依赖

- Prometheus /metrics
- settings 开关


## 说明

Python 模块：async≈CompletableFuture，dict≈Map，本文件描述模块间**调用链**，代码内 `# [zh]` 为逐行注释。

