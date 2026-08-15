package com.simlect.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqIdempotencyKeysTest {

    @Test
    void ragProduct_buildsKey() {
        assertEquals("rag:product:P1", MqIdempotencyKeys.ragProduct("P1"));
    }

    @Test
    void ragFaq_buildsKey() {
        assertEquals("rag:faq:Q1", MqIdempotencyKeys.ragFaq("Q1"));
    }

    @Test
    void payTimeout_buildsKey() {
        assertEquals("pay:timeout:O1", MqIdempotencyKeys.payTimeout("O1"));
    }

    @Test
    void payLogistics_buildsKey() {
        assertEquals("pay:logistics:O1:step:0", MqIdempotencyKeys.payLogistics("O1"));
        assertEquals("pay:logistics:O1:step:3", MqIdempotencyKeys.payLogistics("O1", 3));
    }

    @Test
    void payConfirm_buildsKey() {
        assertEquals("pay:confirm:O1", MqIdempotencyKeys.payConfirm("O1"));
    }

    @Test
    void browseRecord_buildsKey() {
        assertEquals("browse:U1:P1", MqIdempotencyKeys.browseRecord("U1", "P1"));
    }

    @Test
    void signRecord_buildsKey() {
        assertEquals("sign:record:U1:20260801", MqIdempotencyKeys.signRecord("U1", "20260801"));
    }

    @Test
    void notification_buildsKey() {
        assertEquals("notify:U1:order:O1", MqIdempotencyKeys.notification("U1", "order", "O1"));
        assertEquals("notify:U1::", MqIdempotencyKeys.notification("U1", null, null));
    }

    @Test
    void tempBanUnban_buildsKey() {
        assertEquals("tempban:unban:U1:123456789", MqIdempotencyKeys.tempBanUnban("U1", 123456789L));
    }

    @Test
    void emptyArgument_throws() {
        assertThrows(IllegalArgumentException.class, () -> MqIdempotencyKeys.ragProduct(null));
        assertThrows(IllegalArgumentException.class, () -> MqIdempotencyKeys.ragFaq(""));
        assertThrows(IllegalArgumentException.class, () -> MqIdempotencyKeys.payTimeout(null));
        assertThrows(IllegalArgumentException.class, () -> MqIdempotencyKeys.payLogistics(""));
        assertThrows(IllegalArgumentException.class, () -> MqIdempotencyKeys.payConfirm(null));
        assertThrows(IllegalArgumentException.class, () -> MqIdempotencyKeys.browseRecord("U1", ""));
        assertThrows(IllegalArgumentException.class, () -> MqIdempotencyKeys.signRecord(null, "20260801"));
        assertThrows(IllegalArgumentException.class, () -> MqIdempotencyKeys.notification(null, "order", "O1"));
        assertThrows(IllegalArgumentException.class, () -> MqIdempotencyKeys.tempBanUnban("", 1L));
    }
}
