-- HIGH 可靠性 MQ 写入方需要本库具备 Outbox + 补偿表（与业务同库）。
-- 订单库见 05_order.sql / 12_order_outbox.sql；管理端见 10_admin.sql。
-- 本脚本补：user / product / search / coupon。

USE simlect_user;

create table if not exists local_message_outbox
(
    id                bigint auto_increment comment '主键' primary key,
    idempotency_key   varchar(128)                    not null comment '发送幂等键',
    exchange_name     varchar(64)                     not null comment '交换机',
    routing_key       varchar(64)                     not null comment '路由键',
    payload_json      mediumtext                      not null comment '消息体 JSON',
    reliability_level varchar(16) default 'STANDARD'  not null comment 'HIGH/STANDARD',
    status            tinyint     default 0           not null comment '0待发送 1发送中 2已发送 3失败',
    retry_count       int         default 0           not null comment '重试次数',
    error_message     varchar(512)                    null comment '最近失败原因',
    create_time       datetime                        not null comment '创建时间',
    update_time       datetime                        null comment '更新时间',
    sent_time         datetime                        null comment '成功发送时间',
    constraint uk_outbox_idempotency unique (idempotency_key),
    key idx_outbox_status_ctime (status, create_time)
) comment '本地消息 Outbox' charset = utf8mb4;

create table if not exists mq_compensation_log
(
    log_id            int auto_increment comment '日志ID' primary key,
    idempotency_key   varchar(128)               not null comment '幂等键',
    exchange          varchar(64)                not null comment '交换机',
    routing_key       varchar(64)                not null comment '路由键',
    biz_scene         varchar(32)                null comment '业务场景',
    payload_json      mediumtext                 null comment '消息体 JSON',
    reliability_level varchar(16) default 'HIGH' not null comment '发送级别',
    error_message     varchar(512)               null comment '失败原因',
    retry_count       int         default 0      not null comment '重放次数',
    status            int         default 0      not null comment '0待处理 1处理中 2已重放成功 3重放失败 4已忽略',
    create_time       datetime                   not null comment '创建时间',
    update_time       datetime                   null comment '更新时间',
    handle_time       datetime                   null comment '运维处理时间',
    handle_remark     varchar(512)               null comment '处理备注',
    constraint uk_idempotency_key unique (idempotency_key)
) comment 'MQ补偿审查日志' charset = utf8mb4;

USE simlect_product;

create table if not exists local_message_outbox
(
    id                bigint auto_increment comment '主键' primary key,
    idempotency_key   varchar(128)                    not null comment '发送幂等键',
    exchange_name     varchar(64)                     not null comment '交换机',
    routing_key       varchar(64)                     not null comment '路由键',
    payload_json      mediumtext                      not null comment '消息体 JSON',
    reliability_level varchar(16) default 'STANDARD'  not null comment 'HIGH/STANDARD',
    status            tinyint     default 0           not null comment '0待发送 1发送中 2已发送 3失败',
    retry_count       int         default 0           not null comment '重试次数',
    error_message     varchar(512)                    null comment '最近失败原因',
    create_time       datetime                        not null comment '创建时间',
    update_time       datetime                        null comment '更新时间',
    sent_time         datetime                        null comment '成功发送时间',
    constraint uk_outbox_idempotency unique (idempotency_key),
    key idx_outbox_status_ctime (status, create_time)
) comment '本地消息 Outbox' charset = utf8mb4;

create table if not exists mq_compensation_log
(
    log_id            int auto_increment comment '日志ID' primary key,
    idempotency_key   varchar(128)               not null comment '幂等键',
    exchange          varchar(64)                not null comment '交换机',
    routing_key       varchar(64)                not null comment '路由键',
    biz_scene         varchar(32)                null comment '业务场景',
    payload_json      mediumtext                 null comment '消息体 JSON',
    reliability_level varchar(16) default 'HIGH' not null comment '发送级别',
    error_message     varchar(512)               null comment '失败原因',
    retry_count       int         default 0      not null comment '重放次数',
    status            int         default 0      not null comment '0待处理 1处理中 2已重放成功 3重放失败 4已忽略',
    create_time       datetime                   not null comment '创建时间',
    update_time       datetime                   null comment '更新时间',
    handle_time       datetime                   null comment '运维处理时间',
    handle_remark     varchar(512)               null comment '处理备注',
    constraint uk_idempotency_key unique (idempotency_key)
) comment 'MQ补偿审查日志' charset = utf8mb4;

