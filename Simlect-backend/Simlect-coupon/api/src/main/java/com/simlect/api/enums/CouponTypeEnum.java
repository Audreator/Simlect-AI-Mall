package com.simlect.api.enums;

import java.util.Arrays;

public enum CouponTypeEnum {

    FULL(1, "满减卷"),
    DISCOUNT(2, "折扣卷"),
    NOTHRESHOLD(3, "无门槛卷");

    private Integer status;
    private String desc;

    CouponTypeEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static CouponTypeEnum getByStatus(Integer status) {
        return Arrays.stream(CouponTypeEnum.values())
                .filter(value -> value.getStatus().equals(status))
                .findFirst()
                .orElse(null);
    }
}
