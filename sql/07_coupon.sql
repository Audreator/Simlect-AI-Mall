USE simlect_coupon;
create table if not exists discount_coupon
(
    coupon_id          varchar(20)                 not null comment '优惠券ID，如CP20260527001' primary key,
    coupon_name        varchar(100)                not null comment '优惠券名称，如"618满减券"',
    coupon_type        tinyint(1)     default 1    null comment '优惠券类型 1:满减券 2:折扣券 3:无门槛券',
    threshold_amount   decimal(10, 2) default 0.00 null comment '使用门槛金额，0表示无门槛',
    discount_amount    decimal(10, 2)              null comment '优惠金额（满减/无门槛时填写）',
    discount_rate      decimal(3, 2)               null comment '折扣率（折扣券时填写，如0.85表示85折）',
    total_count        int            default 0    null comment '发放总量，0表示不限量',
    remain_count       int            default 0    null comment '剩余数量',
    valid_start_time   datetime                    null comment '有效期开始时间',
    valid_end_time     datetime                    null comment '有效期结束时间',
    status             tinyint(1)     default 1    null comment '状态 0:已停用 1:进行中 2:已过期 3:已发完',
    rushingStatus      tinyint(1)     default 0    null comment '是否秒杀优惠卷 0:否 1:是',
    rushing_start_time datetime                    null comment '秒杀开始时间',
    rushing_end_time   datetime                    null comment '秒杀结束时间',
    create_time        datetime                    null comment '创建时间',
    update_time        datetime                    null comment '更新时间'
) comment '优惠券表' collate = utf8mb4_general_ci row_format = DYNAMIC;

create table if not exists user_coupon
(
    user_coupon_id varchar(32)          not null comment '用户优惠券记录ID' primary key,
    user_id        varchar(15)          not null comment '用户ID',
    coupon_id      varchar(20)          not null comment '优惠券ID',
    receive_time   datetime             null comment '领取/获得时间',
    use_time       datetime             null comment '使用时间',
    order_id       varchar(32)          null comment '核销订单号 order_info.order_id',
    status         tinyint(1) default 0 null comment '状态 0:未使用 1:已使用 2:已过期 3:已作废'
) comment '用户优惠券关联表' collate = utf8mb4_general_ci row_format = DYNAMIC;

create table if not exists sign_streak_coupon_grant
(
    id                bigint auto_increment primary key,
    idempotency_key   varchar(128) not null comment '用户+优惠券+连续天数幂等键',
    user_id           varchar(32)  not null comment '用户ID',
    coupon_id         varchar(20)  not null comment '优惠券ID',
    streak_days       int          not null comment '触发时连续签到天数',
    user_coupon_id    varchar(32)  null comment '发放成功后的用户券ID',
    status            tinyint default 0 not null comment '0处理中 1已发放 2业务拒绝',
    reject_reason     varchar(255) null comment '业务拒绝原因',
    create_time       datetime     not null,
    update_time       datetime     null,
    constraint uk_sign_streak_grant_key unique (idempotency_key),
    constraint uk_sign_streak_grant_biz unique (user_id, coupon_id, streak_days)
) comment '连续签到发券幂等结果' charset = utf8mb4;
