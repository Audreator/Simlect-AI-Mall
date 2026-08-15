# harness/metrics

## 模块职责

Prometheus 运行时指标。

## 核心类 / 文件

- `runtime_sensors.py`

## 调用链（自上而下）

1. `classifier/router/runtime → Counter.inc`

## 调用链图

```mermaid
flowchart LR
  Runtime --> Prometheus
```

## 外部依赖

- Prometheus
