package com.simlect.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.simlect.config.CouponCacheExecutorConfig;
import com.simlect.constants.Constants;
import com.simlect.api.dto.CouponLogicalCacheEntry;
import com.simlect.api.enums.RushingStatusEnum;
import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.utils.JsonUtils;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
public class DiscountCouponCacheComponent {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    @Qualifier(CouponCacheExecutorConfig.CACHE_REBUILD_EXECUTOR)
    private ExecutorService cacheRebuildExecutor;

    private static final TypeReference<PaginationResultVO<DiscountCoupon>> PLAZA_LIST_TYPE =
            new TypeReference<>() {
            };

    public PaginationResultVO<DiscountCoupon> getPlazaList(
            String status,
            Integer pageNo,
            Integer pageSize,
            String keyword,
            Supplier<PaginationResultVO<DiscountCoupon>> dbLoader) {
        String cacheKey = buildPlazaListKey(status, pageNo, pageSize, keyword);
        String raw = stringRedisTemplate.opsForValue().get(cacheKey);
        if (Constants.REDIS_COUPON_NULL_PLACEHOLDER.equals(raw)) {
            return emptyPlazaPage(pageNo, pageSize);
        }
        if (!StringTools.isEmpty(raw)) {
            try {
                CouponLogicalCacheEntry entry = JsonUtils.parseObject(raw, CouponLogicalCacheEntry.class);
                if (entry != null && !StringTools.isEmpty(entry.getPayload())) {
                    PaginationResultVO<DiscountCoupon> cached =
                            JsonUtils.parseObject(entry.getPayload(), PLAZA_LIST_TYPE);
                    if (cached != null) {
                        if (!isLogicallyExpired(entry)) {
                            return cached;
                        }
                        submitPlazaListRebuild(cacheKey, status, pageNo, pageSize, keyword, dbLoader);
                        return cached;
                    }
                }
            } catch (Exception e) {
                log.warn("解析优惠券列表缓存失败 key={}", cacheKey, e);
                stringRedisTemplate.delete(cacheKey);
            }
        }
        PaginationResultVO<DiscountCoupon> fromDb = dbLoader.get();
        writePlazaListCache(cacheKey, fromDb);
        return fromDb;
    }

    public DiscountCoupon getDetail(String couponId, Supplier<DiscountCoupon> dbLoader) {
        if (StringTools.isEmpty(couponId)) {
            return null;
        }
        String cacheKey = Constants.REDIS_KEY_COUPON_DETAIL + couponId;
        String raw = stringRedisTemplate.opsForValue().get(cacheKey);
        if (Constants.REDIS_COUPON_NULL_PLACEHOLDER.equals(raw)) {
            return null;
        }
        if (!StringTools.isEmpty(raw)) {
            try {
                CouponLogicalCacheEntry entry = JsonUtils.parseObject(raw, CouponLogicalCacheEntry.class);
                if (entry != null && !StringTools.isEmpty(entry.getPayload())) {
                    DiscountCoupon cached = JsonUtils.parseObject(entry.getPayload(), DiscountCoupon.class);
                    if (cached != null) {
                        if (!isLogicallyExpired(entry)) {
                            return cached;
                        }
                        submitDetailRebuild(cacheKey, couponId, dbLoader);
                        return cached;
                    }
                }
            } catch (Exception e) {
                log.warn("解析优惠券详情缓存失败 key={}", cacheKey, e);
                stringRedisTemplate.delete(cacheKey);
            }
        }
        DiscountCoupon fromDb = dbLoader.get();
        writeDetailCache(cacheKey, fromDb);
        return fromDb;
    }

    public void invalidateAfterWrite(String couponId) {
        bumpPlazaListVersion();
        if (!StringTools.isEmpty(couponId)) {
            stringRedisTemplate.delete(Constants.REDIS_KEY_COUPON_DETAIL + couponId);
        }
    }

    public void invalidateDetail(String couponId) {
        if (!StringTools.isEmpty(couponId)) {
            stringRedisTemplate.delete(Constants.REDIS_KEY_COUPON_DETAIL + couponId);
        }
    }

    public void warmPlazaListCache(PlazaListLoader loader) {
        if (loader == null) {
            return;
        }
        int pageSize = 15;
        String[] statuses = {
                RushingStatusEnum.ALL.getType(),
                RushingStatusEnum.ONGOING.getType(),
                RushingStatusEnum.UPCOMING.getType(),
                RushingStatusEnum.ENDED.getType()
        };
        for (String status : statuses) {
            try {
                String cacheKey = buildPlazaListKey(status, 1, pageSize, null);
                PaginationResultVO<DiscountCoupon> page = loader.load(status, 1, pageSize, null);
                writePlazaListCache(cacheKey, page);
            } catch (Exception e) {
                log.warn("预热优惠券列表失败 status={}", status, e);
            }
        }
    }

    @FunctionalInterface
    public interface PlazaListLoader {
        PaginationResultVO<DiscountCoupon> load(String status, int pageNo, int pageSize, String keyword);
    }

