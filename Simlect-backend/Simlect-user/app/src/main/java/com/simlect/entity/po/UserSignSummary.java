package com.simlect.entity.po;

import java.io.Serializable;
import java.util.Date;

/**
 * 签到汇总（派生数据）：连续签到天数 / 累计签到天数 / 已用补签次数，
 * 均可由 sign_bitmap 位图重建；读路径优先走 Redis 缓存。
 */
public class UserSignSummary implements Serializable {

    private String userId;
    private Integer continuousDays;
    private Integer totalSignDays;
    private Integer usedCount;
    private String lastSignDate;
    private Date updateTime;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getContinuousDays() {
        return continuousDays;
    }

    public void setContinuousDays(Integer continuousDays) {
        this.continuousDays = continuousDays;
    }

    public Integer getTotalSignDays() {
        return totalSignDays;
    }

    public void setTotalSignDays(Integer totalSignDays) {
        this.totalSignDays = totalSignDays;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public String getLastSignDate() {
        return lastSignDate;
    }

    public void setLastSignDate(String lastSignDate) {
        this.lastSignDate = lastSignDate;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}