package com.simlect.biz.impl;

import com.simlect.api.dto.NotificationMessageDTO;
import com.simlect.api.dto.SignStreakCouponMessageDTO;
import com.simlect.api.vo.SignDataVO;
import com.simlect.biz.SignRewardConfigService;
import com.simlect.biz.SignService;
import com.simlect.biz.SignStreakService;
import com.simlect.biz.UserMemberProfileService;
import com.simlect.component.RedisComponent;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.TransactionalMqSender;
import com.simlect.entity.dto.SignRewardConfigDTO;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.enums.ResponseCodeEnum;
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
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SignServiceImpl implements SignService {

    private static final Logger log = LoggerFactory.getLogger(SignServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("uuuuMM");
    private static final int SIGN_GROWTH = 5;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private Clock businessClock;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserMemberProfileService userMemberProfileService;
    @Resource
    private SignRewardConfigService signRewardConfigService;
    @Resource
    private SignStreakService signStreakService;
    @Resource
    private TransactionalMqSender transactionalMqSender;
    @Resource
    private SignBitmapMapper signBitmapMapper;
    @Resource
    private SignGrowthRecordMapper signGrowthRecordMapper;
    @Resource
    private SignSupplementUsedMapper signSupplementUsedMapper;
    @Resource
    private UserMemberProfileMapper<UserMemberProfile, UserMemberProfileQuery> userMemberProfileMapper;
    @Resource
    private UserSignRecordDetailMapper<UserSignRecordDetail, UserSignRecordDetailQuery> signRecordDetailMapper;

    @Override
    @Transactional(readOnly = true)
    public SignDataVO getSignCalendar(String userId, String yyyyMM) {
        LocalDate today = businessDate(businessClock.instant());
        YearMonth requestedMonth = parseMonth(yyyyMM);
        long bits = loadCalendarBits(userId, requestedMonth, YearMonth.from(today));
        int continuousDays = signStreakService.computeContinuousDays(userId, today);
        int totalSignDays = nullToZero(signBitmapMapper.countTotalSignDays(userId));
        int usedCount = nullToZero(signSupplementUsedMapper.countByUserId(userId));

        List<String> signDates = new ArrayList<>();
        for (int day = 1; day <= requestedMonth.lengthOfMonth(); day++) {
            if ((bits & (1L << (day - 1))) != 0) {
                signDates.add(yyyyMM + String.format("%02d", day));
            }
        }

        SignDataVO result = new SignDataVO();
        result.setContinuousDays(continuousDays);
        result.setSupplementCount(Math.max(0, totalSignDays / 30 - usedCount));
        result.setTotalSignDays(totalSignDays);
        result.setSignDays(signDates);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sign(String userId) {
        Instant operationTime = businessClock.instant();
        LocalDate today = businessDate(operationTime);
        lockUserSignMutation(userId);
        if (!setSignBit(userId, today)) {
            throw new BusinessException("今天已经签到过了哦~");
        }
        Date now = Date.from(operationTime);
        recordGrowthOnce(userId, today, now);
        recordSignDetail(userId, today, 0, now);

        SignSnapshot snapshot = loadSnapshot(userId, today);
        enqueueStreakReward(userId, snapshot.continuousDays());
        enqueueCacheRefresh(userId, today, snapshot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void msign(String userId, String yyyyMMdd) {
        Instant operationTime = businessClock.instant();
        LocalDate today = businessDate(operationTime);
        LocalDate signDate = parseSupplementDate(yyyyMMdd, today);
        lockUserSignMutation(userId);

        int totalBefore = nullToZero(signBitmapMapper.countTotalSignDays(userId));
        int usedBefore = nullToZero(signSupplementUsedMapper.countByUserId(userId));
        if (totalBefore / 30 - usedBefore <= 0) {
            throw new BusinessException("补签次数不足");
        }
        if (!setSignBit(userId, signDate)) {
            throw new BusinessException("该日期已经签到过了哦~");
        }

        Date now = Date.from(operationTime);
        int consumed = nullToZero(signSupplementUsedMapper.insert(userId, yyyyMMdd, now));
        if (consumed != 1) {
            throw new BusinessException("补签次数扣减失败，请稍后重试");
        }
        recordGrowthOnce(userId, signDate, now);
        recordSignDetail(userId, signDate, 1, now);

        SignSnapshot snapshot = loadSnapshot(userId, today);
        enqueueStreakReward(userId, snapshot.continuousDays());
        enqueueSupplementNotification(userId, yyyyMMdd);
        enqueueCacheRefresh(userId, signDate, snapshot);
    }

    private long loadCalendarBits(String userId, YearMonth requestedMonth, YearMonth currentMonth) {
        String yearMonth = requestedMonth.format(MONTH_FORMATTER);
        boolean mutableMonth = requestedMonth.equals(currentMonth);
        if (!mutableMonth) {
            try {
                Long cached = redisComponent.getSignMonthBits(userId, yearMonth);
                if (cached != null) {
                    return cached;
                }
            } catch (Exception e) {
                log.warn("读取签到 Redis 缓存失败，回源 MySQL userId={}, month={}", userId, yearMonth, e);
            }
        }
        SignBitmap row = signBitmapMapper.selectByUserAndMonth(userId, yearMonth);
        long bits = row == null || row.getBits() == null ? 0L : row.getBits();
        try {
            redisComponent.writeSignMonthBitmap(userId, yearMonth, bits);
        } catch (Exception e) {
            log.warn("回填签到 Redis 缓存失败 userId={}, month={}", userId, yearMonth, e);
        }
        return bits;
    }

    private boolean setSignBit(String userId, LocalDate date) {
        String yearMonth = date.format(MONTH_FORMATTER);
        long mask = 1L << (date.getDayOfMonth() - 1);
        int inserted = nullToZero(signBitmapMapper.insertBitIfAbsent(userId, yearMonth, mask));
        if (inserted == 1) {
            return true;
        }
        return nullToZero(signBitmapMapper.setBitIfMissing(userId, yearMonth, mask)) == 1;
    }

    private void recordGrowthOnce(String userId, LocalDate signDate, Date now) {
        int inserted = nullToZero(signGrowthRecordMapper.insertIgnore(
                userId, signDate.format(DATE_FORMATTER), SIGN_GROWTH, now));
        if (inserted == 1) {
            userMemberProfileService.addGrowth(userId, SIGN_GROWTH);
        }
    }

    private void recordSignDetail(String userId, LocalDate signDate, int signType, Date now) {
        UserSignRecordDetail detail = new UserSignRecordDetail();
        detail.setUserId(userId);
        detail.setSignDate(signDate.format(DATE_FORMATTER));
        detail.setSignType(signType);
        detail.setCreateTime(now);
        int inserted = nullToZero(signRecordDetailMapper.insertIgnore(detail));
        if (inserted != 1) {
            throw new BusinessException("签到明细写入失败，请稍后重试");
        }
    }

    private void lockUserSignMutation(String userId) {
        if (StringTools.isEmpty(userId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        userMemberProfileMapper.insertDefaultIgnore(userId);
        if (StringTools.isEmpty(userMemberProfileMapper.selectUserIdForUpdate(userId))) {
            throw new BusinessException("会员档案锁定失败，请稍后重试");
        }
    }

    private SignSnapshot loadSnapshot(String userId, LocalDate today) {
        int continuousDays = signStreakService.computeContinuousDays(userId, today);
        int totalSignDays = nullToZero(signBitmapMapper.countTotalSignDays(userId));
        int usedCount = nullToZero(signSupplementUsedMapper.countByUserId(userId));
        return new SignSnapshot(continuousDays, totalSignDays, usedCount);
    }

    private void enqueueStreakReward(String userId, int continuousDays) {
        SignRewardConfigDTO config = signRewardConfigService.resolveActiveConfig();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        int streakDays = config.getStreakDays() == null ? 7 : config.getStreakDays();
        String couponId = config.getCouponId();
        if (streakDays < 1 || continuousDays < streakDays || continuousDays % streakDays != 0
                || StringTools.isEmpty(couponId)) {
            return;
        }
        SignStreakCouponMessageDTO message = new SignStreakCouponMessageDTO(userId, couponId, continuousDays);
        transactionalMqSender.sendAfterCommit(
                RabbitMQConfig.SIGN_STREAK_COUPON_EXCHANGE,
                RabbitMQConfig.SIGN_STREAK_COUPON_KEY,
                message,
                MqIdempotencyKeys.signStreakCoupon(userId, couponId, continuousDays),
                MessageReliabilityLevelEnum.HIGH);
    }

    private void enqueueSupplementNotification(String userId, String signDate) {
        NotificationMessageDTO message = new NotificationMessageDTO(
                userId,
                "补签成功",
                "补签已获得 " + SIGN_GROWTH + " 成长值",
                "sign",
                signDate);
        transactionalMqSender.sendAfterCommit(
                RabbitMQConfig.NOTIFY_EXCHANGE,
                RabbitMQConfig.NOTIFY_KEY,
                message,
                MqIdempotencyKeys.notification(userId, "sign", signDate),
                MessageReliabilityLevelEnum.HIGH);
    }

    private void enqueueCacheRefresh(String userId, LocalDate changedDate, SignSnapshot snapshot) {
        String yearMonth = changedDate.format(MONTH_FORMATTER);
        SignBitmap row = signBitmapMapper.selectByUserAndMonth(userId, yearMonth);
        long bits = row == null || row.getBits() == null ? 0L : row.getBits();
        transactionalMqSender.sendAfterCommit(() -> {
            try {
                redisComponent.writeSignMonthBitmap(userId, yearMonth, bits);
                redisComponent.writeSignHash(userId, snapshot.continuousDays(),
                        snapshot.totalSignDays(), snapshot.usedCount());
            } catch (Exception e) {
                log.warn("签到事务已提交，但 Redis 缓存刷新失败 userId={}, month={}", userId, yearMonth, e);
            }
        });
    }

    private LocalDate parseSupplementDate(String yyyyMMdd, LocalDate today) {
        if (yyyyMMdd == null || !yyyyMMdd.matches("\\d{8}")) {
            throw new BusinessException("日期格式错误");
        }
        LocalDate signDate;
        try {
            signDate = LocalDate.parse(yyyyMMdd, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式错误");
        }
        if (signDate.isBefore(today.withDayOfMonth(1))) {
            throw new BusinessException("只能补签本月内的日期");
        }
        if (signDate.isAfter(today)) {
            throw new BusinessException("不能补签未来日期");
        }
        return signDate;
    }

    private LocalDate businessDate(Instant instant) {
        return instant.atZone(BUSINESS_ZONE).toLocalDate();
    }

    private YearMonth parseMonth(String yyyyMM) {
        if (yyyyMM == null || !yyyyMM.matches("\\d{6}")) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        try {
            int year = Integer.parseInt(yyyyMM.substring(0, 4));
            int month = Integer.parseInt(yyyyMM.substring(4, 6));
            return YearMonth.of(year, month);
        } catch (RuntimeException e) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record SignSnapshot(int continuousDays, int totalSignDays, int usedCount) {
    }
}
