package com.simlect.biz.impl;

import com.simlect.entity.po.SignBitmap;
import com.simlect.mappers.SignBitmapMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignStreakServiceImplTest {

    @Mock
    private SignBitmapMapper signBitmapMapper;

    @InjectMocks
    private SignStreakServiceImpl signStreakService;

    @Test
    void computeContinuousDays_withoutRowsReturnsZero() {
        LocalDate today = LocalDate.of(2026, 9, 11);
        when(signBitmapMapper.selectAllByUser("U1")).thenReturn(List.of());

        assertEquals(0, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_spansMonthBoundary() {
        LocalDate today = LocalDate.of(2026, 3, 2);
        when(signBitmapMapper.selectAllByUser("U1")).thenReturn(List.of(
                bitmap("202603", bit(1) | bit(2)),
                bitmap("202602", bit(28))));

        assertEquals(3, signStreakService.computeContinuousDays("U1", today));
        verify(signBitmapMapper).selectAllByUser("U1");
    }

    @Test
    void computeContinuousDays_whenTodayUnsignedStartsFromYesterday() {
        LocalDate today = LocalDate.of(2026, 3, 3);
        when(signBitmapMapper.selectAllByUser("U1"))
                .thenReturn(List.of(bitmap("202603", bit(1) | bit(2))));

        assertEquals(2, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_todaySignedYesterdayMissingCountsOne() {
        LocalDate today = LocalDate.of(2026, 3, 3);
        when(signBitmapMapper.selectAllByUser("U1"))
                .thenReturn(List.of(bitmap("202603", bit(1) | bit(3))));

        assertEquals(1, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_stopsAtFirstGap() {
        LocalDate today = LocalDate.of(2026, 3, 5);
        when(signBitmapMapper.selectAllByUser("U1"))
                .thenReturn(List.of(bitmap("202603", bit(1) | bit(2) | bit(4) | bit(5))));

        assertEquals(2, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_supportsThirtyFirstDayAcrossMonthBoundary() {
        LocalDate today = LocalDate.of(2026, 9, 1);
        when(signBitmapMapper.selectAllByUser("U1")).thenReturn(List.of(
                bitmap("202609", bit(1)),
                bitmap("202608", bit(31))));

        assertEquals(2, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_notTruncatedBeyondTwoMonthWindow() {
        // 6-01 起连续签到至 9-04：30 + 31 + 31 + 4 = 96 天，超过旧 62 天窗口不再截断
        LocalDate today = LocalDate.of(2026, 9, 4);
        when(signBitmapMapper.selectAllByUser("U1")).thenReturn(List.of(
                bitmap("202609", bitsThrough(4)),
                bitmap("202608", bitsThrough(31)),
                bitmap("202607", bitsThrough(31)),
                bitmap("202606", bitsThrough(30))));

        assertEquals(96, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_spansYearBoundaryWithoutTruncation() {
        // 2025-11-01 起连续签到至 2026-01-05：30 + 31 + 5 = 66 天，跨年且超过旧 62 天窗口
        LocalDate today = LocalDate.of(2026, 1, 5);
        when(signBitmapMapper.selectAllByUser("U1")).thenReturn(List.of(
                bitmap("202601", bitsThrough(5)),
                bitmap("202512", bitsThrough(31)),
                bitmap("202511", bitsThrough(30))));

        assertEquals(66, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_supplementBehindGapDoesNotChangeStreak() {
        // 今天 9-10 已签、9-09 断档；补签 9-08 位于断档之后，连续天数保持 1 不变
        LocalDate today = LocalDate.of(2026, 9, 10);
        when(signBitmapMapper.selectAllByUser("U1"))
                .thenReturn(List.of(bitmap("202609", bitsThrough(8) | bit(10))));

        assertEquals(1, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_supplementBridgesPreviousStreak() {
        // 今天 9-06 已签，补签 9-05 后与 9-01..9-04 的连续段合并为 6 天
        LocalDate today = LocalDate.of(2026, 9, 6);
        when(signBitmapMapper.selectAllByUser("U1"))
                .thenReturn(List.of(bitmap("202609", bitsThrough(6))));

        assertEquals(6, signStreakService.computeContinuousDays("U1", today));
    }

    private SignBitmap bitmap(String yearMonth, long bits) {
        SignBitmap bitmap = new SignBitmap();
        bitmap.setUserId("U1");
        bitmap.setYearMonth(yearMonth);
        bitmap.setBits(bits);
        return bitmap;
    }

    private long bit(int dayOfMonth) {
        return 1L << (dayOfMonth - 1);
    }

    private long bitsThrough(int dayOfMonth) {
        return (1L << dayOfMonth) - 1;
    }
}