package com.simlect.biz.impl;

import com.simlect.api.dto.CouponRushPrepareDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderMessageDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.dto.PostOrderDTO;
import com.simlect.api.enums.CommentStatusEnum;
import com.simlect.api.enums.OrderCommentStatusEnum;
import com.simlect.api.enums.OrderFromTypeEnum;
import com.simlect.api.enums.OrderItemStatusEnum;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.api.enums.PayChannelEnum;
import com.simlect.api.enums.ProductStatusEnum;
import com.simlect.api.enums.RushingCouponStatusEnum;
import com.simlect.api.enums.UserCouponStatusEnum;
import com.simlect.api.support.CartFeignSupport;
import com.simlect.api.support.CouponFeignSupport;
import com.simlect.api.support.PayFeignSupport;
import com.simlect.api.support.ProductFeignSupport;
import com.simlect.api.support.StockFeignSupport;
import com.simlect.api.support.UserFeignSupport;
import com.simlect.api.vo.CouponBriefVO;
import com.simlect.api.vo.CouponLockResultVO;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.api.vo.OrderCountVO;
import com.simlect.api.vo.ProductInfoSnapshotVO;
import com.simlect.api.vo.ProductPropertyValueSnapshotVO;
import com.simlect.api.vo.ProductSkuSnapshotVO;
import com.simlect.api.vo.UserAddressVO;
import com.simlect.api.vo.UserCouponVO;
import com.simlect.component.RedisComponent;
import com.simlect.component.RemoteCompensateRecorder;
import com.simlect.constants.Constants;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.constants.TransactionalMqSender;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.dto.LogisticsSendDTO;
import com.simlect.entity.po.OrderCouponRel;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderItem;
import com.simlect.entity.po.OrderLogisticsInfo;
import com.simlect.entity.po.ProductItem;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.entity.query.OrderItemQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.exception.PayOrderLifecycleBusyException;
import com.simlect.mappers.OrderCouponRelMapper;
import com.simlect.mappers.OrderInfoMapper;
import com.simlect.mappers.OrderItemMapper;
import com.simlect.mappers.OrderLogisticsInfoMapper;
import com.simlect.state.OrderStateMachine;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderInfoServiceImplTest {

    private static final String USER_ID = "U10001";
    private static final String ORDER_ID = "O202608150001";
    private static final String PAY_ORDER_ID = "PO202608150001";

    @Mock
    private OrderInfoMapper<OrderInfo, OrderInfoQuery> orderInfoMapper;
    @Mock
    private StockFeignSupport stockFeignSupport;
    @Mock
    private ProductFeignSupport productFeignSupport;
    @Mock
    private UserFeignSupport userFeignSupport;
    @Mock
    private CouponFeignSupport couponFeignSupport;
    @Mock
    private OrderItemMapper<OrderItem, OrderItemQuery> orderItemMapper;
    @Mock
    private CartFeignSupport cartFeignSupport;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AppConfig appConfig;
    @Mock
    private OrderCouponRelMapper<OrderCouponRel, com.simlect.entity.query.OrderCouponRelQuery> orderCouponRelMapper;
    @Mock
    private OrderLogisticsInfoMapper<OrderLogisticsInfo, com.simlect.entity.query.OrderLogisticsInfoQuery> orderLogisticsInfoMapper;
    @Mock
    private PayFeignSupport payFeignSupport;
    @Mock
    private ReliableMessageSender reliableMessageSender;
    @Mock
    private TransactionalMqSender transactionalMqSender;
    @Mock
    private RemoteCompensateRecorder remoteCompensateRecorder;

    @InjectMocks
    private OrderInfoServiceImpl service;

    private OrderInfo waitPayOrder;
    private OrderInfo paidOrder;

    @BeforeEach
    void setUp() {
        OrderStateMachine orderStateMachine = new OrderStateMachine();
        ReflectionTestUtils.setField(orderStateMachine, "orderInfoMapper", orderInfoMapper);
        ReflectionTestUtils.setField(service, "orderStateMachine", orderStateMachine);
        when(appConfig.getOrderExpireMinute()).thenReturn(15);
        when(appConfig.getLogisticsSimulateMaxStations()).thenReturn(5);

        waitPayOrder = new OrderInfo();
        waitPayOrder.setOrderId(ORDER_ID);
        waitPayOrder.setUserId(USER_ID);
        waitPayOrder.setPayOrderId(PAY_ORDER_ID);
        waitPayOrder.setOrderStatus(OrderStatusEnum.WAIT_PAYMENT.getStatus());
        waitPayOrder.setPayScene("0");
        waitPayOrder.setPayChannel(PayChannelEnum.ALIPAY_PC.getPayChannel());
        waitPayOrder.setAmount(new BigDecimal("100.00"));
        waitPayOrder.setOrderTime(new Date());
        waitPayOrder.setCommentStatus(OrderCommentStatusEnum.NOT_EVALUATED.getStatus());

        paidOrder = new OrderInfo();
        paidOrder.setOrderId(ORDER_ID);
        paidOrder.setUserId(USER_ID);
        paidOrder.setPayOrderId(PAY_ORDER_ID);
        paidOrder.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        paidOrder.setPayScene("0");
        paidOrder.setAmount(new BigDecimal("100.00"));

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return null;
        }).when(redisComponent).runWithPayOrderLifecycleLock(anyString(), any(Runnable.class));
    }

    private OrderInfo order(OrderStatusEnum status) {
        OrderInfo o = new OrderInfo();
        o.setOrderId(ORDER_ID);
        o.setUserId(USER_ID);
        o.setPayOrderId(PAY_ORDER_ID);
        o.setOrderStatus(status.getStatus());
        o.setPayScene("0");
        o.setPayChannel(PayChannelEnum.ALIPAY_PC.getPayChannel());
        o.setAmount(new BigDecimal("100.00"));
        return o;
    }

    private PayOrderNotifyDTO notifyDto() {
        return new PayOrderNotifyDTO(PAY_ORDER_ID, "CH202608150001");
    }

    private OrderLogisticsInfo logistics() {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        logistics.setUserId(USER_ID);
        logistics.setReceiverName("ReceiverName");
        logistics.setReceiverPhone("13800138000");
        logistics.setReceiverAddress("Beijing Chaoyang");
        return logistics;
    }

    private OrderItem orderItem() {
        OrderItem item = new OrderItem();
        item.setOrderItemId(ORDER_ID + "_1");
        item.setOrderId(ORDER_ID);
        item.setProductId("P1");
        item.setPropertyValueIdHash("H1");
        item.setBuyCount(1);
        return item;
    }

    // ==================== paySuccess 正常支付成功 ====================

    @Test
    void paySuccess_happyPath_marksPaid_sendsLogistics_marksPaySuccess() {
        OrderInfo orderB = order(OrderStatusEnum.WAIT_PAYMENT);
        orderB.setOrderId("O202608150002");
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder, orderB));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(false);

        when(orderLogisticsInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(logistics());
        when(orderLogisticsInfoMapper.selectByOrderId("O202608150002")).thenReturn(null);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);

        service.paySuccess(notifyDto());

        verify(orderInfoMapper, times(2)).updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class));
        verify(transactionalMqSender, times(2)).sendAfterCommit(eq(RabbitMQConfig.PAY_EXCHANGE),
                eq(RabbitMQConfig.PAY_LOGISTICS_DELAY_KEY), any(PayOrderMessageDTO.class), anyString(),
                any(com.simlect.entity.enums.MessageReliabilityLevelEnum.class));
        verify(orderLogisticsInfoMapper).updateByParam(any(OrderLogisticsInfo.class), any());
        verify(orderLogisticsInfoMapper).insert(any(OrderLogisticsInfo.class));
        verify(payFeignSupport).markSuccess(PAY_ORDER_ID, "CH202608150001");
        assertEquals(OrderStatusEnum.PAID.getStatus(), waitPayOrder.getOrderStatus());
        assertEquals("CH202608150001", waitPayOrder.getChannelOrderId());
    }

    @Test
    void paySuccess_usesCoupon_whenRelLocked() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(false);
        when(orderLogisticsInfoMapper.selectByOrderId(anyString())).thenReturn(logistics());
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);

        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        rel.setUserCouponId("UC1");
        rel.setCouponId("C1");
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));

        service.paySuccess(notifyDto());

        verify(couponFeignSupport).changeUserCouponStatus(eq("UC1"), eq(USER_ID),
                eq(UserCouponStatusEnum.CANT.getStatus()), eq(UserCouponStatusEnum.USED.getStatus()), any(Date.class));
        verify(payFeignSupport).markSuccess(PAY_ORDER_ID, "CH202608150001");
    }

    @Test
    void paySuccess_couponAlreadyUsed_isIdempotent() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(false);
        when(orderLogisticsInfoMapper.selectByOrderId(anyString())).thenReturn(logistics());
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);

        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        rel.setUserCouponId("UC1");
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));
        doThrow(new BusinessException("coupon used"))
                .when(couponFeignSupport).changeUserCouponStatus(eq("UC1"), eq(USER_ID),
                        eq(UserCouponStatusEnum.CANT.getStatus()), eq(UserCouponStatusEnum.USED.getStatus()), any());
        UserCouponVO uc = new UserCouponVO();
        uc.setUserCouponId("UC1");
        uc.setUserId(USER_ID);
        uc.setStatus(UserCouponStatusEnum.USED.getStatus());
        when(couponFeignSupport.getUserCoupon("UC1")).thenReturn(uc);

        service.paySuccess(notifyDto());

        verify(payFeignSupport).markSuccess(PAY_ORDER_ID, "CH202608150001");
    }

    @Test
    void paySuccess_couponChangeFails_notUsed_throws() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(false);
        when(orderLogisticsInfoMapper.selectByOrderId(anyString())).thenReturn(logistics());
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);

        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        rel.setUserCouponId("UC1");
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));
        doThrow(new BusinessException("remote fail"))
                .when(couponFeignSupport).changeUserCouponStatus(eq("UC1"), eq(USER_ID),
                        eq(UserCouponStatusEnum.CANT.getStatus()), eq(UserCouponStatusEnum.USED.getStatus()), any());
        when(couponFeignSupport.getUserCoupon("UC1")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.paySuccess(notifyDto()));
        assertEquals("remote fail", ex.getMessage());
        verify(payFeignSupport, never()).markSuccess(anyString(), anyString());
    }

    // ==================== paySuccess 幂等 / 冲突 / 异常 ====================

    @Test
    void paySuccess_nullOrEmptyPayOrderId_throws() {
        assertThrows(BusinessException.class, () -> service.paySuccess(null));
        assertThrows(BusinessException.class, () -> service.paySuccess(new PayOrderNotifyDTO("", null)));
    }

    @Test
    void paySuccess_orderNotFound_throws() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.paySuccess(notifyDto()));
    }

    @Test
    void paySuccess_allPaid_idempotentSkip() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(paidOrder));
        service.paySuccess(notifyDto());
        verify(orderInfoMapper, never()).updateByParam(any(), any());
        verify(payFeignSupport, never()).markSuccess(anyString(), anyString());
    }

    @Test
    void paySuccess_closeMarked_refundLatePayment() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(true);
        when(redisComponent.tryMarkLatePaymentRefundOnce(PAY_ORDER_ID)).thenReturn(true);

        service.paySuccess(notifyDto());

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(payFeignSupport).refund(eq(PAY_ORDER_ID), startsWith("LATE"), amountCaptor.capture(),
                eq(PayChannelEnum.ALIPAY_PC.getPayScene()));
        assertEquals(new BigDecimal("100.00"), amountCaptor.getValue());
        verify(payFeignSupport).markRefunded(PAY_ORDER_ID);
        verify(orderInfoMapper, never()).updateByParam(any(), any());
    }

    @Test
    void paySuccess_orderClosed_refundLatePayment() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(order(OrderStatusEnum.CLOSED)));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(true);
        when(redisComponent.tryMarkLatePaymentRefundOnce(PAY_ORDER_ID)).thenReturn(true);

        service.paySuccess(notifyDto());

        verify(payFeignSupport).refund(eq(PAY_ORDER_ID), startsWith("LATE"), any(BigDecimal.class), anyString());
        verify(payFeignSupport).markRefunded(PAY_ORDER_ID);
    }

    @Test
    void paySuccess_refundIdempotent_alreadyMarked_skip() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(true);
        when(redisComponent.tryMarkLatePaymentRefundOnce(PAY_ORDER_ID)).thenReturn(false);

        service.paySuccess(notifyDto());

        verify(payFeignSupport, never()).refund(anyString(), anyString(), any(), anyString());
    }

    @Test
    void paySuccess_refundZeroAmount_skipRefund() {
        OrderInfo zero = order(OrderStatusEnum.CLOSED);
        zero.setAmount(null);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(zero));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(true);

        service.paySuccess(notifyDto());

        verify(payFeignSupport, never()).refund(anyString(), anyString(), any(), anyString());
    }

    @Test
    void paySuccess_refundFails_clearsMarkAndThrows() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(order(OrderStatusEnum.CLOSED)));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(true);
        when(redisComponent.tryMarkLatePaymentRefundOnce(PAY_ORDER_ID)).thenReturn(true);
        doThrow(new RuntimeException("channel refund fail")).when(payFeignSupport)
                .refund(anyString(), anyString(), any(BigDecimal.class), anyString());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.paySuccess(notifyDto()));
        assertTrue(ex.getMessage().contains("refund fail"));
        verify(redisComponent).clearLatePaymentRefundMark(PAY_ORDER_ID);
    }

    @Test
