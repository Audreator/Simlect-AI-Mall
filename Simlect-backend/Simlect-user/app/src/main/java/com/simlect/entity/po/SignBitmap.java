package com.simlect.entity.po;

import java.io.Serializable;
import java.util.Date;

/**
 * MySQL 权威签到位图：每个用户每月一行，bit 0 对应该月 1 日。
 */
public class SignBitmap implements Serializable {

    private String userId;
    private String yearMonth;
    private Long bits;
    private Date updateTime;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public Long getBits() {
        return bits;
    }

    public void setBits(Long bits) {
        this.bits = bits;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
