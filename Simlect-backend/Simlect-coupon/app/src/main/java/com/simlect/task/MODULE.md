# src / task — task

## 模块职责

定时任务（`@Scheduled`），对账、清理、初始化等后台作业。

## 核心类 / 文件

- `CouponReminderTask.java`

## 调用链（自上而下）

1. `Scheduler → `@Scheduled` 方法`
1. `Task → Service / Mapper 批处理`

## 调用链图

```mermaid
flowchart LR
  Scheduler --> Task --> Service
```

## 外部依赖

- Spring Scheduling
