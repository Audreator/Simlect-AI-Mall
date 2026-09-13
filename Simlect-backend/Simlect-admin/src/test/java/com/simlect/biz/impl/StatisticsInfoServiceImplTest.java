package com.simlect.biz.impl;

import com.simlect.api.support.OrderFeignSupport;
import com.simlect.api.support.UserFeignSupport;
import com.simlect.api.vo.OrderDailyStatsVO;
import com.simlect.api.vo.OrderRangeStatsVO;
import com.simlect.entity.enums.StatisticsDataTypeEnum;
import com.simlect.entity.po.StatisticsInfo;
import com.simlect.entity.query.StatisticsInfoQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.StatisticsDataVO;
import com.simlect.entity.vo.TodayDataVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.StatisticsInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsInfoServiceImplTest {

    @Mock
    private StatisticsInfoMapper<StatisticsInfo, StatisticsInfoQuery> statisticsInfoMapper;
    @Mock
    private OrderFeignSupport orderFeignSupport;
    @Mock
    private UserFeignSupport userFeignSupport;

    private StatisticsInfoServiceImpl statisticsInfoService;

    @BeforeEach
    void setUp() {
        Clock fixedShanghaiClock = Clock.fixed(
                Instant.parse("2026-09-11T17:05:00Z"),
                ZoneId.of("Asia/Shanghai"));
        statisticsInfoService = new StatisticsInfoServiceImpl(fixedShanghaiClock);
        ReflectionTestUtils.setField(statisticsInfoService, "statisticsInfoMapper", statisticsInfoMapper);
        ReflectionTestUtils.setField(statisticsInfoService, "orderFeignSupport", orderFeignSupport);
        ReflectionTestUtils.setField(statisticsInfoService, "userFeignSupport", userFeignSupport);
    }

    @Test
    void findListByPage_delegates() {
        when(statisticsInfoMapper.selectCount(any())).thenReturn(5);
        when(statisticsInfoMapper.selectList(any())).thenReturn(new ArrayList<>());
        StatisticsInfoQuery query = new StatisticsInfoQuery();
        query.setPageNo(1);

        PaginationResultVO<StatisticsInfo> result = statisticsInfoService.findListByPage(query);

        assertEquals(5, result.getTotalCount());
        assertEquals(15, result.getPageSize());
    }

    @Test
    void getTodayData_buildsFourMetrics() {
        OrderRangeStatsVO today = new OrderRangeStatsVO();
        today.setSaleAmount(new BigDecimal("100"));
        today.setSaleOrderCount(new BigDecimal("5"));
        today.setRefundAmount(new BigDecimal("1"));
        OrderRangeStatsVO yesterday = new OrderRangeStatsVO();
        yesterday.setSaleAmount(new BigDecimal("80"));
        yesterday.setSaleOrderCount(new BigDecimal("4"));
        yesterday.setRefundAmount(new BigDecimal("0"));
        when(orderFeignSupport.aggregateRange(anyString(), anyString())).thenReturn(yesterday).thenReturn(today);
        when(userFeignSupport.countByJoinDate(anyString(), anyString())).thenReturn(3);

        List<TodayDataVO> data = statisticsInfoService.getTodayData();

        assertEquals(4, data.size());
        TodayDataVO orderAmount = data.get(0);
        assertEquals("orderAmount", orderAmount.getType());
        assertEquals(0, new BigDecimal("100").compareTo(orderAmount.getTodayValue()));
        assertEquals(0, new BigDecimal("80").compareTo(orderAmount.getYesterdayValue()));
        assertEquals(3, data.get(2).getTodayValue().intValue());
    }

    @Test
    void getTodayData_nullStats_fallsBackToZero() {
        when(orderFeignSupport.aggregateRange(anyString(), anyString())).thenReturn(null);
        when(userFeignSupport.countByJoinDate(anyString(), anyString())).thenReturn(0);

        List<TodayDataVO> data = statisticsInfoService.getTodayData();

        assertEquals(4, data.size());
        assertEquals(BigDecimal.ZERO, data.get(0).getTodayValue());
    }

    @Test
    void loadWeeklyStatisticsData_fillsMissingDaysWithZero() {
        StatisticsInfo info = new StatisticsInfo();
        info.setStatisticsDate(java.time.LocalDate.now().minusDays(1).toString());
        info.setDataType(StatisticsDataTypeEnum.SALE_AMOUNT.getType());
        info.setDataValue(new BigDecimal("42"));
        when(statisticsInfoMapper.selectList(any())).thenReturn(List.of(info));

        List<StatisticsDataVO> list = statisticsInfoService.loadWeeklyStatisticsData();

        assertEquals(4, list.size());
        StatisticsDataVO amount = list.get(0);
        assertEquals(StatisticsDataTypeEnum.SALE_AMOUNT.getType(), amount.getDataType());
        assertEquals(7, amount.getDataList().size());
        // 昨天(索引6)有值 42，其余为 0
        assertEquals(0, new BigDecimal("42").compareTo(amount.getDataList().get(6)));
        assertEquals(0, BigDecimal.ZERO.compareTo(amount.getDataList().get(0)));
    }

    @Test
    void statistics_noBuckets_returnsSilently() {
        when(orderFeignSupport.aggregateDaily("2026-08-29 01:00:00", "2026-09-12 00:59:59"))
                .thenReturn(null);

        assertDoesNotThrow(() -> statisticsInfoService.statistics(null, null));
        verify(orderFeignSupport).aggregateDaily(
                "2026-08-29 01:00:00", "2026-09-12 00:59:59");
        verify(statisticsInfoMapper, never()).insertOrUpdate(any());
    }

    @Test
    void statistics_savesFourDataTypesPerDay() {
        OrderDailyStatsVO day = new OrderDailyStatsVO();
        day.setStatisticsDate("2026-08-01");
        day.setSaleAmount(new BigDecimal("10"));
        day.setSaleCount(new BigDecimal("2"));
        day.setRefundAmount(new BigDecimal("1"));
        day.setRefundCount(new BigDecimal("1"));
        when(orderFeignSupport.aggregateDaily(anyString(), anyString())).thenReturn(List.of(day));
        when(statisticsInfoMapper.insertOrUpdate(any())).thenReturn(1);

        statisticsInfoService.statistics("2026-07-15 01:00:00", "2026-08-01 01:00:00");

        verify(statisticsInfoMapper, times(4)).insertOrUpdate(any());
    }

    @Test
    void statistics_insertFailure_throws() {
        OrderDailyStatsVO day = new OrderDailyStatsVO();
        day.setStatisticsDate("2026-08-01");
        when(orderFeignSupport.aggregateDaily(anyString(), anyString())).thenReturn(List.of(day));
        when(statisticsInfoMapper.insertOrUpdate(any())).thenReturn(0);

        assertThrows(BusinessException.class, () -> statisticsInfoService.statistics("a", "b"));
    }

    @Test
    void statistics_skipsNullDates() {
        OrderDailyStatsVO day = new OrderDailyStatsVO();
        day.setStatisticsDate(null);
        when(orderFeignSupport.aggregateDaily(anyString(), anyString())).thenReturn(List.of(day));

        statisticsInfoService.statistics("a", "b");

        verify(statisticsInfoMapper, never()).insertOrUpdate(any());
    }
}
