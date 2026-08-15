package com.simlect.biz.impl;

import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.support.MqIdempotencyKeys;
import com.simlect.entity.dto.SignRewardConfigDTO;
import com.simlect.api.dto.SignRecordMessageDTO;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.enums.ResponseCodeEnum;
import com.simlect.api.dto.UserCouponCreateDTO;
import com.simlect.api.support.CouponFeignSupport;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.api.enums.UserCouponStatusEnum;
import com.simlect.utils.StringTools;
import com.simlect.api.vo.SignDataVO;
import com.simlect.exception.BusinessException;
import com.simlect.biz.SignCalendarCacheService;
import com.simlect.biz.SignRewardConfigService;
import com.simlect.biz.SignRecordSyncService;
import com.simlect.biz.SignService;
import com.simlect.biz.UserMemberProfileService;
import com.simlect.biz.UserNotificationService;
import com.simlect.utils.DateUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SignServiceImpl implements SignService {

    private static final Logger log = LoggerFactory.getLogger(SignServiceImpl.class);

    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserMemberProfileService userMemberProfileService;
    @Resource
    private UserNotificationService userNotificationService;
    @Resource
    private SignRewardConfigService signRewardConfigService;
    @Resource
    private CouponFeignSupport couponFeignSupport;
    @Resource
    private ReliableMessageSender reliableMessageSender;
    @Resource
    private SignRecordSyncService signRecordSyncService;
    @Resource
    private SignCalendarCacheService signCalendarCacheService;

    private static final int SIGN_GROWTH = 5;
    private static final String SIGN_COUPON_DEDUP = "sign:streak:coupon:";

    @Override
    public SignDataVO getSignCalendar(String userId, String yyyyMM) {
        signRecordSyncService.ensureHashHydrated(userId);
        signCalendarCacheService.ensureCalendarHydrated(userId);
        SignDataVO signDataVO = new SignDataVO();
        if (yyyyMM == null || yyyyMM.length() != 6) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        Integer continuousDays = redisComponent.getContinuousDays(userId);
        Integer remainSignCount = redisComponent.getRemainSignCount(userId);
        List<String> signDateList = new ArrayList<>();
        int days = getDayofMonth(yyyyMM);
        for (int day = 1; day <= days; day++) {
            if (redisComponent.isSign(userId, yyyyMM, day)) {
                String date = yyyyMM + String.format("%02d", day);
                signDateList.add(date);
            }
        }
        signDataVO.setContinuousDays(continuousDays);
        signDataVO.setSupplementCount(remainSignCount);
        signDataVO.setTotalSignDays(redisComponent.totalSignDays(userId));
        signDataVO.setSignDays(signDateList);
        return signDataVO;
    }

    @Override
    public void sign(String userId) {
        signRecordSyncService.ensureHashHydrated(userId);
        String yyyyMM = DateUtil.getTimeOnParttern(0, "yyyyMM");
        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();
        if (redisComponent.isSign(userId, yyyyMM, dayOfMonth)) {
            throw new BusinessException("今天已经签到过了哦~");
        }
        // Lua 原子置位（互斥：并发双请求只有一个成功），成功后加成长值（幂等键防重复加）
        redisComponent.sign(userId);
        grantSignGrowthOnce(userId, DateUtil.getTimeOnParttern(0, "yyyyMMdd"));
        int continuousDays = redisComponent.getContinuousDays(userId);
        int totalSignDays = redisComponent.totalSignDays(userId);
        int usedCount = redisComponent.getUsedCount(userId);
        sendSignRecordToMQ(userId, continuousDays, totalSignDays, usedCount,
                DateUtil.getTimeOnParttern(0, "yyyyMMdd"), 0);
        tryGrantStreakCoupon(userId, continuousDays);
    }

    @Override
    public void msign(String userId, String yyyyMMdd) {
        signRecordSyncService.ensureHashHydrated(userId);
        if (redisComponent.getRemainSignCount(userId) <= 0) {
            throw new BusinessException("补签次数不足");
        }
        // 日期合法性校验：必须为 yyyyMMdd、是合法日期、不早于本月 1 日、不晚于今天
        if (yyyyMMdd == null || yyyyMMdd.length() != 8) {
            throw new BusinessException("日期格式错误");
        }
        LocalDate signDate;
        try {
            signDate = LocalDate.parse(yyyyMMdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            throw new BusinessException("日期格式错误");
        }
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        if (signDate.isBefore(monthStart)) {
            throw new BusinessException("只能补签本月内的日期");
        }
        if (signDate.isAfter(today)) {
            throw new BusinessException("不能补签未来日期");
        }
        String yyyyMM = yyyyMMdd.substring(0, 6);
        int dayOfMonth = Integer.parseInt(yyyyMMdd.substring(6, 8));
        if (redisComponent.isSign(userId, yyyyMM, dayOfMonth)) {
            throw new BusinessException("该日期已经签到过了哦~");
        }
        // Lua 原子补签位（互斥），成功后加成长值（幂等键防重复加）
        redisComponent.supplementSign(userId, yyyyMM, dayOfMonth);
        grantSignGrowthOnce(userId, yyyyMMdd);
        int continuousDays = redisComponent.getContinuousDays(userId);
        int totalSignDays = redisComponent.totalSignDays(userId);
        int usedCount = redisComponent.getUsedCount(userId);
        sendSignRecordToMQ(userId, continuousDays, totalSignDays, usedCount, yyyyMMdd, 1);
        tryGrantStreakCoupon(userId, continuousDays);
        userNotificationService.sendAsync(userId, "补签成功",
                "补签已获得 " + SIGN_GROWTH + " 成长值", "sign", yyyyMMdd);
    }

    /** 签到/补签成长值发放：按 用户+日期 幂等（防并发/重试重复 +5） */
    private void grantSignGrowthOnce(String userId, String yyyyMMdd) {
        String growthKey = "sign:growth:" + userId + ":" + yyyyMMdd;
        if (redisComponent.setIfAbsent(growthKey, "1", 7, java.util.concurrent.TimeUnit.DAYS)) {
            userMemberProfileService.addGrowth(userId, SIGN_GROWTH);
        }
    }

    private void sendSignRecordToMQ(String userId, int continuousDays, int totalSignDays, int usedCount,
                                    String signDate, int signType) {
        try {
            SignRecordMessageDTO message = new SignRecordMessageDTO(
                    userId, continuousDays, totalSignDays, usedCount, signDate, signType);
            reliableMessageSender.sendMessage(
                    RabbitMQConfig.SIGN_RECORD_EXCHANGE,
                    RabbitMQConfig.SIGN_RECORD_KEY,
                    message,
                    MqIdempotencyKeys.signRecord(userId, signDate),
                    MessageReliabilityLevelEnum.HIGH);
            log.info("签到记录已发送到MQ, userId: {}, continuousDays: {}, totalSignDays: {}, usedCount: {}",
                    userId, continuousDays, totalSignDays, usedCount);
        } catch (Exception e) {
            log.error("发送签到记录到MQ失败, userId: {}", userId, e);
            throw e;
        }
    }

    private void tryGrantStreakCoupon(String userId, int continuousDays) {
        SignRewardConfigDTO config = signRewardConfigService.resolveActiveConfig();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        int streakDays = config.getStreakDays() == null ? 7 : config.getStreakDays();
        if (streakDays < 1 || continuousDays < streakDays || continuousDays % streakDays != 0) {
            return;
        }
        String couponId = config.getCouponId();
        if (StringTools.isEmpty(couponId)) {
            return;
        }
        String dedupKey = SIGN_COUPON_DEDUP + userId + ":" + continuousDays;
        if (!redisComponent.setIfAbsent(dedupKey, "1", 60, TimeUnit.DAYS)) {
            return;
        }
        DiscountCouponVO coupon;
        try {
            coupon = couponFeignSupport.getCoupon(couponId);
        } catch (Exception e) {
            log.warn("签到发券查询失败 couponId={}", couponId, e);
            return;
        }
        if (coupon == null) {
            return;
        }
        boolean unlimited = coupon.getTotalCount() != null && coupon.getTotalCount() == 0;
        if (!unlimited && (coupon.getRemainCount() == null || coupon.getRemainCount() <= 0)) {
            return;
        }
        int affected = couponFeignSupport.deductStock(couponId);
        if (affected == 0) {
            return;
        }
        String userCouponId = StringTools.createUserCouponId();
        UserCouponCreateDTO createDTO = new UserCouponCreateDTO();
        createDTO.setUserCouponId(userCouponId);
        createDTO.setUserId(userId);
        createDTO.setCouponId(couponId);
        createDTO.setStatus(UserCouponStatusEnum.NOUSE.getStatus());
        couponFeignSupport.createUserCoupon(createDTO);
        userNotificationService.sendAsync(userId, "签到奖励",
                "连续签到 " + continuousDays + " 天，已发放优惠券「" + coupon.getCouponName() + "」",
                "sign_reward", userCouponId);
    }

    private int getDayofMonth(String yyyyMM){
        if (yyyyMM == null || !yyyyMM.matches("\\d{6}")) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        int year = Integer.parseInt(yyyyMM.substring(0, 4));
        int month = Integer.parseInt(yyyyMM.substring(4, 6));
        if (month < 1 || month > 12) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.lengthOfMonth();
    }
}
