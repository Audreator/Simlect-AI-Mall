package com.simlect.cloud.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simlect.cloud.gateway.config.GatewayInternalProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalTokenGlobalFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private GatewayInternalProperties internalProperties;
    private InternalTokenGlobalFilter filter;

    @BeforeEach
    void setUp() {
        internalProperties = new GatewayInternalProperties();
        internalProperties.setToken("secret-internal");
        internalProperties.setAuthEnabled(true);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
        filter = new InternalTokenGlobalFilter(internalProperties, new ObjectMapper());
    }

    @Test
    void filter_nonInternalPath_passedThrough() {
        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/api/x").build()), chain).block();

        verify(chain).filter(any());
    }

    @Test
    void filter_options_passedThrough() {
        filter.filter(MockServerWebExchange.from(
                MockServerHttpRequest.options("/internal/product/x").build()), chain).block();

        verify(chain).filter(any());
    }

    @Test
    void filter_authDisabled_passedThrough() {
        internalProperties.setAuthEnabled(false);

        filter.filter(MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/product/x").build()), chain).block();

        verify(chain).filter(any());
    }

    @Test
    void filter_validToken_passedThrough() {
        filter.filter(MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/product/x")
                        .header("X-Internal-Token", "secret-internal")
                        .build()), chain).block();

        verify(chain).filter(any());
    }

    @Test
    void filter_missingToken_unauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/product/x").build());

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_wrongToken_unauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/product/x")
                        .header("X-Internal-Token", "wrong")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_returnsOrder() {
        assertEquals(-200, filter.getOrder());
    }
}
