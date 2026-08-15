package com.simlect.web;

import com.simlect.constants.InternalApiHeaders;
import com.simlect.entity.enums.ResponseCodeEnum;
import com.simlect.utils.StringTools;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class InternalApiAuthFilter extends OncePerRequestFilter {

    @Value("${simlect.internal.token:}")
    private String expectedToken;

    @Value("${simlect.internal.auth-enabled:true}")
    private boolean authEnabled;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!authEnabled) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri == null || !uri.contains("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(InternalApiHeaders.INTERNAL_TOKEN);
        // 未配置令牌（空）或令牌不匹配：一律 401（与网关同构，防服务侧 /internal 裸奔）
        if (StringTools.isEmpty(expectedToken) || !expectedToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":\"error\",\"code\":"
                    + ResponseCodeEnum.CODE_901.getCode()
                    + ",\"info\":\"非法内部调用\",\"data\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
