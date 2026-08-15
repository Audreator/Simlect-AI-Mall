package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponRushRateLimitServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private CouponRushRateLimitService service;

    @Test
    void tryAcquire_allowed_whenScriptReturnsOne() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(1L);

        assertTrue(service.tryAcquire("rush:user:u1", 60L, 30));
    }

    @Test
    void tryAcquire_blocked_whenScriptReturnsZero() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(0L);

        assertFalse(service.tryAcquire("rush:user:u1", 60L, 30));
    }

    @Test
    void tryAcquire_blocked_whenScriptReturnsNull() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(null);

        assertFalse(service.tryAcquire("rush:user:u1", 60L, 30));
    }

    @Test
    void checkUserLimit_emptyUserId_throwsLoginFirst() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkUserLimit(null));
        assertTrue(ex.getMessage().contains("请先登录"));
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void checkUserLimit_throttled_throwsRetry() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkUserLimit("u1"));
        assertTrue(ex.getMessage().contains("操作过于频繁"));
    }

    @Test
    void checkUserLimit_passed_usesDefaultWindowAndMax() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(1L);

        service.checkUserLimit("u1");

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate).execute(any(RedisScript.class), keys.capture(), any(), any());
        assertTrue(keys.getValue().contains(Constants.REDIS_KEY_RUSH_RATE_USER + "u1"));
    }

    @Test
    void checkCouponLimit_emptyCouponId_returnsSilently() {
        service.checkCouponLimit(null);
        service.checkCouponLimit("");
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void checkCouponLimit_throttled_throwsBusy() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkCouponLimit("c1"));
        assertTrue(ex.getMessage().contains("当前抢购人数过多"));
    }

    @Test
    void checkCouponLimit_passed() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(1L);

        service.checkCouponLimit("c1");

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate).execute(any(RedisScript.class), keys.capture(), any(), any());
        assertTrue(keys.getValue().contains(Constants.REDIS_KEY_RUSH_RATE_COUPON + "c1"));
    }

    @Test
    void checkUserLimit_customParams_acquires() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(1L);

        service.checkUserLimit("u1", 5, 3L);

        verify(stringRedisTemplate).execute(any(RedisScript.class), anyList(), any(), any());
    }
}
