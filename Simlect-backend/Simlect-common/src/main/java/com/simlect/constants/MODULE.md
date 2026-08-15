# src / constants — constants

## 模块职责

全局常量：MQ 队列名、Redis Key、业务枚举值。

## 核心类 / 文件

- `Constants.java`
- `InternalApiHeaders.java`
- `RabbitMQConfig.java`
- `RabbitMqPersistenceConfig.java`
- `ReliableMessageSender.java`
- `TransactionalMqSender.java`

## 调用链（自上而下）

1. `各模块 import Constants → 避免魔法字符串`

## 调用链图

```mermaid
flowchart LR
  Modules --> Constants
```

## 外部依赖

- 无额外外部依赖
