package com.simlect.biz;

import com.simlect.api.dto.CouponRushPayRequestDTO;
import com.simlect.api.dto.CouponRushPrepareDTO;
import com.simlect.api.dto.CouponRushPrepareRequestDTO;
import com.simlect.api.dto.OrderIdDTO;
import com.simlect.api.dto.OrderStatsRangeDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.dto.UserIdDTO;
import com.simlect.api.enums.OrderItemStatusEnum;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.api.vo.OrderBriefVO;
import com.simlect.api.vo.OrderDailyStatsVO;
import com.simlect.api.vo.OrderRangeStatsVO;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderItem;
import com.simlect.entity.query.OrderInfoQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderInternalServiceTest {

    private static final String ORDER_ID = "O1";
    private static final String USER_ID = "U1";

    @Mock
    private OrderInfoService orderInfoService;

    @InjectMocks
    private OrderInternalService service;

    private OrderInfo order(OrderStatusEnum status) {
        OrderInfo o = new OrderInfo();
        o.setOrderId(ORDER_ID);
        o.setUserId(USER_ID);
        o.setOrderStatus(status.getStatus());
        o.setAmount(new BigDecimal("100.00"));
        o.setOrderTime(new Date());
        return o;
    }

    @Test
    void getOrder_emptyDto_returnsNull() {
        assertNull(service.getOrder(null));
        assertNull(service.getOrder(new OrderIdDTO()));
    }

    @Test
    void getOrder_notExists_returnsNull() {
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(null);
        assertNull(service.getOrder(new OrderIdDTO(ORDER_ID)));
    }

    @Test
    void getOrder_success() {
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(order(OrderStatusEnum.PAID));
        OrderBriefVO vo = service.getOrder(new OrderIdDTO(ORDER_ID));
        assertEquals(ORDER_ID, vo.getOrderId());
        assertEquals(USER_ID, vo.getUserId());
        assertEquals(OrderStatusEnum.PAID.getStatus(), vo.getOrderStatus());
        assertEquals(new BigDecimal("100.00"), vo.getAmount());
    }

    @Test
    void cancelUnpaidForPayTimeout_delegates() {
        when(orderInfoService.cancelUnpaidOrderForPayTimeout(ORDER_ID)).thenReturn(true);
        assertTrue(service.cancelUnpaidForPayTimeout(new OrderIdDTO(ORDER_ID)));
    }

    @Test
    void confirmReceipt_delegates() {
        OrderIdDTO dto = new OrderIdDTO(ORDER_ID);
        dto.setUserId(USER_ID);
        when(orderInfoService.confirmOrderReceipt(USER_ID, ORDER_ID)).thenReturn(true);
        assertTrue(service.confirmReceipt(dto));
    }

    @Test
    void onConfirmed_delegates() {
        OrderIdDTO dto = new OrderIdDTO(ORDER_ID);
        dto.setUserId(USER_ID);
        service.onConfirmed(dto);
        verify(orderInfoService).onOrderConfirmed(USER_ID, ORDER_ID);
    }

    @Test
    void paySuccess_delegates() {
        PayOrderNotifyDTO dto = new PayOrderNotifyDTO("PO1", "CH1");
        service.paySuccess(dto);
        verify(orderInfoService).paySuccess(dto);
    }

    @Test
    void prepareCouponRush_delegates() {
        CouponRushPrepareRequestDTO req = new CouponRushPrepareRequestDTO();
        req.setUserId(USER_ID);
        req.setCouponId("C1");
        CouponRushPrepareDTO expected = new CouponRushPrepareDTO();
        when(orderInfoService.prepareCouponRush(USER_ID, "C1")).thenReturn(expected);
        assertSame(expected, service.prepareCouponRush(req));
    }

    @Test
    void postCouponRushOrder_delegates() {
        CouponRushPayRequestDTO req = new CouponRushPayRequestDTO();
        req.setUserId(USER_ID);
        req.setCouponId("C1");
        req.setPayMethod("alipay_pc");
        PayInfoDTO expected = new PayInfoDTO();
        when(orderInfoService.postCouponRushOrder(USER_ID, "C1", "alipay_pc")).thenReturn(expected);
        assertSame(expected, service.postCouponRushOrder(req));
    }

    @Test
    void syncPaidCouponRushUserCoupons_delegates() {
        UserIdDTO dto = new UserIdDTO();
        dto.setUserId(USER_ID);
        service.syncPaidCouponRushUserCoupons(dto);
        verify(orderInfoService).syncPaidCouponRushUserCoupons(USER_ID);
    }

    @Test
    void cancelOrder_delegates() {
        OrderIdDTO dto = new OrderIdDTO(ORDER_ID);
        dto.setUserId(USER_ID);
        service.cancelOrder(dto);
        verify(orderInfoService).cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT);
    }

    @Test
    void cancelOrder_withoutUserDelegatesAsPaymentTimeout() {
        OrderIdDTO dto = new OrderIdDTO(ORDER_ID);

        service.cancelOrder(dto);

        verify(orderInfoService).cancelOrder(null, ORDER_ID, OrderStatusEnum.CLOSED);
    }

    // ==================== 统计 ====================

    @Test
    void aggregateRange_emptyTime_returnsZeroVO() {
        OrderRangeStatsVO vo = service.aggregateRange(new OrderStatsRangeDTO());
        assertEquals(BigDecimal.ZERO, vo.getSaleAmount());
        assertEquals(BigDecimal.ZERO, vo.getRefundAmount());
    }

    @Test
    void aggregateRange_sumsSaleAndRefund() {
        OrderStatsRangeDTO dto = new OrderStatsRangeDTO();
        dto.setStartTime("2026-08-01 00:00:00");
        dto.setEndTime("2026-08-31 23:59:59");

        OrderInfo sale = order(OrderStatusEnum.PAID);
        sale.setOrderItemList(List.of());
        OrderInfo refunded = order(OrderStatusEnum.REFUNDED);
        refunded.setOrderItemList(List.of());
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class)))
                .thenReturn(List.of(sale), List.of(refunded));

        OrderRangeStatsVO vo = service.aggregateRange(dto);

        assertEquals(new BigDecimal("100.00"), vo.getSaleAmount());
        assertEquals(new BigDecimal("1"), vo.getSaleOrderCount());
        // refunded 订单 item 列表为空 → 退款金额 0
        assertEquals(BigDecimal.ZERO, vo.getRefundAmount());
    }

    @Test
    void aggregateRange_refundAmount_fromNonNormalItems() {
        OrderStatsRangeDTO dto = new OrderStatsRangeDTO();
        dto.setStartTime("2026-08-01 00:00:00");
        dto.setEndTime("2026-08-31 23:59:59");
        OrderInfo refunded = order(OrderStatusEnum.PARTIALLY_REFUNDED);
        OrderItem refundedItem = new OrderItem();
        refundedItem.setOrderItemId("OI2");
        refundedItem.setOrderItemStatus(OrderItemStatusEnum.REFUND.getStatus());
        refundedItem.setItemAmount(new BigDecimal("30.00"));
        OrderItem normalItem = new OrderItem();
        normalItem.setOrderItemId("OI1");
        normalItem.setOrderItemStatus(OrderItemStatusEnum.NORMAL.getStatus());
        normalItem.setItemAmount(new BigDecimal("70.00"));
        refunded.setOrderItemList(List.of(refundedItem, normalItem));
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(refunded), List.of(refunded));

        OrderRangeStatsVO vo = service.aggregateRange(dto);

        // 部分退款单有效销售额 = 100 * 70/100 = 70
        assertEquals(new BigDecimal("70.00"), vo.getSaleAmount());
        // 非正常 item 金额 = 30
        assertEquals(new BigDecimal("30.00"), vo.getRefundAmount());
    }

    @Test
    void aggregateDaily_noOrders_returnsEmpty() {
        OrderStatsRangeDTO dto = new OrderStatsRangeDTO();
        dto.setStartTime("2026-08-01 00:00:00");
        dto.setEndTime("2026-08-31 23:59:59");
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of());
        assertTrue(service.aggregateDaily(dto).isEmpty());
    }

    @Test
    void aggregateDaily_groupsOrders() {
        OrderStatsRangeDTO dto = new OrderStatsRangeDTO();
        dto.setStartTime("2026-08-01 00:00:00");
        dto.setEndTime("2026-08-31 23:59:59");
        OrderInfo o1 = order(OrderStatusEnum.PAID);
        o1.setChannelOrderId("CH1");
        OrderInfo o2 = order(OrderStatusEnum.REFUNDED);
        o2.setChannelOrderId("CH2");
        o2.setOrderItemList(List.of());
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(o1, o2));

        List<OrderDailyStatsVO> list = service.aggregateDaily(dto);

        assertEquals(1, list.size());
        OrderDailyStatsVO vo = list.get(0);
        assertEquals(new BigDecimal("2"), vo.getSaleCount());
        assertEquals(new BigDecimal("1"), vo.getRefundCount());
        assertEquals(new BigDecimal("200.00"), vo.getSaleAmount());
        assertEquals(BigDecimal.ZERO, vo.getRefundAmount());
    }

    @Test
    void aggregateDaily_adjacentOneAmWindowsUseDistinctBusinessDates() {
        OrderStatsRangeDTO firstWindow = new OrderStatsRangeDTO(
                "2026-09-11 01:00:00", "2026-09-12 00:59:59");
        OrderStatsRangeDTO secondWindow = new OrderStatsRangeDTO(
                "2026-09-12 01:00:00", "2026-09-13 00:59:59");
        OrderInfo beforeBoundary = order(OrderStatusEnum.PAID);
        beforeBoundary.setChannelOrderId("CH-BEFORE");
        beforeBoundary.setOrderTime(atShanghai(2026, 9, 12, 0, 59, 59));
        OrderInfo atBoundary = order(OrderStatusEnum.PAID);
        atBoundary.setChannelOrderId("CH-AT");
        atBoundary.setOrderTime(atShanghai(2026, 9, 12, 1, 0, 0));
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class)))
                .thenReturn(List.of(beforeBoundary), List.of(atBoundary));

        List<OrderDailyStatsVO> first = service.aggregateDaily(firstWindow);
        List<OrderDailyStatsVO> second = service.aggregateDaily(secondWindow);

        assertEquals("2026-09-11", first.get(0).getStatisticsDate());
        assertEquals("2026-09-12", second.get(0).getStatisticsDate());
    }

    private Date atShanghai(int year, int month, int day, int hour, int minute, int second) {
        return Date.from(LocalDateTime.of(year, month, day, hour, minute, second)
                .atZone(ZoneId.of("Asia/Shanghai"))
                .toInstant());
    }
}
