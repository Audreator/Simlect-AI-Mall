# src / config — config

## 模块职责

Spring 配置类：`@Configuration`、`@Bean`、拦截器、Sa-Token 等。

## 核心类 / 文件

- `CaptchaRedisConfiguration.java`
- `CouponCacheExecutorConfig.java`
- `FeignSentinelRulesConfig.java`
- `MqAsyncConfiguration.java`
- `OkHttpConfig.java`
- `ProductionSafetyValidator.java`
- `SimlectLoadBalancerAutoConfiguration.java`
- `SimlectLoadBalancerClientConfiguration.java`

## 调用链（自上而下）

1. `应用启动 → 加载 `@Configuration``
1. `注册 Bean / 拦截器 / 过滤器链`

## 调用链图

```mermaid
flowchart LR
  Startup --> Config --> Beans
```

## 外部依赖

- Spring
