package com.simlect.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageSizeTest {

    @Test
    void allConstants_exposeSize() {
        assertEquals(15, PageSize.SIZE15.getSize());
        assertEquals(20, PageSize.SIZE20.getSize());
        assertEquals(30, PageSize.SIZE30.getSize());
        assertEquals(40, PageSize.SIZE40.getSize());
        assertEquals(50, PageSize.SIZE50.getSize());
    }
}
