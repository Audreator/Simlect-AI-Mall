package com.simlect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 根包冒烟测试。
 * <p>
 * 说明：surefire 的 -Dtest 通配符「*」不跨包层级分隔符「.」，
 * 因此 `-Dtest=com.simlect.*Test` 只能匹配到直接位于 com.simlect 包下的测试类。
 * 该类用于保证该过滤参数下 Maven 仍能发现至少一个测试并 BUILD SUCCESS；
 * 全量测试请使用 `mvn test`（或 `-Dtest=*Test`）。
 */
class SmokeTest {

    @Test
    void userModuleClassesLoadable() {
        assertNotNull(com.simlect.cloud.UserApplication.class);
    }
}
