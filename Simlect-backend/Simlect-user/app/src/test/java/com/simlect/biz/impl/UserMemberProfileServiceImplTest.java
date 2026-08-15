package com.simlect.biz.impl;

import com.simlect.api.dto.UserCouponCreateDTO;
import com.simlect.api.support.CouponFeignSupport;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.api.vo.MemberLevelRewardVO;
import com.simlect.biz.MemberLevelRewardConfigService;
import com.simlect.biz.UserNotificationService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.po.UserMemberProfile;
import com.simlect.entity.vo.MemberCenterVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserMemberProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserMemberProfileServiceImplTest {

    @Mock
    private MemberLevelRewardConfigService memberLevelRewardConfigService;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private UserNotificationService userNotificationService;
    @Mock
    private CouponFeignSupport couponFeignSupport;
    @Mock
    private UserMemberProfileMapper<UserMemberProfile, com.simlect.entity.query.UserMemberProfileQuery> userMemberProfileMapper;

    @InjectMocks
    private UserMemberProfileServiceImpl userMemberProfileService;

    private UserMemberProfile buildProfile(int growth, int levelCode) {
        UserMemberProfile profile = new UserMemberProfile();
        profile.setUserId("U1");
        profile.setGrowthValue(growth);
        profile.setLevelCode(levelCode);
        return profile;
    }

    @Test
    void getOrInitProfile_existing_refreshesLevel() {
        UserMemberProfile profile = buildProfile(2000, 1);
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(profile);

        UserMemberProfile result = userMemberProfileService.getOrInitProfile("U1");

        assertEquals(2, result.getLevelCode());
        assertEquals("银卡会员", result.getLevelName());
        verify(userMemberProfileMapper, never()).insert(any());
    }

    @Test
    void getOrInitProfile_missing_createsNormalMember() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(null);

        UserMemberProfile result = userMemberProfileService.getOrInitProfile("U1");

        assertEquals(1, result.getLevelCode());
        assertEquals("普通会员", result.getLevelName());
        assertEquals(0, result.getGrowthValue());
        verify(userMemberProfileMapper).insert(result);
    }

    @Test
    void getMemberCenter_silverLevel_nextIsGold() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(1000, 2));
        when(redisComponent.getMemberLevelClaimed("U1")).thenReturn(new HashSet<>(Set.of(2)));

        MemberCenterVO vo = userMemberProfileService.getMemberCenter("U1");

        assertNotNull(vo.getProfile());
        assertEquals(3, vo.getRewards().size());
        assertEquals(3, vo.getNextLevelCode());
        assertEquals(5000, vo.getNextLevelGrowth());
        assertEquals(4000, vo.getGrowthToNext());
        MemberLevelRewardVO silver = vo.getRewards().get(1);
        assertTrue(silver.getUnlocked());
        assertTrue(silver.getClaimed());
        assertFalse(silver.getClaimable());
    }

    @Test
    void getMemberCenter_goldLevel_noNextLevel() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(6000, 3));
        when(redisComponent.getMemberLevelClaimed("U1")).thenReturn(new HashSet<>());

        MemberCenterVO vo = userMemberProfileService.getMemberCenter("U1");

        assertNull(vo.getNextLevelCode());
        assertEquals(0, vo.getGrowthToNext());
        assertTrue(vo.getRewards().get(2).getUnlocked());
        assertTrue(vo.getRewards().get(2).getClaimable());
    }

    @Test
    void claimLevelReward_invalidLevel_throws() {
        assertThrows(BusinessException.class, () -> userMemberProfileService.claimLevelReward("U1", null));
        assertThrows(BusinessException.class, () -> userMemberProfileService.claimLevelReward("U1", 4));
    }

    @Test
    void claimLevelReward_growthBelowThreshold_throws() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(500, 1));

        assertThrows(BusinessException.class, () -> userMemberProfileService.claimLevelReward("U1", 2));
    }

    @Test
    void claimLevelReward_alreadyClaimed_throws() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(2000, 2));
        when(redisComponent.getMemberLevelClaimed("U1")).thenReturn(new HashSet<>(Set.of(2)));

        assertThrows(BusinessException.class, () -> userMemberProfileService.claimLevelReward("U1", 2));
    }

    @Test
    void claimLevelReward_success_noCouponConfigured() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(2000, 2));
        when(redisComponent.getMemberLevelClaimed("U1")).thenReturn(new HashSet<>());
        when(memberLevelRewardConfigService.resolveLevelCouponId(2)).thenReturn(null);
        when(userMemberProfileMapper.addGrowthValue("U1", 20)).thenReturn(1);
        // 原子自增后读回：2000 + 20 = 2020
        when(userMemberProfileMapper.selectByUserId("U1"))
                .thenReturn(buildProfile(2000, 2))
                .thenReturn(buildProfile(2020, 2));
        when(userMemberProfileMapper.updateByUserId(any(), eq("U1"))).thenReturn(1);

        userMemberProfileService.claimLevelReward("U1", 2);

        verify(redisComponent).addMemberLevelClaimed("U1", 2);
        verify(userNotificationService).sendAsync(eq("U1"), eq("会员升级礼"), anyString(), eq("member_level"), eq("2"));
        verify(userMemberProfileMapper).addGrowthValue("U1", 20);
    }

    @Test
    void claimLevelReward_couponNotFound_throws() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(2000, 2));
        when(redisComponent.getMemberLevelClaimed("U1")).thenReturn(new HashSet<>());
        when(memberLevelRewardConfigService.resolveLevelCouponId(2)).thenReturn("CP1");
        when(couponFeignSupport.getCoupon("CP1")).thenReturn(null);

        assertThrows(BusinessException.class, () -> userMemberProfileService.claimLevelReward("U1", 2));
    }

    @Test
    void claimLevelReward_couponStockEmpty_throws() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(2000, 2));
        when(redisComponent.getMemberLevelClaimed("U1")).thenReturn(new HashSet<>());
        when(memberLevelRewardConfigService.resolveLevelCouponId(2)).thenReturn("CP1");
        DiscountCouponVO coupon = new DiscountCouponVO();
        coupon.setTotalCount(10);
        coupon.setRemainCount(0);
        when(couponFeignSupport.getCoupon("CP1")).thenReturn(coupon);

        assertThrows(BusinessException.class, () -> userMemberProfileService.claimLevelReward("U1", 2));
    }

    @Test
    void claimLevelReward_couponGranted_createsUserCoupon() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(2000, 2));
        when(redisComponent.getMemberLevelClaimed("U1")).thenReturn(new HashSet<>());
        when(memberLevelRewardConfigService.resolveLevelCouponId(2)).thenReturn("CP1");
        DiscountCouponVO coupon = new DiscountCouponVO();
        coupon.setTotalCount(0);
        coupon.setRemainCount(5);
        coupon.setCouponName("银卡礼券");
        when(couponFeignSupport.getCoupon("CP1")).thenReturn(coupon);
        when(couponFeignSupport.deductStock("CP1")).thenReturn(1);
        when(userMemberProfileMapper.addGrowthValue("U1", 20)).thenReturn(1);
        when(userMemberProfileMapper.selectByUserId("U1"))
                .thenReturn(buildProfile(2000, 2))
                .thenReturn(buildProfile(2020, 2));
        when(userMemberProfileMapper.updateByUserId(any(), eq("U1"))).thenReturn(1);

        userMemberProfileService.claimLevelReward("U1", 2);

        verify(couponFeignSupport).createUserCoupon(any(UserCouponCreateDTO.class));
        verify(userNotificationService).sendAsync(eq("U1"), eq("会员升级礼"), contains("银卡礼券"), eq("member_level"), eq("2"));
    }

    @Test
    void addGrowthOnPay_nonPositive_ignored() {
        userMemberProfileService.addGrowthOnPay("U1", null);
        userMemberProfileService.addGrowthOnPay("U1", BigDecimal.ZERO);
        userMemberProfileService.addGrowthOnPay("U1", new BigDecimal("-5"));

        verifyNoInteractions(userMemberProfileMapper);
    }

    @Test
    void addGrowthOnPay_hundredPerPoint() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(0, 1));
        when(userMemberProfileMapper.addGrowthValue("U1", 50)).thenReturn(1);
        // 原子自增后读回：0 + 50 = 50
        when(userMemberProfileMapper.selectByUserId("U1"))
                .thenReturn(buildProfile(0, 1))
                .thenReturn(buildProfile(50, 1));
        when(userMemberProfileMapper.updateByUserId(any(), eq("U1"))).thenReturn(1);

        userMemberProfileService.addGrowthOnPay("U1", new BigDecimal("5000"));

        verify(userMemberProfileMapper).addGrowthValue("U1", 50);
    }

    @Test
    void addGrowth_nonPositive_ignored() {
        userMemberProfileService.addGrowth("U1", 0);
        userMemberProfileService.addGrowth("U1", -1);

        verifyNoInteractions(userMemberProfileMapper);
    }

    @Test
    void addGrowth_updatesProfileAndLevel() {
        when(userMemberProfileMapper.selectByUserId("U1")).thenReturn(buildProfile(1000, 2));
        when(userMemberProfileMapper.addGrowthValue("U1", 4001)).thenReturn(1);
        // 原子自增后读回：1000 + 4001 = 5001 → 金卡
        when(userMemberProfileMapper.selectByUserId("U1"))
                .thenReturn(buildProfile(1000, 2))
                .thenReturn(buildProfile(5001, 2));
        when(userMemberProfileMapper.updateByUserId(any(), eq("U1"))).thenReturn(1);

        userMemberProfileService.addGrowth("U1", 4001);

        verify(userMemberProfileMapper).addGrowthValue("U1", 4001);
        verify(userMemberProfileMapper).updateByUserId(argThat(p ->
                p.getGrowthValue() == 5001 && 3 == p.getLevelCode() && "金卡会员".equals(p.getLevelName())), eq("U1"));
    }
}
