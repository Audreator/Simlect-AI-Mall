package com.simlect.biz.impl;

import com.simlect.api.dto.UserCouponCreateDTO;
import com.simlect.api.support.CouponFeignSupport;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.component.RedisComponent;
import com.simlect.entity.enums.ResponseCodeEnum;
import com.simlect.api.enums.UserCouponStatusEnum;
import com.simlect.entity.po.UserMemberProfile;
import com.simlect.entity.vo.MemberCenterVO;
import com.simlect.api.vo.MemberLevelRewardVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserMemberProfileMapper;
import com.simlect.biz.MemberLevelRewardConfigService;
import com.simlect.biz.UserMemberProfileService;
import com.simlect.biz.UserNotificationService;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service("userMemberProfileService")
public class UserMemberProfileServiceImpl implements UserMemberProfileService {

    private static final int SILVER_GROWTH = 1000;
    private static final int GOLD_GROWTH = 5000;
    private static final int BONUS_GROWTH_SILVER = 20;
    private static final int BONUS_GROWTH_GOLD = 50;

    @Resource
    private MemberLevelRewardConfigService memberLevelRewardConfigService;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserNotificationService userNotificationService;
    @Resource
    private CouponFeignSupport couponFeignSupport;
    @Resource
    private UserMemberProfileMapper<UserMemberProfile, com.simlect.entity.query.UserMemberProfileQuery> userMemberProfileMapper;

    @Override
    public UserMemberProfile getOrInitProfile(String userId) {
        UserMemberProfile profile = userMemberProfileMapper.selectByUserId(userId);
        if (profile != null) {
            refreshLevel(profile);
            return profile;
        }
        profile = new UserMemberProfile();
        profile.setUserId(userId);
        profile.setGrowthValue(0);
        profile.setLevelCode(1);
        profile.setLevelName("普通会员");
        profile.setUpdateTime(new Date());
        userMemberProfileMapper.insert(profile);
        return profile;
    }

    @Override
    public MemberCenterVO getMemberCenter(String userId) {
        UserMemberProfile profile = getOrInitProfile(userId);
        Set<Integer> claimed = redisComponent.getMemberLevelClaimed(userId);
        int growth = profile.getGrowthValue() == null ? 0 : profile.getGrowthValue();
        int levelCode = profile.getLevelCode() == null ? 1 : profile.getLevelCode();

        List<MemberLevelRewardVO> rewards = new ArrayList<>();
        rewards.add(buildReward(1, "普通会员", 0, "入会礼遇", "注册即享基础会员权益", growth, levelCode, claimed));
        rewards.add(buildReward(2, "银卡会员", SILVER_GROWTH, "银卡升级礼", "专属优惠券 + " + BONUS_GROWTH_SILVER + " 成长值", growth, levelCode, claimed));
        rewards.add(buildReward(3, "金卡会员", GOLD_GROWTH, "金卡升级礼", "专属优惠券 + " + BONUS_GROWTH_GOLD + " 成长值", growth, levelCode, claimed));

        MemberCenterVO vo = new MemberCenterVO();
        vo.setProfile(profile);
        vo.setRewards(rewards);
        if (growth < SILVER_GROWTH) {
            vo.setNextLevelCode(2);
            vo.setNextLevelGrowth(SILVER_GROWTH);
            vo.setGrowthToNext(SILVER_GROWTH - growth);
        } else if (growth < GOLD_GROWTH) {
            vo.setNextLevelCode(3);
            vo.setNextLevelGrowth(GOLD_GROWTH);
            vo.setGrowthToNext(GOLD_GROWTH - growth);
        } else {
            vo.setNextLevelCode(null);
            vo.setNextLevelGrowth(null);
            vo.setGrowthToNext(0);
        }
        return vo;
    }

