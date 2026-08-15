package com.simlect.biz.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.simlect.api.dto.SignRecordMessageDTO;
import com.simlect.api.support.CouponFeignSupport;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.api.vo.SignDataVO;
import com.simlect.biz.SignCalendarCacheService;
import com.simlect.biz.SignRecordSyncService;
import com.simlect.biz.SignRewardConfigService;
import com.simlect.biz.UserMemberProfileService;
import com.simlect.biz.UserNotificationService;
import com.simlect.component.RedisComponent;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.dto.SignRewardConfigDTO;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SignServiceImplTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private UserMemberProfileService userMemberProfileService;
    @Mock
    private UserNotificationService userNotificationService;
    @Mock
    private SignRewardConfigService signRewardConfigService;
    @Mock
    private CouponFeignSupport couponFeignSupport;
    @Mock
    private ReliableMessageSender reliableMessageSender;
    @Mock
    private SignRecordSyncService signRecordSyncService;
    @Mock
    private SignCalendarCacheService signCalendarCacheService;

    @InjectMocks
    private SignServiceImpl signService;

    @Test
    void getSignCalendar_invalidMonth_throws() {
        assertThrows(BusinessException.class, () -> signService.getSignCalendar("U1", "2026"));
        assertThrows(BusinessException.class, () -> signService.getSignCalendar("U1", null));
    }

    @Test
    void getSignCalendar_returnsSignDatesOfMonth() {
        when(redisComponent.getContinuousDays("U1")).thenReturn(5);
        when(redisComponent.getRemainSignCount("U1")).thenReturn(2);
        when(redisComponent.totalSignDays("U1")).thenReturn(20);
        when(redisComponent.isSign(eq("U1"), eq("202601"), anyInt()))
                .thenAnswer(inv -> (int) inv.getArgument(2) == 1);

        SignDataVO vo = signService.getSignCalendar("U1", "202601");

        assertEquals(5, vo.getContinuousDays());
        assertEquals(2, vo.getSupplementCount());
        assertEquals(20, vo.getTotalSignDays());
        assertEquals(List.of("20260101"), vo.getSignDays());
        verify(signRecordSyncService).ensureHashHydrated("U1");
        verify(signCalendarCacheService).ensureCalendarHydrated("U1");
    }

    @Test
    void getSignCalendar_februaryLeapYear_29Days() {
        when(redisComponent.isSign(eq("U1"), eq("202402"), anyInt()))
                .thenAnswer(inv -> (int) inv.getArgument(2) == 29);

        SignDataVO vo = signService.getSignCalendar("U1", "202402");

        assertEquals(List.of("20240229"), vo.getSignDays());
    }

    @Test
    void sign_alreadySigned_throws() {
        when(redisComponent.isSign(eq("U1"), anyString(), anyInt())).thenReturn(true);

when(redisComponent.setIfAbsent(startsWith("sign:growth:U1:"), eq("1"), eq(7L), any())).thenReturn(true);
        BusinessException e = assertThrows(BusinessException.class, () -> signService.sign("U1"));
        assertTrue(e.getMessage().contains("今天已经签到过了"));
    }

    @Test
    void sign_success_addsGrowthAndSendsMq() {
        when(redisComponent.isSign(eq("U1"), anyString(), anyInt())).thenReturn(false);
        when(redisComponent.getContinuousDays("U1")).thenReturn(3);
        when(redisComponent.totalSignDays("U1")).thenReturn(8);
        when(redisComponent.getUsedCount("U1")).thenReturn(1);

when(redisComponent.setIfAbsent(startsWith("sign:growth:U1:"), eq("1"), eq(7L), any())).thenReturn(true);
        signService.sign("U1");

        verify(redisComponent).sign("U1");
        verify(userMemberProfileService).addGrowth("U1", 5);
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.SIGN_RECORD_EXCHANGE),
                eq(RabbitMQConfig.SIGN_RECORD_KEY), any(SignRecordMessageDTO.class), anyString(), any());
        verify(signRewardConfigService).resolveActiveConfig();
    }

    @Test
    void sign_streakDay_grantsCouponOnce() {
        when(redisComponent.isSign(eq("U1"), anyString(), anyInt())).thenReturn(false);
        when(redisComponent.getContinuousDays("U1")).thenReturn(7);
        when(redisComponent.totalSignDays("U1")).thenReturn(7);
        when(redisComponent.getUsedCount("U1")).thenReturn(0);

        SignRewardConfigDTO config = new SignRewardConfigDTO();
        config.setEnabled(true);
        config.setStreakDays(7);
        config.setCouponId("CP1");
        when(signRewardConfigService.resolveActiveConfig()).thenReturn(config);
        when(redisComponent.setIfAbsent(anyString(), eq("1"), eq(60L), any())).thenReturn(true);
        DiscountCouponVO coupon = new DiscountCouponVO();
        coupon.setTotalCount(100);
        coupon.setRemainCount(50);
        coupon.setCouponName("签到礼券");
        when(couponFeignSupport.getCoupon("CP1")).thenReturn(coupon);
        when(couponFeignSupport.deductStock("CP1")).thenReturn(1);

when(redisComponent.setIfAbsent(startsWith("sign:growth:U1:"), eq("1"), eq(7L), any())).thenReturn(true);
        signService.sign("U1");

        verify(couponFeignSupport).createUserCoupon(any());
        verify(userNotificationService).sendAsync(eq("U1"), eq("签到奖励"), contains("连续签到 7 天"),
                eq("sign_reward"), anyString());
    }

    @Test
    void msign_noRemainCount_throws() {
        when(redisComponent.getRemainSignCount("U1")).thenReturn(0);

when(redisComponent.setIfAbsent(startsWith("sign:growth:U1:"), eq("1"), eq(7L), any())).thenReturn(true);
        assertThrows(BusinessException.class, () -> signService.msign("U1", todayStr()));
    }

    @Test
    void msign_badDateFormat_throws() {
        when(redisComponent.getRemainSignCount("U1")).thenReturn(2);

when(redisComponent.setIfAbsent(startsWith("sign:growth:U1:"), eq("1"), eq(7L), any())).thenReturn(true);
        assertThrows(BusinessException.class, () -> signService.msign("U1", "2026-01-05"));
    }

    @Test
    void msign_futureDate_throws() {
        when(redisComponent.getRemainSignCount("U1")).thenReturn(2);
        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

when(redisComponent.setIfAbsent(startsWith("sign:growth:U1:"), eq("1"), eq(7L), any())).thenReturn(true);
        assertThrows(BusinessException.class, () -> signService.msign("U1", tomorrow));
    }

    @Test
    void msign_previousMonth_throws() {
        when(redisComponent.getRemainSignCount("U1")).thenReturn(2);
        String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

when(redisComponent.setIfAbsent(startsWith("sign:growth:U1:"), eq("1"), eq(7L), any())).thenReturn(true);
        assertThrows(BusinessException.class, () -> signService.msign("U1", lastMonth));
    }

    @Test
    void msign_alreadySigned_throws() {
        when(redisComponent.getRemainSignCount("U1")).thenReturn(2);
        String today = todayStr();
        String yyyyMM = today.substring(0, 6);
        int day = Integer.parseInt(today.substring(6, 8));
        when(redisComponent.isSign(eq("U1"), eq(yyyyMM), eq(day))).thenReturn(true);

when(redisComponent.setIfAbsent(startsWith("sign:growth:U1:"), eq("1"), eq(7L), any())).thenReturn(true);
        assertThrows(BusinessException.class, () -> signService.msign("U1", today));
    }

    @Test
    void msign_success_supplementsAndNotifies() {
        when(redisComponent.getRemainSignCount("U1")).thenReturn(2);
        when(redisComponent.isSign(anyString(), anyString(), anyInt())).thenReturn(false);
        when(redisComponent.getContinuousDays("U1")).thenReturn(1);
        when(redisComponent.totalSignDays("U1")).thenReturn(2);
        when(redisComponent.getUsedCount("U1")).thenReturn(1);
        String today = todayStr();
        String yyyyMM = today.substring(0, 6);
        int day = Integer.parseInt(today.substring(6, 8));

when(redisComponent.setIfAbsent(startsWith("sign:growth:U1:"), eq("1"), eq(7L), any())).thenReturn(true);
        signService.msign("U1", today);

        verify(redisComponent).supplementSign("U1", yyyyMM, day);
        verify(userMemberProfileService).addGrowth("U1", 5);
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.SIGN_RECORD_EXCHANGE),
                eq(RabbitMQConfig.SIGN_RECORD_KEY),
                argThat(m -> m instanceof SignRecordMessageDTO), anyString(), any());
        verify(userNotificationService).sendAsync(eq("U1"), eq("补签成功"), anyString(), eq("sign"), eq(today));
    }

    private String todayStr() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
