package com.simlect.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MqCompensationLogStatusEnumTest {

    @Test
    void allConstants_exposeStatusAndDesc() {
        assertEquals(0, MqCompensationLogStatusEnum.PENDING.getStatus());
        assertEquals("待处理", MqCompensationLogStatusEnum.PENDING.getDesc());
        assertEquals(1, MqCompensationLogStatusEnum.PROCESSING.getStatus());
        assertEquals(2, MqCompensationLogStatusEnum.REPLAYED.getStatus());
        assertEquals(3, MqCompensationLogStatusEnum.REPLAY_FAILED.getStatus());
        assertEquals("已忽略", MqCompensationLogStatusEnum.IGNORED.getDesc());
    }

    @Test
    void getByStatus_hit() {
        assertEquals(MqCompensationLogStatusEnum.PENDING, MqCompensationLogStatusEnum.getByStatus(0));
        assertEquals(MqCompensationLogStatusEnum.PROCESSING, MqCompensationLogStatusEnum.getByStatus(1));
        assertEquals(MqCompensationLogStatusEnum.REPLAYED, MqCompensationLogStatusEnum.getByStatus(2));
        assertEquals(MqCompensationLogStatusEnum.REPLAY_FAILED, MqCompensationLogStatusEnum.getByStatus(3));
        assertEquals(MqCompensationLogStatusEnum.IGNORED, MqCompensationLogStatusEnum.getByStatus(4));
    }

    @Test
    void getByStatus_missAndNull_returnsNull() {
        assertNull(MqCompensationLogStatusEnum.getByStatus(99));
        assertNull(MqCompensationLogStatusEnum.getByStatus(null));
    }
}
