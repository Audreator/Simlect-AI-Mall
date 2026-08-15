package com.simlect.aspect;

import com.simlect.annotation.CouponRushRateLimit;
import com.simlect.component.CouponRushRateLimitService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.enums.ResponseCodeEnum;
import com.simlect.exception.BusinessException;
import com.simlect.utils.AuthCookieHelper;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Component
@Aspect
@Order(2)
public class CouponRushRateLimitAspect {

    @Resource
    private CouponRushRateLimitService couponRushRateLimitService;
    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AuthCookieHelper authCookieHelper;

    @Before("@annotation(com.simlect.annotation.CouponRushRateLimit)")
    public void beforeRush(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        CouponRushRateLimit limit = method.getAnnotation(CouponRushRateLimit.class);
        if (limit == null) {
            return;
        }
        HttpServletRequest request = currentRequest();
        String userId = resolveUserId(request);
        String couponId = request.getParameter("couponId");
        couponRushRateLimitService.checkUserLimit(userId, limit.userMaxPerMinute(), 60L);
        couponRushRateLimitService.checkCouponLimit(couponId, limit.couponMaxPerSecond(), 1L);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
        return attrs.getRequest();
    }

    private String resolveUserId(HttpServletRequest request) {
        String token = authCookieHelper.resolveWebToken(request);
        if (StringTools.isEmpty(token)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        TokenUserInfoDTO user = redisComponent.getTokenUserInfo(token);
        if (user == null || StringTools.isEmpty(user.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        return user.getUserId();
    }
}
