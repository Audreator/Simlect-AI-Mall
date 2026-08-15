package com.simlect.biz.impl;

import com.simlect.api.support.AdminAuditFeignSupport;
import com.simlect.api.vo.SignDateSyncResultVO;
import com.simlect.component.RedisComponent;
import com.simlect.entity.po.UserSignRecord;
import com.simlect.entity.po.UserSignRecordDetail;
import com.simlect.entity.query.UserSignRecordQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserSignRecordDetailMapper;
import com.simlect.mappers.UserSignRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignCalendarCacheServiceImplTest {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private UserSignRecordDetailMapper<UserSignRecordDetail, com.simlect.entity.query.UserSignRecordDetailQuery> userSignRecordDetailMapper;
    @Mock
    private UserSignRecordMapper<UserSignRecord, UserSignRecordQuery> userSignRecordMapper;
    @Mock
    private AdminAuditFeignSupport adminAuditFeignSupport;

    @InjectMocks
    private SignCalendarCacheServiceImpl signCalendarCacheService;

    private String yesterday() {
        return LocalDate.now().minusDays(1).format(YMD);
    }

    private UserSignRecordDetail detail(String userId, String date, Integer signType) {
        UserSignRecordDetail detail = new UserSignRecordDetail();
        detail.setUserId(userId);
        detail.setSignDate(date);
        detail.setSignType(signType);
        return detail;
    }

    // ==================== ensureCalendarHydrated ====================

    @Test
    void ensureCalendarHydrated_emptyUserId_ignored() {
        signCalendarCacheService.ensureCalendarHydrated("");

        verifyNoInteractions(redisComponent);
    }

    @Test
    void ensureCalendarHydrated_hasNullCache_skipsRebuild() {
        when(redisComponent.hasSignNullCache("U1")).thenReturn(true);

        signCalendarCacheService.ensureCalendarHydrated("U1");

        verify(redisComponent, never()).signRebuildLockKey(anyString());
    }

    @Test
    void ensureCalendarHydrated_hasSnapshot_skipsRebuild() {
        when(redisComponent.hasSignNullCache("U1")).thenReturn(false);
        when(redisComponent.hasSignHashSnapshot("U1")).thenReturn(true);

        signCalendarCacheService.ensureCalendarHydrated("U1");

        verify(redisComponent, never()).signRebuildLockKey(anyString());
    }

    @Test
    void ensureCalendarHydrated_acquiresLock_rebuildsFromDb() {
        when(redisComponent.hasSignNullCache("U1")).thenReturn(false);
        when(redisComponent.hasSignHashSnapshot("U1")).thenReturn(false);
        when(redisComponent.signRebuildLockKey("U1")).thenReturn("lock:U1");
        when(redisComponent.setIfAbsent(eq("lock:U1"), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        // hasSignHashSnapshot checked again inside locked branch
        when(redisComponent.hasSignHashSnapshot("U1")).thenReturn(false);
        when(userSignRecordDetailMapper.selectByUserIdAndDateRange(anyString(), anyString(), anyString()))
                .thenReturn(List.of(detail("U1", yesterday(), 0)));

        signCalendarCacheService.ensureCalendarHydrated("U1");

        verify(redisComponent).deleteIfOwned(eq("lock:U1"), anyString());
        verify(redisComponent).clearSignNullCache("U1");
    }

    @Test
    void ensureCalendarHydrated_noDbRows_setsNullCache() {
        when(redisComponent.hasSignNullCache("U1")).thenReturn(false);
        when(redisComponent.hasSignHashSnapshot("U1")).thenReturn(false);
        when(redisComponent.signRebuildLockKey("U1")).thenReturn("lock:U1");
        when(redisComponent.setIfAbsent(eq("lock:U1"), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisComponent.hasSignHashSnapshot("U1")).thenReturn(false);
        when(userSignRecordDetailMapper.selectByUserIdAndDateRange(anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        signCalendarCacheService.ensureCalendarHydrated("U1");

        verify(redisComponent).setSignNullCache("U1");
        verify(redisComponent).deleteIfOwned(eq("lock:U1"), anyString());
    }

    // ==================== syncSignDatesFromDb ====================

    @Test
    void syncSignDatesFromDb_badEndDate_throws() {
        assertThrows(BusinessException.class,
                () -> signCalendarCacheService.syncSignDatesFromDb("U1", "2026", false));
    }

    @Test
    void syncSignDatesFromDb_todayWithForceFalse_throws() {
        String today = LocalDate.now().format(YMD);

        assertThrows(BusinessException.class,
                () -> signCalendarCacheService.syncSignDatesFromDb("U1", today, false));
    }

    @Test
    void syncSignDatesFromDb_futureDate_throws() {
        String future = LocalDate.now().plusDays(1).format(YMD);

        assertThrows(BusinessException.class,
                () -> signCalendarCacheService.syncSignDatesFromDb("U1", future, true));
    }

    @Test
    void syncSignDatesFromDb_emptyDetails_marksSkipped() {
        when(userSignRecordDetailMapper.selectByUserIdAndDateRange(eq("U1"), anyString(), eq(yesterday())))
                .thenReturn(List.of());

        SignDateSyncResultVO result = signCalendarCacheService.syncSignDatesFromDb("U1", yesterday(), false);

        assertEquals(1, result.getSkipped());
        assertEquals(0, result.getSyncedUsers());
    }

    @Test
    void syncSignDatesFromDb_success_supplementsAndHydrates() {
        List<UserSignRecordDetail> details = List.of(detail("U1", yesterday(), 1));
        when(userSignRecordDetailMapper.selectByUserIdAndDateRange(eq("U1"), anyString(), eq(yesterday())))
                .thenReturn(details);
        when(redisComponent.ensureSignBitmapBit("U1", yesterday())).thenReturn(true);
        when(redisComponent.totalSignDays("U1")).thenReturn(5);
        when(redisComponent.getContinuousDays("U1")).thenReturn(2);
        when(redisComponent.getUsedCount("U1")).thenReturn(1);
        when(userSignRecordMapper.selectByUserId("U1")).thenReturn(null);

        SignDateSyncResultVO result = signCalendarCacheService.syncSignDatesFromDb("U1", yesterday(), false);

        assertEquals(1, result.getSyncedUsers());
        assertEquals(1, result.getSupplementedDates());
        verify(redisComponent).writeSignHash("U1", 2, 5, 1);
        verify(redisComponent).clearSignNullCache("U1");
    }

    // ==================== syncAllSignDatesFromDb ====================

    @Test
    void syncAllSignDatesFromDb_noUsers_emptyResult() {
        when(userSignRecordDetailMapper.selectDistinctUserIdsInDateRange(anyString(), anyString()))
                .thenReturn(List.of());

        SignDateSyncResultVO result = signCalendarCacheService.syncAllSignDatesFromDb(yesterday(), false);

        assertEquals(0, result.getTotalInDb());
    }

    @Test
    void syncAllSignDatesFromDb_processesEachUser() {
        when(userSignRecordDetailMapper.selectDistinctUserIdsInDateRange(anyString(), anyString()))
                .thenReturn(List.of("U1", "U2"));
        when(userSignRecordDetailMapper.selectByUserIdAndDateRange(eq("U1"), anyString(), anyString()))
                .thenReturn(List.of(detail("U1", yesterday(), 0)));
        when(userSignRecordDetailMapper.selectByUserIdAndDateRange(eq("U2"), anyString(), anyString()))
                .thenReturn(List.of());
        when(redisComponent.ensureSignBitmapBit("U1", yesterday())).thenReturn(true);

        SignDateSyncResultVO result = signCalendarCacheService.syncAllSignDatesFromDb(yesterday(), false);

        assertEquals(2, result.getTotalInDb());
        assertEquals(1, result.getSyncedUsers());
        assertEquals(1, result.getSkipped());
        assertEquals(1, result.getSupplementedDates());
    }

    // ==================== forceRebuildToday ====================

    @Test
    void forceRebuildToday_singleUser_noDetails() {
        when(userSignRecordDetailMapper.selectByUserIdAndDateRange(eq("U1"), anyString(), anyString()))
                .thenReturn(List.of());

        SignDateSyncResultVO result = signCalendarCacheService.forceRebuildToday("U1", "admin");

        assertEquals(1, result.getTotalInDb());
        assertEquals(1, result.getSyncedUsers());
        verify(adminAuditFeignSupport).log(eq("admin"), eq("SIGN_FORCE_REBUILD_TODAY"),
                eq("U1"), contains("users=1"));
    }

    @Test
    void forceRebuildToday_detectsRedisOnlyTodayDiff() {
        String today = LocalDate.now().format(YMD);
        String yyyyMM = today.substring(0, 6);
        int day = Integer.parseInt(today.substring(6, 8));
        List<UserSignRecordDetail> details = List.of(detail("U1", yesterday(), 0));
        when(userSignRecordDetailMapper.selectByUserIdAndDateRange(eq("U1"), anyString(), anyString()))
                .thenReturn(details);
        when(redisComponent.isSign("U1", yyyyMM, day)).thenReturn(true);

        SignDateSyncResultVO result = signCalendarCacheService.forceRebuildToday("U1", "admin");

        assertEquals(1, result.getDiffCount());
        assertEquals(1, result.getSyncedUsers());
    }

    // ==================== reconcileRecentHour ====================

    @Test
    void reconcileRecentHour_noRows_returnsZero() {
        when(userSignRecordDetailMapper.selectCreatedAfter(any())).thenReturn(null);

        assertEquals(0, signCalendarCacheService.reconcileRecentHour());
    }

    @Test
    void reconcileRecentHour_repairsBitmapRows() {
        UserSignRecordDetail row = detail("U1", yesterday(), 0);
        when(userSignRecordDetailMapper.selectCreatedAfter(any())).thenReturn(List.of(row));
        when(redisComponent.ensureSignBitmapBit("U1", yesterday())).thenReturn(true);

        assertEquals(1, signCalendarCacheService.reconcileRecentHour());
    }

    @Test
    void reconcileRecentHour_skipsInvalidRows() {
        UserSignRecordDetail row = detail("U1", "", 0);
        when(userSignRecordDetailMapper.selectCreatedAfter(any())).thenReturn(List.of(row));

        assertEquals(0, signCalendarCacheService.reconcileRecentHour());
    }

    // ==================== initTodayBitmapForActiveUsers ====================

    @Test
    void initTodayBitmap_noUsers_returnsZero() {
        when(userSignRecordMapper.selectList(any())).thenReturn(List.of());

        assertEquals(0, signCalendarCacheService.initTodayBitmapForActiveUsers());
    }

    @Test
    void initTodayBitmap_initializesToday() {
        UserSignRecord record = new UserSignRecord();
        record.setUserId("U1");
        when(userSignRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(redisComponent.initTodaySignBitmapIfAbsent(eq("U1"), anyString(), anyInt())).thenReturn(true);

        assertEquals(1, signCalendarCacheService.initTodayBitmapForActiveUsers());
    }
}
