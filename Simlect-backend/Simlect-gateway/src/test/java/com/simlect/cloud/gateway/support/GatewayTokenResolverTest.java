package com.simlect.cloud.gateway.support;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GatewayTokenResolverTest {

    @Test
    void resolveWebToken_headerWins() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/x")
                        .header("token", " header-token ")
                        .queryParam("token", "query-token")
                        .cookie(new HttpCookie("token", "cookie-token"))
                        .build());

        assertEquals("header-token", GatewayTokenResolver.resolveWebToken(exchange));
    }

    @Test
    void resolveWebToken_queryFallback() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/x")
                        .queryParam("token", " query-token ")
                        .cookie(new HttpCookie("token", "cookie-token"))
                        .build());

        assertEquals("query-token", GatewayTokenResolver.resolveWebToken(exchange));
    }

    @Test
    void resolveWebToken_cookieFallback() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/x")
                        .cookie(new HttpCookie("token", " cookie-token "))
                        .build());

        assertEquals("cookie-token", GatewayTokenResolver.resolveWebToken(exchange));
    }

    @Test
    void resolveWebToken_missing_returnsNull() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/x").build());

        assertNull(GatewayTokenResolver.resolveWebToken(exchange));
    }

    @Test
    void resolveAdminToken_headerWins() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/admin-api/x")
                        .header("adminToken", "admin-token")
                        .cookie(new HttpCookie("adminToken", "cookie-admin"))
                        .build());

        assertEquals("admin-token", GatewayTokenResolver.resolveAdminToken(exchange));
    }

    @Test
    void resolveAdminToken_cookieFallback() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/admin-api/x")
                        .cookie(new HttpCookie("adminToken", "cookie-admin"))
                        .build());

        assertEquals("cookie-admin", GatewayTokenResolver.resolveAdminToken(exchange));
    }

    @Test
    void resolveAdminToken_blankHeader_ignored() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/admin-api/x")
                        .header("adminToken", "  ")
                        .cookie(new HttpCookie("adminToken", "cookie-admin"))
                        .build());

        assertEquals("cookie-admin", GatewayTokenResolver.resolveAdminToken(exchange));
    }
}
