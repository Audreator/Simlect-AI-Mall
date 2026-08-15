package com.simlect.api.enums;

import java.util.Arrays;

public enum CouponStatusEnum {

    STOP(0, "已停用"),
    NORMAL(1, "正常"),
    OVERDUE(2, "已过期"),
    FINISH(3, "已发完");

    private Integer status;
    private String desc;

    CouponStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static CouponStatusEnum getByStatus(Integer status) {
        return Arrays.stream(CouponStatusEnum.values())
                .filter(value -> value.getStatus().equals(status))
                .findFirst()
                .orElse(null);
    }
}
