package com.simlect.api.enums;

import java.util.Arrays;
import java.util.Optional;

public enum CommentStatusEnum {

    NORMAL(0, "正常"),
    DEL(1, "已删除"),
    PENDING(2, "待审核");

    private Integer status;
    private String desc;

    CommentStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static CommentStatusEnum getByStatus(Integer status) {
        Optional<CommentStatusEnum> typeEnum = Arrays.stream(CommentStatusEnum.values()).filter(value -> value.getStatus().equals(status)).findFirst();
        return typeEnum == null ? null : typeEnum.get();
    }
}
