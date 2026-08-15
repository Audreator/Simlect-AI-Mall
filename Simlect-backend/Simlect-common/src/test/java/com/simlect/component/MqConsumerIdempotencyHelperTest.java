package com.simlect.component;

import com.simlect.constants.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqConsumerIdempotencyHelperTest {

    private static final String KEY = "msg-key-001";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private MqConsumerIdempotencyHelper helper;

    private Message messageWithHeader() {
        MessageProperties props = new MessageProperties();
        props.setHeader(MqConsumerIdempotencyHelper.HEADER_IDEMPOTENCY_KEY, KEY);
        props.setMessageId("fallback-id");
        return new Message(new byte[]{1}, props);
    }

    private Message messageWithMessageIdOnly() {
        MessageProperties props = new MessageProperties();
        props.setMessageId("fallback-id");
        return new Message(new byte[]{1}, props);
    }

    @Test
    void tryBeginConsume_withHeaderKey_acquiredTrue() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(Constants.REDIS_KEY_MQ_CONSUME_IDEMPOTENT + KEY, "1", 30, TimeUnit.SECONDS))
                .thenReturn(true);

        assertTrue(helper.tryBeginConsume(messageWithHeader(), 30));
        verify(ops).setIfAbsent(Constants.REDIS_KEY_MQ_CONSUME_IDEMPOTENT + KEY, "1", 30, TimeUnit.SECONDS);
    }

    @Test
    void tryBeginConsume_alreadyProcessed_returnsFalse() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(Constants.REDIS_KEY_MQ_CONSUME_IDEMPOTENT + KEY, "1", 30, TimeUnit.SECONDS))
                .thenReturn(false);

        assertFalse(helper.tryBeginConsume(messageWithHeader(), 30));
    }

    @Test
    void tryBeginConsume_messageWithoutKey_allowsConsumeWithoutRedis() {
        assertTrue(helper.tryBeginConsume(null, 30));
        Message bare = new Message(new byte[]{1}, new MessageProperties());
        assertTrue(helper.tryBeginConsume(bare, 30));
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void releaseConsume_deletesKey() {
        helper.releaseConsume(messageWithHeader());
        verify(stringRedisTemplate).delete(Constants.REDIS_KEY_MQ_CONSUME_IDEMPOTENT + KEY);
    }

    @Test
    void releaseConsume_withoutKey_doesNothing() {
        helper.releaseConsume(null);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void resolveIdempotencyKey_prefersHeaderOverMessageId() {
        assertEquals(KEY, helper.resolveIdempotencyKey(messageWithHeader()));
    }

    @Test
    void resolveIdempotencyKey_fallsBackToMessageId() {
        assertEquals("fallback-id", helper.resolveIdempotencyKey(messageWithMessageIdOnly()));
    }

    @Test
    void resolveIdempotencyKey_emptyHeader_ignored() {
        MessageProperties props = new MessageProperties();
        props.setHeader(MqConsumerIdempotencyHelper.HEADER_IDEMPOTENCY_KEY, "");
        props.setMessageId("fallback-id");
        Message message = new Message(new byte[]{1}, props);
        assertEquals("fallback-id", helper.resolveIdempotencyKey(message));
    }

    @Test
    void resolveIdempotencyKey_nullMessage_returnsNull() {
        assertNull(helper.resolveIdempotencyKey(null));
    }

    @Test
    void resolveIdempotencyKey_noKeyAndNoMessageId_returnsNull() {
        Message message = new Message(new byte[]{1}, new MessageProperties());
        assertNull(helper.resolveIdempotencyKey(message));
    }
}
