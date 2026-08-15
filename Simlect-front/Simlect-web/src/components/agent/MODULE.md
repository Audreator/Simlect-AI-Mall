# Simlect 商城 Agent 前端组件

## 模块职责

聊天气泡、商品/订单/确认卡片渲染；WebSocket 流式更新。

## 核心类 / 文件

- `AgentChatItem.vue — 主消息渲染`
- `AgentProductList.vue`
- `AgentOrderList.vue`
- `AgentConfirmCard.vue`

## 调用链（自上而下）

1. `AIAssistantView → AgentChatList → AgentChatItem`
1. `stores/agentMessage → WebSocket manager → push chunk/done`
1. `agentMessageRender.ts 解析 biz_type → 卡片组件`
1. `confirmAction → POST /api/agent/confirmAction`

## 调用链图

```mermaid
flowchart LR
  WS --> agentMessage store --> AgentChatItem
  AgentChatItem --> ProductList
  AgentChatItem --> OrderList
```

## 外部依赖

- Simlect-agent Python API
- WebSocket /ws


