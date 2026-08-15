package com.simlect.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsDataTypeEnumTest {

    @Test
    void allConstants_exposeTypeAndDesc() {
        assertEquals(1, StatisticsDataTypeEnum.SALE_AMOUNT.getType());
        assertEquals("销售金额", StatisticsDataTypeEnum.SALE_AMOUNT.getDesc());
        assertEquals(2, StatisticsDataTypeEnum.SALE_COUNT.getType());
        assertEquals(3, StatisticsDataTypeEnum.REFUND_AMOUNT.getType());
        assertEquals(4, StatisticsDataTypeEnum.REFUND_COUNT.getType());
        assertEquals("退款数量", StatisticsDataTypeEnum.REFUND_COUNT.getDesc());
    }
}