    private MemberLevelRewardVO buildReward(int code, String name, int threshold, String title, String desc,
                                            int growth, int levelCode, Set<Integer> claimed) {
        MemberLevelRewardVO item = new MemberLevelRewardVO();
        item.setLevelCode(code);
        item.setLevelName(name);
        item.setGrowthThreshold(threshold);
        item.setRewardTitle(title);
        item.setRewardDesc(desc);
        boolean unlocked = growth >= threshold && levelCode >= code;
        item.setUnlocked(unlocked);
        boolean isClaimed = claimed.contains(code);
        item.setClaimed(isClaimed);
        item.setClaimable(unlocked && code > 1 && !isClaimed);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimLevelReward(String userId, Integer levelCode) {
        if (levelCode == null || (levelCode != 2 && levelCode != 3)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserMemberProfile profile = getOrInitProfile(userId);
        int growth = profile.getGrowthValue() == null ? 0 : profile.getGrowthValue();
        int threshold = levelCode == 2 ? SILVER_GROWTH : GOLD_GROWTH;
        if (growth < threshold) {
            throw new BusinessException("成长值未达标，暂无法领取");
        }
        Set<Integer> claimed = redisComponent.getMemberLevelClaimed(userId);
        if (claimed.contains(levelCode)) {
            throw new BusinessException("该等级奖励已领取");
        }
        String couponId = memberLevelRewardConfigService.resolveLevelCouponId(levelCode);
        String levelName = levelCode == 2 ? "银卡会员" : "金卡会员";
        int bonusGrowth = levelCode == 2 ? BONUS_GROWTH_SILVER : BONUS_GROWTH_GOLD;
        String couponName = tryGrantCoupon(userId, couponId, levelCode);
        addGrowth(userId, bonusGrowth);
        redisComponent.addMemberLevelClaimed(userId, levelCode);
        String content = "恭喜升级为「" + levelName + "」！已发放 " + bonusGrowth + " 成长值";
        if (!StringTools.isEmpty(couponName)) {
            content += "，优惠券「" + couponName + "」已放入「我的优惠券」";
        }
        userNotificationService.sendAsync(userId, "会员升级礼", content, "member_level", String.valueOf(levelCode));
    }

    private String tryGrantCoupon(String userId, String couponId, int levelCode) {
        if (StringTools.isEmpty(couponId)) {
            return null;
        }
        DiscountCouponVO coupon = couponFeignSupport.getCoupon(couponId);
        if (coupon == null) {
            throw new BusinessException("升级礼券不存在，请联系管理员配置");
        }
        boolean unlimited = coupon.getTotalCount() != null && coupon.getTotalCount() == 0;
        if (!unlimited && (coupon.getRemainCount() == null || coupon.getRemainCount() <= 0)) {
            throw new BusinessException("升级礼券库存不足，请联系管理员补充");
        }
        int affected = couponFeignSupport.deductStock(couponId);
        if (affected == 0) {
            throw new BusinessException("升级礼券发放失败，请稍后重试");
        }
        String userCouponId = StringTools.createUserCouponId();
        UserCouponCreateDTO createDTO = new UserCouponCreateDTO();
        createDTO.setUserCouponId(userCouponId);
        createDTO.setUserId(userId);
        createDTO.setCouponId(couponId);
        createDTO.setStatus(UserCouponStatusEnum.NOUSE.getStatus());
        couponFeignSupport.createUserCoupon(createDTO);
        return coupon.getCouponName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGrowthOnPay(String userId, BigDecimal payAmount) {
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        int add = payAmount.intValue() / 100;
        if (add < 1) {
            add = 1;
        }
        addGrowth(userId, add);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGrowth(String userId, int points) {
        if (points <= 0) {
            return;
        }
        // 确保 profile 行存在
        getOrInitProfile(userId);
        // DB 原子自增（防并发读改写丢失更新）
        Integer affected = userMemberProfileMapper.addGrowthValue(userId, points);
        if (affected == null || affected == 0) {
            throw new BusinessException("成长值更新失败，请稍后重试");
        }
        // 读回最新成长值刷新等级（成长值单调递增，等级最终一致）
        UserMemberProfile latest = userMemberProfileMapper.selectByUserId(userId);
        if (latest != null) {
            refreshLevel(latest);
            latest.setUpdateTime(new Date());
            userMemberProfileMapper.updateByUserId(latest, userId);
        }
    }

    private void refreshLevel(UserMemberProfile profile) {
        int growth = profile.getGrowthValue() == null ? 0 : profile.getGrowthValue();
        if (growth >= 5000) {
            profile.setLevelCode(3);
            profile.setLevelName("金卡会员");
        } else if (growth >= 1000) {
            profile.setLevelCode(2);
            profile.setLevelName("银卡会员");
        } else {
            profile.setLevelCode(1);
            profile.setLevelName("普通会员");
        }
    }
}
