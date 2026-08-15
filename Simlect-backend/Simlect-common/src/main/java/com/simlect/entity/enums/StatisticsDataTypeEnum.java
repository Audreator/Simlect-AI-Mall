package com.simlect.entity.enums;

public enum StatisticsDataTypeEnum {
    SALE_AMOUNT(1, "销售金额"),
    SALE_COUNT(2, "订单数量"),
    REFUND_AMOUNT(3, "退款金额"),
    REFUND_COUNT(4, "退款数量"),
    ;

    private Integer type;

    private String desc;

    StatisticsDataTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
