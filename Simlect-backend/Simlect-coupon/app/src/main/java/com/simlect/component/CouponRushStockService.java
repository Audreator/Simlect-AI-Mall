package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.api.dto.CouponRushStockReconcileDTO;
import com.simlect.api.enums.RushingCouponStatusEnum;
import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.query.DiscountCouponQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.DiscountCouponMapper;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CouponRushStockService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private DiscountCouponMapper<DiscountCoupon, DiscountCouponQuery> discountCouponMapper;

    public String stockKey(String couponId) {
        return Constants.REDIS_KEY_RUSHING_STOCK + couponId;
    }

    private String syncLockKey(String couponId) {
        return Constants.REDIS_KEY_RUSHING_STOCK_SYNCING + couponId;
    }

    private String depletedKey(String couponId) {
        return Constants.REDIS_KEY_RUSHING_STOCK_DEPLETED + couponId;
    }

    public boolean isDbStockDepleted(String couponId) {
        if (StringTools.isEmpty(couponId)) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(depletedKey(couponId)));
    }

    private void markDbStockDepleted(String couponId) {
        stringRedisTemplate.opsForValue().set(depletedKey(couponId), "1");
        log.info("秒杀库存 DB 已为 0，标记不再同步 couponId={}", couponId);
    }

    private void clearDbStockDepleted(String couponId) {
        stringRedisTemplate.delete(depletedKey(couponId));
    }

    public boolean isStockSyncing(String couponId) {
        if (StringTools.isEmpty(couponId)) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(syncLockKey(couponId)));
    }

    public void assertRushNotBlocked(String couponId) {
        if (isStockSyncing(couponId)) {
            throw new BusinessException("库存同步中，请稍后再试");
        }
    }

    public CouponRushStockReconcileDTO syncFromDbIfRedisZero(String couponId) {
        if (StringTools.isEmpty(couponId)) {
            return null;
        }
        if (isDbStockDepleted(couponId)) {
            return null;
        }
        Integer redisStock = getRedisStock(couponId);
        if (redisStock != null && (redisStock > 0 || redisStock == Constants.RUSHING_STOCK_UNLIMITED)) {
            return null;
        }
        DiscountCoupon coupon = discountCouponMapper.selectByCouponId(couponId);
        if (coupon != null && coupon.isUnlimitedStock()) {
            return null;
        }
        return syncFromDbAuthoritative(couponId);
    }

    /** 秒杀库存同步锁释放：Lua 校验持有者后再删，防 TTL 过期后误删他人锁 */
    private static final String SYNC_UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end";

    public CouponRushStockReconcileDTO syncFromDbAuthoritative(String couponId) {
        if (StringTools.isEmpty(couponId)) {
            return new CouponRushStockReconcileDTO();
        }
        String lockKey = syncLockKey(couponId);
        String token = UUID.randomUUID() + ":" + Thread.currentThread().getId();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey, token, Constants.RUSHING_STOCK_SYNC_LOCK_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException("库存同步中，请稍后再试");
        }
        try {
            log.info("秒杀库存开始与 DB 同步 couponId={}", couponId);
            CouponRushStockReconcileDTO dto = reconcileOne(couponId);
            if (dto.getDbRemainCount() != null && dto.getDbRemainCount() <= 0) {
                DiscountCoupon coupon = discountCouponMapper.selectByCouponId(couponId);
                if (coupon == null || !coupon.isUnlimitedStock()) {
                    markDbStockDepleted(couponId);
                }
            }
            log.info("秒杀库存与 DB 同步完成 couponId={}, db={}, redisAfter={}",
                    couponId, dto.getDbRemainCount(), dto.getRedisStockAfter());
            return dto;
        } finally {
            // 仅当自己仍是持有者时删除（防 TTL 过期后被他人获取时误删他人锁）
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(SYNC_UNLOCK_LUA, Long.class);
            stringRedisTemplate.execute(script, List.of(lockKey), token);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseStockAfterDbRefund(String couponId, String userId) {
        if (StringTools.isEmpty(couponId) || StringTools.isEmpty(userId)) {
            return;
        }
        DiscountCoupon locked = discountCouponMapper.selectByCouponIdForUpdate(couponId);
        if (locked == null) {
            log.warn("回补库存失败，券不存在 couponId={}", couponId);
            return;
        }
        Integer affected = discountCouponMapper.addStock(couponId);
        if (affected == null || affected == 0) {
            log.warn("回补库存未加行（可能已达 total_count 上限） couponId={}, userId={}", couponId, userId);
        }
        final String cId = couponId;
        final String uId = userId;
        final boolean stockAdded = affected != null && affected > 0;
        Runnable alignRedis = () -> {
            DiscountCoupon latest = discountCouponMapper.selectByCouponId(cId);
            int redisStock;
            if (latest != null && latest.isUnlimitedStock()) {
                redisStock = Constants.RUSHING_STOCK_UNLIMITED;
            } else {
                int remain = latest == null || latest.getRemainCount() == null
                        ? 0
                        : Math.max(0, latest.getRemainCount());
                redisStock = remain;
                if (remain > 0) {
                    clearDbStockDepleted(cId);
                }
            }
            redisComponent.alignRushStockAfterRelease(cId, uId, redisStock);
            log.info("秒杀库存 Redis 已对齐 couponId={}, redisStock={}", cId, redisStock);
        };
        // 提交后按 DB 剩余对齐 Redis；回滚时释放 Redis 预占（防建单失败/DB 回滚后 Redis 泄漏）
        Runnable rollbackRedis = () -> redisComponent.rollbackRushRedisReserve(cId, uId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        if (stockAdded) {
                            alignRedis.run();
                        } else {
                            rollbackRedis.run();
                        }
                    } else if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        rollbackRedis.run();
                    }
                }
            });
        } else {
            if (stockAdded) {
                alignRedis.run();
            } else {
                rollbackRedis.run();
            }
        }
        log.info("秒杀库存 DB 已回补 couponId={}, addRows={}", couponId, affected);
    }

    public void warmupStock(String couponId, Integer remainCount) {
        warmupStock(couponId, remainCount, null);
    }

    public void warmupStock(String couponId, Integer remainCount, Integer totalCount) {
        if (StringTools.isEmpty(couponId)) {
            return;
        }
        if (totalCount != null && totalCount == 0) {
            stringRedisTemplate.opsForValue().set(
                    stockKey(couponId), String.valueOf(Constants.RUSHING_STOCK_UNLIMITED));
            clearDbStockDepleted(couponId);
            log.info("秒杀库存预热 couponId={}, stock=UNLIMITED", couponId);
            return;
        }
        int stock = remainCount == null ? 0 : Math.max(0, remainCount);
        stringRedisTemplate.opsForValue().set(stockKey(couponId), String.valueOf(stock));
        if (stock > 0) {
            clearDbStockDepleted(couponId);
        }
        log.info("秒杀库存预热 couponId={}, stock={}", couponId, stock);
    }

    public Integer getRedisStock(String couponId) {
        if (StringTools.isEmpty(couponId)) {
            return null;
        }
        String raw = stringRedisTemplate.opsForValue().get(stockKey(couponId));
        if (StringTools.isEmpty(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("秒杀库存 Redis 值非法 couponId={}, raw={}", couponId, raw);
            return null;
        }
    }

    public boolean hasAvailableStock(String couponId) {
        if (isStockSyncing(couponId)) {
            return false;
        }
        if (isDbStockDepleted(couponId)) {
            DiscountCoupon depletedCoupon = discountCouponMapper.selectByCouponId(couponId);
            if (depletedCoupon == null || !depletedCoupon.isUnlimitedStock()) {
                return false;
            }
        }
        Integer redisStock = getRedisStock(couponId);
        if (redisStock == null) {
            DiscountCoupon coupon = discountCouponMapper.selectByCouponId(couponId);
            if (coupon == null) {
                return false;
            }
            warmupStock(couponId, coupon.getRemainCount(), coupon.getTotalCount());
            redisStock = getRedisStock(couponId);
        }
        return redisStock != null
                && (redisStock == Constants.RUSHING_STOCK_UNLIMITED || redisStock > 0);
    }

    public CouponRushStockReconcileDTO reconcileOne(String couponId) {
        CouponRushStockReconcileDTO dto = new CouponRushStockReconcileDTO();
        dto.setCouponId(couponId);
        if (StringTools.isEmpty(couponId)) {
            return dto;
        }
        DiscountCoupon coupon = discountCouponMapper.selectByCouponId(couponId);
        if (coupon == null) {
            return dto;
        }
        if (coupon.isUnlimitedStock()) {
            dto.setDbRemainCount(coupon.getRemainCount());
            Integer before = getRedisStock(couponId);
            dto.setRedisStockBefore(before);
            if (before == null || before != Constants.RUSHING_STOCK_UNLIMITED) {
                warmupStock(couponId, coupon.getRemainCount(), coupon.getTotalCount());
                dto.setAdjusted(true);
                log.warn("秒杀库存对账已修正 couponId={}, redisBefore={}, target=UNLIMITED", couponId, before);
            }
            dto.setRedisStockAfter(getRedisStock(couponId));
            return dto;
        }
        int dbStock = coupon.getRemainCount() == null ? 0 : Math.max(0, coupon.getRemainCount());
        dto.setDbRemainCount(dbStock);
        Integer before = getRedisStock(couponId);
        dto.setRedisStockBefore(before);
        if (before == null || !before.equals(dbStock)) {
            warmupStock(couponId, dbStock, coupon.getTotalCount());
            dto.setAdjusted(true);
            log.warn("秒杀库存对账已修正 couponId={}, redisBefore={}, db={}", couponId, before, dbStock);
        }
        dto.setRedisStockAfter(getRedisStock(couponId));
        return dto;
    }

    public List<CouponRushStockReconcileDTO> reconcileAllRushing() {
        DiscountCouponQuery query = new DiscountCouponQuery();
        query.setRushingstatus(RushingCouponStatusEnum.YES.getStatus());
        List<DiscountCoupon> list = discountCouponMapper.selectList(query);
        List<CouponRushStockReconcileDTO> results = new ArrayList<>();
        if (list == null) {
            return results;
        }
        for (DiscountCoupon coupon : list) {
            if (coupon == null || StringTools.isEmpty(coupon.getCouponId())) {
                continue;
            }
            results.add(reconcileOne(coupon.getCouponId()));
        }
        return results;
    }

    public int warmupAllRushingFromDb() {
        DiscountCouponQuery query = new DiscountCouponQuery();
        query.setRushingstatus(RushingCouponStatusEnum.YES.getStatus());
        List<DiscountCoupon> list = discountCouponMapper.selectList(query);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        for (DiscountCoupon coupon : list) {
            if (coupon == null || StringTools.isEmpty(coupon.getCouponId())) {
                continue;
            }
            warmupStock(coupon.getCouponId(), coupon.getRemainCount(), coupon.getTotalCount());
        }
        log.info("秒杀库存全量预热完成，券数量={}", list.size());
        return list.size();
    }
}
