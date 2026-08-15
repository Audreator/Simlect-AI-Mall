package com.simlect.task;

import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.service.OutboxMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatchTaskTest {

    private static final String LOCK_KEY = Constants.REDIS_KEY_MQ_COMPENSATE + "outbox:dispatch:lock";

    @Mock
    private OutboxMessageService outboxMessageService;
    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private OutboxDispatchTask task;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(task, "batchSize", 30);
        ReflectionTestUtils.setField(task, "maxRetries", 10);
    }

    @Test
    void dispatch_lockNotAcquired_skipsServiceCall() {
        when(redisComponent.setIfAbsent(LOCK_KEY, "1", 4, TimeUnit.SECONDS)).thenReturn(false);

        task.dispatch();

        verify(outboxMessageService, never()).dispatchPendingBatch(anyInt(), anyInt());
    }

    @Test
    void dispatch_lockAcquired_dispatchesPending() {
        when(redisComponent.setIfAbsent(LOCK_KEY, "1", 4, TimeUnit.SECONDS)).thenReturn(true);
        when(outboxMessageService.dispatchPendingBatch(30, 10)).thenReturn(5);

        task.dispatch();

        verify(outboxMessageService).dispatchPendingBatch(30, 10);
    }

    @Test
    void dispatch_lockAcquired_zeroDispatched() {
        when(redisComponent.setIfAbsent(LOCK_KEY, "1", 4, TimeUnit.SECONDS)).thenReturn(true);
        when(outboxMessageService.dispatchPendingBatch(30, 10)).thenReturn(0);

        task.dispatch();

        verify(outboxMessageService).dispatchPendingBatch(30, 10);
    }

    @Test
    void dispatch_serviceThrows_exceptionSwallowed() {
        when(redisComponent.setIfAbsent(LOCK_KEY, "1", 4, TimeUnit.SECONDS)).thenReturn(true);
        when(outboxMessageService.dispatchPendingBatch(30, 10))
                .thenThrow(new RuntimeException("table not exists"));

        assertDoesNotThrow(task::dispatch);
    }
}
