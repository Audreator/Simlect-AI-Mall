-- 签到汇总表：连续签到天数 / 累计签到天数 / 已使用补签次数（派生数据，可由 sign_bitmap 重建）。
-- 读路径：Redis Hash -> 本表 -> 位图重建（首次访问回填）；写入时机：签到 / 补签（同事务 upsert）。

USE simlect_user;

create table if not exists user_sign_summary
(
    user_id         varchar(32) not null comment '用户ID',
    continuous_days int         not null default 0 comment '连续签到天数（截至 last_sign_date）',
    total_sign_days int         not null default 0 comment '累计签到天数',
    used_count      int         not null default 0 comment '已使用补签次数',
    last_sign_date  char(8)     null comment '最近一次签到/补签日期 yyyyMMdd，用于连续天数衰减判断',
    update_time     datetime    not null comment '更新时间',
    primary key (user_id)
) comment '签到汇总（派生缓存，可由位图重建）' charset = utf8mb4;

-- 存量回填：累计天数与已用补签次数可由位图/明细直接聚合；
-- 连续天数与锚点留空，读到空锚点时由首次访问触发一次重建并写回。
insert into user_sign_summary (user_id, continuous_days, total_sign_days, used_count, last_sign_date, update_time)
select b.user_id,
       0,
       b.total_days,
       coalesce(l.used_count, 0) + greatest(coalesce(d.used_count, 0) - coalesce(l.detail_count_at_cutover, 0), 0),
       null,
       now()
from (select user_id, sum(bit_count(bits)) as total_days from sign_bitmap group by user_id) b
         left join sign_supplement_legacy_used l on l.user_id = b.user_id
         left join (select user_id, count(1) as used_count from sign_supplement_used group by user_id) d
                   on d.user_id = b.user_id
on duplicate key update
    total_sign_days = greatest(user_sign_summary.total_sign_days, values(total_sign_days)),
    used_count = greatest(user_sign_summary.used_count, values(used_count)),
    update_time = now();