void paySuccess_partialOrdersUpdated_conflictHandled() {
when(orderInfoMapper.selectList(any(OrderInfoQuery.class)))
.thenReturn(List.of(waitPayOrder, order(OrderStatusEnum.WAIT_PAYMENT)));
when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(false);
when(orderLogisticsInfoMapper.selectByOrderId(anyString())).thenReturn(logistics());
when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class)))
.thenReturn(1, 0);
// 冲突处理内部重新加载：全部已支付 → 幂等跳过，不回滚整体
when(orderInfoMapper.selectList(any(OrderInfoQuery.class)))
.thenReturn(List.of(waitPayOrder, order(OrderStatusEnum.WAIT_PAYMENT)))
.thenReturn(List.of(order(OrderStatusEnum.PAID), order(OrderStatusEnum.PAID)));

// 部分子单失败不再抛异常回滚整体（避免回调无限重试）
service.paySuccess(notifyDto());

verify(payFeignSupport, never()).markSuccess(anyString(), anyString());
}

    @Test
    void paySuccess_noOrderUpdated_conflictPath_throws() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class)))
                .thenReturn(List.of(waitPayOrder), List.of(order(OrderStatusEnum.WAIT_PAYMENT)));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(false);
        when(orderLogisticsInfoMapper.selectByOrderId(anyString())).thenReturn(logistics());
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.paySuccess(notifyDto()));
        assertTrue(ex.getMessage().contains("订单状态异常"));
    }

    // ==================== 秒杀单支付成功 ====================

    @Test
    void paySuccess_couponRush_activatesCoupon() {
        OrderInfo rush = order(OrderStatusEnum.WAIT_PAYMENT);
        rush.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
        rush.setOrderId(ORDER_ID);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(rush));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(false);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        rel.setUserCouponId("UC1");
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));

        service.paySuccess(notifyDto());

        verify(couponFeignSupport).changeUserCouponStatus(eq("UC1"), eq(USER_ID),
                eq(UserCouponStatusEnum.CANT.getStatus()), eq(UserCouponStatusEnum.NOUSE.getStatus()), isNull());
        assertEquals(OrderStatusEnum.COMPLETED.getStatus(), rush.getOrderStatus());
        verify(payFeignSupport).markSuccess(PAY_ORDER_ID, "CH202608150001");
        verify(transactionalMqSender, never()).sendAfterCommit(anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void paySuccess_couponRush_allCompleted_idempotent() {
        OrderInfo rush = order(OrderStatusEnum.COMPLETED);
        rush.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(rush));

        service.paySuccess(notifyDto());

        verify(orderInfoMapper, never()).updateByParam(any(), any());
    }

    @Test
    void paySuccess_couponRush_closeMarked_refund() {
        OrderInfo rush = order(OrderStatusEnum.WAIT_PAYMENT);
        rush.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(rush));
        when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(true);
        when(redisComponent.tryMarkLatePaymentRefundOnce(PAY_ORDER_ID)).thenReturn(true);

        service.paySuccess(notifyDto());

        verify(payFeignSupport).refund(eq(PAY_ORDER_ID), startsWith("LATE"), any(BigDecimal.class), anyString());
    }

    @Test
void paySuccess_couponRush_partialUpdated_conflictHandled() {
OrderInfo rush = order(OrderStatusEnum.WAIT_PAYMENT);
rush.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
OrderInfo completed = order(OrderStatusEnum.COMPLETED);
completed.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(rush, rush));
when(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID)).thenReturn(false);
when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1, 0);
// 冲突处理重新加载：全部 COMPLETED → 幂等跳过
when(orderInfoMapper.selectList(any(OrderInfoQuery.class)))
.thenReturn(List.of(rush, rush))
.thenReturn(List.of(completed, completed));

// 部分失败走冲突处理，不抛异常回滚整体
service.paySuccess(notifyDto());

