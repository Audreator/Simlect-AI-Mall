package com.simlect.biz;

import com.simlect.api.dto.CouponValidateAndLockDTO;
import com.simlect.api.dto.NotificationMessageDTO;
import com.simlect.api.dto.SignStreakCouponMessageDTO;
import com.simlect.api.dto.UserCouponCreateDTO;
import com.simlect.api.dto.UserCouponStatusChangeDTO;
import com.simlect.api.vo.CouponBriefVO;
import com.simlect.api.vo.CouponLockResultVO;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.api.vo.UserCouponVO;
import com.simlect.api.enums.CouponTypeEnum;
import com.simlect.api.enums.CouponStatusEnum;
import com.simlect.api.enums.UserCouponStatusEnum;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.TransactionalMqSender;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.po.SignStreakCouponGrant;
import com.simlect.entity.po.UserCoupon;
import com.simlect.entity.query.DiscountCouponQuery;
import com.simlect.entity.query.UserCouponQuery;
import com.simlect.exception.BusinessException;
import com.simlect.component.CouponRushStockService;
import com.simlect.component.DiscountCouponCacheComponent;
import com.simlect.mappers.DiscountCouponMapper;
import com.simlect.mappers.SignStreakCouponGrantMapper;
import com.simlect.mappers.UserCouponMapper;
import com.simlect.biz.DiscountCouponService;
import com.simlect.utils.OrderPayAmountUtil;
import com.simlect.utils.StringTools;
import com.simlect.support.MqIdempotencyKeys;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class CouponInternalService {

    @Resource
    private UserCouponMapper<UserCoupon, UserCouponQuery> userCouponMapper;
    @Resource
    private DiscountCouponMapper<DiscountCoupon, DiscountCouponQuery> discountCouponMapper;
    @Resource
    private CouponRushStockService couponRushStockService;
    @Resource
    private DiscountCouponService discountCouponService;
    @Resource
    private DiscountCouponCacheComponent discountCouponCacheComponent;
    @Resource
    private SignStreakCouponGrantMapper signStreakCouponGrantMapper;
    @Resource
    private TransactionalMqSender transactionalMqSender;

    private static final int STREAK_GRANT_PENDING = 0;
    private static final int STREAK_GRANT_GRANTED = 1;
    private static final int STREAK_GRANT_REJECTED = 2;

    @Transactional(rollbackFor = Exception.class)
    public CouponLockResultVO validateAndLock(CouponValidateAndLockDTO dto) {
        Date now = new Date();
        UserCoupon userCoupon = userCouponMapper.selectByUserCouponId(dto.getUserCouponId());
        if (userCoupon == null || !dto.getUserId().equals(userCoupon.getUserId())) {
            throw new BusinessException("优惠券不存在");
        }
        if (!UserCouponStatusEnum.NOUSE.getStatus().equals(userCoupon.getStatus())) {
            throw new BusinessException("优惠券不可用");
        }
        DiscountCoupon coupon = discountCouponMapper.selectByCouponId(userCoupon.getCouponId());
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (coupon.getValidStartTime() != null && now.before(coupon.getValidStartTime())) {
            throw new BusinessException("优惠券未到使用时间");
        }
        if (coupon.getValidEndTime() != null && now.after(coupon.getValidEndTime())) {
            throw new BusinessException("优惠券已过期");
        }
        BigDecimal orderAmount = dto.getOrderAmount() == null ? BigDecimal.ZERO : dto.getOrderAmount();
        BigDecimal threshold = coupon.getThresholdAmount() == null ? BigDecimal.ZERO : coupon.getThresholdAmount();
        if (threshold.compareTo(BigDecimal.ZERO) > 0 && orderAmount.compareTo(threshold) < 0) {
            throw new BusinessException("未满足优惠券使用门槛");
        }
        BigDecimal discount = calcCouponDiscount(coupon, orderAmount);
        discount = OrderPayAmountUtil.capCouponDiscountForMinPay(orderAmount, discount);

        CouponLockResultVO result = new CouponLockResultVO();
        result.setCouponId(coupon.getCouponId());
        result.setUserCouponId(dto.getUserCouponId());
        result.setCouponName(coupon.getCouponName());
        result.setDiscountAmount(discount);
        result.setLocked(false);
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            UserCoupon lockBean = new UserCoupon();
            lockBean.setStatus(UserCouponStatusEnum.CANT.getStatus());
            UserCouponQuery lockQuery = new UserCouponQuery();
            lockQuery.setUserCouponId(dto.getUserCouponId());
            lockQuery.setUserId(dto.getUserId());
            lockQuery.setStatus(UserCouponStatusEnum.NOUSE.getStatus());
            int updated = userCouponMapper.updateByParam(lockBean, lockQuery);
            if (updated != 1) {
                throw new BusinessException("优惠券已被使用");
            }
            result.setLocked(true);
        }
        return result;
    }

    public DiscountCouponVO getCoupon(String couponId) {
        DiscountCoupon coupon = discountCouponMapper.selectByCouponId(couponId);
        if (coupon == null) {
            return null;
        }
        DiscountCouponVO vo = new DiscountCouponVO();
        BeanUtils.copyProperties(coupon, vo);
        return vo;
    }

    public CouponBriefVO getCouponBrief(String couponId) {
        DiscountCoupon coupon = discountCouponMapper.selectByCouponId(couponId);
        if (coupon == null) {
            return null;
        }
        CouponBriefVO vo = new CouponBriefVO();
        vo.setCouponId(coupon.getCouponId());
        vo.setCouponName(coupon.getCouponName());
        vo.setCouponType(coupon.getCouponType());
        return vo;
    }

    public UserCouponVO getUserCoupon(String userCouponId) {
        UserCoupon uc = userCouponMapper.selectByUserCouponId(userCouponId);
        if (uc == null) {
            return null;
        }
        UserCouponVO vo = new UserCouponVO();
        BeanUtils.copyProperties(uc, vo);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeUserCouponStatus(UserCouponStatusChangeDTO dto) {
        UserCoupon bean = new UserCoupon();
        bean.setStatus(dto.getToStatus());
        if (dto.getUseTime() != null) {
            bean.setUseTime(dto.getUseTime());
        }
        UserCouponQuery query = new UserCouponQuery();
        query.setUserCouponId(dto.getUserCouponId());
        query.setUserId(dto.getUserId());
        query.setStatus(dto.getFromStatus());
        int updated = userCouponMapper.updateByParam(bean, query);
        if (updated < 1) {
            throw new BusinessException("用户券状态更新失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUserCoupon(UserCouponCreateDTO dto) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserCouponId(dto.getUserCouponId());
        userCoupon.setUserId(dto.getUserId());
        userCoupon.setCouponId(dto.getCouponId());
        userCoupon.setStatus(dto.getStatus());
        userCouponMapper.insert(userCoupon);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deductStock(String couponId) {
        if (StringTools.isEmpty(couponId)) {
            throw new BusinessException("优惠券ID为空");
        }
        DiscountCoupon locked = discountCouponMapper.selectByCouponIdForUpdate(couponId);
        if (locked == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (locked.isUnlimitedStock()) {
            return 1;
        }
        Integer affected = discountCouponMapper.deductStock(couponId);
        return affected == null ? 0 : affected;
    }

    public void assertRushNotBlocked(String couponId) {
        couponRushStockService.assertRushNotBlocked(couponId);
    }

    public boolean hasAvailableRushStock(String couponId) {
        return couponRushStockService.hasAvailableStock(couponId);
    }

    public void syncRushStockFromDbIfRedisZero(String couponId) {
        couponRushStockService.syncFromDbIfRedisZero(couponId);
    }

    public void releaseRushRedisReserve(String couponId, String userId) {
        discountCouponService.releaseRushRedisReserve(couponId, userId);
    }

    public void releaseRushCouponReserve(String couponId, String userId) {
        discountCouponService.releaseRushCouponReserve(couponId, userId);
    }

    public void invalidateCouponCache(String couponId) {
        discountCouponCacheComponent.invalidateAfterWrite(couponId);
    }

    /**
     * 连续签到奖励的券域唯一事务入口：DB 幂等、锁券、扣库存、建用户券和通知 outbox 原子完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public StreakCouponGrantResult grantSignStreakCoupon(SignStreakCouponMessageDTO dto) {
        if (dto == null || StringTools.isEmpty(dto.getUserId()) || StringTools.isEmpty(dto.getCouponId())
                || dto.getStreakDays() == null || dto.getStreakDays() < 1) {
            throw new BusinessException("连续签到奖励消息参数错误");
        }
        String idempotencyKey = MqIdempotencyKeys.signStreakCoupon(
                dto.getUserId(), dto.getCouponId(), dto.getStreakDays());
        Date now = new Date();
        SignStreakCouponGrant pending = new SignStreakCouponGrant();
        pending.setIdempotencyKey(idempotencyKey);
        pending.setUserId(dto.getUserId());
        pending.setCouponId(dto.getCouponId());
        pending.setStreakDays(dto.getStreakDays());
        pending.setCreateTime(now);

        int inserted = nullToZero(signStreakCouponGrantMapper.insertPending(pending));
        if (inserted != 1) {
            SignStreakCouponGrant existing = signStreakCouponGrantMapper.selectByIdempotencyKey(idempotencyKey);
            if (existing == null) {
                throw new IllegalStateException("连续签到奖励幂等记录冲突但无法读取: " + idempotencyKey);
            }
            if (Integer.valueOf(STREAK_GRANT_GRANTED).equals(existing.getStatus())) {
                return new StreakCouponGrantResult("ALREADY_GRANTED", existing.getUserCouponId(), null);
            }
            if (Integer.valueOf(STREAK_GRANT_REJECTED).equals(existing.getStatus())) {
                return new StreakCouponGrantResult("ALREADY_REJECTED", null, existing.getRejectReason());
            }
            throw new IllegalStateException("连续签到奖励幂等记录长期处于处理中: " + idempotencyKey);
        }

        DiscountCoupon coupon = discountCouponMapper.selectByCouponIdForUpdate(dto.getCouponId());
        if (coupon == null) {
            return rejectStreakGrant(idempotencyKey, "奖励优惠券不存在", now);
        }
        if (!CouponStatusEnum.NORMAL.getStatus().equals(coupon.getStatus())) {
            return rejectStreakGrant(idempotencyKey, "奖励优惠券当前不可发放", now);
        }
        if (coupon.getValidStartTime() != null && now.before(coupon.getValidStartTime())) {
            return rejectStreakGrant(idempotencyKey, "奖励优惠券尚未生效", now);
        }
        if (coupon.getValidEndTime() != null && now.after(coupon.getValidEndTime())) {
            return rejectStreakGrant(idempotencyKey, "奖励优惠券已过期", now);
        }

        boolean unlimited = coupon.isUnlimitedStock();
        if (!unlimited && (coupon.getRemainCount() == null || coupon.getRemainCount() <= 0)) {
            return rejectStreakGrant(idempotencyKey, "奖励优惠券库存不足", now);
        }
        if (!unlimited && nullToZero(discountCouponMapper.deductStock(dto.getCouponId())) != 1) {
            return rejectStreakGrant(idempotencyKey, "奖励优惠券库存不足", now);
        }

        String userCouponId = StringTools.createUserCouponId();
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserCouponId(userCouponId);
        userCoupon.setUserId(dto.getUserId());
        userCoupon.setCouponId(dto.getCouponId());
        userCoupon.setReceiveTime(now);
        userCoupon.setStatus(UserCouponStatusEnum.NOUSE.getStatus());
        if (nullToZero(userCouponMapper.insert(userCoupon)) != 1) {
            throw new IllegalStateException("连续签到奖励用户券写入失败");
        }
        if (nullToZero(signStreakCouponGrantMapper.markGranted(idempotencyKey, userCouponId, now)) != 1) {
            throw new IllegalStateException("连续签到奖励幂等记录完成失败");
        }

        NotificationMessageDTO notification = new NotificationMessageDTO(
                dto.getUserId(),
                "签到奖励",
                "连续签到 " + dto.getStreakDays() + " 天，已发放优惠券「" + coupon.getCouponName() + "」",
                "sign_reward",
                userCouponId);
        transactionalMqSender.sendAfterCommit(
                RabbitMQConfig.NOTIFY_EXCHANGE,
                RabbitMQConfig.NOTIFY_KEY,
                notification,
                MqIdempotencyKeys.notification(dto.getUserId(), "sign_reward", userCouponId),
                MessageReliabilityLevelEnum.HIGH);
        return new StreakCouponGrantResult("GRANTED", userCouponId, null);
    }

    private StreakCouponGrantResult rejectStreakGrant(String idempotencyKey, String reason, Date now) {
        if (nullToZero(signStreakCouponGrantMapper.markRejected(idempotencyKey, reason, now)) != 1) {
            throw new IllegalStateException("连续签到奖励拒绝结果写入失败");
        }
        return new StreakCouponGrantResult("REJECTED", null, reason);
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    public record StreakCouponGrantResult(String outcome, String userCouponId, String reason) {
    }

    private BigDecimal calcCouponDiscount(DiscountCoupon coupon, BigDecimal amount) {
        if (coupon == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        Integer type = coupon.getCouponType();
        BigDecimal discount = BigDecimal.ZERO;
        if (CouponTypeEnum.FULL.getStatus().equals(type) || CouponTypeEnum.NOTHRESHOLD.getStatus().equals(type)) {
            discount = coupon.getDiscountAmount() == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        } else if (CouponTypeEnum.DISCOUNT.getStatus().equals(type)) {
            BigDecimal rate = coupon.getDiscountRate();
            if (rate == null) {
                return BigDecimal.ZERO;
            }
            discount = amount.multiply(BigDecimal.ONE.subtract(rate));
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        return discount.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
