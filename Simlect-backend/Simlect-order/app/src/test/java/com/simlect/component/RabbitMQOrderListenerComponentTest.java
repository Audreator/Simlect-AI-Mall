package com.simlect.component;

import com.rabbitmq.client.Channel;
import com.simlect.api.dto.RushingCouponMessageDTO;
import com.simlect.api.support.CouponFeignSupport;
import com.simlect.entity.po.OrderInfo;
import com.simlect.mappers.OrderInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMQOrderListenerComponentTest {

    @Mock
    private OrderInfoMapper<OrderInfo, com.simlect.entity.query.OrderInfoQuery> orderInfoMapper;
    @Mock
    private CouponFeignSupport couponFeignSupport;
    @Mock
    private Channel channel;

    @InjectMocks
    private RabbitMQOrderListenerComponent component;

    private Message mqMessage(long deliveryTag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], props);
    }

    @Test
    void handleOrder_orderExists_acks() throws Exception {
        when(orderInfoMapper.selectByOrderId("O1")).thenReturn(new OrderInfo());
        component.handleOrder(message(), channel, mqMessage(1L));
        verify(channel).basicAck(1L, false);
        verify(couponFeignSupport, never()).getUserCoupon(anyString());
    }

    @Test
    void handleOrder_orderMissingButCouponExists_acks() throws Exception {
        when(orderInfoMapper.selectByOrderId("O1")).thenReturn(null);
        when(couponFeignSupport.getUserCoupon("UC1")).thenReturn(new com.simlect.api.vo.UserCouponVO());
        component.handleOrder(message(), channel, mqMessage(2L));
        verify(channel).basicAck(2L, false);
    }

    @Test
    void handleOrder_bothMissing_ack_skipBuild() throws Exception {
        when(orderInfoMapper.selectByOrderId("O1")).thenReturn(null);
        when(couponFeignSupport.getUserCoupon("UC1")).thenReturn(null);
        component.handleOrder(message(), channel, mqMessage(3L));
        verify(channel).basicAck(3L, false);
    }

    @Test
    void handleOrder_exception_nackWhenNoSync() throws Exception {
        when(orderInfoMapper.selectByOrderId(any())).thenThrow(new RuntimeException("db down"));
        component.handleOrder(message(), channel, mqMessage(4L));
        verify(channel).basicNack(4L, false, false);
    }

    private RushingCouponMessageDTO message() {
        RushingCouponMessageDTO dto = new RushingCouponMessageDTO();
        dto.setOrderId("O1");
        dto.setUserCouponId("UC1");
        return dto;
    }
}
