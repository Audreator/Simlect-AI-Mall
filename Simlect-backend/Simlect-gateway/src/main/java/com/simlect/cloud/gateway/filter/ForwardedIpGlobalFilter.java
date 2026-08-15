package com.simlect.cloud.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * X-Forwarded-For 可信覆写：
 * - 直连（对端非可信代理）：以网关对端 IP 覆写 XFF（防客户端伪造）
 * - nginx 反代（对端命中可信代理列表）：取代理写入的 X-Real-IP（经合法性校验），
 *   避免 nginx 拓扑下全站 IP 折叠为代理 IP（否则登录锁定/限流误伤全站）
 */
@Component
public class ForwardedIpGlobalFilter implements GlobalFilter, Ordered {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    @Value("${simlect.gateway.trusted-proxies:}")
    private String trustedProxies;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return chain.filter(exchange);
        }
        String peerIp = remote.getAddress().getHostAddress();
        String clientIp;
        if (isTrustedProxy(peerIp)) {
            // nginx 反代：取代理写入的 X-Real-IP（必须为合法 IP，否则回退对端）
            String realIp = exchange.getRequest().getHeaders().getFirst(X_REAL_IP);
            clientIp = isValidIp(realIp) ? realIp.trim() : peerIp;
        } else {
            // 直连：以网关看到的对端 IP 为准（客户端无法伪造）
            clientIp = peerIp;
        }
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(X_FORWARDED_FOR, clientIp)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isTrustedProxy(String ip) {
        if (!StringUtils.hasText(trustedProxies) || !StringUtils.hasText(ip)) {
            return false;
        }
        // 精确匹配（不前缀匹配）：防信任 1.2.3.4 时 1.2.3.40~49 被误判为可信代理
        Set<String> trusted = new HashSet<>(Arrays.asList(trustedProxies.split(",")));
        return trusted.contains(ip.trim());
    }

    private boolean isValidIp(String s) {
        if (!StringUtils.hasText(s) || "unknown".equalsIgnoreCase(s)) {
            return false;
        }
        String v = s.trim();
        if (v.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            return true;
        }
        // 纯 IPv6：至少两个冒号（拒绝 IPv4:port 形如 1.2.3.4:8080 被误判）
        // 并拒绝括号形式（[::1]）与 zone-id（fe80::1%eth0）——均非合法 IP 字面量
        return v.indexOf(':') >= 0 && v.indexOf(':') != v.lastIndexOf(':')
                && !v.contains("/") && !v.contains(" ") && !v.contains(",")
                && !v.contains("[") && !v.contains("]") && !v.contains("%");
    }

    @Override
    public int getOrder() {
        return -220; // 早于内部令牌(-200)与用户鉴权，确保下游拿到可信 IP
    }
}
