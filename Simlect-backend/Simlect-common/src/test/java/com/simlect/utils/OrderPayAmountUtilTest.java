package com.simlect.utils;

import com.simlect.constants.Constants;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单实付金额计算：最低支付额、券抵扣上限、渠道金额归一化、多子单补差。
 */
class OrderPayAmountUtilTest {

    // ==================== minOrderPayAmount ====================

    @Test
    void minOrderPayAmount_isOneCent() {
        assertEquals(new BigDecimal("0.01"), OrderPayAmountUtil.minOrderPayAmount());
    }

    // ==================== capCouponDiscountForMinPay ====================

    @Test
    void capCouponDiscount_nullOrderTotal_returnsDiscount() {
        assertEquals(new BigDecimal("5"), OrderPayAmountUtil.capCouponDiscountForMinPay(null, new BigDecimal("5")));
    }

    @Test
    void capCouponDiscount_nullDiscount_returnsZero() {
        assertEquals(BigDecimal.ZERO, OrderPayAmountUtil.capCouponDiscountForMinPay(new BigDecimal("100"), null));
    }

    @Test
    void capCouponDiscount_nonPositiveDiscount_returnsDiscount() {
        assertEquals(new BigDecimal("0"), OrderPayAmountUtil.capCouponDiscountForMinPay(new BigDecimal("100"), BigDecimal.ZERO));
        assertEquals(new BigDecimal("-1"), OrderPayAmountUtil.capCouponDiscountForMinPay(new BigDecimal("100"), new BigDecimal("-1")));
    }

    @Test
    void capCouponDiscount_orderTotalNotAboveMinPay_returnsZero() {
        assertEquals(BigDecimal.ZERO, OrderPayAmountUtil.capCouponDiscountForMinPay(new BigDecimal("0.01"), new BigDecimal("1")));
        assertEquals(BigDecimal.ZERO, OrderPayAmountUtil.capCouponDiscountForMinPay(new BigDecimal("0.005"), new BigDecimal("1")));
    }

    @Test
    void capCouponDiscount_discountWithinLimit_returnsDiscountTwoScale() {
        // 100 - 0.01 = 99.99 上限，抵扣 5 未超
        assertEquals(new BigDecimal("5.00"), OrderPayAmountUtil.capCouponDiscountForMinPay(new BigDecimal("100"), new BigDecimal("5")));
    }

    @Test
    void capCouponDiscount_discountExceedsLimit_cappedToOrderTotalMinusMinPay() {
        // 上限 99.99，抵扣 999 被截断为 99.99
        assertEquals(new BigDecimal("99.99"), OrderPayAmountUtil.capCouponDiscountForMinPay(new BigDecimal("100"), new BigDecimal("999")));
    }

    @Test
    void capCouponDiscount_roundsTwoScale() {
        assertEquals(new BigDecimal("1.23"), OrderPayAmountUtil.capCouponDiscountForMinPay(new BigDecimal("10"), new BigDecimal("1.234")));
        assertEquals(new BigDecimal("9.99"), OrderPayAmountUtil.capCouponDiscountForMinPay(new BigDecimal("10"), new BigDecimal("9.999")));
    }

    // ==================== normalizeChannelPayAmount ====================

    @Test
    void normalizeChannelPayAmount_null_orNonPositive_raisesToMinPay() {
        assertEquals(new BigDecimal("0.01"), OrderPayAmountUtil.normalizeChannelPayAmount(null));
        assertEquals(new BigDecimal("0.01"), OrderPayAmountUtil.normalizeChannelPayAmount(BigDecimal.ZERO));
        assertEquals(new BigDecimal("0.01"), OrderPayAmountUtil.normalizeChannelPayAmount(new BigDecimal("-5")));
    }

    @Test
    void normalizeChannelPayAmount_belowMinPay_raisesToMinPay() {
        assertEquals(new BigDecimal("0.01"), OrderPayAmountUtil.normalizeChannelPayAmount(new BigDecimal("0.005")));
        assertEquals(new BigDecimal("0.01"), OrderPayAmountUtil.normalizeChannelPayAmount(new BigDecimal("0.0099")));
    }

    @Test
    void normalizeChannelPayAmount_normalAmount_twoScale() {
        assertEquals(new BigDecimal("10.50"), OrderPayAmountUtil.normalizeChannelPayAmount(new BigDecimal("10.5")));
        assertEquals(new BigDecimal("10.56"), OrderPayAmountUtil.normalizeChannelPayAmount(new BigDecimal("10.555")));
    }

    @Test
    void formatChannelPayAmount_plainString() {
        assertEquals("10.50", OrderPayAmountUtil.formatChannelPayAmount(new BigDecimal("10.5")));
        assertEquals("0.01", OrderPayAmountUtil.formatChannelPayAmount(null));
    }
}

