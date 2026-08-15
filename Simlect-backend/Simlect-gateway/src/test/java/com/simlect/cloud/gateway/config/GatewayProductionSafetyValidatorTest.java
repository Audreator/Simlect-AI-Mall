package com.simlect.cloud.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GatewayProductionSafetyValidatorTest {

    @Test
    void validate_notProductionReady_returns() {
        GatewayProductionSafetyValidator validator = new GatewayProductionSafetyValidator();
        ReflectionTestUtils.setField(validator, "productionReady", false);
        ReflectionTestUtils.setField(validator, "internalToken", "your-token");

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_productionReady_weakToken_throws() {
        GatewayProductionSafetyValidator validator = new GatewayProductionSafetyValidator();
        ReflectionTestUtils.setField(validator, "productionReady", true);
        ReflectionTestUtils.setField(validator, "internalToken", "your-token");

        IllegalStateException e = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(e.getMessage().contains("生产就绪校验失败"));
    }

    @Test
    void validate_productionReady_emptyToken_throws() {
        GatewayProductionSafetyValidator validator = new GatewayProductionSafetyValidator();
        ReflectionTestUtils.setField(validator, "productionReady", true);
        ReflectionTestUtils.setField(validator, "internalToken", "  ");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validate_productionReady_strongToken_ok() {
        GatewayProductionSafetyValidator validator = new GatewayProductionSafetyValidator();
        ReflectionTestUtils.setField(validator, "productionReady", true);
        ReflectionTestUtils.setField(validator, "internalToken", "S3cure-Token-123!");

        assertDoesNotThrow(validator::validate);
    }
}

class GatewayAuthPropertiesTest {

    @Test
    void defaults() {
        GatewayAuthProperties properties = new GatewayAuthProperties();
        assertTrue(properties.isEnabled());
        assertNotNull(properties.getWebExcludePaths());
        assertNotNull(properties.getAdminExcludePaths());
    }

    @Test
    void setterRoundTrip() {
        GatewayAuthProperties properties = new GatewayAuthProperties();
        properties.setEnabled(false);
        properties.setWebExcludePaths(List.of("/api/account/login"));
        properties.setAdminExcludePaths(List.of("/admin-api/account/login"));

        assertFalse(properties.isEnabled());
        assertEquals(List.of("/api/account/login"), properties.getWebExcludePaths());
        assertEquals(List.of("/admin-api/account/login"), properties.getAdminExcludePaths());
    }
}

class GatewayInternalPropertiesTest {

    @Test
    void defaults() {
        GatewayInternalProperties properties = new GatewayInternalProperties();
        // 安全默认：令牌默认空（未配置时 /internal/** 一律 401，禁止默认弱值）
        assertEquals("", properties.getToken());
        assertTrue(properties.isAuthEnabled());
    }

    @Test
    void setterRoundTrip() {
        GatewayInternalProperties properties = new GatewayInternalProperties();
        properties.setToken("strong-token");
        properties.setAuthEnabled(false);

        assertEquals("strong-token", properties.getToken());
        assertFalse(properties.isAuthEnabled());
    }
}

class GatewayRateLimitPropertiesTest {

    @Test
    void defaults() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        assertTrue(properties.isEnabled());
        assertEquals(200, properties.getDefaultQps());
        assertEquals(30, properties.getAuthQps());
    }

    @Test
    void setterRoundTrip() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(false);
        properties.setDefaultQps(500);
        properties.setAuthQps(50);

        assertFalse(properties.isEnabled());
        assertEquals(500, properties.getDefaultQps());
        assertEquals(50, properties.getAuthQps());
    }
}
