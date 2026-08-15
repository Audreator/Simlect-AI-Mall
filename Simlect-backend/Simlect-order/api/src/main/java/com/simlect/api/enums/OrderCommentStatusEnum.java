package com.simlect.api.enums;

import java.util.Arrays;
import java.util.Optional;

public enum OrderCommentStatusEnum {

    NOT_EVALUATED(0, "未评价"),
    EVALUATED(1, "已评价"),
    ADDITIONAL_EVALUATED(2, "已追评");

    private Integer status;
    private String desc;

    OrderCommentStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderCommentStatusEnum getByStatus(Integer status) {
        Optional<OrderCommentStatusEnum> typeEnum = Arrays.stream(OrderCommentStatusEnum.values()).filter(value -> value.getStatus().equals(status)).findFirst();
        return typeEnum == null ? null : typeEnum.get();
    }
}
