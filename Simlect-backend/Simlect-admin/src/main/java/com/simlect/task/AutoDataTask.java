package com.simlect.task;

import com.simlect.entity.enums.DateTimePatternEnum;
import com.simlect.biz.StatisticsInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class AutoDataTask {

    private static final int CATCH_UP_DAYS = 14;
    private static final String BUSINESS_ZONE_ID = "Asia/Shanghai";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of(BUSINESS_ZONE_ID);
    private static final DateTimeFormatter DAY_FORMATTER =
            DateTimeFormatter.ofPattern(DateTimePatternEnum.YYYY_MM_DD.getPattern());

    @Resource
    private StatisticsInfoService statisticsInfoService;

    // 每天的凌晨一点自动统计昨天的数据
    @Scheduled(cron = "0 0 1 * * ?", zone = BUSINESS_ZONE_ID)
    public void autoCountYesterdayData(){
        runDailyStatistics(LocalDate.now(BUSINESS_ZONE));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpAfterApplicationReady() {
        runCatchUpStatistics(LocalDate.now(BUSINESS_ZONE), "启动补算");
    }

    void runDailyStatistics(LocalDate today) {
        // 每天重算一个有界窗口：既覆盖昨天，也能自动修复进程未重启时的瞬时失败。
        runCatchUpStatistics(today, "每日补算");
    }

    private void runCatchUpStatistics(LocalDate today, String scene) {
        runStatistics(today.minusDays(CATCH_UP_DAYS), today, scene);
    }

    private void runStatistics(LocalDate startDay, LocalDate endDay, String scene) {
        String start = startDay.format(DAY_FORMATTER) + " 01:00:00";
        // order_time is DATETIME (second precision). Ending at 00:59:59 makes
        // consecutive 01:00 business-day windows disjoint despite an inclusive query.
        String end = endDay.format(DAY_FORMATTER) + " 00:59:59";
        try {
            statisticsInfoService.statistics(start, end);
            log.info("{}完成，范围 {} ~ {}", scene, start, end);
        } catch (Exception e) {
            // 启动补算和单次调度失败都不能拖垮管理服务；下一次启动或日调度会重算同一有界窗口。
            log.error("{}失败，范围 {} ~ {}，将在下次启动或调度时重试", scene, start, end, e);
        }
    }
}
