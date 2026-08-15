# 配置中心

## 模块职责

Pydantic Settings：LLM/Redis/MySQL/ES/Java/MCP/RAG/熔断/限流。

## 核心类 / 文件

- `settings.py — get_settings()`

## 调用链（自上而下）

1. `全模块 import get_settings → 读 .env`

## 调用链图

```mermaid
flowchart LR
  all --> get_settings --> env
```

## 外部依赖

- .env


## 说明

Python 模块：async≈CompletableFuture，dict≈Map，本文件描述模块间**调用链**，代码内 `# [zh]` 为逐行注释。

