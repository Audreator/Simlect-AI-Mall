package com.simlect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 根包冒烟测试，保证 `-Dtest=com.simlect.*Test` 过滤下仍有可执行用例（surefire 通配符不跨包层级）。
 * 全量测试请使用 `mvn test`（或 `-Dtest=*Test`）。
 */
class SmokeTest {

    @Test
    void gatewayModuleClassesLoadable() {
        assertNotNull(com.simlect.cloud.gateway.GatewayApplication.class);
    }
}
