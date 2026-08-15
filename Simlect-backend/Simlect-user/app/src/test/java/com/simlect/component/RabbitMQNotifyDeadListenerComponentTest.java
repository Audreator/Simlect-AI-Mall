package com.simlect.component;

import com.simlect.api.dto.NotificationMessageDTO;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQNotifyDeadListenerComponentTest {

    @Mock
    private MqPublisherConfirmHelper mqPublisherConfirmHelper;
    @Mock
    private Channel channel;

    @InjectMocks
    private RabbitMQNotifyDeadListenerComponent component;

    private Message deadMessage(int recycleCount, String messageId) {
        MessageProperties props = new MessageProperties();
        props.setMessageId(messageId);
        props.setDeliveryTag(recycleCount + 100L);
        if (recycleCount > 0) {
            props.setHeader(RabbitMQNotifyDeadListenerComponent.HEADER_RECYCLE_COUNT, recycleCount);
        }
        return MessageBuilder.withBody("{\"userId\":\"u1\",\"title\":\"t\",\"content\":\"c\",\"bizType\":\"rush_coupon\",\"bizId\":\"b1\"}".getBytes())
                .andProperties(props)
                .build();
    }

    @Test
    void firstDeadLetter_isRecycledBack_withCountHeader() throws Exception {
        Message msg = deadMessage(0, "m1");
        component.handleDeadNotify(new NotificationMessageDTO("u1", "t", "c", "rush_coupon", "b1"),
                channel, msg);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mqPublisherConfirmHelper, times(1))
                .sendAndAwaitConfirm(anyString(), anyString(), captor.capture(), any(), anyString());
        assertNotNull(captor.getValue().getMessageProperties().getHeader(
                RabbitMQNotifyDeadListenerComponent.HEADER_RECYCLE_COUNT));
        assertEquals(1, ((Number) captor.getValue().getMessageProperties().getHeader(
                RabbitMQNotifyDeadListenerComponent.HEADER_RECYCLE_COUNT)).intValue());
        // 处理完成后 ack 死信（防死信再入队）
        verify(channel, times(1)).basicAck(100L, false);
    }

    @Test
    void repeatedDeadLetter_incrementsCount() throws Exception {
        Message msg = deadMessage(2, "m2");
        component.handleDeadNotify(new NotificationMessageDTO("u1", "t", "c", "rush_coupon", "b1"),
                channel, msg);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mqPublisherConfirmHelper).sendAndAwaitConfirm(anyString(), anyString(), captor.capture(), any(), anyString());
        assertEquals(3, ((Number) captor.getValue().getMessageProperties().getHeader(
                RabbitMQNotifyDeadListenerComponent.HEADER_RECYCLE_COUNT)).intValue());
        verify(channel).basicAck(102L, false);
    }

    @Test
    void overLimitDeadLetter_isDroppedWithAudit_noRecycle() throws Exception {
        Message msg = deadMessage(3, "m3");
        component.handleDeadNotify(new NotificationMessageDTO("u1", "t", "c", "rush_coupon", "b1"),
                channel, msg);

        // 超限丢弃：不再重投，仅 ack 死信（可审计日志已由 error 级输出）
        verify(mqPublisherConfirmHelper, never()).sendAndAwaitConfirm(anyString(), anyString(), any(), any(), anyString());
        verify(channel, times(1)).basicAck(103L, false);
    }

    @Test
    void recycleFailure_acksDeadLetter_noNack() throws Exception {
        Message msg = deadMessage(1, "m4");
        doThrow(new RuntimeException("broker down")).when(mqPublisherConfirmHelper)
                .sendAndAwaitConfirm(anyString(), anyString(), any(), any(), anyString());
        component.handleDeadNotify(new NotificationMessageDTO("u1", "t", "c", "rush_coupon", "b1"),
                channel, msg);

        // 重投失败：ack 丢弃（防死信死循环），仅告警日志
        verify(channel, times(1)).basicAck(101L, false);
    }
}
