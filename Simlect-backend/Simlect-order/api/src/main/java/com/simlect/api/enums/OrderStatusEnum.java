package com.simlect.api.enums;

import java.util.Arrays;

public enum OrderStatusEnum {
    DELETE(-1, "已删除"),
    WAIT_PAYMENT(0, "待付款"),
    PAID(1, "已付款,待发货"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "交易取消"),
    CLOSED(5, "交易关闭"),
    REFUNDED(6, "已退款,交易关闭"),
    PARTIALLY_REFUNDED(7, "部分退款"),
    WAIT_COMMENT(8, "待评价");

    private Integer status;
    private String desc;

    OrderStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatusEnum getByStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return Arrays.stream(OrderStatusEnum.values())
                .filter(value -> value.getStatus().equals(status))
                .findFirst()
                .orElse(null);
    }
}
