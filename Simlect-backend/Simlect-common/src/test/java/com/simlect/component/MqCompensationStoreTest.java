package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.entity.dto.MqCompensationRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqCompensationStoreTest {

    private static final String KEY = "order:20260815";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private MqCompensationStore store;

    private MqCompensationRecord record() {
        MqCompensationRecord record = new MqCompensationRecord();
        record.setIdempotencyKey(KEY);
        record.setExchange("pay.exchange");
        record.setRoutingKey("pay.timeout.dead");
        record.setPayload("{}");
        record.setFailedAt(1234L);
        return record;
    }

    @Test
    void saveToRedis_setsValueAndPendingIndex() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zset);

        store.saveToRedis(record());

        verify(ops).set(eq(Constants.REDIS_KEY_MQ_COMPENSATE + KEY), anyString(), anyLong(), any(TimeUnit.class));
        verify(zset).add(eq(Constants.REDIS_KEY_MQ_COMPENSATE_PENDING), eq(KEY), eq(1234.0d));
    }

    @Test
    void saveToRedis_failedAtZero_usesCurrentTimeMillisAsScore() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zset);
        MqCompensationRecord record = record();
        record.setFailedAt(0L);

        store.saveToRedis(record);

        verify(zset).add(eq(Constants.REDIS_KEY_MQ_COMPENSATE_PENDING), eq(KEY), anyDouble());
    }

    @Test
    void saveToRedis_nullRecordOrEmptyKey_skips() {
        store.saveToRedis(null);
        store.saveToRedis(new MqCompensationRecord());
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void saveToRedis_redisException_caughtNotThrown() {
        when(stringRedisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        store.saveToRedis(record());

        verify(stringRedisTemplate, never()).opsForZSet();
    }

    @Test
    void remove_deletesValueAndPendingIndex() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zset);

        store.remove(KEY);

        verify(stringRedisTemplate).delete(Constants.REDIS_KEY_MQ_COMPENSATE + KEY);
        verify(zset).remove(Constants.REDIS_KEY_MQ_COMPENSATE_PENDING, KEY);
    }

    @Test
    void remove_emptyKey_skips() {
        store.remove(null);
        store.remove("");
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void listPendingKeys_withLimit() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zset);
        when(zset.range(Constants.REDIS_KEY_MQ_COMPENSATE_PENDING, 0, 4)).thenReturn(Set.of(KEY));

        Set<String> keys = store.listPendingKeys(5);

        assertEquals(Set.of(KEY), keys);
        verify(zset).range(Constants.REDIS_KEY_MQ_COMPENSATE_PENDING, 0, 4);
    }

    @Test
    void listPendingKeys_nonPositiveLimit_defaultsTo20() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zset);
        when(zset.range(Constants.REDIS_KEY_MQ_COMPENSATE_PENDING, 0, 19)).thenReturn(Set.of(KEY));

        assertEquals(Set.of(KEY), store.listPendingKeys(0));
        assertEquals(Set.of(KEY), store.listPendingKeys(-3));
    }

    @Test
    void listPendingKeys_nullResult_returnsEmptySet() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zset);
        when(zset.range(Constants.REDIS_KEY_MQ_COMPENSATE_PENDING, 0, 4)).thenReturn(null);

        assertTrue(store.listPendingKeys(5).isEmpty());
    }
}
