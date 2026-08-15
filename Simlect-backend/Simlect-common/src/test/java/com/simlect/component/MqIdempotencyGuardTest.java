package com.simlect.component;

import com.simlect.constants.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqIdempotencyGuardTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private MqIdempotencyGuard guard;

    @Test
    void tryAcquireSend_firstTime_returnsTrue() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(Constants.REDIS_KEY_MQ_SEND_IDEMPOTENT + "k1", "SENT", 60, TimeUnit.SECONDS))
                .thenReturn(true);

        assertTrue(guard.tryAcquireSend("k1", 60));
        verify(ops).setIfAbsent(Constants.REDIS_KEY_MQ_SEND_IDEMPOTENT + "k1", "SENT", 60, TimeUnit.SECONDS);
    }

    @Test
    void tryAcquireSend_duplicate_returnsFalse() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(Constants.REDIS_KEY_MQ_SEND_IDEMPOTENT + "k1", "SENT", 60, TimeUnit.SECONDS))
                .thenReturn(false);

        assertFalse(guard.tryAcquireSend("k1", 60));
    }

    @Test
    void tryAcquireSend_emptyKey_throws() {
        assertThrows(IllegalArgumentException.class, () -> guard.tryAcquireSend(null, 60));
        assertThrows(IllegalArgumentException.class, () -> guard.tryAcquireSend("", 60));
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void tryAcquireSend_nonPositiveTtl_throws() {
        assertThrows(IllegalArgumentException.class, () -> guard.tryAcquireSend("k1", 0));
        assertThrows(IllegalArgumentException.class, () -> guard.tryAcquireSend("k1", -1));
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void releaseSend_deletesKey() {
        guard.releaseSend("k1");
        verify(stringRedisTemplate).delete(Constants.REDIS_KEY_MQ_SEND_IDEMPOTENT + "k1");
    }

    @Test
    void releaseSend_emptyKey_doesNothing() {
        guard.releaseSend(null);
        guard.releaseSend("");
        verify(stringRedisTemplate, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }
}
