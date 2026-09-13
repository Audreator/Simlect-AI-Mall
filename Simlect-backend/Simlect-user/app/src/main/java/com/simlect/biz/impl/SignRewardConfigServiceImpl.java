package com.simlect.biz.impl;

import com.simlect.api.support.CouponFeignSupport;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.component.RedisComponent;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.dto.SignRewardConfigDTO;
import com.simlect.exception.BusinessException;
import com.simlect.biz.SignRewardConfigService;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("signRewardConfigService")
@Slf4j
public class SignRewardConfigServiceImpl implements SignRewardConfigService {

    private static final int DEFAULT_STREAK_DAYS = 7;

    @Resource
    private RedisComponent redisComponent;
    @Resource
    private CouponFeignSupport couponFeignSupport;
    @Resource
    private AppConfig appConfig;

    @Override
    public SignRewardConfigDTO getConfig() {
        SignRewardConfigDTO dto = redisComponent.getSignRewardConfig();
        if (dto == null) {
            dto = defaultFromYml();
        }
        normalize(dto);
        enrichCouponName(dto);
        return dto;
    }

    @Override
    public void saveConfig(SignRewardConfigDTO config) {
        if (config == null) {
            throw new BusinessException("配置不能为空");
        }
        SignRewardConfigDTO toSave = new SignRewardConfigDTO();
        toSave.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        int streakDays = config.getStreakDays() == null ? DEFAULT_STREAK_DAYS : config.getStreakDays();
        if (streakDays < 1 || streakDays > 30) {
            throw new BusinessException("连续天数需为 1~30");
        }
        toSave.setStreakDays(streakDays);
        if (toSave.getEnabled()) {
            if (StringTools.isEmpty(config.getCouponId())) {
                throw new BusinessException("请选择要发放的优惠券");
            }
            String couponId = config.getCouponId().trim();
            DiscountCouponVO coupon = couponFeignSupport.getCoupon(couponId);
            if (coupon == null) {
                throw new BusinessException("优惠券不存在");
            }
            toSave.setCouponId(couponId);
            toSave.setCouponName(coupon.getCouponName());
        } else {
            toSave.setCouponId(null);
            toSave.setCouponName(null);
        }
        redisComponent.saveSignRewardConfig(toSave);
    }

    @Override
    public SignRewardConfigDTO resolveActiveConfig() {
        SignRewardConfigDTO dto;
        try {
            dto = redisComponent.getSignRewardConfig();
        } catch (Exception e) {
            log.warn("读取连续签到奖励配置失败，将使用本地默认配置", e);
            dto = null;
        }
        if (dto != null) {
            normalize(dto);
            return Boolean.TRUE.equals(dto.getEnabled()) && !StringTools.isEmpty(dto.getCouponId()) ? dto : null;
        }
        String ymlCouponId = appConfig.getSignStreakCouponId();
        if (StringTools.isEmpty(ymlCouponId)) {
            return null;
        }
        SignRewardConfigDTO fallback = new SignRewardConfigDTO();
        fallback.setEnabled(true);
        fallback.setCouponId(ymlCouponId);
        fallback.setStreakDays(DEFAULT_STREAK_DAYS);
        return fallback;
    }

    private SignRewardConfigDTO defaultFromYml() {
        SignRewardConfigDTO dto = new SignRewardConfigDTO();
        dto.setEnabled(false);
        dto.setStreakDays(DEFAULT_STREAK_DAYS);
        String ymlId = appConfig.getSignStreakCouponId();
        if (!StringTools.isEmpty(ymlId)) {
            dto.setEnabled(true);
            dto.setCouponId(ymlId);
        }
        return dto;
    }

    private void normalize(SignRewardConfigDTO dto) {
        if (dto.getEnabled() == null) {
            dto.setEnabled(false);
        }
        if (dto.getStreakDays() == null || dto.getStreakDays() < 1) {
            dto.setStreakDays(DEFAULT_STREAK_DAYS);
        }
    }

    private void enrichCouponName(SignRewardConfigDTO dto) {
        if (StringTools.isEmpty(dto.getCouponId())) {
            return;
        }
        try {
            DiscountCouponVO coupon = couponFeignSupport.getCoupon(dto.getCouponId());
            if (coupon != null) {
                dto.setCouponName(coupon.getCouponName());
            }
        } catch (Exception ignored) {
            // 配置页展示名可选
        }
    }
}
