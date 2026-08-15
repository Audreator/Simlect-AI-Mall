# Simlect-web / router

## 模块职责

前端路由配置：路径、懒加载、导航守卫。

## 核心类 / 文件

- `index.ts`

## 调用链（自上而下）

1. `main.ts → createRouter(routes)`
1. `beforeEach 守卫 → 鉴权 → next()`

## 调用链图

```mermaid
flowchart LR
  main --> router --> View
```

## 外部依赖

- Vue Router
