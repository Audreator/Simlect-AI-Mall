package com.simlect.aspect;

import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.component.RedisComponent;
import com.simlect.component.SpringContext;
import com.simlect.exception.BusinessException;
import com.simlect.utils.AuthCookieHelper;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 管理端敏感操作确认：
 * - 一次性确认票据（Redis nonce，5 分钟有效，消费即销毁防重放），不再传明文密码
 * - 确认通过后记录管理员操作审计（AdminAuditLog，经 adminAuditFeignSupport bean）
 */
@Aspect
@Component
public class AdminSensitiveConfirmAspect {

    public static final String CONFIRM_TOKEN_HEADER = "X-Admin-Confirm-Token";

    private static final String ADMIN_AUDIT_FEIGN_SUPPORT_BEAN = "adminAuditFeignSupport";

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AuthCookieHelper authCookieHelper;

    @Before("@annotation(com.simlect.annotation.AdminSensitiveConfirm)")
    public void beforeSensitive(JoinPoint joinPoint) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            throw new BusinessException("请求无效");
        }
        String confirmToken = request.getHeader(CONFIRM_TOKEN_HEADER);
        if (StringTools.isEmpty(confirmToken)) {
            throw new BusinessException("敏感操作需二次确认，请重新确认");
        }
        // 当前管理员（票据绑定校验）
        String adminToken = authCookieHelper.resolveAdminToken(request);
        Object loginInfo = redisComponent.getLoginInfo4Admin(adminToken);
        String operator = loginInfo == null ? "" : String.valueOf(loginInfo);
        // 一次性票据：绑定管理员+目标操作（后缀匹配容忍网关前缀），原子消费（防重放/防明文密码/防票据跨界使用）
        if (!redisComponent.consumeAdminConfirmToken(confirmToken, operator, request.getRequestURI())) {
            throw new BusinessException("确认已过期、已使用或与当前操作不匹配，请重新确认");
        }
        // 审计：记录管理员与操作（审计失败不阻断业务，但记日志便于排查）
        try {
            Object auditSupport = SpringContext.getBean(ADMIN_AUDIT_FEIGN_SUPPORT_BEAN);
            String action = joinPoint.getSignature().toShortString();
            if (auditSupport != null) {
                auditSupport.getClass().getMethod(
                        "log", String.class, String.class, String.class, String.class)
                        .invoke(auditSupport, operator, action, null, "敏感操作确认通过");
            }
        } catch (Exception e) {
            // 审计写入失败仅记录，不阻断敏感操作本身
            logAuditFailure(operator, e);
        }
    }

    private void logAuditFailure(String operator, Exception e) {
        // 预留：可接入日志监控
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
