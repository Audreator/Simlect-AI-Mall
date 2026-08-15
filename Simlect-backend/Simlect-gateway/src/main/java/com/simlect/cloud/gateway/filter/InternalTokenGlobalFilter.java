package com.simlect.cloud.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simlect.cloud.gateway.config.GatewayInternalProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 这个类干什么？
 * Gateway 上校验「是不是内部服务在调」：只处理 /internal/**。
 * 请求头必须带 X-Internal-Token，且和配置里的 simlect.internal.token 一致，否则 401。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：Feign 跨服务、Agent/MCP 调 Java 内部接口（读订单、锁库存、提案数据等）
 * - 角色：东西向（服务→服务）门卫。order=-200，比用户登录 Filter 更早执行。
 *   和用户 Cookie 是两套体系：内部调用不查 mall:token:web:。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. FeignInternalAuthInterceptor / JavaInternalClient 自动加 X-Internal-Token
 *    → 作用：标明「我是内部调用方」
 * 2. 本 Filter 比对配置中的 token
 *    → 作用：挡住公网乱扫 /internal、假冒内部调用
 * 3. 通过后路由到 lb://simlect-* 的 InternalController
 *    → 作用：真正执行库存/订单等内部 API
 * </pre>
 * 现状是共享对称令牌（演示够用）；上线应升级 mTLS 或 OAuth2 client credentials。
 */
@Component
public class InternalTokenGlobalFilter implements GlobalFilter, Ordered {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final int CODE_UNAUTHORIZED = 401;

    private final GatewayInternalProperties internalProperties;
    private final ObjectMapper objectMapper;

    public InternalTokenGlobalFilter(GatewayInternalProperties internalProperties,
                                     ObjectMapper objectMapper) {
        this.internalProperties = internalProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path == null || !path.startsWith("/internal/")) {
            return chain.filter(exchange);
        }
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }
        if (!internalProperties.isAuthEnabled()) {
            return chain.filter(exchange);
        }
        String expected = internalProperties.getToken();
        String actual = exchange.getRequest().getHeaders().getFirst(INTERNAL_TOKEN_HEADER);
        // 未配置令牌（空值）一律 401：禁止空令牌放行内部接口
        if (StringUtils.hasText(expected) && expected.equals(actual)) {
            return chain.filter(exchange);
        }
        return unauthorized(exchange, "invalid internal token");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("code", CODE_UNAUTHORIZED);
        body.put("info", msg);
        body.put("data", null);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":\"error\",\"code\":401,\"info\":\"" + msg + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
