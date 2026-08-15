package com.simlect.biz.impl;

import com.simlect.api.support.UserFeignSupport;
import com.simlect.mappers.UserCouponMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CouponReminderServiceImpl 优惠券到期提醒单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CouponReminderServiceImplTest {

    @Mock
    private UserCouponMapper<com.simlect.entity.po.UserCoupon, com.simlect.entity.query.UserCouponQuery> userCouponMapper;
    @Mock
    private UserFeignSupport userFeignSupport;
    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private CouponReminderServiceImpl couponReminderService;

    @Test
    void remindExpiringCoupons_noRows_skips() {
        when(userCouponMapper.selectExpiringUnusedByCursor(500, null, null)).thenReturn(null);

        couponReminderService.remindExpiringCoupons();

        verify(userFeignSupport, never()).sendNotifyAsync(any(), any(), any(), any(), any());
    }

    @Test
    void remindExpiringCoupons_emptyRows_skips() {
        when(userCouponMapper.selectExpiringUnusedByCursor(500, null, null)).thenReturn(List.of());

        couponReminderService.remindExpiringCoupons();

        verify(userFeignSupport, never()).sendNotifyAsync(any(), any(), any(), any(), any());
    }

    @Test
    void remindExpiringCoupons_sendsNotifyPerValidRow() {
        Map<String, Object> row1 = Map.of("userId", "U1", "userCouponId", "UC1", "couponName", "满减券");
        Map<String, Object> row2 = Map.of("userId", "U2", "userCouponId", "UC2", "couponName", "折扣券");
        when(userCouponMapper.selectExpiringUnusedByCursor(500, null, null)).thenReturn(List.of(row1, row2));
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        couponReminderService.remindExpiringCoupons();

        verify(userFeignSupport).sendNotifyAsync("U1", "优惠券即将过期",
                "您的「满减券」将在 3 天内过期，请尽快使用", "coupon_expire", "UC1");
        verify(userFeignSupport).sendNotifyAsync("U2", "优惠券即将过期",
                "您的「折扣券」将在 3 天内过期，请尽快使用", "coupon_expire", "UC2");
    }

    @Test
    void remindExpiringCoupons_skipsInvalidRows() {
        Map<String, Object> noUser = Map.of("userCouponId", "UC1", "couponName", "券");
        Map<String, Object> noCoupon = Map.of("userId", "U1", "couponName", "券");
        Map<String, Object> nullName = Map.of("userId", "U1", "userCouponId", "UC1");
        Map<String, Object> numericId = Map.of("userId", 12345L, "userCouponId", "UC9", "couponName", "券");
        when(userCouponMapper.selectExpiringUnusedByCursor(anyInt(), nullable(Date.class), nullable(String.class)))
                .thenReturn(List.of(noUser, noCoupon, nullName, numericId));
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        couponReminderService.remindExpiringCoupons();

        // nullName 行（用户券缺失名）与 numericId 行各发送一条
        verify(userFeignSupport).sendNotifyAsync("12345", "优惠券即将过期",
                "您的「券」将在 3 天内过期，请尽快使用", "coupon_expire", "UC9");
        verify(userFeignSupport).sendNotifyAsync("U1", "优惠券即将过期",
                "您的「优惠券」将在 3 天内过期，请尽快使用", "coupon_expire", "UC1");
        verify(userFeignSupport, org.mockito.Mockito.times(2))
                .sendNotifyAsync(any(), any(), any(), any(), any());
    }
}
