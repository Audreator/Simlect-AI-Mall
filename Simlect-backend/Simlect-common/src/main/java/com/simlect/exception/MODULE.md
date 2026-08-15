# src / exception — exception

## 模块职责

业务异常与全局异常处理。

## 核心类 / 文件

- `BusinessException.java`
- `PayOrderLifecycleBusyException.java`

## 调用链（自上而下）

1. `Service throw BusinessException → `@ControllerAdvice` → ResponseVO.error`

## 调用链图

```mermaid
flowchart LR
  Service --> Exception --> Handler
```

## 外部依赖

- 无额外外部依赖
