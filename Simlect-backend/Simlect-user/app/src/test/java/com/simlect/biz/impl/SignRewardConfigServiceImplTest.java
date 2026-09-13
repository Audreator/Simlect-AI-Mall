package com.simlect.biz.impl;

import com.simlect.api.support.CouponFeignSupport;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.component.RedisComponent;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.dto.SignRewardConfigDTO;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignRewardConfigServiceImplTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private CouponFeignSupport couponFeignSupport;
    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private SignRewardConfigServiceImpl signRewardConfigService;

    @Test
    void getConfig_noCache_usesYmlDefaults() {
        when(redisComponent.getSignRewardConfig()).thenReturn(null);
        when(appConfig.getSignStreakCouponId()).thenReturn(null);

        SignRewardConfigDTO dto = signRewardConfigService.getConfig();

        assertNotNull(dto);
        assertEquals(7, dto.getStreakDays());
        assertFalse(dto.getEnabled());
    }

    @Test
    void getConfig_ymlCouponId_enables() {
        when(redisComponent.getSignRewardConfig()).thenReturn(null);
        when(appConfig.getSignStreakCouponId()).thenReturn("CP-yml");

        SignRewardConfigDTO dto = signRewardConfigService.getConfig();

        assertTrue(dto.getEnabled());
        assertEquals("CP-yml", dto.getCouponId());
    }

    @Test
    void getConfig_fromCache_enrichesCouponName() {
        SignRewardConfigDTO cached = new SignRewardConfigDTO();
        cached.setEnabled(true);
        cached.setStreakDays(10);
        cached.setCouponId("CP1");
        when(redisComponent.getSignRewardConfig()).thenReturn(cached);
        DiscountCouponVO coupon = new DiscountCouponVO();
        coupon.setCouponName("签到礼");
        when(couponFeignSupport.getCoupon("CP1")).thenReturn(coupon);

        SignRewardConfigDTO dto = signRewardConfigService.getConfig();

        assertEquals("签到礼", dto.getCouponName());
    }

    @Test
    void saveConfig_null_throws() {
        assertThrows(BusinessException.class, () -> signRewardConfigService.saveConfig(null));
    }

    @Test
    void saveConfig_streakOutOfRange_throws() {
        SignRewardConfigDTO config = new SignRewardConfigDTO();
        config.setStreakDays(31);

        assertThrows(BusinessException.class, () -> signRewardConfigService.saveConfig(config));
    }

    @Test
    void saveConfig_enabledWithoutCoupon_throws() {
        SignRewardConfigDTO config = new SignRewardConfigDTO();
        config.setEnabled(true);
        config.setStreakDays(7);
        config.setCouponId(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> signRewardConfigService.saveConfig(config));
        assertTrue(e.getMessage().contains("优惠券"));
    }

    @Test
    void saveConfig_enabledCouponNotFound_throws() {
        SignRewardConfigDTO config = new SignRewardConfigDTO();
        config.setEnabled(true);
        config.setStreakDays(7);
        config.setCouponId("CP1");
        when(couponFeignSupport.getCoupon("CP1")).thenReturn(null);

        assertThrows(BusinessException.class, () -> signRewardConfigService.saveConfig(config));
    }

    @Test
    void saveConfig_success_persistsTrimmed() {
        SignRewardConfigDTO config = new SignRewardConfigDTO();
        config.setEnabled(true);
        config.setStreakDays(7);
        config.setCouponId(" CP1 ");
        DiscountCouponVO coupon = new DiscountCouponVO();
        coupon.setCouponName("签到礼");
        when(couponFeignSupport.getCoupon("CP1")).thenReturn(coupon);

        signRewardConfigService.saveConfig(config);

        verify(redisComponent).saveSignRewardConfig(argThat(dto ->
                "CP1".equals(dto.getCouponId()) && dto.getEnabled() && 7 == dto.getStreakDays()));
    }

    @Test
    void saveConfig_disabled_clearsCoupon() {
        SignRewardConfigDTO config = new SignRewardConfigDTO();
        config.setEnabled(false);
        config.setStreakDays(7);
        config.setCouponId("CP1");

        signRewardConfigService.saveConfig(config);

        verify(redisComponent).saveSignRewardConfig(argThat(dto ->
                dto.getCouponId() == null && !dto.getEnabled()));
        verifyNoInteractions(couponFeignSupport);
    }

    @Test
    void resolveActiveConfig_noRedisConfig_usesYml() {
        when(redisComponent.getSignRewardConfig()).thenReturn(null);
        when(appConfig.getSignStreakCouponId()).thenReturn("CP-yml");

        SignRewardConfigDTO dto = signRewardConfigService.resolveActiveConfig();

        assertNotNull(dto);
        assertTrue(dto.getEnabled());
        assertEquals("CP-yml", dto.getCouponId());
        assertEquals(7, dto.getStreakDays());
    }

    @Test
    void resolveActiveConfig_noRedisNoYml_returnsNull() {
        when(redisComponent.getSignRewardConfig()).thenReturn(null);
        when(appConfig.getSignStreakCouponId()).thenReturn(null);

        assertNull(signRewardConfigService.resolveActiveConfig());
    }

    @Test
    void resolveActiveConfig_disabledRedisConfig_overridesYmlFallback() {
        SignRewardConfigDTO cached = new SignRewardConfigDTO();
        cached.setEnabled(false);
        cached.setCouponId("CP1");
        when(redisComponent.getSignRewardConfig()).thenReturn(cached);

        assertNull(signRewardConfigService.resolveActiveConfig());
        verify(appConfig, never()).getSignStreakCouponId();
    }

    @Test
    void resolveActiveConfig_redisFailure_usesYmlWithoutBreakingSignFlow() {
        when(redisComponent.getSignRewardConfig()).thenThrow(new IllegalStateException("redis down"));
        when(appConfig.getSignStreakCouponId()).thenReturn("CP-yml");

        SignRewardConfigDTO dto = signRewardConfigService.resolveActiveConfig();

        assertNotNull(dto);
        assertTrue(dto.getEnabled());
        assertEquals("CP-yml", dto.getCouponId());
        assertEquals(7, dto.getStreakDays());
    }
}
