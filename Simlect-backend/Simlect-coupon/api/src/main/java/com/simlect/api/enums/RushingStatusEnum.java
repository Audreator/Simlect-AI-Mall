package com.simlect.api.enums;

import java.util.Arrays;
import java.util.Optional;

public enum RushingStatusEnum {
    ALL("all", "全部"),
    UPCOMING("upcoming", "即将开始"),
    ONGOING("ongoing", "进行中"),
    ENDED("ended", "已结束");

    RushingStatusEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private String type;

    private String desc;
}
