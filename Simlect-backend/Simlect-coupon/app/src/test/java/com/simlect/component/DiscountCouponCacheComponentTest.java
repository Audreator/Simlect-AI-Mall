package com.simlect.component;

import com.simlect.api.dto.CouponLogicalCacheEntry;
import com.simlect.constants.Constants;
import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DiscountCouponCacheComponent 优惠券缓存组件（空占位/逻辑过期/异步重建/失效）单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DiscountCouponCacheComponentTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ExecutorService cacheRebuildExecutor;

    @InjectMocks
    private DiscountCouponCacheComponent cacheComponent;

    private static final String COUPON_ID = "CP1";
    private static final String DETAIL_KEY = Constants.REDIS_KEY_COUPON_DETAIL + COUPON_ID;

    private void mockValueOps() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private PaginationResultVO<DiscountCoupon> page() {
        DiscountCoupon c = new DiscountCoupon();
        c.setCouponId(COUPON_ID);
        c.setCouponName("券");
        return new PaginationResultVO<>(1, 15, 1, 1, List.of(c));
    }

    // ==================== getDetail ====================

    @Test
    void getDetail_emptyCouponId_returnsNull() {
        assertNull(cacheComponent.getDetail("", () -> new DiscountCoupon()));
        assertNull(cacheComponent.getDetail(null, () -> new DiscountCoupon()));
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void getDetail_nullPlaceholder_returnsNull() {
        mockValueOps();
        when(valueOperations.get(DETAIL_KEY)).thenReturn(Constants.REDIS_COUPON_NULL_PLACEHOLDER);

        assertNull(cacheComponent.getDetail(COUPON_ID, () -> new DiscountCoupon()));
    }

    @Test
    void getDetail_cacheHitNotExpired_returnsCached() {
        mockValueOps();
        DiscountCoupon cached = new DiscountCoupon();
        cached.setCouponId(COUPON_ID);
        CouponLogicalCacheEntry entry = new CouponLogicalCacheEntry(
                JsonUtils.toJson(cached), System.currentTimeMillis() + 60_000L);
        when(valueOperations.get(DETAIL_KEY)).thenReturn(JsonUtils.toJson(entry));

        DiscountCoupon result = cacheComponent.getDetail(COUPON_ID, () -> {
            throw new AssertionError("db loader must not be called");
        });

        assertNotNull(result);
        assertEquals(COUPON_ID, result.getCouponId());
        verify(cacheRebuildExecutor, never()).execute(any());
    }

    @Test
    void getDetail_logicallyExpired_submitsRebuildAndReturnsCached() {
        mockValueOps();
        DiscountCoupon cached = new DiscountCoupon();
        cached.setCouponId(COUPON_ID);
        CouponLogicalCacheEntry entry = new CouponLogicalCacheEntry(
                JsonUtils.toJson(cached), System.currentTimeMillis() - 60_000L);
        when(valueOperations.get(DETAIL_KEY)).thenReturn(JsonUtils.toJson(entry));
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                .thenReturn(true);

        DiscountCoupon result = cacheComponent.getDetail(COUPON_ID, () -> cached);

        assertNotNull(result);
        verify(cacheRebuildExecutor).execute(any(Runnable.class));
    }

    @Test
    void getDetail_miss_loadsFromDbAndWrites() {
        mockValueOps();
        when(valueOperations.get(DETAIL_KEY)).thenReturn(null);
        DiscountCoupon fromDb = new DiscountCoupon();
        fromDb.setCouponId(COUPON_ID);

        DiscountCoupon result = cacheComponent.getDetail(COUPON_ID, () -> fromDb);

        assertSame(fromDb, result);
        verify(valueOperations).set(eq(DETAIL_KEY), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void getDetail_missDbNull_writesPlaceholder() {
        mockValueOps();
        when(valueOperations.get(DETAIL_KEY)).thenReturn(null);

        assertNull(cacheComponent.getDetail(COUPON_ID, () -> null));

        verify(valueOperations).set(eq(DETAIL_KEY), eq(Constants.REDIS_COUPON_NULL_PLACEHOLDER),
                eq(Constants.COUPON_CACHE_NULL_TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    // ==================== getPlazaList ====================

    @Test
    void getPlazaList_nullPlaceholder_returnsEmptyPage() {
        mockValueOps();
        when(valueOperations.get(anyString())).thenReturn(Constants.REDIS_COUPON_NULL_PLACEHOLDER);

        PaginationResultVO<DiscountCoupon> result =
                cacheComponent.getPlazaList("all", 1, 15, null, () -> {
                    throw new AssertionError("db loader must not be called");
                });

        assertTrue(result.getList().isEmpty());
        assertEquals(0, result.getTotalCount());
    }

    @Test
    void getPlazaList_cacheHitNotExpired_returnsCached() {
        mockValueOps();
        CouponLogicalCacheEntry entry = new CouponLogicalCacheEntry(
                JsonUtils.toJson(page()), System.currentTimeMillis() + 60_000L);
        when(valueOperations.get(anyString())).thenReturn(JsonUtils.toJson(entry));

        PaginationResultVO<DiscountCoupon> result =
                cacheComponent.getPlazaList("all", 1, 15, null, () -> {
                    throw new AssertionError("db loader must not be called");
                });

        assertEquals(1, result.getList().size());
        assertEquals(COUPON_ID, result.getList().get(0).getCouponId());
    }

    @Test
    void getPlazaList_miss_loadsFromDbAndWrites() {
        mockValueOps();
        when(valueOperations.get(anyString())).thenReturn(null);
        PaginationResultVO<DiscountCoupon> fromDb = page();

        PaginationResultVO<DiscountCoupon> result = cacheComponent.getPlazaList("all", 1, 15, "kw", () -> fromDb);

        assertSame(fromDb, result);
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void getPlazaList_emptyDbPage_writesPlaceholder() {
        mockValueOps();
        when(valueOperations.get(anyString())).thenReturn(null);
        PaginationResultVO<DiscountCoupon> empty = new PaginationResultVO<>(
                0, 15, 1, 0, Collections.emptyList());

        PaginationResultVO<DiscountCoupon> result = cacheComponent.getPlazaList("all", 1, 15, null, () -> empty);

        assertTrue(result.getList().isEmpty());
        verify(valueOperations).set(anyString(), eq(Constants.REDIS_COUPON_NULL_PLACEHOLDER),
                eq(Constants.COUPON_CACHE_NULL_TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    // ==================== 失效与预热 ====================

    @Test
    void invalidateAfterWrite_bumpsVersionAndDeletesDetail() {
        mockValueOps();

        cacheComponent.invalidateAfterWrite(COUPON_ID);

        verify(valueOperations).increment(Constants.REDIS_KEY_COUPON_PLAZA_CACHE_VERSION);
        verify(stringRedisTemplate).delete(DETAIL_KEY);
    }

    @Test
    void invalidateAfterWrite_emptyCouponId_onlyBumpsVersion() {
        mockValueOps();

        cacheComponent.invalidateAfterWrite(null);

        verify(valueOperations).increment(Constants.REDIS_KEY_COUPON_PLAZA_CACHE_VERSION);
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void invalidateDetail_deletesOnlyDetail() {
        cacheComponent.invalidateDetail(COUPON_ID);
        verify(stringRedisTemplate).delete(DETAIL_KEY);
    }

    @Test
    void warmPlazaListCache_warmsFourStatuses() {
        mockValueOps();
        DiscountCouponCacheComponent.PlazaListLoader loader =
                (status, pageNo, pageSize, keyword) -> page();

        cacheComponent.warmPlazaListCache(loader);

        verify(valueOperations, org.mockito.Mockito.times(4))
                .set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void warmPlazaListCache_nullLoader_skips() {
        cacheComponent.warmPlazaListCache(null);
        verify(stringRedisTemplate, never()).opsForValue();
    }
}
