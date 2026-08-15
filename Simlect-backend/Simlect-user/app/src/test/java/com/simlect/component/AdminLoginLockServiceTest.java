package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLoginLockServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private AdminLoginLockService adminLoginLockService;

    @Test
    void ensureNotLocked_emptyIp_ignored() {
        adminLoginLockService.ensureNotLocked(null);
        adminLoginLockService.ensureNotLocked("");

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void ensureNotLocked_locked_throws() {
        when(stringRedisTemplate.hasKey(Constants.REDIS_KEY_ADMIN_LOGIN_LOCK + "1.1.1.1"))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> adminLoginLockService.ensureNotLocked("1.1.1.1"));
    }

    @Test
    void ensureNotLocked_free_ok() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        assertDoesNotThrow(() -> adminLoginLockService.ensureNotLocked("1.1.1.1"));
    }

    @Test
    void recordFailure_firstFailure_setsExpire() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment(Constants.REDIS_KEY_ADMIN_LOGIN_FAIL + "1.1.1.1")).thenReturn(1L);

        adminLoginLockService.recordFailure("1.1.1.1");

        verify(stringRedisTemplate).expire(
                Constants.REDIS_KEY_ADMIN_LOGIN_FAIL + "1.1.1.1", 15 * 60L, TimeUnit.SECONDS);
        verify(ops, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void recordFailure_reachesLimit_locksIp() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(5L);

        adminLoginLockService.recordFailure("1.1.1.1");

        verify(ops).set(Constants.REDIS_KEY_ADMIN_LOGIN_LOCK + "1.1.1.1", "1",
                15 * 60L, TimeUnit.SECONDS);
    }

    @Test
    void clearFailures_deletesBothKeys() {
        adminLoginLockService.clearFailures("1.1.1.1");

        verify(stringRedisTemplate).delete(Constants.REDIS_KEY_ADMIN_LOGIN_FAIL + "1.1.1.1");
        verify(stringRedisTemplate).delete(Constants.REDIS_KEY_ADMIN_LOGIN_LOCK + "1.1.1.1");
    }
}
