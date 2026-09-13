package com.simlect.api.dto;

import java.io.Serializable;

public class SignStreakCouponMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String couponId;
    private Integer streakDays;

    public SignStreakCouponMessageDTO() {
    }

    public SignStreakCouponMessageDTO(String userId, String couponId, Integer streakDays) {
        this.userId = userId;
        this.couponId = couponId;
        this.streakDays = streakDays;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCouponId() {
        return couponId;
    }

    public void setCouponId(String couponId) {
        this.couponId = couponId;
    }

    public Integer getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(Integer streakDays) {
        this.streakDays = streakDays;
    }
}
