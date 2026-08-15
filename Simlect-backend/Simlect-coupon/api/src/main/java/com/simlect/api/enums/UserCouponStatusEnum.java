package com.simlect.api.enums;

import java.util.Arrays;
import java.util.Optional;

public enum UserCouponStatusEnum {
    NOUSE(0, "未使用"),
    USED(1, "已使用"),
    OVERDUE(2, "已过期"),
    CANT(3, "已作废");

    private Integer status;
    private String desc;

    UserCouponStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static UserCouponStatusEnum getByStatus(Integer status) {
        Optional<UserCouponStatusEnum> typeEnum = Arrays.stream(UserCouponStatusEnum.values()).filter(value -> value.getStatus().equals(status)).findFirst();
        return typeEnum == null ? null : typeEnum.get();
    }
}
