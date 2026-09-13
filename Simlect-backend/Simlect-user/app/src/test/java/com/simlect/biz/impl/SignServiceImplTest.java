package com.simlect.biz.impl;

import com.simlect.api.dto.NotificationMessageDTO;
import com.simlect.api.dto.SignStreakCouponMessageDTO;
import com.simlect.api.vo.SignDataVO;
import com.simlect.biz.SignRewardConfigService;
import com.simlect.biz.SignStreakService;
import com.simlect.biz.UserMemberProfileService;
import com.simlect.component.RedisComponent;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.TransactionalMqSender;
import com.simlect.entity.dto.SignRewardConfigDTO;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.po.SignBitmap;
import com.simlect.entity.po.UserMemberProfile;
import com.simlect.entity.po.UserSignRecordDetail;
import com.simlect.entity.query.UserMemberProfileQuery;
import com.simlect.entity.query.UserSignRecordDetailQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.SignBitmapMapper;
import com.simlect.mappers.SignGrowthRecordMapper;
import com.simlect.mappers.SignSupplementUsedMapper;
import com.simlect.mappers.UserMemberProfileMapper;
import com.simlect.mappers.UserSignRecordDetailMapper;
import com.simlect.support.MqIdempotencyKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignServiceImplTest {

    private static final String USER_ID = "U1";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Instant TEST_INSTANT = Instant.parse("2026-09-12T16:30:00Z");
    private static final LocalDate TEST_TODAY = LocalDate.of(2026, 9, 13);

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private UserMemberProfileService userMemberProfileService;
    @Mock
    private SignRewardConfigService signRewardConfigService;
    @Mock
    private SignStreakService signStreakService;
    @Mock
    private TransactionalMqSender transactionalMqSender;
    @Mock
    private SignBitmapMapper signBitmapMapper;
    @Mock
    private SignGrowthRecordMapper signGrowthRecordMapper;
    @Mock
    private SignSupplementUsedMapper signSupplementUsedMapper;
    @Mock
    private UserMemberProfileMapper<UserMemberProfile, UserMemberProfileQuery> userMemberProfileMapper;
    @Mock
    private UserSignRecordDetailMapper<UserSignRecordDetail, UserSignRecordDetailQuery> signRecordDetailMapper;

    @InjectMocks
    private SignServiceImpl signService;

    @BeforeEach
    void setBusinessClock() {
        ReflectionTestUtils.setField(signService, "businessClock", Clock.fixed(TEST_INSTANT, BUSINESS_ZONE));
    }

    @Test
    void getSignCalendar_invalidMonth_throws() {
        assertThrows(BusinessException.class, () -> signService.getSignCalendar(USER_ID, "2026"));
        assertThrows(BusinessException.class, () -> signService.getSignCalendar(USER_ID, "202613"));
        assertThrows(BusinessException.class, () -> signService.getSignCalendar(USER_ID, null));
    }

    @Test
    void getSignCalendar_currentMonthAlwaysReadsMysql() {
        String currentMonth = YearMonth.from(TEST_TODAY).format(MONTH_FORMATTER);
        when(signBitmapMapper.selectByUserAndMonth(USER_ID, currentMonth))
                .thenReturn(bitmap(currentMonth, bit(1) | bit(3)));
        when(signStreakService.computeContinuousDays(eq(USER_ID), any(LocalDate.class))).thenReturn(4);
        when(signBitmapMapper.countTotalSignDays(USER_ID)).thenReturn(65);
        when(signSupplementUsedMapper.countByUserId(USER_ID)).thenReturn(1);

        SignDataVO result = signService.getSignCalendar(USER_ID, currentMonth);

        assertEquals(List.of(currentMonth + "01", currentMonth + "03"), result.getSignDays());
        assertEquals(4, result.getContinuousDays());
        assertEquals(65, result.getTotalSignDays());
        assertEquals(1, result.getSupplementCount());
        verify(redisComponent, never()).getSignMonthBits(anyString(), anyString());
        verify(redisComponent).writeSignMonthBitmap(USER_ID, currentMonth, bit(1) | bit(3));
    }

    @Test
    void getSignCalendar_historicalMonthUsesRedisCache() {
        YearMonth historical = YearMonth.from(TEST_TODAY).minusMonths(1);
        String yearMonth = historical.format(MONTH_FORMATTER);
        int lastDay = historical.lengthOfMonth();
        when(redisComponent.getSignMonthBits(USER_ID, yearMonth)).thenReturn(bit(lastDay));

        SignDataVO result = signService.getSignCalendar(USER_ID, yearMonth);

        assertEquals(List.of(yearMonth + String.format("%02d", lastDay)), result.getSignDays());
        verify(signBitmapMapper, never()).selectByUserAndMonth(anyString(), anyString());
        verify(redisComponent, never()).writeSignMonthBitmap(anyString(), anyString(), anyLong());
    }

    @Test
    void getSignCalendar_historicalCacheMissFallsBackToMysqlAndBackfills() {
        String yearMonth = YearMonth.from(TEST_TODAY).minusMonths(2).format(MONTH_FORMATTER);
        when(redisComponent.getSignMonthBits(USER_ID, yearMonth)).thenReturn(null);
        when(signBitmapMapper.selectByUserAndMonth(USER_ID, yearMonth))
                .thenReturn(bitmap(yearMonth, bit(2)));

        SignDataVO result = signService.getSignCalendar(USER_ID, yearMonth);

        assertEquals(List.of(yearMonth + "02"), result.getSignDays());
        verify(redisComponent).writeSignMonthBitmap(USER_ID, yearMonth, bit(2));
    }

    @Test
    void sign_alreadySignedRejectsWithoutSideEffects() {
        stubUserLock();
        when(signBitmapMapper.insertBitIfAbsent(eq(USER_ID), anyString(), anyLong())).thenReturn(0);
        when(signBitmapMapper.setBitIfMissing(eq(USER_ID), anyString(), anyLong())).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> signService.sign(USER_ID));

        assertTrue(error.getMessage().contains("已经签到"));
        verify(signGrowthRecordMapper, never()).insertIgnore(anyString(), anyString(), anyInt(), any());
        verify(signRecordDetailMapper, never()).insertIgnore(any());
    }

    @Test
    void sign_successWritesMysqlAndEnqueuesRewardAndCacheRefresh() {
        LocalDate today = TEST_TODAY;
        String todayText = today.format(DATE_FORMATTER);
        String yearMonth = today.format(MONTH_FORMATTER);
        long mask = bit(today.getDayOfMonth());
        stubUserLock();
        when(signBitmapMapper.insertBitIfAbsent(USER_ID, yearMonth, mask)).thenReturn(1);
        when(signGrowthRecordMapper.insertIgnore(eq(USER_ID), eq(todayText), eq(5), any())).thenReturn(1);
        when(signRecordDetailMapper.insertIgnore(any())).thenReturn(1);
        when(signStreakService.computeContinuousDays(USER_ID, today)).thenReturn(7);
        when(signBitmapMapper.countTotalSignDays(USER_ID)).thenReturn(31);
        when(signSupplementUsedMapper.countByUserId(USER_ID)).thenReturn(1);
        when(signBitmapMapper.selectByUserAndMonth(USER_ID, yearMonth))
                .thenReturn(bitmap(yearMonth, mask));
        SignRewardConfigDTO config = new SignRewardConfigDTO();
        config.setEnabled(true);
        config.setStreakDays(7);
        config.setCouponId("CP1");
        when(signRewardConfigService.resolveActiveConfig()).thenReturn(config);

        signService.sign(USER_ID);

        verify(userMemberProfileMapper).insertDefaultIgnore(USER_ID);
        verify(userMemberProfileMapper).selectUserIdForUpdate(USER_ID);
        verify(userMemberProfileService).addGrowth(USER_ID, 5);
        ArgumentCaptor<UserSignRecordDetail> detailCaptor = ArgumentCaptor.forClass(UserSignRecordDetail.class);
        verify(signRecordDetailMapper).insertIgnore(detailCaptor.capture());
        assertEquals(todayText, detailCaptor.getValue().getSignDate());
        assertEquals(0, detailCaptor.getValue().getSignType());
        verify(transactionalMqSender).sendAfterCommit(
                eq(RabbitMQConfig.SIGN_STREAK_COUPON_EXCHANGE),
                eq(RabbitMQConfig.SIGN_STREAK_COUPON_KEY),
                any(SignStreakCouponMessageDTO.class),
                eq(MqIdempotencyKeys.signStreakCoupon(USER_ID, "CP1", 7)),
                eq(MessageReliabilityLevelEnum.HIGH));

        ArgumentCaptor<Runnable> refreshCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(transactionalMqSender).sendAfterCommit(refreshCaptor.capture());
        refreshCaptor.getValue().run();
        verify(redisComponent).writeSignMonthBitmap(USER_ID, yearMonth, mask);
        verify(redisComponent).writeSignHash(USER_ID, 7, 31, 1);
    }

    @Test
    void sign_existingGrowthRecordDoesNotIncrementGrowthAgain() {
        stubSuccessfulSignMutation();
        when(signGrowthRecordMapper.insertIgnore(eq(USER_ID), anyString(), eq(5), any())).thenReturn(0);
        when(signRecordDetailMapper.insertIgnore(any())).thenReturn(1);

        signService.sign(USER_ID);

        verify(userMemberProfileService, never()).addGrowth(anyString(), anyInt());
        verify(signRecordDetailMapper).insertIgnore(any());
    }

    @Test
    void msign_rejectsMalformedFutureAndPreviousMonthDatesBeforeLocking() {
        String future = TEST_TODAY.plusDays(1).format(DATE_FORMATTER);
        String previousMonth = TEST_TODAY.minusMonths(1).format(DATE_FORMATTER);

        assertThrows(BusinessException.class, () -> signService.msign(USER_ID, "2026-01-05"));
        BusinessException invalidCalendarDate = assertThrows(BusinessException.class,
                () -> signService.msign(USER_ID, "20260230"));
        assertEquals("日期格式错误", invalidCalendarDate.getMessage());
        assertThrows(BusinessException.class, () -> signService.msign(USER_ID, future));
        assertThrows(BusinessException.class, () -> signService.msign(USER_ID, previousMonth));
        verify(userMemberProfileMapper, never()).selectUserIdForUpdate(anyString());
    }

    @Test
    void msign_insufficientQuotaRejectsBeforeChangingBitmap() {
        stubUserLock();
        when(signBitmapMapper.countTotalSignDays(USER_ID)).thenReturn(29);
        when(signSupplementUsedMapper.countByUserId(USER_ID)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> signService.msign(USER_ID, todayText()));

        assertTrue(error.getMessage().contains("补签次数不足"));
        verify(signBitmapMapper, never()).insertBitIfAbsent(anyString(), anyString(), anyLong());
    }

    @Test
    void msign_alreadySignedDoesNotConsumeQuota() {
        LocalDate today = TEST_TODAY;
        stubUserLock();
        when(signBitmapMapper.countTotalSignDays(USER_ID)).thenReturn(30);
        when(signSupplementUsedMapper.countByUserId(USER_ID)).thenReturn(0);
        when(signBitmapMapper.insertBitIfAbsent(eq(USER_ID), anyString(), anyLong())).thenReturn(0);
        when(signBitmapMapper.setBitIfMissing(eq(USER_ID), anyString(), anyLong())).thenReturn(0);

        assertThrows(BusinessException.class, () -> signService.msign(USER_ID, today.format(DATE_FORMATTER)));

        verify(signSupplementUsedMapper, never()).insert(anyString(), anyString(), any());
    }

    @Test
    void msign_successConsumesQuotaWritesDetailAndEnqueuesNotification() {
        LocalDate today = TEST_TODAY;
        String signDate = today.format(DATE_FORMATTER);
        String yearMonth = today.format(MONTH_FORMATTER);
        long mask = bit(today.getDayOfMonth());
        stubUserLock();
        when(signBitmapMapper.countTotalSignDays(USER_ID)).thenReturn(30, 31);
        when(signSupplementUsedMapper.countByUserId(USER_ID)).thenReturn(0, 1);
        when(signBitmapMapper.insertBitIfAbsent(USER_ID, yearMonth, mask)).thenReturn(1);
        when(signSupplementUsedMapper.insert(eq(USER_ID), eq(signDate), any())).thenReturn(1);
        when(signGrowthRecordMapper.insertIgnore(eq(USER_ID), eq(signDate), eq(5), any())).thenReturn(1);
        when(signRecordDetailMapper.insertIgnore(any())).thenReturn(1);
        when(signStreakService.computeContinuousDays(USER_ID, today)).thenReturn(2);
        when(signBitmapMapper.selectByUserAndMonth(USER_ID, yearMonth))
                .thenReturn(bitmap(yearMonth, mask));

        signService.msign(USER_ID, signDate);

        verify(signSupplementUsedMapper).insert(eq(USER_ID), eq(signDate), any());
        verify(userMemberProfileService).addGrowth(USER_ID, 5);
        ArgumentCaptor<UserSignRecordDetail> detailCaptor = ArgumentCaptor.forClass(UserSignRecordDetail.class);
        verify(signRecordDetailMapper).insertIgnore(detailCaptor.capture());
        assertEquals(1, detailCaptor.getValue().getSignType());
        assertEquals(signDate, detailCaptor.getValue().getSignDate());
        verify(transactionalMqSender).sendAfterCommit(
                eq(RabbitMQConfig.NOTIFY_EXCHANGE),
                eq(RabbitMQConfig.NOTIFY_KEY),
                any(NotificationMessageDTO.class),
                eq(MqIdempotencyKeys.notification(USER_ID, "sign", signDate)),
                eq(MessageReliabilityLevelEnum.HIGH));

        ArgumentCaptor<Runnable> refreshCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(transactionalMqSender).sendAfterCommit(refreshCaptor.capture());
        refreshCaptor.getValue().run();
        verify(redisComponent).writeSignMonthBitmap(USER_ID, yearMonth, mask);
        verify(redisComponent).writeSignHash(USER_ID, 2, 31, 1);
    }

    @Test
    void msign_quotaConsumptionFailureStopsGrowthAndDetail() {
        stubUserLock();
        when(signBitmapMapper.countTotalSignDays(USER_ID)).thenReturn(30);
        when(signSupplementUsedMapper.countByUserId(USER_ID)).thenReturn(0);
        when(signBitmapMapper.insertBitIfAbsent(eq(USER_ID), anyString(), anyLong())).thenReturn(1);
        when(signSupplementUsedMapper.insert(eq(USER_ID), anyString(), any())).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> signService.msign(USER_ID, todayText()));

        assertTrue(error.getMessage().contains("扣减失败"));
        verify(signGrowthRecordMapper, never()).insertIgnore(anyString(), anyString(), anyInt(), any());
        verify(signRecordDetailMapper, never()).insertIgnore(any());
    }

    private void stubUserLock() {
        when(userMemberProfileMapper.selectUserIdForUpdate(USER_ID)).thenReturn(USER_ID);
    }

    private void stubSuccessfulSignMutation() {
        LocalDate today = TEST_TODAY;
        String yearMonth = today.format(MONTH_FORMATTER);
        long mask = bit(today.getDayOfMonth());
        stubUserLock();
        when(signBitmapMapper.insertBitIfAbsent(USER_ID, yearMonth, mask)).thenReturn(1);
        when(signBitmapMapper.selectByUserAndMonth(USER_ID, yearMonth))
                .thenReturn(bitmap(yearMonth, mask));
    }

    private SignBitmap bitmap(String yearMonth, long bits) {
        SignBitmap bitmap = new SignBitmap();
        bitmap.setUserId(USER_ID);
        bitmap.setYearMonth(yearMonth);
        bitmap.setBits(bits);
        return bitmap;
    }

    private long bit(int dayOfMonth) {
        return 1L << (dayOfMonth - 1);
    }

    private String todayText() {
        return TEST_TODAY.format(DATE_FORMATTER);
    }
}
