package com.simlect.cloud.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * CSRF 防护：对非安全方法（POST/PUT/DELETE/PATCH）校验 Origin/Referer 同源。
 * <p>
 * - 放行：OPTIONS、GET/HEAD（无副作用）、/internal/**（内部令牌体系）、/notify/**（支付宝回调无来源）
 * - 携带 Origin 时，其 host 必须与请求 Host 一致（或命中配置的额外允许来源）
 * - 未携带 Origin 的浏览器跨站 POST 在 SameSite=Lax 下已不带 Cookie，双保险
 */
@Component
public class OriginCheckGlobalFilter implements GlobalFilter, Ordered {

    private static final String ORIGIN_HEADER = "Origin";
    private static final String REFERER_HEADER = "Referer";

    private static final List<String> SKIP_PREFIXES = Arrays.asList(
            "/internal/", "/notify/", "/api/notify/");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();
        if (method == null || !requiresOriginCheck(method)) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        for (String prefix : SKIP_PREFIXES) {
            if (path != null && path.startsWith(prefix)) {
                return chain.filter(exchange);
            }
        }
        if (originMatches(exchange)) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private boolean requiresOriginCheck(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT
                || method == HttpMethod.DELETE || method == HttpMethod.PATCH;
    }

    private boolean originMatches(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String origin = request.getHeaders().getFirst(ORIGIN_HEADER);
        String source = origin;
        if (!StringUtils.hasText(source)) {
            // 老客户端/非浏览器携带 Referer 时也校验
            source = request.getHeaders().getFirst(REFERER_HEADER);
        }
        if (!StringUtils.hasText(source)) {
            // 无来源头：非浏览器客户端（curl/SDK），放行
            return true;
        }
        String sourceHost = extractHost(source);
        if (!StringUtils.hasText(sourceHost)) {
            return false;
        }
        // Host 头可能携带端口（localhost:8080），归一化后再比较（避免非 80/443 端口部署误伤）
        String targetHost = stripPort(request.getHeaders().getFirst("Host"));
        if (!StringUtils.hasText(targetHost)) {
            return false;
        }
        // 配置的额外允许来源（可选）
        if (StringUtils.hasText(allowedOrigins) && isAllowedOrigin(sourceHost)) {
            return true;
        }
        return sourceHost.equalsIgnoreCase(targetHost);
    }

    @Value("${simlect.gateway.allowed-origins:}")
    private String allowedOrigins;

    private boolean isAllowedOrigin(String host) {
        for (String item : allowedOrigins.split(",")) {
            if (item != null && !item.trim().isEmpty() && host.equalsIgnoreCase(item.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 去掉 host:port 中的端口部分；IPv6 字面量 [::1]:8080 返回去括号的 ::1（与 URI.getHost() 输出对齐） */
    private String stripPort(String hostWithPort) {
        if (!StringUtils.hasText(hostWithPort)) {
            return null;
        }
        String h = hostWithPort.trim();
        if (h.startsWith("[")) {
            int close = h.indexOf(']');
            return close >= 0 ? h.substring(1, close) : h;
        }
        int colon = h.indexOf(':');
        return colon >= 0 ? h.substring(0, colon) : h;
    }

    private String extractHost(String urlOrOrigin) {
        try {
            URI uri = URI.create(urlOrOrigin);
            String host = uri.getHost();
            if (host == null) {
                // 形如 "https://example.com" 应能解析 host；兜底去掉协议
                String withoutScheme = urlOrOrigin.replaceFirst("(?i)^[a-z][a-z0-9+.-]*://", "");
                int slash = withoutScheme.indexOf('/');
                if (slash >= 0) {
                    withoutScheme = withoutScheme.substring(0, slash);
                }
                return stripPort(withoutScheme);
            }
            return host;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        return -150; // 早于用户鉴权（-100 级别），晚于内部令牌校验
    }
}
