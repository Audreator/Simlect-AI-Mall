# api/routes

## 模块职责

FastAPI 路由子模块，每个文件一组 REST/SSE 端点。

## 核心类 / 文件

- `__init__.py`
- `agent.py`

## 调用链（自上而下）

1. `main include_router → routes/*.py → services`

## 调用链图

```mermaid
flowchart LR
  main --> routes --> services
```

## 外部依赖

- FastAPI
