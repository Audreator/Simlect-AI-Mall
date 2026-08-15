package com.simlect.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxMessageStatusEnumTest {

    @Test
    void allConstants_exposeStatusAndDesc() {
        assertEquals(0, OutboxMessageStatusEnum.PENDING.getStatus());
        assertEquals("待发送", OutboxMessageStatusEnum.PENDING.getDesc());
        assertEquals(1, OutboxMessageStatusEnum.SENDING.getStatus());
        assertEquals(2, OutboxMessageStatusEnum.SENT.getStatus());
        assertEquals("已发送", OutboxMessageStatusEnum.SENT.getDesc());
        assertEquals(3, OutboxMessageStatusEnum.FAILED.getStatus());
        assertEquals("失败", OutboxMessageStatusEnum.FAILED.getDesc());
    }
}
