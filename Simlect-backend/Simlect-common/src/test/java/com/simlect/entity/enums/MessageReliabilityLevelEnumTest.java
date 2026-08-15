package com.simlect.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageReliabilityLevelEnumTest {

    @Test
    void high_exposesCodeAndDescription() {
        assertEquals("high", MessageReliabilityLevelEnum.HIGH.getCode());
        assertEquals("高并发 - 异步刷盘+补偿", MessageReliabilityLevelEnum.HIGH.getDescription());
    }

    @Test
    void standard_exposesCodeAndDescription() {
        assertEquals("standard", MessageReliabilityLevelEnum.STANDARD.getCode());
        assertEquals("同步刷盘+重试", MessageReliabilityLevelEnum.STANDARD.getDescription());
    }
}