USE simlect_search;

create table if not exists local_message_outbox
(
    id                bigint auto_increment comment '主键' primary key,
    idempotency_key   varchar(128)                    not null comment '发送幂等键',
    exchange_name     varchar(64)                     not null comment '交换机',
    routing_key       varchar(64)                     not null comment '路由键',
    payload_json      mediumtext                      not null comment '消息体 JSON',
    reliability_level varchar(16) default 'STANDARD'  not null comment 'HIGH/STANDARD',
    status            tinyint     default 0           not null comment '0待发送 1发送中 2已发送 3失败',
    retry_count       int         default 0           not null comment '重试次数',
    error_message     varchar(512)                    null comment '最近失败原因',
    create_time       datetime                        not null comment '创建时间',
    update_time       datetime                        null comment '更新时间',
    sent_time         datetime                        null comment '成功发送时间',
    constraint uk_outbox_idempotency unique (idempotency_key),
    key idx_outbox_status_ctime (status, create_time)
) comment '本地消息 Outbox' charset = utf8mb4;

create table if not exists mq_compensation_log
(
    log_id            int auto_increment comment '日志ID' primary key,
    idempotency_key   varchar(128)               not null comment '幂等键',
    exchange          varchar(64)                not null comment '交换机',
    routing_key       varchar(64)                not null comment '路由键',
    biz_scene         varchar(32)                null comment '业务场景',
    payload_json      mediumtext                 null comment '消息体 JSON',
    reliability_level varchar(16) default 'HIGH' not null comment '发送级别',
    error_message     varchar(512)               null comment '失败原因',
    retry_count       int         default 0      not null comment '重放次数',
    status            int         default 0      not null comment '0待处理 1处理中 2已重放成功 3重放失败 4已忽略',
    create_time       datetime                   not null comment '创建时间',
    update_time       datetime                   null comment '更新时间',
    handle_time       datetime                   null comment '运维处理时间',
    handle_remark     varchar(512)               null comment '处理备注',
    constraint uk_idempotency_key unique (idempotency_key)
) comment 'MQ补偿审查日志' charset = utf8mb4;

USE simlect_coupon;

create table if not exists local_message_outbox
(
    id                bigint auto_increment comment '主键' primary key,
    idempotency_key   varchar(128)                    not null comment '发送幂等键',
    exchange_name     varchar(64)                     not null comment '交换机',
    routing_key       varchar(64)                     not null comment '路由键',
    payload_json      mediumtext                      not null comment '消息体 JSON',
    reliability_level varchar(16) default 'STANDARD'  not null comment 'HIGH/STANDARD',
    status            tinyint     default 0           not null comment '0待发送 1发送中 2已发送 3失败',
    retry_count       int         default 0           not null comment '重试次数',
    error_message     varchar(512)                    null comment '最近失败原因',
    create_time       datetime                        not null comment '创建时间',
    update_time       datetime                        null comment '更新时间',
    sent_time         datetime                        null comment '成功发送时间',
    constraint uk_outbox_idempotency unique (idempotency_key),
    key idx_outbox_status_ctime (status, create_time)
) comment '本地消息 Outbox' charset = utf8mb4;

create table if not exists mq_compensation_log
(
    log_id            int auto_increment comment '日志ID' primary key,
    idempotency_key   varchar(128)               not null comment '幂等键',
    exchange          varchar(64)                not null comment '交换机',
    routing_key       varchar(64)                not null comment '路由键',
    biz_scene         varchar(32)                null comment '业务场景',
    payload_json      mediumtext                 null comment '消息体 JSON',
    reliability_level varchar(16) default 'HIGH' not null comment '发送级别',
    error_message     varchar(512)               null comment '失败原因',
    retry_count       int         default 0      not null comment '重放次数',
    status            int         default 0      not null comment '0待处理 1处理中 2已重放成功 3重放失败 4已忽略',
    create_time       datetime                   not null comment '创建时间',
    update_time       datetime                   null comment '更新时间',
    handle_time       datetime                   null comment '运维处理时间',
    handle_remark     varchar(512)               null comment '处理备注',
    constraint uk_idempotency_key unique (idempotency_key)
) comment 'MQ补偿审查日志' charset = utf8mb4;
