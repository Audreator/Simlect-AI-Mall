package com.simlect.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MqPublisherConfirmHelperTest {

    private static final String EXCHANGE = "pay.exchange";
    private static final String ROUTING_KEY = "pay.timeout.dead";
    private static final String CORRELATION_ID = "corr-001";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MqPublisherConfirmHelper helper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(helper, "defaultConfirmTimeoutMs", 5000L);
        ReflectionTestUtils.setField(helper, "confirmTimeoutByExchange",
                new java.util.HashMap<String, Long>());
    }

    @Test
    void attachCallbacks_registersCallbacksAndMandatory() {
        helper.attachCallbacks();
        verify(rabbitTemplate).setConfirmCallback(helper);
        verify(rabbitTemplate).setReturnsCallback(helper);
        verify(rabbitTemplate).setMandatory(true);
    }

    @Test
    void confirm_nullCorrelationData_ignored() {
        helper.confirm(null, true, "");
    }

    @Test
    void confirm_ackCompletesFutureTrue() throws Exception {
        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(4);
            helper.confirm(cd, true, "");
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), eq("body"), any(), any(CorrelationData.class));

        helper.sendAndAwaitConfirm(EXCHANGE, ROUTING_KEY, "body", null, CORRELATION_ID);
    }

    @Test
    void confirm_nackCompletesFutureFalse_throwsAmqpException() {
        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(4);
            helper.confirm(cd, false, "nack reason");
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), eq("body"), any(), any(CorrelationData.class));

        org.springframework.amqp.AmqpException ex = assertThrows(org.springframework.amqp.AmqpException.class,
                () -> helper.sendAndAwaitConfirm(EXCHANGE, ROUTING_KEY, "body", null, CORRELATION_ID));
        assertTrue(ex.getMessage().contains("\u672a\u786e\u8ba4"));
    }

    @Test
    void sendAndAwaitConfirm_timeout_throwsAmqpException() {
        ReflectionTestUtils.setField(helper, "defaultConfirmTimeoutMs", 10L);

        org.springframework.amqp.AmqpException ex = assertThrows(org.springframework.amqp.AmqpException.class,
                () -> helper.sendAndAwaitConfirm(EXCHANGE, ROUTING_KEY, "body", null, CORRELATION_ID));
        assertTrue(ex.getMessage().contains("\u8d85\u65f6"));

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), eq("body"), any(), any(CorrelationData.class));
    }

    @Test
    void returnedMessage_completesFutureFalse() throws Exception {
        ConcurrentHashMap<String, CompletableFuture<Boolean>> pending =
                new ConcurrentHashMap<>();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pending.put(CORRELATION_ID, future);
        ReflectionTestUtils.setField(helper, "pending", pending);

        MessageProperties props = new MessageProperties();
        props.setMessageId(CORRELATION_ID);
        ReturnedMessage returned = new ReturnedMessage(new Message(new byte[]{1}, props), 312, "unroutable", EXCHANGE, ROUTING_KEY);

        helper.returnedMessage(returned);

        assertFalse(future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void returnedMessage_withoutMessageId_ignored() {
        ConcurrentHashMap<String, CompletableFuture<Boolean>> pending =
                new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(helper, "pending", pending);

        MessageProperties props = new MessageProperties();
        props.setMessageId(null);
        ReturnedMessage returned = new ReturnedMessage(new Message(new byte[]{1}, props), 312, "unroutable", EXCHANGE, ROUTING_KEY);

        helper.returnedMessage(returned);

        assertTrue(pending.isEmpty());
    }

    @Test
    void sendAndAwaitConfirm_cleansPendingOnSuccess() throws Exception {
        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(4);
            helper.confirm(cd, true, "");
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), eq("body"), any(), any(CorrelationData.class));

        helper.sendAndAwaitConfirm(EXCHANGE, ROUTING_KEY, "body", null, CORRELATION_ID);

        ConcurrentHashMap<?, ?> pending = (ConcurrentHashMap<?, ?>) ReflectionTestUtils.getField(helper, "pending");
        assertTrue(pending.isEmpty());
    }
}
