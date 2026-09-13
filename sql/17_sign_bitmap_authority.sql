-- 签到方案 B：MySQL 位图权威迁移（可重复执行）。
-- 应用新版本前先执行建表与回填；旧 user_sign_record 暂时保留，仅用于回滚观察。

USE simlect_user;

create table if not exists sign_bitmap
(
    user_id     varchar(32)  not null,
    `year_month` char(6)    not null,
    bits        int unsigned default 0 not null,
    update_time datetime     not null,
    primary key (user_id, `year_month`)
) comment 'MySQL 权威签到位图' charset = utf8mb4;

create table if not exists sign_growth_record
(
    id          bigint auto_increment primary key,
    user_id     varchar(32) not null,
    sign_date   char(8)     not null,
    amount      int         not null,
    create_time datetime    not null,
    constraint uk_sign_growth_user_date unique (user_id, sign_date)
) comment '签到成长值幂等记录' charset = utf8mb4;

create table if not exists sign_supplement_used
(
    user_id     varchar(32) not null,
    sign_date   char(8)     not null,
    create_time datetime    not null,
    primary key (user_id, sign_date),
    key idx_supplement_user_time (user_id, create_time)
) comment '补签次数消耗记录' charset = utf8mb4;

create table if not exists sign_supplement_legacy_used
(
    user_id     varchar(32) not null,
    used_count  int         not null,
    detail_count_at_cutover int not null,
    source      varchar(32) not null,
    update_time datetime    not null,
    primary key (user_id)
) comment '旧签到补签额度迁移审计' charset = utf8mb4;

SET @legacy_cutover_column_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'simlect_user'
      AND TABLE_NAME = 'sign_supplement_legacy_used'
      AND COLUMN_NAME = 'detail_count_at_cutover'
);
SET @legacy_cutover_ddl = IF(
    @legacy_cutover_column_exists = 0,
    'ALTER TABLE sign_supplement_legacy_used ADD COLUMN detail_count_at_cutover int NOT NULL DEFAULT 0 AFTER used_count',
    'SELECT 1'
);
PREPARE legacy_cutover_stmt FROM @legacy_cutover_ddl;
EXECUTE legacy_cutover_stmt;
DEALLOCATE PREPARE legacy_cutover_stmt;

INSERT INTO sign_bitmap (user_id, `year_month`, bits, update_time)
SELECT user_id,
       LEFT(sign_date, 6),
       CAST(BIT_OR(CAST(1 AS UNSIGNED) << (CAST(RIGHT(sign_date, 2) AS UNSIGNED) - 1)) AS UNSIGNED),
       MAX(create_time)
FROM user_sign_record_detail
WHERE sign_date REGEXP '^[0-9]{8}$'
  AND CAST(RIGHT(sign_date, 2) AS UNSIGNED) BETWEEN 1 AND 31
GROUP BY user_id, LEFT(sign_date, 6)
ON DUPLICATE KEY UPDATE
    bits = sign_bitmap.bits | VALUES(bits),
    update_time = GREATEST(sign_bitmap.update_time, VALUES(update_time));

-- 仅建立历史幂等基线，不重复给已有签到补发成长值。
INSERT IGNORE INTO sign_growth_record (user_id, sign_date, amount, create_time)
SELECT user_id, sign_date, 5, create_time
FROM user_sign_record_detail;

INSERT IGNORE INTO sign_supplement_used (user_id, sign_date, create_time)
SELECT user_id, sign_date, create_time
FROM user_sign_record_detail
WHERE sign_type = 1;

-- 兼容汇总表可作为 Redis 快照导入前的保底审计基线。运行 sql/18 的 Redis
-- 快照导入后，同一用户会以 Redis usedCount 覆盖/抬高此总数；运行时按
-- legacy_total + (current_detail_count - detail_count_at_cutover) 递增，确保切流后的
-- 每一次新补签都会立即消耗额度。
INSERT INTO sign_supplement_legacy_used
    (user_id, used_count, detail_count_at_cutover, source, update_time)
SELECT r.user_id,
       GREATEST(r.used_count, COALESCE(d.detail_count, 0)),
       COALESCE(d.detail_count, 0),
       'user_sign_record',
       NOW()
FROM user_sign_record r
LEFT JOIN (
    SELECT user_id, COUNT(1) AS detail_count
    FROM sign_supplement_used
    GROUP BY user_id
) d ON d.user_id COLLATE utf8mb4_general_ci = r.user_id
WHERE r.used_count IS NOT NULL AND (r.used_count > 0 OR COALESCE(d.detail_count, 0) > 0)
ON DUPLICATE KEY UPDATE
    source = IF(VALUES(used_count) >= sign_supplement_legacy_used.used_count,
                VALUES(source), sign_supplement_legacy_used.source),
    -- 切流快照一经建立不可移动；否则切流后重跑脚本会吞掉新增消费。
    detail_count_at_cutover = sign_supplement_legacy_used.detail_count_at_cutover,
    used_count = GREATEST(sign_supplement_legacy_used.used_count, VALUES(used_count)),
    update_time = NOW();

USE simlect_coupon;

create table if not exists sign_streak_coupon_grant
(
    id                bigint auto_increment primary key,
    idempotency_key   varchar(128) not null,
    user_id           varchar(32)  not null,
    coupon_id         varchar(20)  not null,
    streak_days       int          not null,
    user_coupon_id    varchar(32)  null,
    status            tinyint default 0 not null,
    reject_reason     varchar(255) null,
    create_time       datetime     not null,
    update_time       datetime     null,
    constraint uk_sign_streak_grant_key unique (idempotency_key),
    constraint uk_sign_streak_grant_biz unique (user_id, coupon_id, streak_days)
) comment '连续签到发券幂等结果' charset = utf8mb4;

create table if not exists local_message_outbox
(
    id bigint auto_increment primary key,
    idempotency_key varchar(128) not null,
    exchange_name varchar(64) not null,
    routing_key varchar(64) not null,
    payload_json mediumtext not null,
    reliability_level varchar(16) default 'STANDARD' not null,
    status tinyint default 0 not null,
    retry_count int default 0 not null,
    error_message varchar(512) null,
    create_time datetime not null,
    update_time datetime null,
    sent_time datetime null,
    constraint uk_outbox_idempotency unique (idempotency_key),
    key idx_outbox_status_ctime (status, create_time)
) comment '本地消息 Outbox' charset = utf8mb4;

create table if not exists mq_compensation_log
(
    log_id int auto_increment primary key,
    idempotency_key varchar(128) not null,
    exchange varchar(64) not null,
    routing_key varchar(64) not null,
    biz_scene varchar(32) null,
    payload_json mediumtext null,
    reliability_level varchar(16) default 'HIGH' not null,
    error_message varchar(512) null,
    retry_count int default 0 not null,
    status int default 0 not null,
    create_time datetime not null,
    update_time datetime null,
    handle_time datetime null,
    handle_remark varchar(512) null,
    constraint uk_idempotency_key unique (idempotency_key)
) comment 'MQ补偿审查日志' charset = utf8mb4;
