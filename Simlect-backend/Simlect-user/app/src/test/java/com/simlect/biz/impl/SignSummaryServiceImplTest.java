package com.simlect.biz.impl;

import com.simlect.biz.SignStreakService;
import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.entity.dto.SignSummarySnapshot;
import com.simlect.entity.po.UserSignSummary;
import com.simlect.mappers.SignBitmapMapper;
import com.simlect.mappers.SignSupplementUsedMapper;
import com.simlect.mappers.SignSummaryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignSummaryServiceImplTest {

    private static final String USER_ID = "U1";
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 13);

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private SignSummaryMapper signSummaryMapper;
    @Mock
    private SignSummaryWriter signSummaryWriter;
    @Mock
    private SignStreakService signStreakService;
    @Mock
    private SignBitmapMapper signBitmapMapper;
    @Mock
    private SignSupplementUsedMapper signSupplementUsedMapper;

    @InjectMocks
    private SignSummaryServiceImpl summaryService;

    @Test
    void load_cacheHitWithTodayAnchor_returnsCachedValuesWithoutDb() {
        when(redisComponent.getSignHashSnapshot(USER_ID)).thenReturn(cache(5, 31, 1, "20260913"));

        SignSummarySnapshot snapshot = summaryService.load(USER_ID, TODAY);

        assertEquals(5, snapshot.continuousDays());
        assertEquals(31, snapshot.totalSignDays());
        assertEquals(1, snapshot.usedCount());
        verify(signSummaryMapper, never()).selectByUserId(any());
    }

    @Test
    void load_cacheHitWithYesterdayAnchor_keepsCachedContinuousDays() {
        when(redisComponent.getSignHashSnapshot(USER_ID)).thenReturn(cache(5, 31, 1, "20260912"));

        SignSummarySnapshot snapshot = summaryService.load(USER_ID, TODAY);

        assertEquals(5, snapshot.continuousDays());
        verify(signSummaryMapper, never()).selectByUserId(any());
    }

    @Test
    void load_cacheHitWithStaleAnchor_zeroesContinuousDaysOnly() {
        when(redisComponent.getSignHashSnapshot(USER_ID)).thenReturn(cache(5, 31, 1, "20260910"));

        SignSummarySnapshot snapshot = summaryService.load(USER_ID, TODAY);

        assertEquals(0, snapshot.continuousDays());
        assertEquals(31, snapshot.totalSignDays());
        assertEquals(1, snapshot.usedCount());
        verify(signSummaryMapper, never()).selectByUserId(any());
    }

    @Test
    void load_cacheMissDbHit_backfillsRedis() {
        when(redisComponent.getSignHashSnapshot(USER_ID)).thenReturn(null);
        when(signSummaryMapper.selectByUserId(USER_ID)).thenReturn(row(4, 40, 2, "20260912"));

        SignSummarySnapshot snapshot = summaryService.load(USER_ID, TODAY);

        assertEquals(4, snapshot.continuousDays());
        assertEquals(40, snapshot.totalSignDays());
        assertEquals(2, snapshot.usedCount());
        verify(redisComponent).writeSignHash(USER_ID, 4, 40, 2, "20260912");
    }

    @Test
    void load_allMiss_rebuildsFromBitmapAndBackfillsSnapshot() {
        when(redisComponent.getSignHashSnapshot(USER_ID)).thenReturn(null);
        when(signSummaryMapper.selectByUserId(USER_ID)).thenReturn(null);
        when(signStreakService.computeContinuousDays(USER_ID, TODAY)).thenReturn(3);
        when(signBitmapMapper.countTotalSignDays(USER_ID)).thenReturn(31);
        when(signSupplementUsedMapper.countByUserId(USER_ID)).thenReturn(1);
        when(signSummaryMapper.selectLatestSignDate(USER_ID)).thenReturn("20260913");
        when(signSummaryWriter.writeBackAndRead(eq(USER_ID), eq(3), eq(31), eq(1), eq("20260913"), any(Date.class)))
                .thenReturn(row(3, 31, 1, "20260913"));

        SignSummarySnapshot snapshot = summaryService.load(USER_ID, TODAY);

        assertEquals(3, snapshot.continuousDays());
        assertEquals(31, snapshot.totalSignDays());
        assertEquals(1, snapshot.usedCount());
        verify(signSummaryWriter).writeBackAndRead(eq(USER_ID), eq(3), eq(31), eq(1), eq("20260913"), any(Date.class));
        verify(redisComponent).writeSignHash(USER_ID, 3, 31, 1, "20260913");
    }

    @Test
    void load_neverSigned_returnsZeroSnapshot() {
        when(redisComponent.getSignHashSnapshot(USER_ID)).thenReturn(null);
        when(signSummaryMapper.selectByUserId(USER_ID)).thenReturn(null);
        when(signStreakService.computeContinuousDays(USER_ID, TODAY)).thenReturn(0);
        when(signBitmapMapper.countTotalSignDays(USER_ID)).thenReturn(0);
        when(signSupplementUsedMapper.countByUserId(USER_ID)).thenReturn(0);
        when(signSummaryMapper.selectLatestSignDate(USER_ID)).thenReturn(null);
        when(signSummaryWriter.writeBackAndRead(eq(USER_ID), eq(0), eq(0), eq(0), isNull(), any(Date.class)))
                .thenReturn(row(0, 0, 0, null));

        SignSummarySnapshot snapshot = summaryService.load(USER_ID, TODAY);

        assertEquals(0, snapshot.continuousDays());
        assertEquals(0, snapshot.totalSignDays());
        assertEquals(0, snapshot.usedCount());
        verify(signSummaryWriter).writeBackAndRead(eq(USER_ID), eq(0), eq(0), eq(0), isNull(), any(Date.class));
        verify(redisComponent).writeSignHash(USER_ID, 0, 0, 0, null);
    }

    @Test
    void persist_upsertsSnapshotWithAnchorDate() {
        summaryService.persist(USER_ID, new SignSummarySnapshot(5, 31, 1, null), TODAY, new Date());

        verify(signSummaryMapper).upsert(eq(USER_ID), eq(5), eq(31), eq(1), eq("20260913"), any(Date.class));
    }

    private Map<String, String> cache(int continuousDays, int totalSignDays, int usedCount, String lastSignDate) {
        Map<String, String> hash = new HashMap<>();
        hash.put(Constants.CONTINUOUS_DAYS, String.valueOf(continuousDays));
        hash.put(Constants.TOTAL_SIGN_DAYS, String.valueOf(totalSignDays));
        hash.put(Constants.USED_COUNT, String.valueOf(usedCount));
        hash.put(Constants.LAST_SIGN_DATE, lastSignDate == null ? "" : lastSignDate);
        return hash;
    }

    private UserSignSummary row(int continuousDays, int totalSignDays, int usedCount, String lastSignDate) {
        UserSignSummary summary = new UserSignSummary();
        summary.setUserId(USER_ID);
        summary.setContinuousDays(continuousDays);
        summary.setTotalSignDays(totalSignDays);
        summary.setUsedCount(usedCount);
        summary.setLastSignDate(lastSignDate);
        return summary;
    }
}