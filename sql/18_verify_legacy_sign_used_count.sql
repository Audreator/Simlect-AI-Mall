-- sql/18 Redis 快照导入后的切流前核验。
USE simlect_user;

-- 任何 legacy 总数大于按日明细数的用户都必须保留该差额，运行时会取二者较大值。
SELECT l.user_id,
       l.used_count AS imported_used_count,
       l.detail_count_at_cutover,
       COUNT(s.user_id) AS dated_supplement_count,
       l.used_count + GREATEST(COUNT(s.user_id) - l.detail_count_at_cutover, 0) AS effective_used_count,
       l.source,
       l.update_time
FROM sign_supplement_legacy_used l
LEFT JOIN sign_supplement_used s ON s.user_id = l.user_id
GROUP BY l.user_id, l.used_count, l.detail_count_at_cutover, l.source, l.update_time
ORDER BY l.user_id;

-- 导入缺口为零时，使用旧汇总表作最后一道保底比对；有差异应阻断切流。
SELECT r.user_id,
       r.used_count AS user_record_used_count,
       COALESCE(l.used_count, 0)
           + GREATEST(COALESCE(s.dated_count, 0) - COALESCE(l.detail_count_at_cutover, 0), 0)
           AS effective_used_count
FROM user_sign_record r
LEFT JOIN (
    SELECT user_id, COUNT(1) AS dated_count
    FROM sign_supplement_used
    GROUP BY user_id
) s
       ON s.user_id COLLATE utf8mb4_general_ci = r.user_id
LEFT JOIN sign_supplement_legacy_used l
       ON l.user_id COLLATE utf8mb4_general_ci = r.user_id
WHERE r.used_count > COALESCE(l.used_count, 0)
           + GREATEST(COALESCE(s.dated_count, 0) - COALESCE(l.detail_count_at_cutover, 0), 0);
