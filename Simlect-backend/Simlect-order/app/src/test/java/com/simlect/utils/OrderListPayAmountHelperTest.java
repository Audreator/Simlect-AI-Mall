package com.simlect.utils;

import com.simlect.entity.po.OrderInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 多子单最低支付额补差（参考 EShop OrderPayAmountUtilTest 思路）。
 */
class OrderListPayAmountHelperTest {

    @Test
    void emptyOrNullList_noop() {
        OrderListPayAmountHelper.ensureOrderListMinTotalPay(null);
        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of());
    }

    @Test
    void sumAboveMinPay_unchanged() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("60.00"));
        OrderInfo o2 = new OrderInfo();
        o2.setAmount(new BigDecimal("40.00"));

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1, o2));

        assertEquals(new BigDecimal("60.00"), o1.getAmount());
        assertEquals(new BigDecimal("40.00"), o2.getAmount());
    }

    @Test
    void sumBelowMinPay_lastOrderGetsDiff() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("0.00"));
        OrderInfo o2 = new OrderInfo();
        o2.setAmount(new BigDecimal("0.00"));

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1, o2));

        // 最小支付 0.01，总金额 0 → 最后一单补 0.01
        assertEquals(new BigDecimal("0.00"), o1.getAmount());
        assertEquals(new BigDecimal("0.01"), o2.getAmount());
    }

    @Test
    void nullAmountTreatedAsZero() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("0.00"));
        OrderInfo o2 = new OrderInfo();
        o2.setAmount(null);

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1, o2));

        assertEquals(new BigDecimal("0.01"), o2.getAmount());
    }

    @Test
    void singleOrderBelowMinPay_raisedToMinPay() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("0.005"));

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1));

        assertEquals(new BigDecimal("0.01"), o1.getAmount());
    }

    @Test
    void discountDrivenScenario() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("0.00"));
        OrderInfo o2 = new OrderInfo();
        o2.setAmount(new BigDecimal("5.00"));

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1, o2));

        // 合计 5.00 >= 0.01，不补差
        assertEquals(new BigDecimal("0.00"), o1.getAmount());
        assertEquals(new BigDecimal("5.00"), o2.getAmount());
    }
}
