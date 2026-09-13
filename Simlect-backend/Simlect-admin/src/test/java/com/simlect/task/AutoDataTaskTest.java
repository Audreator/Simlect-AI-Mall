package com.simlect.task;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.simlect.biz.StatisticsInfoService;

class AutoDataTaskTest {

    @Test
    void statisticsRunsOnScheduleButNotDuringApplicationStartup() throws Exception {
        Method method = AutoDataTask.class.getMethod("autoCountYesterdayData");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertNotNull(scheduled);
        org.junit.jupiter.api.Assertions.assertEquals("Asia/Shanghai", scheduled.zone());
        assertFalse(method.isAnnotationPresent(PostConstruct.class));
    }

    @Test
    void applicationReadyTriggersNonBlockingCatchUp() throws Exception {
        Method method = AutoDataTask.class.getMethod("catchUpAfterApplicationReady");
        EventListener listener = method.getAnnotation(EventListener.class);

        assertNotNull(listener);
        assertDoesNotThrow(() -> invokeCatchUpWithFailingService());
    }

    @Test
    void dailyRunRecomputesFixedLookbackWindowWithoutOneAmOverlap() {
        StatisticsInfoService service = mock(StatisticsInfoService.class);
        AutoDataTask task = taskWith(service);

        task.runDailyStatistics(LocalDate.of(2026, 9, 12));

        verify(service).statistics("2026-08-29 01:00:00", "2026-09-12 00:59:59");
    }

    @Test
    void nextDailyRunRetriesWindowCoveredByPreviousFailure() {
        StatisticsInfoService service = mock(StatisticsInfoService.class);
        doThrow(new RuntimeException("temporary outage")).doNothing()
                .when(service).statistics(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
        AutoDataTask task = taskWith(service);

        task.runDailyStatistics(LocalDate.of(2026, 9, 12));
        task.runDailyStatistics(LocalDate.of(2026, 9, 13));

        verify(service).statistics("2026-08-29 01:00:00", "2026-09-12 00:59:59");
        verify(service).statistics("2026-08-30 01:00:00", "2026-09-13 00:59:59");
    }

    @Test
    void startupCatchUpRecomputesLastFourteenCompletedBusinessDays() {
        StatisticsInfoService service = mock(StatisticsInfoService.class);
        AutoDataTask task = taskWith(service);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));

        task.catchUpAfterApplicationReady();

        verify(service).statistics(
                today.minusDays(14) + " 01:00:00",
                today + " 00:59:59");
    }

    private void invokeCatchUpWithFailingService() {
        StatisticsInfoService service = mock(StatisticsInfoService.class);
        doThrow(new RuntimeException("order unavailable"))
                .when(service).statistics(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
        taskWith(service).catchUpAfterApplicationReady();
    }

    private AutoDataTask taskWith(StatisticsInfoService service) {
        AutoDataTask task = new AutoDataTask();
        ReflectionTestUtils.setField(task, "statisticsInfoService", service);
        return task;
    }
}
