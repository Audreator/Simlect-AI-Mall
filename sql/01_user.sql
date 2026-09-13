USE simlect_user;

create table if not exists user_info
(
    user_id         varchar(10)          not null comment '用户id' primary key,
    nick_name       varchar(20)          not null comment '昵称',
    avatar          varchar(100)         null comment '头像',
    email           varchar(150)         not null comment '邮箱',
    password        varchar(100)         not null,
    sex             tinyint(1)           null comment '0:女 1:男 2:未知',
    join_time       datetime             not null comment '加入时间',
    last_login_time datetime             null comment '最后登录时间',
    last_login_ip   varchar(100)         null comment '最后登录IP',
    status          tinyint(1) default 1 not null comment '0:禁用 1:正常',
    constraint idx_key_email unique (email),
    constraint idx_nick_name unique (nick_name)
) comment '用户信息' collate = utf8mb4_general_ci row_format = DYNAMIC;

create table if not exists user_address
(
    address_id   varchar(15)  not null comment '地址ID' primary key,
    user_id      varchar(15)  null comment '用户ID',
    address      varchar(300) null comment '详细地址',
    addressee    varchar(25)  null comment '收货人',
    phone        varchar(15)  null comment '手机号码',
    default_type tinyint      null comment '默认类型0:非默认  1:默认'
) collate = utf8mb4_general_ci row_format = DYNAMIC;
create index idx_user_id on user_address (user_id);

create table if not exists user_member_profile
(
    user_id      varchar(15)       not null primary key,
    level_code   tinyint default 1 not null comment '等级 1起',
    growth_value int     default 0 not null comment '成长值',
    level_name   varchar(30)       null,
    update_time  datetime          null
) comment '会员成长' collate = utf8mb4_general_ci row_format = DYNAMIC;

create table if not exists user_sign_record
(
    user_id         varchar(15)   not null primary key,
    continuous_days int           not null comment '连续签到天数',
    total_sign_days int default 1 not null comment '总签到天数',
    used_count      int           not null comment '已使用补签次数'
) comment '签到记录' collate = utf8mb4_general_ci row_format = DYNAMIC;

create table if not exists user_sign_record_detail
(
    id          bigint auto_increment comment '主键' primary key,
    user_id     varchar(32)       not null comment '用户ID',
    sign_date   char(8)           not null comment '签到日期 yyyyMMdd',
    sign_type   tinyint default 0 not null comment '0普通签到 1补签',
    create_time datetime          not null comment '创建时间',
    constraint uk_user_sign_date unique (user_id, sign_date)
) comment '用户签到明细' charset = utf8mb4;
create index idx_create_time on user_sign_record_detail (create_time);
create index idx_sign_date on user_sign_record_detail (sign_date);

create table if not exists sign_bitmap
(
    user_id     varchar(32)  not null comment '用户ID',
    `year_month` char(6)    not null comment '月份 yyyyMM',
    bits        int unsigned default 0 not null comment 'bit0 对应 1 日，最多使用低 31 位',
    update_time datetime     not null comment '更新时间',
    primary key (user_id, `year_month`)
) comment 'MySQL 权威签到位图' charset = utf8mb4;

create table if not exists sign_growth_record
(
    id          bigint auto_increment primary key,
    user_id     varchar(32) not null comment '用户ID',
    sign_date   char(8)     not null comment '签到日期 yyyyMMdd',
    amount      int         not null comment '本次成长值',
    create_time datetime    not null comment '创建时间',
    constraint uk_sign_growth_user_date unique (user_id, sign_date)
) comment '签到成长值幂等记录' charset = utf8mb4;

create table if not exists sign_supplement_used
(
    user_id     varchar(32) not null comment '用户ID',
    sign_date   char(8)     not null comment '补签日期 yyyyMMdd',
    create_time datetime    not null comment '创建时间',
    primary key (user_id, sign_date),
    key idx_supplement_user_time (user_id, create_time)
) comment '补签次数消耗记录' charset = utf8mb4;

create table if not exists sign_supplement_legacy_used
(
    user_id     varchar(32) not null comment '用户ID',
    used_count  int         not null comment '切换前已消耗补签总次数',
    detail_count_at_cutover int not null comment '切流时已有按日明细数',
    source      varchar(32) not null comment '迁移来源',
    update_time datetime    not null comment '迁移更新时间',
    primary key (user_id)
) comment '旧签到补签额度迁移审计' charset = utf8mb4;

create table if not exists user_notification
(
    notification_id varchar(32)          not null primary key,
    user_id         varchar(15)          not null,
    title           varchar(100)         not null,
    content         varchar(500)         null,
    biz_type        varchar(30)          null comment 'order/coupon/system',
    biz_id          varchar(32)          null,
    read_status     tinyint(1) default 0 not null comment '0未读 1已读',
    create_time     datetime             not null
) comment '用户站内通知' collate = utf8mb4_general_ci row_format = DYNAMIC;
create index idx_notification_user_read on user_notification (user_id, read_status, create_time);

create table if not exists user_product_favorite
(
    favorite_id varchar(32) not null comment '收藏ID' primary key,
    user_id     varchar(15) not null comment '用户ID',
    product_id  varchar(15) not null comment '商品ID',
    create_time datetime    not null comment '收藏时间',
    constraint uk_favorite_user_product unique (user_id, product_id)
) comment '用户商品收藏' collate = utf8mb4_general_ci row_format = DYNAMIC;
create index idx_favorite_user_time on user_product_favorite (user_id, create_time);

create table if not exists user_browse_history
(
    history_id  bigint auto_increment comment '记录ID' primary key,
    user_id     varchar(15) not null comment '用户ID',
    product_id  varchar(15) not null comment '商品ID',
    browse_time datetime    not null comment '浏览时间',
    constraint uk_browse_user_product unique (user_id, product_id)
) comment '浏览足迹' collate = utf8mb4_general_ci row_format = DYNAMIC;
create index idx_browse_user_time on user_browse_history (user_id, browse_time);

-- 图像审核写入侧在 user 服务（见 ImageModeration*）；admin 可只读/复核
create table if not exists image_moderation_record
(
    record_id       int auto_increment comment '记录ID' primary key,
    user_id         varchar(32)   not null comment '上传用户ID',
    user_ip         varchar(64)   null comment '上传IP',
    image_path      varchar(512)  not null comment '图片相对路径',
    scene           varchar(32)   not null comment '场景：avatar/comment',
    order_id        varchar(32)   null comment '关联订单ID（评论场景）',
    conclusion_type int           null comment '百度结论类型 1合规 2不合规 3疑似 4失败',
    conclusion      varchar(128)  null comment '百度结论文案',
    baidu_response  text          null comment '百度原始响应摘要',
    status          int default 0 not null comment '0待人工复核 1已通过 2确认违规 3误报驳回',
    create_time     datetime      not null comment '创建时间',
    handle_time     datetime      null comment '处理时间',
    handle_remark   varchar(512)  null comment '处理备注'
) comment '图像内容审核记录' charset = utf8mb4;
