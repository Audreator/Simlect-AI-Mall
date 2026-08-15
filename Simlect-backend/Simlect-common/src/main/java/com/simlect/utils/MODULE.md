# src / utils — utils

## 模块职责

静态工具类，字符串/日期/订单号等纯函数。

## 核心类 / 文件

- `AuthCookieHelper.java`
- `CheckCodeGenerator.java`
- `CollectionCompare.java`
- `DateUtil.java`
- `FileUtils.java`
- `ImageCompressUtils.java`
- `IpUtils.java`
- `JsonUtils.java`
- `OrderPayAmountUtil.java`
- `ProcessUtils.java`
- `StringTools.java`

## 调用链（自上而下）

1. `Service/Controller → Utils 静态方法 → 返回值`

## 调用链图

```mermaid
flowchart LR
  Service --> Utils
```

## 外部依赖

- 无额外外部依赖
