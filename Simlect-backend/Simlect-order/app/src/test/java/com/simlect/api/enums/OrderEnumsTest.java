package com.simlect.api.enums;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Simlect-order 模块业务枚举的取值/反查测试。
 */
class OrderEnumsTest {

    // ==================== OrderStatusEnum ====================

    @Test
    void orderStatus_values() {
        assertEquals(-1, OrderStatusEnum.DELETE.getStatus());
        assertEquals(0, OrderStatusEnum.WAIT_PAYMENT.getStatus());
        assertEquals(1, OrderStatusEnum.PAID.getStatus());
        assertEquals(2, OrderStatusEnum.SHIPPED.getStatus());
        assertEquals(3, OrderStatusEnum.COMPLETED.getStatus());
        assertEquals(4, OrderStatusEnum.CANCELLED.getStatus());
        assertEquals(5, OrderStatusEnum.CLOSED.getStatus());
        assertEquals(6, OrderStatusEnum.REFUNDED.getStatus());
        assertEquals(7, OrderStatusEnum.PARTIALLY_REFUNDED.getStatus());
        assertEquals(8, OrderStatusEnum.WAIT_COMMENT.getStatus());
    }

    @Test
    void orderStatus_getByStatus() {
        assertEquals(OrderStatusEnum.PAID, OrderStatusEnum.getByStatus(1));
        assertEquals("已付款,待发货", OrderStatusEnum.PAID.getDesc());
        assertNull(OrderStatusEnum.getByStatus(999));
    }

    // ==================== OrderFromTypeEnum ====================

    @Test
    void orderFromType_values() {
        assertEquals(0, OrderFromTypeEnum.PRODUCT.getType());
        assertEquals(1, OrderFromTypeEnum.CART.getType());
        assertEquals(2, OrderFromTypeEnum.COUPON.getType());
        assertEquals(OrderFromTypeEnum.CART, OrderFromTypeEnum.getByType(1));
        assertEquals("优惠券秒杀", OrderFromTypeEnum.COUPON.getDesc());
        assertNull(OrderFromTypeEnum.getByType(99));
    }

    // ==================== OrderItemStatusEnum ====================

    @Test
    void orderItemStatus_values() {
        assertEquals(0, OrderItemStatusEnum.REFUND.getStatus());
        assertEquals(1, OrderItemStatusEnum.NORMAL.getStatus());
        assertEquals(OrderItemStatusEnum.NORMAL, OrderItemStatusEnum.getByStatus(1));
        assertNull(OrderItemStatusEnum.getByStatus(99));
    }

    // ==================== CommentStatusEnum ====================

    @Test
    void commentStatus_values() {
        assertEquals(0, CommentStatusEnum.NORMAL.getStatus());
        assertEquals(1, CommentStatusEnum.DEL.getStatus());
        assertEquals(2, CommentStatusEnum.PENDING.getStatus());
        assertEquals(CommentStatusEnum.PENDING, CommentStatusEnum.getByStatus(2));
        assertThrows(NoSuchElementException.class, () -> CommentStatusEnum.getByStatus(99));
    }

    // ==================== OrderCommentStatusEnum ====================

    @Test
    void orderCommentStatus_values() {
        assertEquals(OrderCommentStatusEnum.NOT_EVALUATED, OrderCommentStatusEnum.getByStatus(0));
        assertEquals(OrderCommentStatusEnum.EVALUATED, OrderCommentStatusEnum.getByStatus(1));
        assertEquals(OrderCommentStatusEnum.ADDITIONAL_EVALUATED, OrderCommentStatusEnum.getByStatus(2));
    }

    // ==================== LogisticsStatusEnum ====================

    @Test
    void logisticsStatus_values() {
        assertEquals(LogisticsStatusEnum.PENDING_SHIPMENT, LogisticsStatusEnum.getByStatus(0));
        assertEquals(LogisticsStatusEnum.IN_TRANSIT, LogisticsStatusEnum.getByStatus(1));
        assertEquals(LogisticsStatusEnum.DELIVERED, LogisticsStatusEnum.getByStatus(2));
    }

    // ==================== PayChannelEnum ====================

    @Test
    void payChannel_resolveBySceneAndChannel() {
        assertEquals(PayChannelEnum.ALIPAY_PC, PayChannelEnum.getByPayScene("alipay_pc"));
        assertEquals(PayChannelEnum.ALIPAY_WAP, PayChannelEnum.getByPayScene("alipay_wap"));
        assertEquals(PayChannelEnum.ALIPAY_PC, PayChannelEnum.resolve("alipay"));
        assertEquals(PayChannelEnum.ALIPAY_WAP, PayChannelEnum.resolve("alipay_wap"));
        assertNull(PayChannelEnum.resolve("wechat"));
        assertNull(PayChannelEnum.resolve(null));
    }

    // ==================== OrderStatusDTO 静态转换 ====================

    @Test
    void orderStatusDTO_fromEnum() {
        com.simlect.api.dto.OrderStatusDTO dto = com.simlect.api.dto.OrderStatusDTO.getByStatus(OrderStatusEnum.SHIPPED);
        assertEquals(2, dto.getStatus());
        assertEquals("已发货", dto.getDesc());
    }
}
