# Simlect-web / integrations

## 模块职责

第三方或跨模块集成注册表。

## 核心类 / 文件

- `featureRegistry.ts`

## 调用链（自上而下）

1. `featureRegistry → 动态加载能力模块`

## 调用链图

```mermaid
flowchart LR
  App --> integrations
```

## 外部依赖

- 无额外外部依赖
