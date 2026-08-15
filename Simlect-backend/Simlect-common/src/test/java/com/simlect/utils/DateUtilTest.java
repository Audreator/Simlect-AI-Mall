package com.simlect.utils;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日期工具。
 */
class DateUtilTest {

    @Test
    void format_basicPattern() {
        Date d = new Date(1700000000000L); // 2023-11-14 22:13:20 UTC
        String s = DateUtil.format(d, "yyyy-MM-dd");
        assertEquals(10, s.length());
        assertEquals(s, DateUtil.format(d, "yyyy-MM-dd"));
    }

    @Test
    void parse_returnsDateForValidInput() {
        Date d = DateUtil.parse("2024-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss");
        assertEquals("2024-01-01 12:00:00", DateUtil.format(d, "yyyy-MM-dd HH:mm:ss"));
    }

    @Test
    void parse_invalidInput_throwsBusinessException() {
        assertThrows(com.simlect.exception.BusinessException.class,
                () -> DateUtil.parse("not-a-date", "yyyy-MM-dd HH:mm:ss"));
        assertThrows(com.simlect.exception.BusinessException.class,
                () -> DateUtil.parse("2024-13-99 25:00:00", "yyyy-MM-dd HH:mm:ss"));
    }

    @Test
    void getMinAfter_formatsFutureMinutes() {
        String s = DateUtil.getMinAfter(30, "HH:mm");
        assertTrue(s.matches("\\d{2}:\\d{2}"));
    }

    @Test
    void getTimeOnParttern_formatsPastDays() {
        String s = DateUtil.getTimeOnParttern(3, "yyyy-MM-dd");
        assertTrue(s.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void isAfterOneAM_variousTimes() {
        assertTrue(DateUtil.isAfterOneAM("01:00:00"));
        assertTrue(DateUtil.isAfterOneAM("12:30:00"));
        assertFalse(DateUtil.isAfterOneAM("00:59:59"));
        assertFalse(DateUtil.isAfterOneAM(null));
        assertFalse(DateUtil.isAfterOneAM(""));
        // 完整日期时间格式提取时分秒
        assertTrue(DateUtil.isAfterOneAM("2024-01-01 02:00:00"));
        assertFalse(DateUtil.isAfterOneAM("2024-01-01 00:30:00"));
        // 非法格式
        assertFalse(DateUtil.isAfterOneAM("not-a-time"));
    }

    @Test
    void isDayDifferenceAtLeastOne_variousPairs() {
        assertTrue(DateUtil.isDayDifferenceAtLeastOne("2024-01-02 00:00:00", "2024-01-01 00:00:00"));
        assertTrue(DateUtil.isDayDifferenceAtLeastOne("2024-01-03 00:00:00", "2024-01-01 23:59:59"));
        assertFalse(DateUtil.isDayDifferenceAtLeastOne("2024-01-01 00:00:00", "2024-01-02 00:00:00"));
        assertFalse(DateUtil.isDayDifferenceAtLeastOne("2024-01-01 10:00:00", "2024-01-01 20:00:00"));
        assertFalse(DateUtil.isDayDifferenceAtLeastOne(null, "2024-01-02 00:00:00"));
        assertFalse(DateUtil.isDayDifferenceAtLeastOne("2024-01-02 00:00:00", null));
    }
}

