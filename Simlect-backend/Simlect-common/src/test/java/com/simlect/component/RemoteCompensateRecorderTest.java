package com.simlect.component;

import com.simlect.constants.InternalApiHeaders;
import com.simlect.entity.dto.MqCompensationRecord;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.po.ProductItem;
import com.simlect.service.MqCompensationLogService;
import com.simlect.service.impl.MqCompensationLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RemoteCompensateRecorderTest {

    @Mock
    private MqCompensationLogService mqCompensationLogService;

    @InjectMocks
    private RemoteCompensateRecorder recorder;

    private ProductItem item() {
        ProductItem item = new ProductItem();
        item.setProductId("P1");
        item.setBuyCount(2);
        return item;
    }

    @Test
    void recordStockChangeBatch_persistsHighLevelRecord() {
        recorder.recordStockChangeBatch("biz-1", "op-1", List.of(item()), new RuntimeException("stock err"));

        ArgumentCaptor<MqCompensationRecord> captor = ArgumentCaptor.forClass(MqCompensationRecord.class);
        verify(mqCompensationLogService).saveFromFailure(captor.capture());
        MqCompensationRecord record = captor.getValue();
        assertEquals(77, record.getIdempotencyKey().length());
        assertEquals(true, record.getIdempotencyKey().startsWith("remote:stock:"));
        assertEquals(InternalApiHeaders.REMOTE_COMPENSATE_EXCHANGE, record.getExchange());
        assertEquals(InternalApiHeaders.REMOTE_STOCK_CHANGE_BATCH, record.getRoutingKey());
        assertEquals(MessageReliabilityLevelEnum.HIGH, record.getReliabilityLevel());
        assertEquals(0, record.getRetryCount());
        assertEquals("stock err", record.getErrorMessage());
        assertNotNull(record.getPayload());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) record.getPayload();
        assertEquals("op-1", payload.get("operationId"));
        assertEquals(1, ((List<?>) payload.get("items")).size());
        assertNotNull(record.getFailedAt());
    }

    @Test
    void recordStockChangeBatch_nullError_usesDefaultMessage() {
        recorder.recordStockChangeBatch("biz-1", "op-1", List.of(item()), null);

        ArgumentCaptor<MqCompensationRecord> captor = ArgumentCaptor.forClass(MqCompensationRecord.class);
        verify(mqCompensationLogService).saveFromFailure(captor.capture());
        assertEquals("remote compensate failed", captor.getValue().getErrorMessage());
    }

    @Test
    void recordStockChangeBatch_emptyArgs_skips() {
        recorder.recordStockChangeBatch(null, "op-1", List.of(item()), null);
        recorder.recordStockChangeBatch("biz-1", null, List.of(item()), null);
        recorder.recordStockChangeBatch("biz-1", "op-1", null, null);
        recorder.recordStockChangeBatch("biz-1", "op-1", List.of(), null);

        verifyNoInteractions(mqCompensationLogService);
    }

    @Test
    void recordCouponUnlock_persistsPayloadMap() {
        recorder.recordCouponUnlock("biz-2", "uc-9", "u-7", 1, 2, new RuntimeException("unlock err"));

        ArgumentCaptor<MqCompensationRecord> captor = ArgumentCaptor.forClass(MqCompensationRecord.class);
        verify(mqCompensationLogService).saveFromFailure(captor.capture());
        MqCompensationRecord record = captor.getValue();
        assertEquals("remote:coupon:unlock:biz-2", record.getIdempotencyKey());
        assertEquals(InternalApiHeaders.REMOTE_COUPON_UNLOCK, record.getRoutingKey());
        assertEquals(MessageReliabilityLevelEnum.HIGH, record.getReliabilityLevel());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) record.getPayload();
        assertEquals("uc-9", payload.get("userCouponId"));
        assertEquals("u-7", payload.get("userId"));
        assertEquals(1, payload.get("fromStatus"));
        assertEquals(2, payload.get("toStatus"));
    }

    @Test
    void recordCouponUnlock_emptyArgs_skips() {
        recorder.recordCouponUnlock(null, "uc-9", "u-7", 1, 2, null);
        recorder.recordCouponUnlock("biz-2", "", "u-7", 1, 2, null);

        verifyNoInteractions(mqCompensationLogService);
    }

    @Test
    void persist_serviceThrows_exceptionSwallowed() {
        doThrow(new RuntimeException("db down")).when(mqCompensationLogService).saveFromFailure(any());

        recorder.recordStockChangeBatch("biz-1", "op-1", List.of(item()), null);
    }

    @Test
    void stockCompensationKey_isBoundedForMaxOperationId() {
        recorder.recordStockChangeBatch("biz-1", "x".repeat(128), List.of(item()), null);

        ArgumentCaptor<MqCompensationRecord> captor = ArgumentCaptor.forClass(MqCompensationRecord.class);
        verify(mqCompensationLogService).saveFromFailure(captor.capture());
        assertEquals(77, captor.getValue().getIdempotencyKey().length());
    }

    @Test
    void saveFromFailure_usesIndependentTransaction() throws Exception {
        Transactional transactional = MqCompensationLogServiceImpl.class
                .getMethod("saveFromFailure", MqCompensationRecord.class)
                .getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }
}
