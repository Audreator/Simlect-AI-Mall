package com.simlect.biz.impl;

import com.simlect.api.support.CouponFeignSupport;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.component.RedisComponent;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.dto.MemberLevelRewardConfigDTO;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberLevelRewardConfigServiceImplTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private CouponFeignSupport couponFeignSupport;
    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private MemberLevelRewardConfigServiceImpl memberLevelRewardConfigService;

    @Test
    void getConfig_noCache_usesYml() {
        when(redisComponent.getMemberLevelRewardConfig()).thenReturn(null);
        when(appConfig.getMemberLevel2CouponId()).thenReturn("CP2");
        when(appConfig.getMemberLevel3CouponId()).thenReturn(null);

        MemberLevelRewardConfigDTO dto = memberLevelRewardConfigService.getConfig();

        assertEquals("CP2", dto.getLevel2CouponId());
        assertNull(dto.getLevel3CouponId());
    }

    @Test
    void getConfig_fromCache_enrichesNames() {
        MemberLevelRewardConfigDTO cached = new MemberLevelRewardConfigDTO();
        cached.setLevel2CouponId("CP2");
        cached.setLevel3CouponId("CP3");
        when(redisComponent.getMemberLevelRewardConfig()).thenReturn(cached);
        DiscountCouponVO c2 = new DiscountCouponVO();
        c2.setCouponName("银卡礼券");
        DiscountCouponVO c3 = new DiscountCouponVO();
        c3.setCouponName("金卡礼券");
        when(couponFeignSupport.getCoupon("CP2")).thenReturn(c2);
        when(couponFeignSupport.getCoupon("CP3")).thenReturn(c3);

        MemberLevelRewardConfigDTO dto = memberLevelRewardConfigService.getConfig();

        assertEquals("银卡礼券", dto.getLevel2CouponName());
        assertEquals("金卡礼券", dto.getLevel3CouponName());
    }

    @Test
    void saveConfig_null_throws() {
        assertThrows(BusinessException.class, () -> memberLevelRewardConfigService.saveConfig(null));
    }

    @Test
    void saveConfig_couponNotFound_throws() {
        MemberLevelRewardConfigDTO config = new MemberLevelRewardConfigDTO();
        config.setLevel2CouponId("CP2");
        when(couponFeignSupport.getCoupon("CP2")).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> memberLevelRewardConfigService.saveConfig(config));
        assertTrue(e.getMessage().contains("银卡"));
    }

    @Test
    void saveConfig_success_persists() {
        MemberLevelRewardConfigDTO config = new MemberLevelRewardConfigDTO();
        config.setLevel2CouponId(" CP2 ");
        config.setLevel3CouponId("");
        DiscountCouponVO c2 = new DiscountCouponVO();
        c2.setCouponName("银卡礼券");
        when(couponFeignSupport.getCoupon("CP2")).thenReturn(c2);

        memberLevelRewardConfigService.saveConfig(config);

        verify(redisComponent).saveMemberLevelRewardConfig(argThat(dto ->
                "CP2".equals(dto.getLevel2CouponId()) && dto.getLevel3CouponId() == null));
    }

    @Test
    void resolveLevelCouponId_redisConfigWins() {
        MemberLevelRewardConfigDTO cached = new MemberLevelRewardConfigDTO();
        cached.setLevel2CouponId("CP2-r");
        when(redisComponent.getMemberLevelRewardConfig()).thenReturn(cached);

        assertEquals("CP2-r", memberLevelRewardConfigService.resolveLevelCouponId(2));
    }

    @Test
    void resolveLevelCouponId_fallsBackToYml() {
        when(redisComponent.getMemberLevelRewardConfig()).thenReturn(null);
        when(appConfig.getMemberLevel2CouponId()).thenReturn(" CP2-y ");

        assertEquals("CP2-y", memberLevelRewardConfigService.resolveLevelCouponId(2));
        assertNull(memberLevelRewardConfigService.resolveLevelCouponId(1));
        assertNull(memberLevelRewardConfigService.resolveLevelCouponId(9));
    }
}
