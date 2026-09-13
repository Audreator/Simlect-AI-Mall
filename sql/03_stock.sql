USE simlect_stock;
CREATE TABLE IF NOT EXISTS sku_stock (
    product_id             varchar(15)  NOT NULL COMMENT '商品ID',
    property_value_id_hash varchar(32)  NOT NULL COMMENT '属性值id组hash',
    stock                  int          NOT NULL COMMENT '库存',
    PRIMARY KEY (product_id, property_value_id_hash)
) COMMENT 'SKU库存（从 product_sku.stock 迁出）' COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS stock_change_operation (
    operation_id   varchar(128) NOT NULL COMMENT '业务幂等键',
    affected_rows  int          NULL COMMENT '完成时影响的SKU行数，NULL表示事务处理中，-1表示已取消墓碑',
    create_time    datetime     NOT NULL,
    update_time    datetime     NOT NULL,
    PRIMARY KEY (operation_id)
) COMMENT '库存批量变更幂等记录' COLLATE = utf8mb4_general_ci;
