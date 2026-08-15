# Simlect-web / layouts

## 模块职责

页面布局壳：MainLayout、PcMainLayout 等，包裹 router-view。

## 核心类 / 文件

- `AdaptiveMainLayout.vue`
- `AdaptiveSubPageLayout.vue`
- `MainLayout.vue`
- `PcMainLayout.vue`
- `PcSubPageLayout.vue`
- `SubPageLayout.vue`

## 调用链（自上而下）

1. `Route meta.layout → Layout 组件 → slot/router-view`

## 调用链图

```mermaid
flowchart LR
  Router --> Layout --> View
```

## 外部依赖

- 无额外外部依赖
