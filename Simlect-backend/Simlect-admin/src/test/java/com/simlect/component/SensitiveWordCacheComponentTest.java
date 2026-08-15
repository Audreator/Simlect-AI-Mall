package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.entity.dto.SensitiveWordCacheItem;
import com.simlect.entity.po.SensitiveWord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensitiveWordCacheComponentTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private SensitiveWordCacheComponent sensitiveWordCacheComponent;

    @Test
    void loadFromRedis_missing_returnsNull() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(Constants.REDIS_KEY_SENSITIVE_WORD_PAYLOAD)).thenReturn(null);

        assertNull(sensitiveWordCacheComponent.loadFromRedis());
    }

    @Test
    void loadFromRedis_parsesJson() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(Constants.REDIS_KEY_SENSITIVE_WORD_PAYLOAD))
                .thenReturn("[{\"word\":\"广告\",\"replaceWord\":\"***\"}]");

        List<SensitiveWordCacheItem> items = sensitiveWordCacheComponent.loadFromRedis();

        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("广告", items.get(0).getWord());
        assertEquals("***", items.get(0).getReplaceWord());
    }

    @Test
    void loadFromRedis_invalidJson_returnsNull() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("{broken");

        assertNull(sensitiveWordCacheComponent.loadFromRedis());
    }

    @Test
    void saveToRedis_serializesAndBumpsVersion() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        SensitiveWord word = new SensitiveWord();
        word.setWord("广告");
        word.setReplaceWord("广而告之");

        sensitiveWordCacheComponent.saveToRedis(List.of(word));

        verify(ops).set(eq(Constants.REDIS_KEY_SENSITIVE_WORD_PAYLOAD),
                contains("\"广告\""));
        verify(ops).set(eq(Constants.REDIS_KEY_SENSITIVE_WORD_VERSION), anyString());
    }

    @Test
    void saveToRedis_nullList_savesEmptyArray() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);

        sensitiveWordCacheComponent.saveToRedis(null);

        verify(ops).set(Constants.REDIS_KEY_SENSITIVE_WORD_PAYLOAD, "[]");
    }

    @Test
    void getVersion_missing_returnsZero() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);

        assertEquals(0L, sensitiveWordCacheComponent.getVersion());
    }

    @Test
    void getVersion_invalid_returnsZero() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("abc");

        assertEquals(0L, sensitiveWordCacheComponent.getVersion());
    }

    @Test
    void getVersion_parses() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("12345");

        assertEquals(12345L, sensitiveWordCacheComponent.getVersion());
    }
}