    private void submitPlazaListRebuild(
            String cacheKey,
            String status,
            Integer pageNo,
            Integer pageSize,
            String keyword,
            Supplier<PaginationResultVO<DiscountCoupon>> dbLoader) {
        String token = tryRebuildLock(cacheKey);
        if (token == null) {
            return;
        }
        cacheRebuildExecutor.execute(() -> {
            try {
                PaginationResultVO<DiscountCoupon> fresh = dbLoader.get();
                writePlazaListCache(cacheKey, fresh);
            } catch (Exception e) {
                log.error("异步重建优惠券列表缓存失败 key={}", cacheKey, e);
            } finally {
                releaseRebuildLock(cacheKey, token);
            }
        });
    }

    private void submitDetailRebuild(String cacheKey, String couponId, Supplier<DiscountCoupon> dbLoader) {
        String token = tryRebuildLock(cacheKey);
        if (token == null) {
            return;
        }
        cacheRebuildExecutor.execute(() -> {
            try {
                DiscountCoupon fresh = dbLoader.get();
                writeDetailCache(cacheKey, fresh);
            } catch (Exception e) {
                log.error("异步重建优惠券详情缓存失败 couponId={}", couponId, e);
            } finally {
                releaseRebuildLock(cacheKey, token);
            }
        });
    }

    private void writePlazaListCache(String cacheKey, PaginationResultVO<DiscountCoupon> page) {
        if (page == null || page.getList() == null || page.getList().isEmpty()) {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    Constants.REDIS_COUPON_NULL_PLACEHOLDER,
                    Constants.COUPON_CACHE_NULL_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
            return;
        }
        String payload = JsonUtils.toJson(page);
        writeLogicalEntry(cacheKey, payload);
    }

    private void writeDetailCache(String cacheKey, DiscountCoupon coupon) {
        if (coupon == null) {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    Constants.REDIS_COUPON_NULL_PLACEHOLDER,
                    Constants.COUPON_CACHE_NULL_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
            return;
        }
        writeLogicalEntry(cacheKey, JsonUtils.toJson(coupon));
    }

    private void writeLogicalEntry(String cacheKey, String payload) {
        long logicalExpireAt = System.currentTimeMillis()
                + Constants.COUPON_CACHE_LOGICAL_TTL_SECONDS * 1000L;
        CouponLogicalCacheEntry entry = new CouponLogicalCacheEntry(payload, logicalExpireAt);
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                JsonUtils.toJson(entry),
                Constants.COUPON_CACHE_PHYSICAL_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private boolean isLogicallyExpired(CouponLogicalCacheEntry entry) {
        if (entry == null || entry.getLogicalExpireAt() == null) {
            return true;
        }
        return System.currentTimeMillis() >= entry.getLogicalExpireAt();
    }

    /**
     * 重建缓存互斥锁：返回持有者 token；未获取到返回 null（防并发重复重建）。
     * 锁值带持有者标识，释放时校验持有者（防 TTL 过期后被他人获取时误删他人锁）。
     */
    private String tryRebuildLock(String cacheKey) {
        String lockKey = Constants.REDIS_KEY_COUPON_REBUILD_LOCK + hashKey(cacheKey);
        String token = UUID.randomUUID() + ":" + Thread.currentThread().getId();
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                token,
                Constants.COUPON_CACHE_REBUILD_LOCK_SECONDS,
                TimeUnit.SECONDS
        );
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    private void releaseRebuildLock(String cacheKey, String token) {
        if (StringTools.isEmpty(token)) {
            return;
        }
        String lockKey = Constants.REDIS_KEY_COUPON_REBUILD_LOCK + hashKey(cacheKey);
        stringRedisTemplate.execute(
                new DefaultRedisScript<>(DELETE_IF_OWNED_LUA, Long.class),
                Collections.singletonList(lockKey),
                token);
    }

    private static final String DELETE_IF_OWNED_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end";

    private void bumpPlazaListVersion() {
        stringRedisTemplate.opsForValue().increment(Constants.REDIS_KEY_COUPON_PLAZA_CACHE_VERSION);
    }

    private long currentPlazaVersion() {
        String ver = stringRedisTemplate.opsForValue().get(Constants.REDIS_KEY_COUPON_PLAZA_CACHE_VERSION);
        if (StringTools.isEmpty(ver)) {
            return 0L;
        }
        try {
            return Long.parseLong(ver);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String buildPlazaListKey(String status, Integer pageNo, Integer pageSize, String keyword) {
        String kw = StringTools.isEmpty(keyword) ? "_" : hashKey(keyword);
        int pn = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int ps = pageSize == null || pageSize < 1 ? 15 : pageSize;
        String st = StringTools.isEmpty(status) ? RushingStatusEnum.ALL.getType() : status;
        return Constants.REDIS_KEY_COUPON_PLAZA_LIST
                + currentPlazaVersion() + ":"
                + st + ":"
                + pn + ":"
                + ps + ":"
                + kw;
    }

    private PaginationResultVO<DiscountCoupon> emptyPlazaPage(Integer pageNo, Integer pageSize) {
        int pn = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int ps = pageSize == null || pageSize < 1 ? 15 : pageSize;
        return new PaginationResultVO<>(0, ps, pn, 0, java.util.Collections.emptyList());
    }

    private static String hashKey(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
}