verify(payFeignSupport, never()).markSuccess(anyString(), anyString());
}

    // ==================== cancelOrder 用户取消 ====================

    @Test
    void cancelOrder_userCancel_succeeds() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(orderItem()));

        service.cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT);

        ArgumentCaptor<OrderInfo> updateCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).updateByParam(updateCaptor.capture(), any(OrderInfoQuery.class));
        assertEquals(OrderStatusEnum.CANCELLED.getStatus(), updateCaptor.getValue().getOrderStatus());
        verify(payFeignSupport).markClosed(PAY_ORDER_ID);
        verify(stockFeignSupport).changeStockBatchIdempotent(anyList(), eq("order-close-restock:" + PAY_ORDER_ID));
        verify(redisComponent).tryMarkPayOrderCloseOnce(PAY_ORDER_ID);
    }

    @Test
    void cancelOrder_userCancel_withChannelOrder_closesChannelOrder() {
        waitPayOrder.setChannelOrderId("CH001");
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(orderItem()));

        service.cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT);

        verify(payFeignSupport).closeOrder(PAY_ORDER_ID, PayChannelEnum.ALIPAY_PC.getPayScene());
    }

    @Test
    void cancelOrder_notExists_throws() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT));
    }

    @Test
    void cancelOrder_paid_cannotCancel() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(paidOrder);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT));
        assertEquals("当前订单已支付无法取消", ex.getMessage());
    }

    @Test
    void cancelOrder_otherUser_notExists() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        assertThrows(BusinessException.class,
                () -> service.cancelOrder("U99999", ORDER_ID, OrderStatusEnum.WAIT_PAYMENT));
    }

    @Test
    void cancelOrder_notWaitPayment_cannotCancel() {
        OrderInfo shipped = order(OrderStatusEnum.SHIPPED);
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(shipped);
        assertThrows(BusinessException.class,
                () -> service.cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT));
    }

    @Test
    void cancelOrder_conditionUpdateFails_throws() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(0);
        assertThrows(BusinessException.class,
                () -> service.cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT));
    }

    @Test
    void cancelOrder_restoreStock_fails_recordsCompensation() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(orderItem()));
        doThrow(new RuntimeException("stock service down")).when(stockFeignSupport)
                .changeStockBatchIdempotent(anyList(), eq("order-close-restock:" + PAY_ORDER_ID));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT));
        assertTrue(ex.getMessage().contains("库存回补失败"));
        verify(remoteCompensateRecorder).recordStockChangeBatch(
                eq(PAY_ORDER_ID), eq("order-close-restock:" + PAY_ORDER_ID), anyList(), any(Exception.class));
    }

    @Test
    void cancelOrder_userCancel_releasesCoupon_whenCant() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(orderItem()));
        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        rel.setUserCouponId("UC1");
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));
        UserCouponVO uc = new UserCouponVO();
        uc.setUserCouponId("UC1");
        uc.setUserId(USER_ID);
        uc.setStatus(UserCouponStatusEnum.CANT.getStatus());
        when(couponFeignSupport.getUserCoupon("UC1")).thenReturn(uc);

        service.cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT);

        verify(couponFeignSupport).changeUserCouponStatus(eq("UC1"), eq(USER_ID),
                eq(UserCouponStatusEnum.CANT.getStatus()), eq(UserCouponStatusEnum.NOUSE.getStatus()), isNull());
    }

    // ==================== cancelOrder 超时关单（userId=null） ====================

    @Test
    void cancelOrder_timeout_closeAllUnpaid() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(orderItem()));

        service.cancelOrder(null, ORDER_ID, OrderStatusEnum.CLOSED);

        ArgumentCaptor<OrderInfo> updateCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).updateByParam(updateCaptor.capture(), any(OrderInfoQuery.class));
        assertEquals(OrderStatusEnum.CLOSED.getStatus(), updateCaptor.getValue().getOrderStatus());
        verify(payFeignSupport).markClosed(PAY_ORDER_ID);
        verify(stockFeignSupport).changeStockBatchIdempotent(anyList(), eq("order-close-restock:" + PAY_ORDER_ID));
    }

    @Test
    void cancelOrder_timeout_closeIdempotent_redisGate() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(false);

        service.cancelOrder(null, ORDER_ID, OrderStatusEnum.CLOSED);

        verify(orderInfoMapper, never()).updateByParam(any(), any());
        verify(payFeignSupport, never()).markClosed(anyString());
    }

    @Test
    void cancelOrder_timeout_transactionRollback_clearsRedisCloseMark() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(orderItem()));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.cancelOrder(null, ORDER_ID, OrderStatusEnum.CLOSED);
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(redisComponent).clearPayOrderCloseMark(PAY_ORDER_ID);
    }

    @Test
    void cancelOrder_timeout_paidSubOrder_skipsAll() {
        OrderInfo paidSub = order(OrderStatusEnum.PAID);
        paidSub.setOrderId("O2");
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder, paidSub));

        service.cancelOrder(null, ORDER_ID, OrderStatusEnum.CLOSED);

        verify(orderInfoMapper, never()).updateByParam(any(), any());
        verify(payFeignSupport, never()).markClosed(anyString());
    }

    @Test
    void cancelOrder_timeout_noWaitSubOrders_skip() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(0);

        service.cancelOrder(null, ORDER_ID, OrderStatusEnum.CLOSED);

        verify(payFeignSupport, never()).markClosed(anyString());
    }

    // ==================== 秒杀单关单 ====================

    @Test
    void cancelOrder_couponRush_userCancel_releaseRushReserve() {
        OrderInfo rush = order(OrderStatusEnum.WAIT_PAYMENT);
        rush.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(rush);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        rel.setCouponId("C1");
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));

        service.cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT);

        ArgumentCaptor<OrderInfo> updateCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).updateByParam(updateCaptor.capture(), any(OrderInfoQuery.class));
        assertEquals(OrderStatusEnum.CANCELLED.getStatus(), updateCaptor.getValue().getOrderStatus());
        verify(couponFeignSupport).releaseRushCouponReserve("C1", USER_ID);
        verify(stockFeignSupport, never()).changeStockBatch(anyList());
    }

    @Test
    void cancelOrder_couponRush_timeout_closed() {
        OrderInfo rush = order(OrderStatusEnum.WAIT_PAYMENT);
        rush.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(rush);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of());

        service.cancelOrder(null, ORDER_ID, OrderStatusEnum.CLOSED);

        ArgumentCaptor<OrderInfo> updateCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).updateByParam(updateCaptor.capture(), any(OrderInfoQuery.class));
        assertEquals(OrderStatusEnum.CLOSED.getStatus(), updateCaptor.getValue().getOrderStatus());
    }

    // ==================== cancelUnpaidOrderForPayTimeout (MQ 入口) ====================

    @Test
    void cancelEntryPointsAreTransactional() throws Exception {
        assertNotNull(OrderInfoServiceImpl.class
                .getMethod("cancelOrder", String.class, String.class, OrderStatusEnum.class)
                .getAnnotation(Transactional.class));
        assertNotNull(OrderInfoServiceImpl.class
                .getMethod("cancelUnpaidOrderForPayTimeout", String.class)
                .getAnnotation(Transactional.class));
    }

    @Test
    void cancelUnpaidOrderForPayTimeout_notFound_returnsTrue() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(null);
        assertTrue(service.cancelUnpaidOrderForPayTimeout(ORDER_ID));
    }

    @Test
    void cancelUnpaidOrderForPayTimeout_notWaitPay_returnsTrue() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(paidOrder);
        assertTrue(service.cancelUnpaidOrderForPayTimeout(ORDER_ID));
    }

    @Test
    void cancelUnpaidOrderForPayTimeout_paidSubOrder_returnsTrue() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        OrderInfo paidSub = order(OrderStatusEnum.PAID);
        paidSub.setOrderId("O2");
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder, paidSub));
        assertTrue(service.cancelUnpaidOrderForPayTimeout(ORDER_ID));
        verify(orderInfoMapper, never()).updateByParam(any(), any());
    }

    @Test
    void cancelUnpaidOrderForPayTimeout_success_returnsTrue() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        when(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID)).thenReturn(true);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(orderItem()));

        assertTrue(service.cancelUnpaidOrderForPayTimeout(ORDER_ID));
        verify(payFeignSupport).markClosed(PAY_ORDER_ID);
    }

    @Test
    void cancelUnpaidOrderForPayTimeout_lockBusy_returnsFalse() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        doThrow(new PayOrderLifecycleBusyException()).when(redisComponent)
                .runWithPayOrderLifecycleLock(anyString(), any(Runnable.class));

        assertFalse(service.cancelUnpaidOrderForPayTimeout(ORDER_ID));
    }

    // ==================== prepareCouponRush ====================

    private DiscountCouponVO rushCoupon() {
        DiscountCouponVO vo = new DiscountCouponVO();
        vo.setCouponId("C1");
        vo.setCouponName("SeckillCoupon");
        vo.setRushingstatus(RushingCouponStatusEnum.YES.getStatus());
        vo.setRemainCount(10);
        vo.setTotalCount(100);
        return vo;
    }

    @Test
    void prepareCouponRush_success_createsOrder() {
        DiscountCouponVO coupon = rushCoupon();
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        when(couponFeignSupport.hasAvailableRushStock("C1")).thenReturn(true);
        when(redisComponent.rushingCoupon(eq("C1"), eq(USER_ID), anyString())).thenReturn(0);
        when(couponFeignSupport.deductStock("C1")).thenReturn(1);
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenReturn(1);
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(orderCouponRelMapper.insert(any(OrderCouponRel.class))).thenReturn(1);

        CouponRushPrepareDTO dto = service.prepareCouponRush(USER_ID, "C1");

        assertNotNull(dto);
        assertEquals("C1", dto.getCouponId());
        assertEquals("SeckillCoupon", dto.getCouponName());
        assertEquals(new BigDecimal(Constants.RUSHING_COUPON_PAY_AMOUNT), dto.getPayAmount());
        assertNotNull(dto.getUserCouponId());
        assertNotNull(dto.getOrderId());
        assertNotNull(dto.getPayOrderId());
        assertNotNull(dto.getPayExpireAt());
        verify(couponFeignSupport).createUserCoupon(any());
        verify(couponFeignSupport).invalidateCouponCache("C1");
        verify(transactionalMqSender).sendAfterCommit(eq(RabbitMQConfig.PAY_EXCHANGE),
                eq(RabbitMQConfig.PAY_TIMEOUT_DELAY_KEY), any(PayOrderMessageDTO.class), anyString(), any());
        verify(couponFeignSupport).syncRushStockFromDbIfRedisZero("C1");
    }

    @Test
    void prepareCouponRush_rushCode1_stockEmpty() {
        DiscountCouponVO coupon = rushCoupon();
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        when(couponFeignSupport.hasAvailableRushStock("C1")).thenReturn(true);
        when(redisComponent.rushingCoupon(eq("C1"), eq(USER_ID), anyString())).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertEquals("库存不足！", ex.getMessage());
        verify(couponFeignSupport).syncRushStockFromDbIfRedisZero("C1");
    }

    @Test
    void prepareCouponRush_rushCode2_duplicate() {
        DiscountCouponVO coupon = rushCoupon();
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        when(couponFeignSupport.hasAvailableRushStock("C1")).thenReturn(true);
        when(redisComponent.rushingCoupon(eq("C1"), eq(USER_ID), anyString())).thenReturn(2);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertEquals("不能重复下单！", ex.getMessage());
    }

    @Test
    void prepareCouponRush_rushCode3_network() {
        DiscountCouponVO coupon = rushCoupon();
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        when(couponFeignSupport.hasAvailableRushStock("C1")).thenReturn(true);
        when(redisComponent.rushingCoupon(eq("C1"), eq(USER_ID), anyString())).thenReturn(3);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertEquals("网络异常，请稍后再试~", ex.getMessage());
    }

    @Test
    void prepareCouponRush_deductZero_releaseReserve() {
        DiscountCouponVO coupon = rushCoupon();
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        when(couponFeignSupport.hasAvailableRushStock("C1")).thenReturn(true);
        when(redisComponent.rushingCoupon(eq("C1"), eq(USER_ID), anyString())).thenReturn(0);
        when(couponFeignSupport.deductStock("C1")).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertTrue(ex.getMessage().contains("库存不足或并发冲突"));
        verify(couponFeignSupport).releaseRushRedisReserve("C1", USER_ID);
    }

    @Test
    void prepareCouponRush_lockedCouponNoStock_releaseReserve() {
        DiscountCouponVO coupon = rushCoupon();
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        when(couponFeignSupport.hasAvailableRushStock("C1")).thenReturn(true);
        when(redisComponent.rushingCoupon(eq("C1"), eq(USER_ID), anyString())).thenReturn(0);
        DiscountCouponVO locked = rushCoupon();
        locked.setRemainCount(0);
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon, locked);

        assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        verify(couponFeignSupport).releaseRushRedisReserve("C1", USER_ID);
    }

    @Test
    void prepareCouponRush_notRushingCoupon_throws() {
        DiscountCouponVO coupon = rushCoupon();
        coupon.setRushingstatus(RushingCouponStatusEnum.NO.getStatus());
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertEquals("该优惠券不是抢购状态", ex.getMessage());
    }

    @Test
    void prepareCouponRush_couponNotFound_throws() {
        when(couponFeignSupport.getCoupon("C1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertEquals("优惠券不存在", ex.getMessage());
    }

    @Test
    void prepareCouponRush_beforeStartTime_throws() {
        DiscountCouponVO coupon = rushCoupon();
        coupon.setRushingStartTime(new Date(System.currentTimeMillis() + 60_000));
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertEquals("该优惠券未开始抢购", ex.getMessage());
    }

    @Test
    void prepareCouponRush_afterEndTime_throws() {
        DiscountCouponVO coupon = rushCoupon();
        coupon.setRushingEndTime(new Date(System.currentTimeMillis() - 60_000));
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertEquals("该优惠券已结束抢购", ex.getMessage());
    }

    @Test
    void prepareCouponRush_noRushStock_throws() {
        DiscountCouponVO coupon = rushCoupon();
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        when(couponFeignSupport.hasAvailableRushStock("C1")).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertEquals("库存不足！", ex.getMessage());
        verify(couponFeignSupport).syncRushStockFromDbIfRedisZero("C1");
    }

    @Test
    void prepareCouponRush_orderCreateFails_businessException_released() {
        DiscountCouponVO coupon = rushCoupon();
        when(couponFeignSupport.getCoupon("C1")).thenReturn(coupon);
        when(couponFeignSupport.hasAvailableRushStock("C1")).thenReturn(true);
        when(redisComponent.rushingCoupon(eq("C1"), eq(USER_ID), anyString())).thenReturn(0);
        when(couponFeignSupport.deductStock("C1")).thenReturn(1);
        doThrow(new RuntimeException("db down")).when(orderInfoMapper).insert(any(OrderInfo.class));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.prepareCouponRush(USER_ID, "C1"));
        assertEquals("下单失败，请稍后重试", ex.getMessage());
        verify(couponFeignSupport).releaseRushCouponReserve("C1", USER_ID);
    }

    // ==================== postCouponRushOrder ====================

    @Test
    void postCouponRushOrder_invalidPayMethod_throws() {
        assertThrows(BusinessException.class, () -> service.postCouponRushOrder(USER_ID, "C1", "wechat"));
    }

    @Test
    void postCouponRushOrder_noQualification_throws() {
        when(redisComponent.getRushUserCouponId(USER_ID, "C1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.postCouponRushOrder(USER_ID, "C1", "alipay_pc"));
        assertEquals("抢购资格已失效，请返回重新抢购", ex.getMessage());
    }

    @Test
    void postCouponRushOrder_orderNotExists_throws() {
        when(redisComponent.getRushUserCouponId(USER_ID, "C1")).thenReturn("UC1");
        when(couponFeignSupport.getCoupon("C1")).thenReturn(rushCoupon());
        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.postCouponRushOrder(USER_ID, "C1", "alipay_pc"));
    }

    @Test
    void postCouponRushOrder_closed_throws() {
        when(redisComponent.getRushUserCouponId(USER_ID, "C1")).thenReturn("UC1");
        when(couponFeignSupport.getCoupon("C1")).thenReturn(rushCoupon());
        OrderInfo rush = order(OrderStatusEnum.CLOSED);
        rush.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(rush);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.postCouponRushOrder(USER_ID, "C1", "alipay_pc"));
        assertEquals("订单已关闭，请重新抢购", ex.getMessage());
    }

    @Test
    void postCouponRushOrder_alreadyPaid_throws() {
        when(redisComponent.getRushUserCouponId(USER_ID, "C1")).thenReturn("UC1");
        when(couponFeignSupport.getCoupon("C1")).thenReturn(rushCoupon());
        OrderInfo rush = order(OrderStatusEnum.PAID);
        rush.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(rush);

        assertThrows(BusinessException.class, () -> service.postCouponRushOrder(USER_ID, "C1", "alipay_pc"));
    }

    @Test
    void postCouponRushOrder_success_returnsPayInfo() {
        when(redisComponent.getRushUserCouponId(USER_ID, "C1")).thenReturn("UC1");
        when(couponFeignSupport.getCoupon("C1")).thenReturn(rushCoupon());
        OrderInfo rush = order(OrderStatusEnum.WAIT_PAYMENT);
        rush.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
        rush.setSubject("SeckillSubject");
        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(rush);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        PayInfoDTO payInfo = new PayInfoDTO();
        payInfo.setPayOrderId(PAY_ORDER_ID);
        payInfo.setPayInfo("https://pay.example/alipay?x=1");
        when(payFeignSupport.getPayUrl(anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(payInfo);

        PayInfoDTO result = service.postCouponRushOrder(USER_ID, "C1", "alipay_pc");

        assertNotNull(result);
        assertEquals(ORDER_ID, result.getOrderId());
        verify(payFeignSupport).createPending(eq(USER_ID), eq(PAY_ORDER_ID), eq(ORDER_ID),
                eq(new BigDecimal("100.00")), eq("alipay_pc"));
        verify(orderInfoMapper).updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class));
    }

    // ==================== getPayInfo ====================

    @Test
    void getPayInfo_notExists_throws() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(null);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.getPayInfo(USER_ID, ORDER_ID));
    }

    @Test
    void getPayInfo_closed_throws() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(order(OrderStatusEnum.CLOSED));
        assertThrows(BusinessException.class, () -> service.getPayInfo(USER_ID, ORDER_ID));
    }

    @Test
    void getPayInfo_paid_throws() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(paidOrder);
        assertThrows(BusinessException.class, () -> service.getPayInfo(USER_ID, ORDER_ID));
    }

    @Test
    void getPayInfo_success_cancelsPreviousChannelOrder() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        PayInfoDTO payInfo = new PayInfoDTO();
        payInfo.setPayOrderId(PAY_ORDER_ID);
        when(payFeignSupport.getPayUrl(any(), any(), nullable(String.class), any(BigDecimal.class)))
                .thenReturn(payInfo);

        PayInfoDTO result = service.getPayInfo(USER_ID, ORDER_ID);

        assertNotNull(result);
        verify(payFeignSupport).closeOrder(PAY_ORDER_ID, PayChannelEnum.ALIPAY_PC.getPayScene());
        verify(orderInfoMapper).updateByOrderId(any(OrderInfo.class), eq(ORDER_ID));
    }

    @Test
    void getPayInfo_fallbackByPayOrderId_andPatchesNullChannel() {
        waitPayOrder.setPayChannel(null);
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(null);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(waitPayOrder));
        PayInfoDTO payInfo = new PayInfoDTO();
        payInfo.setPayOrderId(PAY_ORDER_ID);
        when(payFeignSupport.getPayUrl(any(), any(), nullable(String.class), any(BigDecimal.class)))
                .thenReturn(payInfo);

        PayInfoDTO result = service.getPayInfo(USER_ID, ORDER_ID);

        assertNotNull(result);
        verify(orderInfoMapper).updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class));
        assertEquals(PayChannelEnum.ALIPAY_PC.getPayScene(), waitPayOrder.getPayChannel());
    }

    // ==================== refund ====================

    private OrderInfo orderWithItems(int normalCount) {
        OrderInfo o = order(OrderStatusEnum.PAID);
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < normalCount; i++) {
            OrderItem item = new OrderItem();
            item.setOrderItemId(ORDER_ID + "_" + (i + 1));
            item.setOrderId(ORDER_ID);
            item.setProductId("P1");
            item.setPropertyValueIdHash("H1");
            item.setBuyCount(1);
            item.setItemAmount(new BigDecimal("50.00"));
            item.setOrderItemStatus(OrderItemStatusEnum.NORMAL.getStatus());
            items.add(item);
        }
        o.setOrderItemList(items);
        return o;
    }

    private OrderItem refundableItem() {
        OrderItem item = new OrderItem();
        item.setOrderId(ORDER_ID);
        item.setOrderItemId(ORDER_ID + "_1");
        item.setProductId("P1");
        item.setPropertyValueIdHash("H1");
        item.setBuyCount(1);
        item.setOrderItemStatus(OrderItemStatusEnum.NORMAL.getStatus());
        item.setItemAmount(new BigDecimal("50.00"));
        return item;
    }

    @Test
    void refund_nullItem_throws() {
        assertThrows(BusinessException.class, () -> service.refund(null, USER_ID));
    }

    @Test
    void refund_orderNotFound_throws() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.refund(new OrderItem(), USER_ID));
    }

    @Test
    void refund_wrongUser_throws() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(orderWithItems(1)));
        assertThrows(BusinessException.class, () -> service.refund(refundableItem(), "U99999"));
    }

    @Test
    void refund_notRefundableStatus_throws() {
        OrderInfo cancelled = orderWithItems(1);
        cancelled.setOrderStatus(OrderStatusEnum.CANCELLED.getStatus());
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(cancelled));
        assertThrows(BusinessException.class, () -> service.refund(refundableItem(), USER_ID));
    }

    @Test
    void refund_itemAlreadyRefunded_throws() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(orderWithItems(1)));
        OrderItem item = refundableItem();
        item.setOrderItemStatus(OrderItemStatusEnum.REFUND.getStatus());
        assertThrows(BusinessException.class, () -> service.refund(item, USER_ID));
    }

    @Test
    void refund_zeroAmount_throws() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(orderWithItems(1)));
        OrderItem item = refundableItem();
        item.setItemAmount(BigDecimal.ZERO);
        assertThrows(BusinessException.class, () -> service.refund(item, USER_ID));
    }

    @Test
    void refund_singleItem_refundsFullPaidAmount_andRestoresStockAfterPay() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(orderWithItems(1)));
        when(redisComponent.tryMarkOrderRefundOnce(anyString())).thenReturn(true);
        when(orderItemMapper.updateByParam(any(OrderItem.class), any(OrderItemQuery.class))).thenReturn(1);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        OrderItem item = refundableItem();

        service.refund(item, USER_ID);

        // 整单退款：退实付全额（不按原价）
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(payFeignSupport).refund(eq(PAY_ORDER_ID), anyString(), amountCaptor.capture(),
                eq(PayChannelEnum.ALIPAY_PC.getPayScene()));
        assertEquals(new BigDecimal("100.00"), amountCaptor.getValue());
        // 退款成功后才回补库存
        InOrder inOrder = inOrder(payFeignSupport, stockFeignSupport);
        inOrder.verify(payFeignSupport).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        inOrder.verify(stockFeignSupport).changeStockBatchIdempotent(
                anyList(), startsWith("order-refund-restock:"));
        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).updateByParam(orderCaptor.capture(), any(OrderInfoQuery.class));
        assertEquals(OrderStatusEnum.REFUNDED.getStatus(), orderCaptor.getValue().getOrderStatus());
        verify(redisComponent).tryMarkOrderRefundOnce(item.getOrderItemId());
    }

    @Test
    void refund_multiItems_apportionsByPaidAmount_andMarksPartialRefunded() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(orderWithItems(2)));
        when(redisComponent.tryMarkOrderRefundOnce(anyString())).thenReturn(true);
        when(orderItemMapper.updateByParam(any(OrderItem.class), any(OrderItemQuery.class))).thenReturn(1);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        OrderItem item = refundableItem();

        service.refund(item, USER_ID);

        // 100 实付 / 两件各 50：分摊 50.00（未用券时比例 1:1）
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(payFeignSupport).refund(eq(PAY_ORDER_ID), anyString(), amountCaptor.capture(),
                eq(PayChannelEnum.ALIPAY_PC.getPayScene()));
        assertEquals(new BigDecimal("50.00"), amountCaptor.getValue());
        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).updateByParam(orderCaptor.capture(), any(OrderInfoQuery.class));
        assertEquals(OrderStatusEnum.PARTIALLY_REFUNDED.getStatus(), orderCaptor.getValue().getOrderStatus());
    }

    @Test
    void refund_couponOrder_apportionsRefundBelowOriginalPrice() {
        // 商品 100 元、用券实付 0.01：部分退款最多退 0.01，防超退
        OrderInfo couponOrder = orderWithItems(1);
        couponOrder.setAmount(new BigDecimal("0.01"));
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(couponOrder));
        when(redisComponent.tryMarkOrderRefundOnce(anyString())).thenReturn(true);
        when(orderItemMapper.updateByParam(any(OrderItem.class), any(OrderItemQuery.class))).thenReturn(1);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);

        service.refund(refundableItem(), USER_ID);

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(payFeignSupport).refund(eq(PAY_ORDER_ID), anyString(), amountCaptor.capture(),
                eq(PayChannelEnum.ALIPAY_PC.getPayScene()));
        assertEquals(new BigDecimal("0.01"), amountCaptor.getValue());
    }

    @Test
    void refund_idempotencyGuard_blocksDuplicateSubmit() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(orderWithItems(1)));
        when(redisComponent.tryMarkOrderRefundOnce(anyString())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.refund(refundableItem(), USER_ID));

        assertTrue(ex.getMessage().contains("重复"));
        verify(payFeignSupport, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        verify(stockFeignSupport, never()).changeStockBatchIdempotent(anyList(), anyString());
    }


    @Test
    void refund_lastItem_afterPartialRefund_doesNotOverRefund() {
        // A(60)+B(60) 用券 20 实付 100：先退 A 分摊 50；再退 B（最后一项）只能退 50（paid-已退累计）
        OrderInfo order = orderWithItems(2);
        order.setAmount(new BigDecimal("100.00"));
        OrderItem itemA = order.getOrderItemList().get(0);
        OrderItem itemB = order.getOrderItemList().get(1);
        itemA.setOrderItemStatus(OrderItemStatusEnum.REFUND.getStatus());
        itemA.setRefundAmount(new BigDecimal("50.00"));  // A 已退 50
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(order));
        when(redisComponent.tryMarkOrderRefundOnce(anyString())).thenReturn(true);
        when(orderItemMapper.updateByParam(any(OrderItem.class), any(OrderItemQuery.class))).thenReturn(1);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);

        OrderItem itemBRefund = new OrderItem();
        itemBRefund.setOrderId(ORDER_ID);
        itemBRefund.setOrderItemId(itemB.getOrderItemId());
        itemBRefund.setProductId("P1");
        itemBRefund.setPropertyValueIdHash("H1");
        itemBRefund.setBuyCount(1);
        itemBRefund.setItemAmount(new BigDecimal("60.00"));
        itemBRefund.setOrderItemStatus(OrderItemStatusEnum.NORMAL.getStatus());
        service.refund(itemBRefund, USER_ID);

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(payFeignSupport).refund(eq(PAY_ORDER_ID), anyString(), amountCaptor.capture(),
                eq(PayChannelEnum.ALIPAY_PC.getPayScene()));
        // 最后一项只能退剩余 50，累计 100 不超实付
        assertEquals(new BigDecimal("50.00"), amountCaptor.getValue());
    }    @Test
    void refund_optimisticLockFails_throws_andClearsMark() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(orderWithItems(1)));
        when(redisComponent.tryMarkOrderRefundOnce(anyString())).thenReturn(true);
        when(orderItemMapper.updateByParam(any(OrderItem.class), any(OrderItemQuery.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.refund(refundableItem(), USER_ID));

        assertTrue(ex.getMessage().contains("订单项状态"));
        verify(payFeignSupport, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
    }

    @Test
    void refund_remoteRefundFails_propagates() {
        // 远程退款失败：异常向上传播（幂等标记由事务回滚回调清理，防重试二次退款）
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(orderWithItems(1)));
        when(redisComponent.tryMarkOrderRefundOnce(anyString())).thenReturn(true);
        when(orderItemMapper.updateByParam(any(OrderItem.class), any(OrderItemQuery.class))).thenReturn(1);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        doThrow(new BusinessException("远程退款失败"))
                .when(payFeignSupport).refund(anyString(), anyString(), any(BigDecimal.class), anyString());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.refund(refundableItem(), USER_ID));

        assertTrue(ex.getMessage().contains("远程退款失败"));
        verify(stockFeignSupport, never()).changeStockBatchIdempotent(anyList(), anyString());
    }

    @Test
    void refund_stockRollbackFails_recordsCompensation_andKeepsRefund() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(orderWithItems(1)));
        when(redisComponent.tryMarkOrderRefundOnce(anyString())).thenReturn(true);
        when(orderItemMapper.updateByParam(any(OrderItem.class), any(OrderItemQuery.class))).thenReturn(1);
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        doThrow(new BusinessException("stock service down")).when(stockFeignSupport)
                .changeStockBatchIdempotent(anyList(), startsWith("order-refund-restock:"));
        OrderItem item = refundableItem();

        // 不抛异常：退款已成功，库存回补失败仅记补偿（不撤销已成功的退款）
        service.refund(item, USER_ID);

        verify(payFeignSupport).refund(eq(PAY_ORDER_ID), anyString(), any(BigDecimal.class),
                eq(PayChannelEnum.ALIPAY_PC.getPayScene()));
        verify(remoteCompensateRecorder).recordStockChangeBatch(
                anyString(), startsWith("order-refund-restock:"), anyList(), any(Exception.class));
    }

    // ==================== 查询/列表/物流/评价关联 ====================

    @Test
    void findListByParam_queryUser_enrichesUserBrief() {
        OrderInfo o = order(OrderStatusEnum.PAID);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(o));
        OrderInfoQuery q = new OrderInfoQuery();
        q.setQueryUser(true);
        when(userFeignSupport.mapBriefByUserIds(anyList())).thenReturn(
                Map.of(USER_ID, new com.simlect.api.vo.UserBriefVO(USER_ID, "NickName", "a.png")));

        List<OrderInfo> list = service.findListByParam(q);

        assertEquals("NickName", list.get(0).getNickName());
        assertEquals("a.png", list.get(0).getAvatar());
    }

    @Test
    void findListByParam_empty_returnsEmpty() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of());
        OrderInfoQuery q = new OrderInfoQuery();
        q.setQueryUser(true);
        assertTrue(service.findListByParam(q).isEmpty());
    }

    @Test
    void findCountByParam_and_findListByPage() {
        when(orderInfoMapper.selectCount(any(OrderInfoQuery.class))).thenReturn(25);
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(order(OrderStatusEnum.PAID)));
        OrderInfoQuery q = new OrderInfoQuery();
        q.setPageNo(2);
        q.setPageSize(10);

        PaginationResultVO<OrderInfo> page = service.findListByPage(q);

        assertEquals(25, page.getTotalCount());
        assertEquals(10, page.getPageSize());
        assertEquals(3, page.getPageTotal());
        assertEquals(25, service.findCountByParam(q));
    }

    @Test
    void crudHonorsStateMachineBoundaries() {
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenReturn(1);
        when(orderInfoMapper.insertBatch(anyList())).thenReturn(2, 1);
        when(orderInfoMapper.updateByParam(any(), any())).thenReturn(1);
        when(orderInfoMapper.updateByOrderId(any(), anyString())).thenReturn(1);
        OrderInfo completed = order(OrderStatusEnum.COMPLETED);
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(completed);
        when(orderInfoMapper.selectByOrderId("NEW_ORDER")).thenReturn(null);

        OrderInfo single = new OrderInfo();
        assertEquals(1, service.add(single));
        assertEquals(OrderStatusEnum.WAIT_PAYMENT.getStatus(), single.getOrderStatus());
        OrderInfo first = new OrderInfo();
        OrderInfo second = new OrderInfo();
        assertEquals(2, service.addBatch(List.of(first, second)));
        assertEquals(OrderStatusEnum.WAIT_PAYMENT.getStatus(), first.getOrderStatus());
        assertEquals(OrderStatusEnum.WAIT_PAYMENT.getStatus(), second.getOrderStatus());
        assertEquals(0, service.addBatch(List.of()));
        OrderInfo newOrder = new OrderInfo();
        newOrder.setOrderId("NEW_ORDER");
        assertEquals(1, service.addOrUpdateBatch(List.of(newOrder)));
        assertEquals(OrderStatusEnum.WAIT_PAYMENT.getStatus(), newOrder.getOrderStatus());
        assertThrows(BusinessException.class,
                () -> service.addOrUpdateBatch(List.of(order(OrderStatusEnum.PAID))));
        assertEquals(0, service.addOrUpdateBatch(null));
        OrderInfoQuery updateQuery = new OrderInfoQuery();
        updateQuery.setOrderId(ORDER_ID);
        assertEquals(1, service.updateByParam(new OrderInfo(), updateQuery));
        OrderInfoQuery deleteQuery = new OrderInfoQuery();
        deleteQuery.setOrderId(ORDER_ID);
        assertThrows(BusinessException.class, () -> service.deleteByParam(deleteQuery));
        assertEquals(completed, service.getOrderInfoByOrderId(ORDER_ID));
        assertEquals(1, service.updateOrderInfoByOrderId(new OrderInfo(), ORDER_ID));
        assertEquals(1, service.deleteOrderInfoByOrderId(ORDER_ID));
        verify(orderInfoMapper, never()).deleteByParam(any());
        verify(orderInfoMapper, never()).deleteByOrderId(anyString());
        verify(orderInfoMapper, never()).insertOrUpdateBatch(anyList());
    }

    @Test
    void addAllOrderToDelayQueue_sendsPerOrder() {
        service.addAllOrderToDelayQueue(List.of(order(OrderStatusEnum.WAIT_PAYMENT), order(OrderStatusEnum.WAIT_PAYMENT)));
        verify(transactionalMqSender, times(2)).sendAfterCommit(eq(RabbitMQConfig.PAY_EXCHANGE),
                eq(RabbitMQConfig.PAY_TIMEOUT_DELAY_KEY), any(PayOrderMessageDTO.class), anyString(), any());
    }

    @Test
    void getOrderCountInfo_countsByStatus() {
        OrderInfo o1 = order(OrderStatusEnum.WAIT_PAYMENT);
        o1.setOrderId("O1");
        OrderInfo o2 = order(OrderStatusEnum.PAID);
        o2.setOrderId("O2");
        OrderInfo o3 = order(OrderStatusEnum.SHIPPED);
        o3.setOrderId("O3");
        OrderInfo o4 = order(OrderStatusEnum.COMPLETED);
        o4.setOrderId("O4");
        o4.setCommentStatus(OrderCommentStatusEnum.NOT_EVALUATED.getStatus());
        OrderInfo o5 = order(OrderStatusEnum.COMPLETED);
        o5.setOrderId("O5");
        o5.setCommentStatus(OrderCommentStatusEnum.EVALUATED.getStatus());
        OrderInfo o6 = order(OrderStatusEnum.COMPLETED);
        o6.setOrderId("O6");
        o6.setPayScene("2");
        o6.setCommentStatus(OrderCommentStatusEnum.NOT_EVALUATED.getStatus());
        OrderInfo o7 = order(OrderStatusEnum.DELETE);
        o7.setOrderId("O7");
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class)))
                .thenReturn(List.of(o1, o2, o3, o4, o5, o6, o7));

        List<OrderCountVO> counts = service.getOrderCountInfo(USER_ID);

        assertEquals(5, counts.size());
        assertEquals(1, counts.get(0).getCount()); // pendingPayment
        assertEquals(1, counts.get(1).getCount()); // pendingShipment
        assertEquals(1, counts.get(2).getCount()); // pendingReceipt
        assertEquals(1, counts.get(3).getCount()); // pendingComment
        assertEquals(3, counts.get(4).getCount()); // completed
    }

    @Test
    void confirmOrderReceipt_emptyOrderId_throws() {
        assertThrows(BusinessException.class, () -> service.confirmOrderReceipt(USER_ID, ""));
    }

    @Test
    void confirmOrderReceipt_notExists_throws() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.confirmOrderReceipt(USER_ID, ORDER_ID));
    }

    @Test
    void confirmOrderReceipt_wrongUser_throws() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(order(OrderStatusEnum.SHIPPED));
        assertThrows(BusinessException.class, () -> service.confirmOrderReceipt("U99999", ORDER_ID));
    }

    @Test
    void confirmOrderReceipt_completed_returnsFalse() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(order(OrderStatusEnum.COMPLETED));
        assertFalse(service.confirmOrderReceipt(USER_ID, ORDER_ID));
    }

    @Test
    void confirmOrderReceipt_updateFails_returnsFalse() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(order(OrderStatusEnum.SHIPPED));
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(0);
        assertFalse(service.confirmOrderReceipt(USER_ID, ORDER_ID));
    }

    @Test
    void confirmOrderReceipt_success_increasesSales() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(order(OrderStatusEnum.SHIPPED));
        when(orderInfoMapper.updateByParam(any(OrderInfo.class), any(OrderInfoQuery.class))).thenReturn(1);
        OrderItem item = orderItem();
        item.setBuyCount(3);
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(item));

        assertTrue(service.confirmOrderReceipt(USER_ID, ORDER_ID));

        verify(productFeignSupport).increaseSales("P1", 3);
    }

    @Test
    void onOrderConfirmed_notEvaluated_sendsReceiptNotify() {
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(waitPayOrder);
        service.onOrderConfirmed(USER_ID, ORDER_ID);
        verify(userFeignSupport).addGrowthOnPay(USER_ID, new BigDecimal("100.00"));
        verify(userFeignSupport).sendNotifyAsync(eq(USER_ID), eq("确认收货成功"), anyString(), eq("order"), eq(ORDER_ID));
    }

    @Test
    void onOrderConfirmed_evaluated_sendsReCommentNotify() {
        OrderInfo evaluated = order(OrderStatusEnum.COMPLETED);
        evaluated.setCommentStatus(OrderCommentStatusEnum.EVALUATED.getStatus());
        when(orderInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(evaluated);
        service.onOrderConfirmed(USER_ID, ORDER_ID);
        verify(userFeignSupport).sendNotifyAsync(eq(USER_ID), eq("追评提醒"), anyString(), eq("comment_re"), eq(ORDER_ID));
    }

    @Test
    void enrichCouponInfo_couponRush_setsOriginalAmount() {
        OrderInfo rush = order(OrderStatusEnum.WAIT_PAYMENT);
        rush.setPayScene("2");
        rush.setAmount(new BigDecimal("0.01"));
        service.enrichCouponInfo(rush);
        assertEquals(new BigDecimal("0.01"), rush.getOriginalAmount());
        assertEquals(0, rush.getCouponDiscountAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void enrichCouponInfo_normalWithDiscount_setsCouponName() {
        OrderInfo o = order(OrderStatusEnum.PAID);
        OrderItem item = orderItem();
        item.setOrderItemStatus(OrderItemStatusEnum.NORMAL.getStatus());
        item.setItemAmount(new BigDecimal("100.00"));
        o.setOrderItemList(List.of(item));
        o.setAmount(new BigDecimal("90.00"));
        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        rel.setCouponId("C1");
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));
        CouponBriefVO brief = new CouponBriefVO();
        brief.setCouponName("满减券");
        brief.setCouponType(1);
        when(couponFeignSupport.getCouponBrief("C1")).thenReturn(brief);

        service.enrichCouponInfo(o);

        assertEquals(new BigDecimal("100.00"), o.getOriginalAmount());
        assertEquals(new BigDecimal("10.00"), o.getCouponDiscountAmount());
        assertEquals("满减券", o.getCouponName());
    }

    @Test
    void enrichCouponInfo_noDiscount_skipCoupon() {
        OrderInfo o = order(OrderStatusEnum.PAID);
        OrderItem item = orderItem();
        item.setOrderItemStatus(OrderItemStatusEnum.NORMAL.getStatus());
        item.setItemAmount(new BigDecimal("100.00"));
        o.setOrderItemList(List.of(item));
        o.setAmount(new BigDecimal("100.00"));

        service.enrichCouponInfo(o);

        assertEquals(0, o.getCouponDiscountAmount().compareTo(BigDecimal.ZERO));
        verify(couponFeignSupport, never()).getCouponBrief(anyString());
    }

    @Test
    void syncPaidCouponRushUserCoupons_activatesCantCoupons() {
        OrderInfo o = order(OrderStatusEnum.PAID);
        o.setPayScene("2");
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of(o));
        OrderCouponRel rel = new OrderCouponRel();
        rel.setOrderId(ORDER_ID);
        rel.setUserCouponId("UC1");
        when(orderCouponRelMapper.selectList(any())).thenReturn(List.of(rel));
        UserCouponVO uc = new UserCouponVO();
        uc.setUserCouponId("UC1");
        uc.setStatus(UserCouponStatusEnum.CANT.getStatus());
        when(couponFeignSupport.getUserCoupon("UC1")).thenReturn(uc);

        service.syncPaidCouponRushUserCoupons(USER_ID);

        verify(couponFeignSupport).changeUserCouponStatus(eq("UC1"), eq(USER_ID),
                eq(UserCouponStatusEnum.CANT.getStatus()), eq(UserCouponStatusEnum.NOUSE.getStatus()), isNull());
    }

    @Test
    void syncPaidCouponRushUserCoupons_noOrders_skip() {
        when(orderInfoMapper.selectList(any(OrderInfoQuery.class))).thenReturn(List.of());
        service.syncPaidCouponRushUserCoupons(USER_ID);
        verify(couponFeignSupport, never()).getUserCoupon(anyString());
    }

    @Test
    void findByProductNameFuzzy_filtersByItemName() {
        OrderInfo o1 = order(OrderStatusEnum.PAID);
        OrderItem i1 = new OrderItem();
        i1.setProductName("Apple");
        o1.setOrderItemList(List.of(i1));
        OrderInfo o2 = order(OrderStatusEnum.PAID);
        OrderItem i2 = new OrderItem();
        i2.setProductName("Banana");
        o2.setOrderItemList(List.of(i2));
        PaginationResultVO<OrderInfo> result = new PaginationResultVO<>();
        result.setList(List.of(o1, o2));

        PaginationResultVO<OrderInfo> filtered = service.findByProductNameFuzzy(result, "Apple");

        assertEquals(1, filtered.getList().size());
        assertEquals("Apple", filtered.getList().get(0).getOrderItemList().get(0).getProductName());
    }

    // ==================== postOrder 普通下单 ====================

    private void stubPostOrderBasics(PostOrderDTO dto) {
        UserAddressVO address = new UserAddressVO();
        address.setAddressId("A1");
        address.setUserId(USER_ID);
        address.setAddress("Beijing Chaoyang Road 1");
        address.setAddressee("ZhangSan");
        address.setPhone("13800138000");
        when(userFeignSupport.getAddress(anyString(), anyString())).thenReturn(address);

        ProductItem p = dto.getOrderList().get(0);
        ProductInfoSnapshotVO info = new ProductInfoSnapshotVO();
        info.setProductId(p.getProductId());
        info.setProductName("TestProduct");
        info.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        info.setCover("cover.jpg");
        when(productFeignSupport.toProductInfoMap(any())).thenReturn(Map.of(p.getProductId(), info));

        ProductPropertyValueSnapshotVO pv = new ProductPropertyValueSnapshotVO();
        pv.setProductId(p.getProductId());
        pv.setPropertyValueId("pv1");
        pv.setPropertyName("Color");
        pv.setPropertyValue("Red");
        pv.setPropertyCover("pv_cover.jpg");
        when(productFeignSupport.toPropertyValueMap(any()))
                .thenReturn(Map.of(p.getProductId() + "pv1", pv));

        ProductSkuSnapshotVO sku = new ProductSkuSnapshotVO();
        sku.setProductId(p.getProductId());
        sku.setPropertyValueIdHash("H1");
        sku.setPropertyValueIds("pv1");
        sku.setPrice(new BigDecimal("100.00"));
        when(productFeignSupport.toSkuMapByPropertyValueIds(any()))
                .thenReturn(Map.of(p.getProductId() + "pv1", sku));

        when(stockFeignSupport.getAvailable(anyString(), anyString())).thenReturn(100);
        LogisticsSendDTO send = new LogisticsSendDTO();
        send.setSenderName("SenderName");
        send.setSenderPhone("13900139000");
        send.setSenderAddress("Shanghai Pudong");
        when(redisComponent.getLogisticsInfo()).thenReturn(send);
    }

    private PostOrderDTO postOrderDTO() {
        PostOrderDTO dto = new PostOrderDTO();
        dto.setPayMethod("alipay_pc");
        dto.setAddressId("A1");
        dto.setOrderFrom(OrderFromTypeEnum.PRODUCT.getType());
        ProductItem p = new ProductItem();
        p.setProductId("P1");
        p.setPropertyValueIds("pv1");
        p.setBuyCount(2);
        dto.setOrderList(List.of(p));
        return dto;
    }

    @Test
    void postOrder_happyPath_withCoupon() {
        PostOrderDTO dto = postOrderDTO();
        dto.setUserCouponId("UC1");
        stubPostOrderBasics(dto);
        CouponLockResultVO lockResult = new CouponLockResultVO();
        lockResult.setLocked(true);
        lockResult.setDiscountAmount(new BigDecimal("5.00"));
        lockResult.setCouponId("C1");
        when(couponFeignSupport.validateAndLock(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(lockResult);
        when(orderInfoMapper.insertBatch(anyList())).thenReturn(1);
        when(orderItemMapper.insertBatch(anyList())).thenReturn(1);
        when(orderLogisticsInfoMapper.insert(any(OrderLogisticsInfo.class))).thenReturn(1);
        when(stockFeignSupport.changeStockBatchIdempotent(
                anyList(), startsWith("order-create-deduct:"))).thenReturn(1);
        PayInfoDTO payInfo = new PayInfoDTO();
        payInfo.setPayOrderId("PO-NEW");
        when(payFeignSupport.getPayUrl(anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(payInfo);

        PayInfoDTO result = service.postOrder(USER_ID, dto);

        assertNotNull(result);
        // 商品页下单不走购物车清理
        verify(cartFeignSupport, never()).deleteBatch(anyList());
        verify(orderInfoMapper).insertBatch(anyList());
        verify(orderItemMapper).insertBatch(anyList());
        verify(orderLogisticsInfoMapper).insert(any(OrderLogisticsInfo.class));
        verify(orderCouponRelMapper).insert(any(OrderCouponRel.class));
        // 优惠后金额 100*2 - 5 = 195
        ArgumentCaptor<List<OrderInfo>> ordersCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderInfoMapper).insertBatch(ordersCaptor.capture());
        assertEquals(new BigDecimal("195.00"), ordersCaptor.getValue().get(0).getAmount());
        verify(payFeignSupport).getPayUrl(eq("alipay_pc"), anyString(), anyString(), eq(new BigDecimal("195.00")));
        verify(payFeignSupport).createPending(eq(USER_ID), anyString(), anyString(),
                eq(new BigDecimal("195.00")), eq("alipay_pc"));
        verify(transactionalMqSender).sendAfterCommit(eq(RabbitMQConfig.PAY_EXCHANGE),
                eq(RabbitMQConfig.PAY_TIMEOUT_DELAY_KEY), any(PayOrderMessageDTO.class), anyString(), any());
    }

    @Test
    void postOrder_cartOrder_clearsCartAndSetsSubject() {
        PostOrderDTO dto = postOrderDTO();
        dto.setOrderFrom(OrderFromTypeEnum.CART.getType());
        stubPostOrderBasics(dto);
        when(orderInfoMapper.insertBatch(anyList())).thenReturn(1);
        when(orderItemMapper.insertBatch(anyList())).thenReturn(1);
        when(orderLogisticsInfoMapper.insert(any(OrderLogisticsInfo.class))).thenReturn(1);
        when(stockFeignSupport.changeStockBatchIdempotent(
                anyList(), startsWith("order-create-deduct:"))).thenReturn(1);
        PayInfoDTO payInfo = new PayInfoDTO();
        payInfo.setPayOrderId("PO-NEW");
        when(payFeignSupport.getPayUrl(anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(payInfo);

        service.postOrder(USER_ID, dto);

        verify(cartFeignSupport).deleteBatch(anyList());
        ArgumentCaptor<List<OrderInfo>> ordersCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderInfoMapper).insertBatch(ordersCaptor.capture());
        assertNotNull(ordersCaptor.getValue().get(0).getSubject());
    }

    @Test
    void postOrder_invalidPayMethod_throws() {
        PostOrderDTO dto = postOrderDTO();
        dto.setPayMethod("wechat");
        assertThrows(BusinessException.class, () -> service.postOrder(USER_ID, dto));
    }

    @Test
    void postOrder_invalidOrderFrom_throws() {
        PostOrderDTO dto = postOrderDTO();
        dto.setOrderFrom(99);
        assertThrows(BusinessException.class, () -> service.postOrder(USER_ID, dto));
    }

    @Test
    void postOrder_addressMissing_throws() {
        PostOrderDTO dto = postOrderDTO();
        when(userFeignSupport.getAddress(anyString(), anyString())).thenReturn(new UserAddressVO());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.postOrder(USER_ID, dto));
        assertTrue(ex.getMessage().contains("地址"));
    }

    @Test
    void postOrder_productOffShelf_throws() {
        PostOrderDTO dto = postOrderDTO();
        stubPostOrderBasics(dto);
        ProductInfoSnapshotVO info = new ProductInfoSnapshotVO();
        info.setProductId("P1");
        info.setProductName("OffShelfProduct");
        info.setStatus(ProductStatusEnum.OFF_SALE.getStatus());
        when(productFeignSupport.toProductInfoMap(any())).thenReturn(Map.of("P1", info));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.postOrder(USER_ID, dto));
        assertEquals("商品不存在或已下架", ex.getMessage());
    }

    @Test
    void postOrder_stockInsufficient_throws() {
        PostOrderDTO dto = postOrderDTO();
        stubPostOrderBasics(dto);
        when(stockFeignSupport.getAvailable(anyString(), anyString())).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.postOrder(USER_ID, dto));
        assertTrue(ex.getMessage().contains("库存不足"));
    }

    @Test
    void postOrder_buyCountZero_throws() {
        PostOrderDTO dto = postOrderDTO();
        dto.getOrderList().get(0).setBuyCount(0);
        stubPostOrderBasics(dto);
        assertThrows(BusinessException.class, () -> service.postOrder(USER_ID, dto));
    }

    @Test
    void postOrder_emptyOrderList_throws() {
        PostOrderDTO dto = postOrderDTO();
        dto.setOrderList(List.of());
        dto.setOrderFrom(OrderFromTypeEnum.CART.getType());
        UserAddressVO address = new UserAddressVO();
        address.setAddress("Beijing");
        address.setAddressee("ZhangSan");
        address.setPhone("13800138000");
        when(userFeignSupport.getAddress(anyString(), anyString())).thenReturn(address);
        assertThrows(BusinessException.class, () -> service.postOrder(USER_ID, dto));
    }

    @Test
    void postOrder_failAfterStockDeducted_compensatesStockAndCoupon() {
        PostOrderDTO dto = postOrderDTO();
        dto.setUserCouponId("UC1");
        stubPostOrderBasics(dto);
        CouponLockResultVO lockResult = new CouponLockResultVO();
        lockResult.setLocked(true);
        lockResult.setDiscountAmount(new BigDecimal("5.00"));
        lockResult.setCouponId("C1");
        when(couponFeignSupport.validateAndLock(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(lockResult);
        when(orderInfoMapper.insertBatch(anyList())).thenReturn(1);
        when(orderItemMapper.insertBatch(anyList())).thenReturn(1);
        when(orderLogisticsInfoMapper.insert(any(OrderLogisticsInfo.class))).thenReturn(1);
        when(stockFeignSupport.changeStockBatchIdempotent(
                anyList(), startsWith("order-create-deduct:"))).thenReturn(1);
        doThrow(new RuntimeException("create pending fail")).when(payFeignSupport).createPending(anyString(), anyString(), anyString(), any(), anyString());

        assertThrows(RuntimeException.class, () -> service.postOrder(USER_ID, dto));

        // 库存扣减（正数）后失败 → 回补（负数）
        verify(stockFeignSupport).changeStockBatchIdempotent(
                anyList(), startsWith("order-create-deduct:"));
        verify(stockFeignSupport).changeStockBatchIdempotent(
                anyList(), startsWith("order-create-compensate:"), startsWith("order-create-deduct:"));
        verify(couponFeignSupport).changeUserCouponStatus(eq("UC1"), eq(USER_ID),
                eq(UserCouponStatusEnum.CANT.getStatus()), eq(UserCouponStatusEnum.NOUSE.getStatus()), isNull());
    }

    @Test
    void postOrder_compensateFails_recordsCompensation() {
        PostOrderDTO dto = postOrderDTO();
        dto.setUserCouponId("UC1");
        stubPostOrderBasics(dto);
        CouponLockResultVO lockResult = new CouponLockResultVO();
        lockResult.setLocked(true);
        lockResult.setDiscountAmount(new BigDecimal("5.00"));
        lockResult.setCouponId("C1");
        when(couponFeignSupport.validateAndLock(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(lockResult);
        when(orderInfoMapper.insertBatch(anyList())).thenReturn(1);
        when(orderItemMapper.insertBatch(anyList())).thenReturn(1);
        when(orderLogisticsInfoMapper.insert(any(OrderLogisticsInfo.class))).thenReturn(1);
        when(stockFeignSupport.changeStockBatchIdempotent(
                anyList(), startsWith("order-create-deduct:"))).thenReturn(1);
        when(stockFeignSupport.changeStockBatchIdempotent(
                anyList(), startsWith("order-create-compensate:"), startsWith("order-create-deduct:")))
                .thenThrow(new RuntimeException("compensate also fail"));
        doThrow(new RuntimeException("create pending fail")).when(payFeignSupport).createPending(anyString(), anyString(), anyString(), any(), anyString());
        doThrow(new RuntimeException("coupon unlock fail")).when(couponFeignSupport).changeUserCouponStatus(anyString(), anyString(),
                any(), any(), any());

        assertThrows(RuntimeException.class, () -> service.postOrder(USER_ID, dto));

        verify(remoteCompensateRecorder).recordStockChangeBatch(
                anyString(), startsWith("order-create-compensate:"), startsWith("order-create-deduct:"),
                anyList(), any(Exception.class));
        verify(remoteCompensateRecorder).recordCouponUnlock(anyString(), eq("UC1"), eq(USER_ID),
                eq(UserCouponStatusEnum.CANT.getStatus()), eq(UserCouponStatusEnum.NOUSE.getStatus()), any(Exception.class));
    }
}
