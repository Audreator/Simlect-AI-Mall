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
        when(signBitmapMapper.selectRecentMonths("U1", "202609", "202608")).thenReturn(List.of());

        assertEquals(0, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_spansMonthBoundary() {
        LocalDate today = LocalDate.of(2026, 3, 2);
        when(signBitmapMapper.selectRecentMonths("U1", "202603", "202602")).thenReturn(List.of(
                bitmap("202603", bit(1) | bit(2)),
                bitmap("202602", bit(28))));

        assertEquals(3, signStreakService.computeContinuousDays("U1", today));
        verify(signBitmapMapper).selectRecentMonths("U1", "202603", "202602");
    }

    @Test
    void computeContinuousDays_whenTodayUnsignedStartsFromYesterday() {
        LocalDate today = LocalDate.of(2026, 3, 3);
        when(signBitmapMapper.selectRecentMonths("U1", "202603", "202602"))
                .thenReturn(List.of(bitmap("202603", bit(1) | bit(2))));

        assertEquals(2, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_stopsAtFirstGap() {
        LocalDate today = LocalDate.of(2026, 3, 5);
        when(signBitmapMapper.selectRecentMonths("U1", "202603", "202602"))
                .thenReturn(List.of(bitmap("202603", bit(1) | bit(2) | bit(4) | bit(5))));

        assertEquals(2, signStreakService.computeContinuousDays("U1", today));
    }

    @Test
    void computeContinuousDays_supportsThirtyFirstDayAcrossMonthBoundary() {
        LocalDate today = LocalDate.of(2026, 9, 1);
        when(signBitmapMapper.selectRecentMonths("U1", "202609", "202608")).thenReturn(List.of(
                bitmap("202609", bit(1)),
                bitmap("202608", bit(31))));

        assertEquals(2, signStreakService.computeContinuousDays("U1", today));
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
}
