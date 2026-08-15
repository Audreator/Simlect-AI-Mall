package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageCensorRateLimitServiceTest {

    @Mock
    private CouponRushRateLimitService rateLimitService;

    @InjectMocks
    private ImageCensorRateLimitService imageCensorRateLimitService;

    @Test
    void checkUserAndIp_anonymousUser_throws() {
        assertThrows(BusinessException.class, () -> imageCensorRateLimitService.checkUserAndIp("", "1.1.1.1"));
    }

    @Test
    void checkUserAndIp_userRateLimited_throws() {
        when(rateLimitService.tryAcquire(Constants.REDIS_RATE_LIMIT + "img:user:U1", 5, 1))
                .thenReturn(false);

        assertThrows(BusinessException.class, () -> imageCensorRateLimitService.checkUserAndIp("U1", "1.1.1.1"));
    }

    @Test
    void checkUserAndIp_ipRateLimited_throws() {
        when(rateLimitService.tryAcquire(Constants.REDIS_RATE_LIMIT + "img:user:U1", 5, 1))
                .thenReturn(true);
        when(rateLimitService.tryAcquire(Constants.REDIS_RATE_LIMIT + "img:ip:1.1.1.1", 5, 1))
                .thenReturn(false);

        assertThrows(BusinessException.class, () -> imageCensorRateLimitService.checkUserAndIp("U1", "1.1.1.1"));
    }

    @Test
    void checkUserAndIp_ok_passesBothLimits() {
        when(rateLimitService.tryAcquire(anyString(), anyLong(), anyInt())).thenReturn(true);

        assertDoesNotThrow(() -> imageCensorRateLimitService.checkUserAndIp("U1", "1.1.1.1"));
        verify(rateLimitService, times(2)).tryAcquire(anyString(), anyLong(), anyInt());
    }

    @Test
    void checkUserAndIp_emptyIp_usesUnknown() {
        when(rateLimitService.tryAcquire(anyString(), anyLong(), anyInt())).thenReturn(true);

        assertDoesNotThrow(() -> imageCensorRateLimitService.checkUserAndIp("U1", ""));

        verify(rateLimitService).tryAcquire(Constants.REDIS_RATE_LIMIT + "img:ip:unknown", 5, 1);
    }
}
