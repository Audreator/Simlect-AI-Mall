# Simlect-web / stores

## 模块职责

Pinia 全局状态：auth、cart、agentMessage 等。

## 核心类 / 文件

- `agentMessage.ts`
- `auth.ts`
- `cart.ts`
- `coupon.ts`
- `device.ts`
- `pcAgentPanel.ts`
- `search.ts`

## 调用链（自上而下）

1. `View/composable → store.action()`
1. `store → api 请求 → 更新 state → 视图响应式刷新`

## 调用链图

```mermaid
flowchart LR
  View --> PiniaStore --> API
```

## 外部依赖

- Pinia
