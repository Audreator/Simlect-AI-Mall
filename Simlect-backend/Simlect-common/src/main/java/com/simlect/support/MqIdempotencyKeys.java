package com.simlect.support;

import com.simlect.utils.StringTools;

public final class MqIdempotencyKeys {

    private MqIdempotencyKeys() {
    }

    public static String ragProduct(String productId) {
        return "rag:product:" + require(productId);
    }

    public static String ragFaq(String questionId) {
        return "rag:faq:" + require(questionId);
    }

    public static String payTimeout(String orderId) {
        return "pay:timeout:" + require(orderId);
    }

    public static String payLogistics(String orderId) {
        return payLogistics(orderId, 0);
    }

    public static String payLogistics(String orderId, int step) {
        return "pay:logistics:" + require(orderId) + ":step:" + step;
    }

    public static String payConfirm(String orderId) {
        return "pay:confirm:" + require(orderId);
    }

    public static String browseRecord(String userId, String productId) {
        return "browse:" + require(userId) + ":" + require(productId);
    }

    public static String signRecord(String userId, String yyyyMMdd) {
        return "sign:record:" + require(userId) + ":" + require(yyyyMMdd);
    }

    public static String signStreakCoupon(String userId, String couponId, int streakDays) {
        if (streakDays < 1) {
            throw new IllegalArgumentException("连续签到天数必须大于 0");
        }
        return "sign:streak:coupon:" + require(userId) + ":" + require(couponId) + ":" + streakDays;
    }

    public static String notification(String userId, String bizType, String bizId) {
        return "notify:" + require(userId) + ":"
                + (bizType == null ? "" : bizType) + ":"
                + (bizId == null ? "" : bizId);
    }

    public static String tempBanUnban(String userId, long unbanAtMs) {
        return "tempban:unban:" + require(userId) + ":" + unbanAtMs;
    }

    private static String require(String value) {
        if (StringTools.isEmpty(value)) {
            throw new IllegalArgumentException("MQ 幂等键参数不能为空");
        }
        return value;
    }
}
