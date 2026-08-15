-- ============================================================
-- EShop(Simlect) 升级脚本：order_item 增加 refund_amount 列
-- 用途：部分退款后按已退累计计算最后一项退款金额，防累计超退
-- 说明：可在已部署库上重复执行（带 information_schema 存在性判断）
-- ============================================================

SET @db := DATABASE();

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_item'
      AND COLUMN_NAME = 'refund_amount'
);

SET @sql := IF(
    @has_col = 0,
    'ALTER TABLE order_item ADD COLUMN refund_amount decimal(10,2) NULL COMMENT ''实际退款金额（券后分摊，防累计超退）'' AFTER refund_order_id',
    'SELECT ''refund_amount already exists, skip'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
