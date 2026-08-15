# auth

## 模块职责

认证模块：Token 解析、FastAPI Depends 登录注入。

## 核心类 / 文件

- `__init__.py`
- `token_service.py`

## 调用链（自上而下）

1. `api Depends → auth/deps → token_service → Redis`

## 调用链图

```mermaid
flowchart LR
  Route --> deps --> Redis
```

## 外部依赖

- Redis
