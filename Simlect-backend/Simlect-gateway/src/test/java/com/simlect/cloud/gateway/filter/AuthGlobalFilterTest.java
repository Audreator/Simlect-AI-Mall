package com.simlect.cloud.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simlect.cloud.gateway.config.GatewayAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthGlobalFilterTest {

    @Mock
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private GatewayFilterChain chain;

    private GatewayAuthProperties authProperties;
    private AuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        authProperties = new GatewayAuthProperties();
        authProperties.setEnabled(true);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
        filter = new AuthGlobalFilter(authProperties, reactiveStringRedisTemplate, new ObjectMapper());
    }

    private MockServerWebExchange exchange(MockServerHttpRequest request) {
        return MockServerWebExchange.from(request);
    }

    @Test
    void filter_disabled_passesThrough() {
        authProperties.setEnabled(false);

        filter.filter(exchange(MockServerHttpRequest.get("/api/secret").build()), chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(reactiveStringRedisTemplate);
    }

    @Test
    void filter_options_passesThrough() {
        filter.filter(exchange(MockServerHttpRequest.options("/api/secret").build()), chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(reactiveStringRedisTemplate);
    }

    @Test
    void filter_infraAndInternalPaths_passedThrough() {
        filter.filter(exchange(MockServerHttpRequest.get("/actuator/health").build()), chain).block();
        filter.filter(exchange(MockServerHttpRequest.get("/internal/product/x").build()), chain).block();

        verify(chain, times(2)).filter(any());
        verifyNoInteractions(reactiveStringRedisTemplate);
    }

    @Test
    void filter_webExcludePath_passedThrough() {
        authProperties.setWebExcludePaths(List.of("/api/account/login"));

        filter.filter(exchange(MockServerHttpRequest.get("/api/account/login").build()), chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(reactiveStringRedisTemplate);
    }

    @Test
    void filter_apiWithoutToken_unauthorized() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/product/list").build());

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertTrue(exchange.getResponse().getHeaders().getContentType().toString().contains("application/json"));
    }

    @Test
    void filter_apiWithValidSession_injectsUserIdHeader() {
        when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mall:token:web:tok"))
                .thenReturn(Mono.just("{\"userId\":\"U1\"}"));
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/product/list")
                .header("token", "tok").build());

        filter.filter(exchange, chain).block();

        ServerHttpRequest mutated = verifyChainCalledAndGetRequest();
        assertEquals("U1", mutated.getHeaders().getFirst("X-User-Id"));
        assertEquals("1", mutated.getHeaders().getFirst("X-User-Token-Verified"));
    }

    @Test
    void filter_apiWithValidSession_noUserId_stillVerifies() {
        when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mall:token:web:tok"))
                .thenReturn(Mono.just("{\"nickName\":\"nick\"}"));
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/product/list")
                .header("token", "tok").build());

        filter.filter(exchange, chain).block();

        ServerHttpRequest mutated = verifyChainCalledAndGetRequest();
        assertNull(mutated.getHeaders().getFirst("X-User-Id"));
        assertEquals("1", mutated.getHeaders().getFirst("X-User-Token-Verified"));
    }

    @Test
    void filter_apiWithMissingSession_unauthorized() {
        when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mall:token:web:tok")).thenReturn(Mono.empty());
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/product/list")
                .header("token", "tok").build());

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
    }

    @Test
    void filter_adminApiWithValidToken_passes() {
        when(reactiveStringRedisTemplate.hasKey("mall:token:admin:admin-tok"))
                .thenReturn(Mono.just(true));
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/admin-api/home")
                .header("adminToken", "admin-tok").build());

        filter.filter(exchange, chain).block();

        ServerHttpRequest mutated = verifyChainCalledAndGetRequest();
        assertEquals("1", mutated.getHeaders().getFirst("X-Admin-Token-Verified"));
    }

    @Test
    void filter_adminApiInvalidToken_unauthorized() {
        when(reactiveStringRedisTemplate.hasKey("mall:token:admin:admin-tok"))
                .thenReturn(Mono.just(false));
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/admin-api/home")
                .header("adminToken", "admin-tok").build());

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
    }

    @Test
    void filter_adminApiWithoutToken_unauthorized() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/admin-api/home").build());

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
    }

    @Test
    void filter_adminExcludePath_passedThrough() {
        authProperties.setAdminExcludePaths(List.of("/admin-api/account/login"));

        filter.filter(exchange(MockServerHttpRequest.get("/admin-api/account/login").build()), chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(reactiveStringRedisTemplate);
    }

    @Test
    void filter_unknownPath_passedThrough() {
        filter.filter(exchange(MockServerHttpRequest.get("/some/other").build()), chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(reactiveStringRedisTemplate);
    }

    @Test
    void filter_returnsOrder() {
        assertEquals(-100, filter.getOrder());
    }

    private ServerHttpRequest verifyChainCalledAndGetRequest() {
        org.mockito.ArgumentCaptor<ServerWebExchange> captor =
                org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        return captor.getValue().getRequest();
    }
}
