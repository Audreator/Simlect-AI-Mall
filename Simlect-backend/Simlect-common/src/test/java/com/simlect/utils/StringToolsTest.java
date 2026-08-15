package com.simlect.utils;

import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通用字符串工具。
 */
class StringToolsTest {

    @Test
    void isEmpty_variousInputs() {
        assertTrue(StringTools.isEmpty(null));
        assertTrue(StringTools.isEmpty(""));
        assertTrue(StringTools.isEmpty("null"));
        assertTrue(StringTools.isEmpty("\u0000"));
        assertTrue(StringTools.isEmpty("   "));
        assertFalse(StringTools.isEmpty("a"));
        assertFalse(StringTools.isEmpty(" null "));
    }

    @Test
    void upperCaseFirstLetter_rules() {
        assertEquals("Name", StringTools.upperCaseFirstLetter("name"));
        assertEquals("", StringTools.upperCaseFirstLetter(""));
        assertNull(StringTools.upperCaseFirstLetter(null));
        // 第二个字母大写时不转首字母（保持原样）
        assertEquals("aId", StringTools.upperCaseFirstLetter("aId"));
        assertEquals("Name2", StringTools.upperCaseFirstLetter("name2"));
    }

    @Test
    void getFileSuffix_extractsSuffixWithDot() {
        assertEquals(".jpg", StringTools.getFileSuffix("a.jpg"));
        assertEquals(".png", StringTools.getFileSuffix("b.logo.png"));
        assertEquals("", StringTools.getFileSuffix("noext"));
        assertEquals("", StringTools.getFileSuffix("trailing."));
        assertEquals("", StringTools.getFileSuffix(null));
        assertEquals("", StringTools.getFileSuffix(""));
    }

    @Test
    void getRandomString_lengthAndCharset() {
        String s = StringTools.getRandomString(10);
        assertNotNull(s);
        assertEquals(10, s.length());
        String n = StringTools.getRandomNumber(8);
        assertTrue(n.matches("\\d{8}"));
    }

    @Test
    void encodeByMD5_emptyReturnsNull() {
        assertNull(StringTools.encodeByMD5(null));
        assertNull(StringTools.encodeByMD5(""));
        assertEquals("900150983cd24fb0d6963f7d28e17f72",
                StringTools.encodeByMD5("abc"));
    }

    @Test
    void pathIsOK_blocksParentDirTraversal() {
        assertTrue(StringTools.pathIsOK("a/b/c.txt"));
        assertFalse(StringTools.pathIsOK("../etc/passwd"));
        assertFalse(StringTools.pathIsOK("a/../../b"));
    }

    @Test
    void idGenerators_formatAndLength() {
        assertTrue(StringTools.createOrderId().length() >= 17);
        assertEquals(30, StringTools.createPayOrderId().length());
        assertTrue(StringTools.createPayOrderId().matches("\\d{30}"));
        assertTrue(StringTools.createCouponId().startsWith("CP"));
        assertEquals(30, StringTools.createUserCouponId().length());
        assertTrue(StringTools.createNotificationId().startsWith("N"));
        assertEquals(31, StringTools.createNotificationId().length());
        assertTrue(StringTools.createTradeId().startsWith("T"));
        assertTrue(StringTools.createTradeId().length() >= 18);
    }

    @Test
    void checkParam_allFieldsEmpty_throws() {
        class EmptyBean {
            private String name;
            private Integer age;

            public String getName() {
                return name;
            }

            public Integer getAge() {
                return age;
            }
        }
        assertThrows(BusinessException.class, () -> StringTools.checkParam(new EmptyBean()));
    }

    @Test
    void checkParam_anyFieldNonEmpty_passes() {
        class NonEmptyBean {
            private String name;

            public String getName() {
                return "x";
            }
        }
        assertDoesNotThrow(() -> StringTools.checkParam(new NonEmptyBean()));
    }

    @Test
    void getCurrentDate_notNull() {
        assertNotNull(StringTools.getCurrentDate());
    }
}

