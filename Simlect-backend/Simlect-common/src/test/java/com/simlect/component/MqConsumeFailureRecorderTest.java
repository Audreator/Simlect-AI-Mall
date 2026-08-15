package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.entity.dto.MqCompensationRecord;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.service.MqCompensationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqConsumeFailureRecorderTest {

    @Mock
    private MqCompensationStore mqCompensationStore;
    @Mock
    private MqCompensationLogService mqCompensationLogService;
    @Mock
    private MqConsumerIdempotencyHelper mqConsumerIdempotencyHelper;

    @InjectMocks
    private MqConsumeFailureRecorder recorder;

    private Message messageWithDeliveryTag() {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(123L);
        return new Message(new byte[]{1}, props);
    }

    @Test
    void record_buildsCompensationRecord_andPersists() {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn("msg-key");
        Exception error = new RuntimeException("boom");

        recorder.record("pay.queue", messageWithDeliveryTag(), "payload", error);

        ArgumentCaptor<MqCompensationRecord> captor = ArgumentCaptor.forClass(MqCompensationRecord.class);
        verify(mqCompensationLogService).saveFromFailure(captor.capture());
        verify(mqCompensationStore).saveToRedis(captor.getValue());
        MqCompensationRecord record = captor.getValue();
        assertEquals(Constants.MQ_CONSUME_FAILURE_EXCHANGE, record.getExchange());
        assertEquals("pay.queue", record.getRoutingKey());
        assertEquals("consume:pay.queue:msg-key", record.getIdempotencyKey());
        assertEquals("payload", record.getPayload());
        assertEquals(MessageReliabilityLevelEnum.STANDARD, record.getReliabilityLevel());
        assertEquals("boom", record.getErrorMessage());
        assertNotNull(record.getFailedAt());
    }

    @Test
    void record_withoutIdempotencyKey_usesDeliveryTag() {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn(null);

        recorder.record("pay.queue", messageWithDeliveryTag(), "payload", null);

        ArgumentCaptor<MqCompensationRecord> captor = ArgumentCaptor.forClass(MqCompensationRecord.class);
        verify(mqCompensationLogService).saveFromFailure(captor.capture());
        assertEquals("consume:pay.queue:123", captor.getValue().getIdempotencyKey());
        assertEquals("consume failed", captor.getValue().getErrorMessage());
    }

    @Test
    void record_emptyQueueName_returnsImmediately() {
        recorder.record("", messageWithDeliveryTag(), "payload", null);
        recorder.record(null, messageWithDeliveryTag(), "payload", null);

        verifyNoInteractions(mqCompensationLogService, mqCompensationStore, mqConsumerIdempotencyHelper);
    }

    @Test
    void record_serviceThrows_exceptionSwallowed() {
        when(mqConsumerIdempotencyHelper.resolveIdempotencyKey(any())).thenReturn("msg-key");
        doThrow(new RuntimeException("db down")).when(mqCompensationLogService).saveFromFailure(any());

        recorder.record("pay.queue", messageWithDeliveryTag(), "payload", null);

        verify(mqCompensationStore, never()).saveToRedis(any());
    }
}
