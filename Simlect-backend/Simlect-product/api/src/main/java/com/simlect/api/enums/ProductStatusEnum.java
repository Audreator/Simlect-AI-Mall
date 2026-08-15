package com.simlect.api.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ProductStatusEnum {
    DELETE(-1, "已删除"), OFF_SALE(0, "未上架"), ON_SALE(1, "已上架");
    private Integer status;
    private String desc;

    ProductStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static ProductStatusEnum getByStatus(Integer status) {
        Optional<ProductStatusEnum> typeEnum = Arrays.stream(ProductStatusEnum.values()).filter(value -> value.getStatus().equals(status)).findFirst();
        return typeEnum == null ||typeEnum.isEmpty()? null : typeEnum.get();
    }
}
