package com.simlect.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimePatternEnumTest {

    @Test
    void allConstants_exposeExpectedPattern() {
        assertEquals("yyyy-MM-dd HH:mm:ss", DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern());
        assertEquals("yyyy-MM-dd", DateTimePatternEnum.YYYY_MM_DD.getPattern());
        assertEquals("yyyy-MM", DateTimePatternEnum.YYYYMM.getPattern());
    }
}
