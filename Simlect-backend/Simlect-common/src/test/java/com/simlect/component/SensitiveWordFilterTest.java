package com.simlect.component;

import com.simlect.constants.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensitiveWordFilterTest {

    private static final String VERSION_KEY = Constants.REDIS_KEY_SENSITIVE_WORD_VERSION;
    private static final String PAYLOAD_KEY = Constants.REDIS_KEY_SENSITIVE_WORD_PAYLOAD;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private SensitiveWordFilter filter;

    private void stubCache(String version, String payload) {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(VERSION_KEY)).thenReturn(version);
        when(ops.get(PAYLOAD_KEY)).thenReturn(payload);
    }

    @Test
    void replaceSensitiveWords_customWord_usesReplaceWord() {
        stubCache("1", "[{\"word\":\"赌博\",\"replaceWord\":\"**\"}]");

        String result = filter.replaceSensitiveWords("最近赌博很疯狂");

        assertEquals("最近**很疯狂", result);
    }

    @Test
    void replaceSensitiveWords_wordWithoutReplaceWord_usesStars() {
        stubCache("1", "[{\"word\":\"违禁品\"}]");

        String result = filter.replaceSensitiveWords("持有违禁品违法");

        assertEquals("持有***违法", result);
    }

    @Test
    void replaceSensitiveWords_multipleWords_replacedRightToLeft() {
        stubCache("1", "[{\"word\":\"赌博\",\"replaceWord\":\"**\"},{\"word\":\"违禁品\",\"replaceWord\":\"##\"}]");

        String result = filter.replaceSensitiveWords("违禁品与赌博");

        assertEquals("##与**", result);
    }

    @Test
    void replaceSensitiveWords_emptyText_returnsSame() {
        assertNull(filter.replaceSensitiveWords(null));
        String blank = "";
        assertSame(blank, filter.replaceSensitiveWords(blank));
    }

    @Test
    void replaceSensitiveWords_noCache_engineNull_returnsText() {
        stubCache(null, null);

        String text = "没有敏感词";
        assertSame(text, filter.replaceSensitiveWords(text));
    }

    @Test
    void replaceSensitiveWords_payloadBlank_returnsText() {
        stubCache("1", "  ");

        String text = "内容不变";
        assertSame(text, filter.replaceSensitiveWords(text));
    }

    @Test
    void replaceSensitiveWords_cleanText_notReplaced() {
        stubCache("1", "[{\"word\":\"赌博\",\"replaceWord\":\"**\"}]");

        String text = "今天天气不错";
        assertSame(text, filter.replaceSensitiveWords(text));
    }

    @Test
    void replaceSensitiveWords_sameVersion_skipsReload() {
        stubCache("5", "[{\"word\":\"赌博\",\"replaceWord\":\"**\"}]");

        assertEquals("**", filter.replaceSensitiveWords("赌博"));
        // 第二次调用版本未变，应命中本地引擎
        assertEquals("**", filter.replaceSensitiveWords("赌博"));
        verify(stringRedisTemplate.opsForValue()).get(PAYLOAD_KEY);
    }
}
