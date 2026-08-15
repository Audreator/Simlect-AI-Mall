USE simlect_order;
create table if not exists order_info
(
    order_id         varchar(32)                 not null comment '订单ID' primary key,
    amount           decimal(10, 2)              null comment '金额',
    goods_amount     decimal(10, 2)              null comment '商品原价合计（未优惠�?,
    discount_amount  decimal(10, 2) default 0.00 not null comment '优惠抵扣总额（券+活动等）',
    coupon_discount  decimal(10, 2) default 0.00 not null comment '优惠券抵扣金�?,
    user_coupon_id   varchar(32)                 null comment '使用的用户券ID user_coupon.user_coupon_id',
    coupon_id        varchar(20)                 null comment '券模板ID discount_coupon.coupon_id',
    user_id          varchar(15)                 null comment '用户ID',
    order_time       datetime                    null comment '订单创建时间',
    order_status     tinyint(1)                  null comment '-1已删�?0:待付�?1:已付�?待发�? 2:已发�? 3:已完�?4:已取�?5:已关�?6:已退�?7:部分退�?,
    pay_channel      varchar(10)                 null comment '支付通道',
    pay_scene        varchar(20)                 null comment '支付场景',
    pay_order_id     varchar(32)                 null comment '支付订单�?,
    channel_order_Id varchar(50)                 null comment '通道ID',
    comment_status   tinyint        default 0    null comment '评价状�?0:未评�? 1:已评�? 2:已追�?,
    subject          varchar(200)                null comment '订单标题'
) comment '订单信息' collate = utf8mb4_general_ci row_format = DYNAMIC;

create table if not exists order_item
(
    order_item_id          varchar(40)    not null comment '订单明细ID' primary key,
    order_id               varchar(32)    not null comment '订单ID',
    cover                  varchar(100)   null comment '封面',
    product_id             varchar(15)    not null comment '商品ID',
    product_name           varchar(200)   null comment '商品名称',
    property_value_id_hash varchar(32)    not null comment '属性值id组hash',
    property_info          varchar(150)   null comment '属性信�?,
    item_amount            decimal(10, 2) null comment '价格',
    buy_count              int            null comment '数量',
    order_item_status      tinyint(1)     null comment '状�?1:正常 0:已退�?,
    remark                 varchar(300)   null comment '备注',
    refund_order_id        varchar(32)    null comment '退款订单号',
    refund_amount          decimal(10, 2) null comment 'ʵ���˿��ȯ���̯�����ۼƳ��ˣ�'

) comment '订单明细�? collate = utf8mb4_general_ci row_format = DYNAMIC;

create table if not exists order_comment
(
    order_id          varchar(32)       not null comment '订单ID' primary key,
    product_id        varchar(15)       not null comment '商品ID',
    comment_content   varchar(300)      null comment '评价内容',
    comment_time      datetime          null comment '评价时间',
    comment_images    varchar(300)      null comment '评价图片',
    star              int               null comment '评价星级',
    comment_biz_reply varchar(255)      null comment '商家回复',
    recomment_content varchar(300)      null comment '追评',
    recomment_time    datetime          null comment '追评时间',
    recomment_images  varchar(300)      null comment '追评图片',
    user_id           varchar(15)       null comment '用户ID',
    property_info     varchar(150)      null comment '属性信�?,
    status            tinyint default 0 null comment '0:正常 1:已删�?
) collate = utf8mb4_general_ci row_format = DYNAMIC;

create table if not exists order_coupon_rel
(
    id              bigint auto_increment primary key,
    order_id        varchar(32)    not null,
    user_coupon_id  varchar(32)    not null,
    coupon_id       varchar(20)    not null,
    discount_amount decimal(10, 2) not null,
    create_time     datetime       not null,
    constraint uk_order_user_coupon unique (order_id, user_coupon_id)
) comment '订单优惠券核销明细' collate = utf8mb4_general_ci;

