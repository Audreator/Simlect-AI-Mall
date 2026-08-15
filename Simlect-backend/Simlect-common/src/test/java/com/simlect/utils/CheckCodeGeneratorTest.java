package com.simlect.utils;

import com.simlect.component.RedisComponent;
import com.simlect.entity.vo.CheckCodeVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckCodeGeneratorTest {

    @Mock
    private RedisComponent redisComponent;

    @Test
    void generate_returnsBase64ImageAndSavedKey() {
        when(redisComponent.saveCheckCode(anyString())).thenReturn("uuid-1");

        CheckCodeVO vo = CheckCodeGenerator.generate(redisComponent);

        assertNotNull(vo.getCheckCode());
        assertNotNull(vo.getCheckCodeKey());
        assertEquals("uuid-1", vo.getCheckCodeKey());
        assertTrue(vo.getCheckCode().length() > 0);
    }

    @Test
    void generate_codeIsFourNumericDigits() {
        when(redisComponent.saveCheckCode(anyString())).thenReturn("uuid-1");

        CheckCodeGenerator.generate(redisComponent);

        verify(redisComponent).saveCheckCode(org.mockito.ArgumentMatchers.matches("\\d{4}"));
    }

    @Test
    void generate_checkCodeIsDataUriImage() {
        when(redisComponent.saveCheckCode(anyString())).thenReturn("uuid-1");

        CheckCodeVO vo = CheckCodeGenerator.generate(redisComponent);

        assertTrue(vo.getCheckCode().startsWith("data:image/"));
        assertTrue(vo.getCheckCode().contains("base64,"));
    }
}
