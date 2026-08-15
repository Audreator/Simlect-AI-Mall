package com.simlect.task;

import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.service.MqCompensationLogService;
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
class MqCompensationAutoReplayTaskTest {

    private static final String LOCK_KEY = Constants.REDIS_KEY_MQ_COMPENSATE_AUTO_REPLAY_LOCK;

    @Mock
    private MqCompensationLogService mqCompensationLogService;
    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private MqCompensationAutoReplayTask task;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(task, "batchSize", 10);
        ReflectionTestUtils.setField(task, "maxRetries", 5);
    }

    @Test
    void autoReplay_lockNotAcquired_skipsServiceCall() {
        when(redisComponent.setIfAbsent(LOCK_KEY, "1", 55, TimeUnit.SECONDS)).thenReturn(false);

        task.autoReplay();

        verify(mqCompensationLogService, never()).autoReplayPendingSendFailures(anyInt(), anyInt());
    }

    @Test
    void autoReplay_lockAcquired_replaysPending() {
        when(redisComponent.setIfAbsent(LOCK_KEY, "1", 55, TimeUnit.SECONDS)).thenReturn(true);
        when(mqCompensationLogService.autoReplayPendingSendFailures(10, 5)).thenReturn(3);

        task.autoReplay();

        verify(mqCompensationLogService).autoReplayPendingSendFailures(10, 5);
    }

    @Test
    void autoReplay_lockAcquired_serviceReturnsZero_noLoggingIssue() {
        when(redisComponent.setIfAbsent(LOCK_KEY, "1", 55, TimeUnit.SECONDS)).thenReturn(true);
        when(mqCompensationLogService.autoReplayPendingSendFailures(10, 5)).thenReturn(0);

        task.autoReplay();

        verify(mqCompensationLogService).autoReplayPendingSendFailures(10, 5);
    }

    @Test
    void autoReplay_serviceThrows_exceptionSwallowed() {
        when(redisComponent.setIfAbsent(LOCK_KEY, "1", 55, TimeUnit.SECONDS)).thenReturn(true);
        when(mqCompensationLogService.autoReplayPendingSendFailures(10, 5))
                .thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(task::autoReplay);
    }
}
