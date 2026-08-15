# src / cloud — cloud

## 模块职责

Spring Boot 启动类与云原生配置入口。

## 核心类 / 文件

- `CouponApplication.java`

## 调用链（自上而下）

1. ``main` → SpringApplication.run`
1. `扫描 `@ComponentScan` → 加载 Controller/Service/Mapper Bean`

## 调用链图

```mermaid
flowchart LR
  main --> SpringBoot --> Beans
```

## 外部依赖

- Spring Boot
