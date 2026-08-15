package com.simlect.api.enums;

import java.util.Arrays;

public enum RushingCouponStatusEnum {
    NO(0, "否"),
    YES(1, "是");

    private Integer status;
    private String desc;

    RushingCouponStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static RushingCouponStatusEnum getByStatus(Integer status) {
        return Arrays.stream(RushingCouponStatusEnum.values())
                .filter(value -> value.getStatus().equals(status))
                .findFirst()
                .orElse(null);
    }
}
