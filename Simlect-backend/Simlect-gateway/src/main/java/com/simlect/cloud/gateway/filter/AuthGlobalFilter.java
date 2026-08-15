package com.simlect.cloud.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simlect.cloud.gateway.config.GatewayAuthProperties;
import com.simlect.cloud.gateway.support.GatewayTokenResolver;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 这个类干什么？
 * Gateway 上校验「用户有没有登录」：从 Cookie/Header 取出 token，去 Redis 查会话。
 * 查得到 → 把用户 ID 写进请求头转给后面的微服务；查不到 → 返回登录超时（业务码 901）。
 * 注意：用的是 Redis 里的随机 token，不是 JWT。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：所有需要登录的 C 端/管理端接口（/api/**、/admin-api/**、/ws/**）
 * - 角色：南北向（浏览器→网关）的会话门卫。order=-100，排在内部令牌 Filter（-200）后面。
 *   /internal/** 不归它管（内部调用走 InternalTokenGlobalFilter）。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. 用户先登录：AccountController.login → RedisComponent.saveTokenUserInfo
 *    → 作用：Redis 写入 mall:token:web:{token}，浏览器带上 Cookie
 * 2. 之后每次请求：浏览器 → Nginx → 本 Filter.filter
 *    → 作用：ReactiveRedis GET 会话；成功则注入 X-User-Id 等头
 * 3. chain.filter → 具体微服务
 *    → 作用：业务服务信任网关已验过登录，直接用 X-User-Id
 * </pre>
 * 约束：WebFlux 里不能写阻塞 JDBC；这里只查 Redis。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String REDIS_KEY_TOKEN_WEB = "mall:token:web:";
    private static final String REDIS_KEY_TOKEN_ADMIN = "mall:token:admin:";
    private static final int CODE_LOGIN_TIMEOUT = 901;

    private final GatewayAuthProperties authProperties;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthGlobalFilter(GatewayAuthProperties authProperties,
                            ReactiveStringRedisTemplate reactiveStringRedisTemplate,
                            ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 鉴权入口：跳过健康检查/OPTIONS/内部路径后，按 web/admin 查 Redis 会话并改写请求头。
     * 内部调用（/internal/**）应走 {@link InternalTokenGlobalFilter}，本方法会直接放行该路径。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!authProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }
        String path = request.getURI().getPath();
        if (isInfraPath(path) || isInternalPath(path)) {
            return chain.filter(exchange);
        }

        if (path.startsWith("/admin-api/")) {
            if (matchAny(path, authProperties.getAdminExcludePaths())) {
                return chain.filter(exchange);
            }
            String adminToken = GatewayTokenResolver.resolveAdminToken(exchange);
            if (!StringUtils.hasText(adminToken)) {
                return unauthorized(exchange, "登录超时");
            }
            return reactiveStringRedisTemplate.hasKey(REDIS_KEY_TOKEN_ADMIN + adminToken)
                    .flatMap(exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            ServerHttpRequest mutated = request.mutate()
                                    .header("X-Admin-Token-Verified", "1")
                                    .build();
                            return chain.filter(exchange.mutate().request(mutated).build());
                        }
                        return unauthorized(exchange, "登录超时");
                    });
        }

        if (path.startsWith("/api/") || path.startsWith("/ws/")) {
            if (path.startsWith("/api/") && matchAny(path, authProperties.getWebExcludePaths())) {
                return chain.filter(exchange);
            }
            String token = GatewayTokenResolver.resolveWebToken(exchange);
            if (!StringUtils.hasText(token)) {
                return unauthorized(exchange, "登录超时");
            }
            return reactiveStringRedisTemplate.opsForValue().get(REDIS_KEY_TOKEN_WEB + token)
                    .flatMap(sessionJson -> {
                        if (!StringUtils.hasText(sessionJson)) {
                            return unauthorized(exchange, "登录超时");
                        }
                        String userId = extractUserId(sessionJson);
                        ServerHttpRequest.Builder builder = request.mutate()
                                .header("X-User-Token-Verified", "1");
                        if (StringUtils.hasText(userId)) {
                            builder.header("X-User-Id", userId);
                        }
                        return chain.filter(exchange.mutate().request(builder.build()).build());
                    })
                    .switchIfEmpty(unauthorized(exchange, "登录超时"));
        }

        return chain.filter(exchange);
    }

    private boolean isInfraPath(String path) {
        // 仅放行健康检查，避免整站 actuator 暴露业务细节
        return "/actuator/health".equals(path)
                || "/actuator/health/liveness".equals(path)
                || "/actuator/health/readiness".equals(path)
                || "/favicon.ico".equals(path);
    }

    /** Session auth is skipped; {@link InternalTokenGlobalFilter} validates the token. */
    private boolean isInternalPath(String path) {
        return path != null && path.startsWith("/internal/");
    }

    private boolean matchAny(String path, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (!StringUtils.hasText(pattern)) {
                continue;
            }
            if (pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }

    private String extractUserId(String sessionJson) {
        try {
            JsonNode node = objectMapper.readTree(sessionJson);
            JsonNode userId = node.get("userId");
            return userId == null || userId.isNull() ? null : userId.asText();
        } catch (Exception ex) {
            return null;
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("code", CODE_LOGIN_TIMEOUT);
        body.put("info", msg);
        body.put("data", null);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":901,\"info\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
