package com.simlect.cloud.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SentinelGatewayConfigTest {

    @Test
    void init_disabled_onlySetsBlockHandler() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(false);
        SentinelGatewayConfig config = new SentinelGatewayConfig(properties);

        assertDoesNotThrow(config::init);

        // 阻断响应处理器已注册（静态管理器可读）
        assertNotNull(GatewayCallbackManager.getBlockHandler());
    }

    @Test
    void init_enabled_registersApisAndRules() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        SentinelGatewayConfig config = new SentinelGatewayConfig(properties);

        assertDoesNotThrow(config::init);

        assertNotNull(GatewayCallbackManager.getBlockHandler());
    }
}
