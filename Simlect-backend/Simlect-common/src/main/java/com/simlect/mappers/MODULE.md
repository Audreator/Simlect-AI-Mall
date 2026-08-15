# src / mappers — mappers

## 模块职责

MyBatis Mapper 数据访问层，SQL 映射。类比 MyBatis `@Mapper` / JPA Repository。

## 核心类 / 文件

- `BaseMapper.java`
- `BaseMapperTableSplit.java`
- `LocalMessageOutboxMapper.java`
- `MqCompensationLogMapper.java`

## 调用链（自上而下）

1. `ServiceImpl → XxxMapper.select/insert/update`
1. `Mapper XML / 注解 SQL → MySQL 表`

## 调用链图

```mermaid
flowchart LR
  Service --> Mapper --> MySQL
```

## 外部依赖

- MySQL
- MyBatis-Plus
