package com.simlect.component;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqListenerHelperTest {

    private static final String QUEUE = "pay.queue";
    private static final long DELIVERY_TAG = 7L;

    @Mock
    private MqConsumerIdempotencyHelper mqConsumerIdempotencyHelper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private MqConsumeFailureRecorder mqConsumeFailureRecorder;
    @Mock
    private Channel channel;

    @InjectMocks
    private MqListenerHelper helper;

    private Message message() {
        MessageProperties props = new MessageProperties();
        props.setMessageId("m-1");
        return new Message(new byte[]{1}, props);
    }

    @Test
    void tryBeginConsume_delegatesToIdempotencyHelper() {
        when(mqConsumerIdempotencyHelper.tryBeginConsume(any(), eq(3600L))).thenReturn(true);

        assertTrue(helper.tryBeginConsume(message(), 3600L));
    }

    @Test
    void releaseConsume_delegatesToIdempotencyHelper() {
        helper.releaseConsume(message());

        verify(mqConsumerIdempotencyHelper).releaseConsume(any());
    }

    @Test
    void resolveIdempotencyKey_delegatesToIdempotencyHelper() {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn("k-1");

        assertEquals("k-1", helper.resolveIdempotencyKey(message()));
    }

    @Test
    void nackWithRetryOrDlq_noIdempotencyKey_dlqDirectly() throws IOException {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn(null);

        helper.nackWithRetryOrDlq(channel, DELIVERY_TAG, message(), QUEUE, "p", new RuntimeException("x"));

        verify(mqConsumeFailureRecorder).record(eq(QUEUE), any(), eq("p"), any(RuntimeException.class));
        verify(channel).basicNack(DELIVERY_TAG, false, false);
    }

    @Test
    void nackWithRetryOrDlq_firstAttempt_setsExpireAndRequeues() throws IOException {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn("k-1");
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment("mall:mq:consume:retry:pay.queue:k-1")).thenReturn(1L);

        helper.nackWithRetryOrDlq(channel, DELIVERY_TAG, message(), QUEUE, "p", new RuntimeException("x"));

        verify(stringRedisTemplate).expire("mall:mq:consume:retry:pay.queue:k-1", 1, TimeUnit.DAYS);
        verify(mqConsumerIdempotencyHelper).releaseConsume(any());
        verify(channel).basicNack(DELIVERY_TAG, false, true);
        verify(mqConsumeFailureRecorder, never()).record(any(), any(), any(), any());
    }

    @Test
    void nackWithRetryOrDlq_withinMaxRetries_requeuesWithoutExpire() throws IOException {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn("k-1");
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment("mall:mq:consume:retry:pay.queue:k-1")).thenReturn(3L);

        helper.nackWithRetryOrDlq(channel, DELIVERY_TAG, message(), QUEUE, "p", new RuntimeException("x"));

        verify(stringRedisTemplate, never()).expire(any(), anyLong(), any());
        verify(channel).basicNack(DELIVERY_TAG, false, true);
        verify(mqConsumeFailureRecorder, never()).record(any(), any(), any(), any());
    }

    @Test
    void nackWithRetryOrDlq_exceedMaxRetries_recordsAndDlq() throws IOException {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn("k-1");
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment("mall:mq:consume:retry:pay.queue:k-1")).thenReturn(4L);

        helper.nackWithRetryOrDlq(channel, DELIVERY_TAG, message(), QUEUE, "p", new RuntimeException("x"));

        verify(mqConsumeFailureRecorder).record(eq(QUEUE), any(), eq("p"), any(RuntimeException.class));
        verify(stringRedisTemplate).delete("mall:mq:consume:retry:pay.queue:k-1");
        verify(channel).basicNack(DELIVERY_TAG, false, false);
    }

    @Test
    void nackWithRetryOrDlq_attemptNull_treatedAsDlq() throws IOException {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn("k-1");
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment("mall:mq:consume:retry:pay.queue:k-1")).thenReturn(null);

        helper.nackWithRetryOrDlq(channel, DELIVERY_TAG, message(), QUEUE, "p", new RuntimeException("x"));

        verify(mqConsumeFailureRecorder).record(eq(QUEUE), any(), eq("p"), any(RuntimeException.class));
        verify(channel).basicNack(DELIVERY_TAG, false, false);
    }

    @Test
    void clearConsumeRetry_deletesRetryKey() {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn("k-1");

        helper.clearConsumeRetry(QUEUE, message());

        verify(stringRedisTemplate).delete("mall:mq:consume:retry:pay.queue:k-1");
    }

    @Test
    void clearConsumeRetry_noIdempotencyKey_skips() {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn(null);

        helper.clearConsumeRetry(QUEUE, message());

        verifyNoInteractions(stringRedisTemplate);
    }
}
