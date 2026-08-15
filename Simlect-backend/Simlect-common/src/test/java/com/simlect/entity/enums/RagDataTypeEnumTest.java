package com.simlect.entity.enums;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagDataTypeEnumTest {

    @Test
    void constants_exposeTypeAndDesc() {
        assertEquals("product", RagDataTypeEnum.PRODUCT.getType());
        assertEquals("商品数据", RagDataTypeEnum.PRODUCT.getDesc());
        assertEquals("faq", RagDataTypeEnum.FAQ.getType());
        assertEquals("FAQ数据", RagDataTypeEnum.FAQ.getDesc());
    }

    @Test
    void getByType_hit() {
        assertEquals(RagDataTypeEnum.PRODUCT, RagDataTypeEnum.getByType("product"));
        assertEquals(RagDataTypeEnum.FAQ, RagDataTypeEnum.getByType("faq"));
    }

    @Test
    void getByType_miss_throwsNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> RagDataTypeEnum.getByType("other"));
    }
}
