package com.simlect.component;

import com.simlect.api.dto.RushingCouponMessageDTO;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.api.enums.UserCouponStatusEnum;
import com.simlect.api.support.OrderFeignSupport;
import com.simlect.api.vo.OrderBriefVO;
import com.simlect.biz.DiscountCouponService;
import com.simlect.biz.UserCouponService;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.entity.po.UserCoupon;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RabbitMQOrderDeadListListenerComponent 秒杀死信兜底消费单元测试。
 */
@ExtendWith(MockitoExtension.class)
class RabbitMQOrderDeadListListenerComponentTest {

    @Mock
    private DiscountCouponService discountCouponService;
    @Mock
    private UserCouponService userCouponService;
    @Mock
    private OrderFeignSupport orderFeignSupport;
    @Mock
    private MqListenerHelper mqListenerHelper;

    @InjectMocks
    private RabbitMQOrderDeadListListenerComponent listener;

    private static final String USER_COUPON_ID = "UC1";
    private static final String USER_ID = "U1";
    private static final String COUPON_ID = "CP1";
    private static final String ORDER_ID = "O1";
    private static final long DELIVERY_TAG = 42L;

    private RushingCouponMessageDTO message(String orderId) {
        RushingCouponMessageDTO message = new RushingCouponMessageDTO();
        message.setUserCouponId(USER_COUPON_ID);
        message.setUserId(USER_ID);
        message.setCouponId(COUPON_ID);
        message.setOrderId(orderId);
        return message;
    }

    private Message rabbitMessage() {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(DELIVERY_TAG);
        return new Message(new byte[]{1}, props);
    }

    private void mockConsumeAllowed() {
        when(mqListenerHelper.tryBeginConsume(any(), eq(MqListenerHelper.CONSUME_IDEMPOTENCY_TTL_STANDARD_SECONDS)))
                .thenReturn(true);
    }

    private OrderBriefVO order(int status) {
        OrderBriefVO vo = new OrderBriefVO();
        vo.setOrderStatus(status);
        return vo;
    }

    @Test
    void handleDeadOrder_idempotencyRejected_acksOnly() throws Exception {
        Channel channel = mock(Channel.class);
        when(mqListenerHelper.tryBeginConsume(any(), eq(MqListenerHelper.CONSUME_IDEMPOTENCY_TTL_STANDARD_SECONDS)))
                .thenReturn(false);

        listener.handleDeadOrder(message(ORDER_ID), channel, rabbitMessage());

        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(userCouponService, never()).getUserCouponByUserCouponId(anyString());
    }

    @Test
    void handleDeadOrder_noOrderNoCoupon_releasesReserveAndAcks() throws Exception {
        mockConsumeAllowed();
        Channel channel = mock(Channel.class);
        when(userCouponService.getUserCouponByUserCouponId(USER_COUPON_ID)).thenReturn(null);

        listener.handleDeadOrder(message(null), channel, rabbitMessage());

        verify(discountCouponService).releaseRushCouponReserve(COUPON_ID, USER_ID);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void handleDeadOrder_orderMissingUserCouponMissing_rollsBackRedisOnly() throws Exception {
        mockConsumeAllowed();
        Channel channel = mock(Channel.class);
        when(userCouponService.getUserCouponByUserCouponId(USER_COUPON_ID)).thenReturn(null);
        when(orderFeignSupport.getOrder(ORDER_ID)).thenReturn(null);

        listener.handleDeadOrder(message(ORDER_ID), channel, rabbitMessage());

        verify(discountCouponService).releaseRushRedisReserve(COUPON_ID, USER_ID);
        verify(discountCouponService, never()).releaseRushCouponReserve(anyString(), anyString());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void handleDeadOrder_waitPayment_cancelsOrder() throws Exception {
        mockConsumeAllowed();
        Channel channel = mock(Channel.class);
        when(userCouponService.getUserCouponByUserCouponId(USER_COUPON_ID)).thenReturn(new UserCoupon());
        when(orderFeignSupport.getOrder(ORDER_ID)).thenReturn(order(OrderStatusEnum.WAIT_PAYMENT.getStatus()));

        listener.handleDeadOrder(message(ORDER_ID), channel, rabbitMessage());

        verify(orderFeignSupport).cancelOrder(ORDER_ID, null);
        verify(mqListenerHelper).clearConsumeRetry(RabbitMQConfig.RUSHING_DEAD_QUEUE, rabbitMessage());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void handleDeadOrder_cantCouponWithoutOrder_rollsBackRedisOnly() throws Exception {
        mockConsumeAllowed();
        Channel channel = mock(Channel.class);
        UserCoupon uc = new UserCoupon();
        uc.setStatus(UserCouponStatusEnum.CANT.getStatus());
        when(userCouponService.getUserCouponByUserCouponId(USER_COUPON_ID)).thenReturn(uc);
        when(orderFeignSupport.getOrder(ORDER_ID)).thenReturn(null);

        listener.handleDeadOrder(message(ORDER_ID), channel, rabbitMessage());

        verify(discountCouponService).releaseRushRedisReserve(COUPON_ID, USER_ID);
        verify(orderFeignSupport, never()).cancelOrder(anyString(), any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void handleDeadOrder_exception_nacksWithRetryOrDlq() throws Exception {
        mockConsumeAllowed();
        Channel channel = mock(Channel.class);
        when(userCouponService.getUserCouponByUserCouponId(USER_COUPON_ID))
                .thenThrow(new IllegalStateException("db down"));
        RushingCouponMessageDTO msg = message(ORDER_ID);
        Message mqMessage = rabbitMessage();

        listener.handleDeadOrder(msg, channel, mqMessage);

        verify(mqListenerHelper).nackWithRetryOrDlq(eq(channel), eq(DELIVERY_TAG), eq(mqMessage),
                eq(RabbitMQConfig.RUSHING_DEAD_QUEUE), eq(msg), any(Exception.class));
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
