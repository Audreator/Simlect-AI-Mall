package com.simlect.api.enums;

import java.util.Arrays;
import java.util.Optional;

public enum CommendTypeEnum {
    NOT_COMMEND(0, "未推荐"), COMMEND(1, "已推荐");

    private Integer type;

    private String desc;

    CommendTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static CommendTypeEnum getByType(Integer commendType) {
            Optional<CommendTypeEnum> typeEnum = Arrays.stream(CommendTypeEnum.values()).filter(value -> value.getType().equals(commendType)).findFirst();
            return typeEnum == null ||typeEnum.isEmpty()? null : typeEnum.get();
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