create table if not exists comment_report
(
    report_id        int auto_increment comment '举报ID' primary key,
    order_id         varchar(64)       not null comment '被举报评论所属订单ID',
    product_id       varchar(64)       null comment '商品ID',
    reporter_user_id varchar(64)       null comment '举报人用户ID',
    reason           varchar(50)       not null comment '举报理由',
    detail           varchar(500)      null comment '补充说明',
    comment_snapshot varchar(1000)     null comment '举报时评论内容快�?,
    status           tinyint default 0 not null comment '0:待处�?1:已处�?2:已驳�?,
    report_time      datetime          null comment '举报时间',
    handle_time      datetime          null comment '处理时间',
    handle_remark    varchar(500)      null comment '处理备注'
) comment '评论举报';

-- Outbox / 补偿（与业务同库，避免漏�?12_order_outbox.sql�?create table if not exists local_message_outbox
(
    id                bigint auto_increment comment '主键' primary key,
    idempotency_key   varchar(128)                   not null comment '发送幂等键',
    exchange_name     varchar(64)                    not null comment '交换�?,
    routing_key       varchar(64)                    not null comment '路由�?,
    payload_json      mediumtext                     not null comment '消息�?JSON',
    reliability_level varchar(16) default 'STANDARD' not null comment 'HIGH/STANDARD',
    status            tinyint     default 0          not null comment '0待发�?1发送中 2已发�?3失败',
    retry_count       int         default 0          not null comment '重试次数',
    error_message     varchar(512)                   null comment '最近失败原�?,
    create_time       datetime                       not null comment '创建时间',
    update_time       datetime                       null comment '更新时间',
    sent_time         datetime                       null comment '成功发送时�?,
    constraint uk_outbox_idempotency unique (idempotency_key),
    key idx_outbox_status_ctime (status, create_time)
) comment '本地消息 Outbox（事务后可靠投递）' charset = utf8mb4;

create table if not exists mq_compensation_log
(
    log_id            int auto_increment comment '日志ID' primary key,
    idempotency_key   varchar(128)               not null comment '幂等�?,
    exchange          varchar(64)                not null comment '交换�?,
    routing_key       varchar(64)                not null comment '路由�?,
    biz_scene         varchar(32)                null comment '业务场景',
    payload_json      mediumtext                 null comment '消息�?JSON',
    reliability_level varchar(16) default 'HIGH' not null comment '发送级�?,
    error_message     varchar(512)               null comment '失败原因',
    retry_count       int         default 0      not null comment '重放次数',
    status            int         default 0      not null comment '0待处�?1处理�?2已重放成�?3重放失败 4已忽�?,
    create_time       datetime                   not null comment '创建时间',
    update_time       datetime                   null comment '更新时间',
    handle_time       datetime                   null comment '运维处理时间',
    handle_remark     varchar(512)               null comment '处理备注',
    constraint uk_idempotency_key unique (idempotency_key)
) comment 'MQ补偿审查日志（订单库�? charset = utf8mb4;

-- 物流信息（订单域，同库）
create table if not exists order_logistics_info
(
    order_id          varchar(32)       not null comment '订单编号' primary key,
    user_id           varchar(15)       null comment '用户ID',
    logistics_no      varchar(128)      null comment '物流单号',
    logistics_company varchar(100)      null comment '物流公司',
    sender_name       varchar(100)      null comment '发货人姓�?,
    sender_phone      varchar(20)       null comment '发货人电�?,
    sender_address    varchar(500)      null comment '发货地址',
    receiver_name     varchar(100)      null comment '收件人姓�?,
    receiver_phone    varchar(20)       null comment '收件人电�?,
    receiver_address  varchar(500)      null comment '收件地址',
    logistics_status  tinyint default 0 null comment '物流状态：0待发�?1运输�?2已送达 3订单取消'
) comment '物流信息�? collate = utf8mb4_general_ci row_format = DYNAMIC;

create table if not exists order_logistics_info_record
(
    record_id      int auto_increment comment '记录ID' primary key,
    order_id       varchar(32)  not null comment '订单ID',
    record_time    datetime     null comment '记录时间',
    record_address varchar(150) null comment '记录地址'
) collate = utf8mb4_general_ci row_format = DYNAMIC;
