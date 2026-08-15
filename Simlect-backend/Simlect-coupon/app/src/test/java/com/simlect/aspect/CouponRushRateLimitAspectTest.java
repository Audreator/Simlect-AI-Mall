package com.simlect.aspect;

import com.simlect.annotation.CouponRushRateLimit;
import com.simlect.component.CouponRushRateLimitService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.exception.BusinessException;
import com.simlect.utils.AuthCookieHelper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CouponRushRateLimitAspect 秒杀限流切面单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CouponRushRateLimitAspectTest {

    @Mock
    private CouponRushRateLimitService couponRushRateLimitService;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private JoinPoint joinPoint;
    @Mock
    private MethodSignature signature;

    @InjectMocks
    private CouponRushRateLimitAspect aspect;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        System.clearProperty("dev");
    }

    @CouponRushRateLimit
    private void rushEndpoint(String couponId) {
    }

    private void mockJoinPoint() throws NoSuchMethodException {
        Method method = CouponRushRateLimitAspectTest.class
                .getDeclaredMethod("rushEndpoint", String.class);
        doReturn(signature).when(joinPoint).getSignature();
        doReturn(method).when(signature).getMethod();
    }

    private MockHttpServletRequest requestWithCoupon(String couponId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("couponId", couponId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    @Test
    void beforeRush_withToken_checksUserAndCouponLimits() throws Exception {
        mockJoinPoint();
        requestWithCoupon("CP1");
        TokenUserInfoDTO user = new TokenUserInfoDTO();
        user.setUserId("U1");
        doReturn("token-1").when(authCookieHelper).resolveWebToken(any());
        when(redisComponent.getTokenUserInfo("token-1")).thenReturn(user);

        aspect.beforeRush(joinPoint);

        verify(couponRushRateLimitService).checkUserLimit("U1", 30, 60L);
        verify(couponRushRateLimitService).checkCouponLimit("CP1", 200, 1L);
    }

    @Test
    void beforeRush_missingToken_throwsLoginTimeout() throws Exception {
        mockJoinPoint();
        requestWithCoupon("CP1");
        doReturn(null).when(authCookieHelper).resolveWebToken(any());

        BusinessException e = assertThrows(BusinessException.class, () -> aspect.beforeRush(joinPoint));

        assertEquals(Integer.valueOf(901), e.getCode());
    }

    @Test
    void beforeRush_tokenUserMissing_throwsLoginTimeout() throws Exception {
        mockJoinPoint();
        requestWithCoupon("CP1");
        doReturn("token-2").when(authCookieHelper).resolveWebToken(any());
        when(redisComponent.getTokenUserInfo("token-2")).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class, () -> aspect.beforeRush(joinPoint));

        assertEquals(Integer.valueOf(901), e.getCode());
    }

    @Test
    void beforeRush_devBypassRemoved_usesTokenSession() throws Exception {
        // dev 后门已移除：固定 userId 不再生效，必须走 token 会话解析
        mockJoinPoint();
        requestWithCoupon("CP1");
        doReturn("token-dev").when(authCookieHelper).resolveWebToken(any());
        TokenUserInfoDTO sessionUser = new TokenUserInfoDTO();
        sessionUser.setUserId("U1");
        doReturn(sessionUser).when(redisComponent).getTokenUserInfo("token-dev");

        aspect.beforeRush(joinPoint);

        verify(couponRushRateLimitService).checkUserLimit("U1", 30, 60L);
        verify(redisComponent).getTokenUserInfo("token-dev");
    }

    @Test
    void beforeRush_devBypassRemoved_invalidSession_throwsLoginTimeout() throws Exception {
        mockJoinPoint();
        requestWithCoupon("CP1");
        doReturn("token-dev").when(authCookieHelper).resolveWebToken(any());
        doReturn(null).when(redisComponent).getTokenUserInfo(anyString());

        assertThrows(BusinessException.class, () -> aspect.beforeRush(joinPoint));
        verify(couponRushRateLimitService, never()).checkUserLimit(anyString(), anyInt(), anyLong());
    }

    @Test
    void beforeRush_noRequestContext_throwsServerError() throws Exception {
        mockJoinPoint();
        RequestContextHolder.resetRequestAttributes();

        BusinessException e = assertThrows(BusinessException.class, () -> aspect.beforeRush(joinPoint));

        assertEquals(Integer.valueOf(500), e.getCode());
    }
}
