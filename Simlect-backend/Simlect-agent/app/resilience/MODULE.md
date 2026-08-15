# 熔断器

## 模块职责

Embedding/RAG 外部调用保护。类比 Resilience4j。

## 核心类 / 文件

- `circuit_breaker.py — CircuitRegistry`

## 调用链（自上而下）

1. `rag/embedding.embed_text → circuit_registry.get('embedding')`
1. `连续失败 → OPEN → 短路`

## 调用链图

```mermaid
flowchart LR
  embed --> circuit_breaker
```

## 外部依赖

- settings 阈值


## 说明

Python 模块：async≈CompletableFuture，dict≈Map，本文件描述模块间**调用链**，代码内 `# [zh]` 为逐行注释。

