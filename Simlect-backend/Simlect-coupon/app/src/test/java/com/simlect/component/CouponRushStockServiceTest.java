package com.simlect.component;

import com.simlect.api.dto.CouponRushStockReconcileDTO;
import com.simlect.constants.Constants;
import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.query.DiscountCouponQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.DiscountCouponMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CouponRushStockService 秒杀券库存（Redis 预热/同步/回补/对账）单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CouponRushStockServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private DiscountCouponMapper<DiscountCoupon, DiscountCouponQuery> discountCouponMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CouponRushStockService couponRushStockService;

    private static final String COUPON_ID = "CP1";
    private static final String USER_ID = "U1";

    private void mockValueOps() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private DiscountCoupon coupon(int totalCount, Integer remainCount) {
        DiscountCoupon c = new DiscountCoupon();
        c.setCouponId(COUPON_ID);
        c.setTotalCount(totalCount);
        c.setRemainCount(remainCount);
        return c;
    }

    // ==================== 基础状态 ====================

    @Test
    void stockKey_concatenatesPrefix() {
        assertEquals("mall:rushing:stock:" + COUPON_ID, couponRushStockService.stockKey(COUPON_ID));
    }

    @Test
    void isDbStockDepleted_emptyId_returnsFalse() {
        assertFalse(couponRushStockService.isDbStockDepleted(""));
        assertFalse(couponRushStockService.isDbStockDepleted(null));
        verify(stringRedisTemplate, never()).hasKey(anyString());
    }

    @Test
    void isStockSyncing_queriesKey() {
        when(stringRedisTemplate.hasKey("mall:rushing:stock:syncing:" + COUPON_ID)).thenReturn(true);
        assertTrue(couponRushStockService.isStockSyncing(COUPON_ID));
    }

    @Test
    void assertRushNotBlocked_syncing_throws() {
        when(stringRedisTemplate.hasKey("mall:rushing:stock:syncing:" + COUPON_ID)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponRushStockService.assertRushNotBlocked(COUPON_ID));

        assertEquals("库存同步中，请稍后再试", e.getMessage());
    }

    @Test
    void assertRushNotBlocked_notSyncing_passes() {
        when(stringRedisTemplate.hasKey("mall:rushing:stock:syncing:" + COUPON_ID)).thenReturn(false);

        assertDoesNotThrow(() -> couponRushStockService.assertRushNotBlocked(COUPON_ID));
    }

    // ==================== warmupStock ====================

    @Test
    void warmupStock_emptyId_skips() {
        couponRushStockService.warmupStock("", 10, 100);
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void warmupStock_zeroTotal_setsUnlimitedAndClearsDepleted() {
        mockValueOps();

        couponRushStockService.warmupStock(COUPON_ID, 5, 0);

        verify(valueOperations).set("mall:rushing:stock:" + COUPON_ID, String.valueOf(Constants.RUSHING_STOCK_UNLIMITED));
        verify(stringRedisTemplate).delete("mall:rushing:stock:depleted:" + COUPON_ID);
    }

    @Test
    void warmupStock_positiveRemain_setsStock() {
        mockValueOps();

        couponRushStockService.warmupStock(COUPON_ID, 50, 100);

        verify(valueOperations).set("mall:rushing:stock:" + COUPON_ID, "50");
        verify(stringRedisTemplate).delete("mall:rushing:stock:depleted:" + COUPON_ID);
    }

    @Test
    void warmupStock_nullRemain_setsZero() {
        mockValueOps();

        couponRushStockService.warmupStock(COUPON_ID, null, 100);

        verify(valueOperations).set("mall:rushing:stock:" + COUPON_ID, "0");
        verify(stringRedisTemplate, never()).delete("mall:rushing:stock:depleted:" + COUPON_ID);
    }

    // ==================== getRedisStock ====================

    @Test
    void getRedisStock_emptyId_returnsNull() {
        assertNull(couponRushStockService.getRedisStock(""));
    }

    @Test
    void getRedisStock_invalidRaw_returnsNull() {
        mockValueOps();
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn("abc");

        assertNull(couponRushStockService.getRedisStock(COUPON_ID));
    }

    @Test
    void getRedisStock_validRaw_parses() {
        mockValueOps();
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn(" 8 ");

        assertEquals(8, couponRushStockService.getRedisStock(COUPON_ID));
    }

    // ==================== syncFromDbIfRedisZero ====================

    @Test
    void syncFromDbIfRedisZero_redisHasStock_returnsNull() {
        mockValueOps();
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn("5");

        assertNull(couponRushStockService.syncFromDbIfRedisZero(COUPON_ID));
    }

    @Test
    void syncFromDbIfRedisZero_unlimitedCoupon_returnsNull() {
        mockValueOps();
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn("0");
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(0, 100));

        assertNull(couponRushStockService.syncFromDbIfRedisZero(COUPON_ID));
    }

    @Test
    void syncFromDbIfRedisZero_zeroStock_syncsFromDb() {
        mockValueOps();
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn("0");
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(100, 80));
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                .thenReturn(true);

        CouponRushStockReconcileDTO dto = couponRushStockService.syncFromDbIfRedisZero(COUPON_ID);

        assertNotNull(dto);
        assertEquals(80, dto.getDbRemainCount());
        verify(stringRedisTemplate).execute(any(), anyList(), anyString());
    }

    // ==================== syncFromDbAuthoritative ====================

    @Test
    void syncFromDbAuthoritative_lockBusy_throws() {
        mockValueOps();
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                .thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponRushStockService.syncFromDbAuthoritative(COUPON_ID));

        assertEquals("库存同步中，请稍后再试", e.getMessage());
    }

    @Test
    void syncFromDbAuthoritative_dbDepleted_marksDepletedKey() {
        mockValueOps();
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                .thenReturn(true);
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(100, 0));
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn("0");

        CouponRushStockReconcileDTO dto = couponRushStockService.syncFromDbAuthoritative(COUPON_ID);

        assertEquals(0, dto.getDbRemainCount());
        verify(valueOperations).set("mall:rushing:stock:depleted:" + COUPON_ID, "1");
        verify(stringRedisTemplate).execute(any(), anyList(), anyString());
    }

    // ==================== releaseStockAfterDbRefund ====================

    @Test
    void releaseStockAfterDbRefund_emptyArgs_skips() {
        couponRushStockService.releaseStockAfterDbRefund("", USER_ID);
        couponRushStockService.releaseStockAfterDbRefund(COUPON_ID, "");
        verify(discountCouponMapper, never()).selectByCouponIdForUpdate(anyString());
    }

    @Test
    void releaseStockAfterDbRefund_couponMissing_returns() {
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(null);

        couponRushStockService.releaseStockAfterDbRefund(COUPON_ID, USER_ID);

        verify(discountCouponMapper, never()).addStock(anyString());
    }

    @Test
    void releaseStockAfterDbRefund_alignsRedisImmediately() {
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(coupon(100, 60));
        when(discountCouponMapper.addStock(COUPON_ID)).thenReturn(1);
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(100, 61));

        couponRushStockService.releaseStockAfterDbRefund(COUPON_ID, USER_ID);

        verify(discountCouponMapper).addStock(COUPON_ID);
        verify(redisComponent).alignRushStockAfterRelease(COUPON_ID, USER_ID, 61);
    }

    @Test
    void releaseStockAfterDbRefund_addStockNoRows_rollsBackRedisReserve() {
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(coupon(100, 100));
        when(discountCouponMapper.addStock(COUPON_ID)).thenReturn(0);

        couponRushStockService.releaseStockAfterDbRefund(COUPON_ID, USER_ID);

        verify(redisComponent).rollbackRushRedisReserve(COUPON_ID, USER_ID);
        verify(redisComponent, never()).alignRushStockAfterRelease(anyString(), anyString(), anyInt());
    }

    @Test
    void releaseStockAfterDbRefund_unlimitedCoupon_alignsUnlimited() {
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(coupon(0, 999));
        when(discountCouponMapper.addStock(COUPON_ID)).thenReturn(1);
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(0, 999));

        couponRushStockService.releaseStockAfterDbRefund(COUPON_ID, USER_ID);

        verify(redisComponent).alignRushStockAfterRelease(COUPON_ID, USER_ID, Constants.RUSHING_STOCK_UNLIMITED);
    }

    // ==================== hasAvailableStock ====================

    @Test
    void hasAvailableStock_syncing_returnsFalse() {
        when(stringRedisTemplate.hasKey("mall:rushing:stock:syncing:" + COUPON_ID)).thenReturn(true);

        assertFalse(couponRushStockService.hasAvailableStock(COUPON_ID));
    }

    @Test
    void hasAvailableStock_depletedAndLimited_returnsFalse() {
        when(stringRedisTemplate.hasKey("mall:rushing:stock:syncing:" + COUPON_ID)).thenReturn(false);
        when(stringRedisTemplate.hasKey("mall:rushing:stock:depleted:" + COUPON_ID)).thenReturn(true);
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(100, 0));

        assertFalse(couponRushStockService.hasAvailableStock(COUPON_ID));
    }

    @Test
    void hasAvailableStock_redisNullAndCouponMissing_returnsFalse() {
        mockValueOps();
        when(stringRedisTemplate.hasKey("mall:rushing:stock:syncing:" + COUPON_ID)).thenReturn(false);
        when(stringRedisTemplate.hasKey("mall:rushing:stock:depleted:" + COUPON_ID)).thenReturn(false);
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn(null);
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(null);

        assertFalse(couponRushStockService.hasAvailableStock(COUPON_ID));
    }

    @Test
    void hasAvailableStock_redisMissing_warmsUpFromDb() {
        mockValueOps();
        when(stringRedisTemplate.hasKey("mall:rushing:stock:syncing:" + COUPON_ID)).thenReturn(false);
        when(stringRedisTemplate.hasKey("mall:rushing:stock:depleted:" + COUPON_ID)).thenReturn(false);
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn(null, "30");
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(100, 30));

        assertTrue(couponRushStockService.hasAvailableStock(COUPON_ID));

        verify(valueOperations).set("mall:rushing:stock:" + COUPON_ID, "30");
    }

    @Test
    void hasAvailableStock_unlimited_returnsTrue() {
        mockValueOps();
        when(stringRedisTemplate.hasKey("mall:rushing:stock:syncing:" + COUPON_ID)).thenReturn(false);
        when(stringRedisTemplate.hasKey("mall:rushing:stock:depleted:" + COUPON_ID)).thenReturn(false);
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID))
                .thenReturn(String.valueOf(Constants.RUSHING_STOCK_UNLIMITED));

        assertTrue(couponRushStockService.hasAvailableStock(COUPON_ID));
    }

    @Test
    void hasAvailableStock_zeroStock_returnsFalse() {
        mockValueOps();
        when(stringRedisTemplate.hasKey("mall:rushing:stock:syncing:" + COUPON_ID)).thenReturn(false);
        when(stringRedisTemplate.hasKey("mall:rushing:stock:depleted:" + COUPON_ID)).thenReturn(false);
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn("0");

        assertFalse(couponRushStockService.hasAvailableStock(COUPON_ID));
    }

    // ==================== reconcileOne / 全量 ====================

    @Test
    void reconcileOne_unlimitedWithMismatch_adjusts() {
        mockValueOps();
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(0, 100));
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn("5");

        CouponRushStockReconcileDTO dto = couponRushStockService.reconcileOne(COUPON_ID);

        assertEquals(100, dto.getDbRemainCount());
        assertEquals(5, dto.getRedisStockBefore());
        assertTrue(dto.isAdjusted());
        verify(valueOperations).set("mall:rushing:stock:" + COUPON_ID, String.valueOf(Constants.RUSHING_STOCK_UNLIMITED));
    }

    @Test
    void reconcileOne_matchingUnlimited_noAdjust() {
        mockValueOps();
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(0, 100));
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID))
                .thenReturn(String.valueOf(Constants.RUSHING_STOCK_UNLIMITED));

        CouponRushStockReconcileDTO dto = couponRushStockService.reconcileOne(COUPON_ID);

        assertFalse(dto.isAdjusted());
        verify(valueOperations, never()).set(anyString(), anyString());
    }

    @Test
    void reconcileOne_normalMismatch_adjustsToDb() {
        mockValueOps();
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(coupon(100, 45));
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn("10", "45");

        CouponRushStockReconcileDTO dto = couponRushStockService.reconcileOne(COUPON_ID);

        assertTrue(dto.isAdjusted());
        verify(valueOperations).set("mall:rushing:stock:" + COUPON_ID, "45");
    }

    @Test
    void reconcileOne_couponMissing_returnsEmptyDto() {
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(null);

        CouponRushStockReconcileDTO dto = couponRushStockService.reconcileOne(COUPON_ID);

        assertNull(dto.getDbRemainCount());
    }

    @Test
    void reconcileAllRushing_skipsInvalidAndReconciles() {
        DiscountCoupon valid = coupon(100, 50);
        DiscountCoupon nullId = new DiscountCoupon();
        when(discountCouponMapper.selectList(any(DiscountCouponQuery.class)))
                .thenReturn(Arrays.asList(valid, null, nullId));
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(valid);
        mockValueOps();
        when(valueOperations.get("mall:rushing:stock:" + COUPON_ID)).thenReturn("50");

        List<CouponRushStockReconcileDTO> results = couponRushStockService.reconcileAllRushing();

        assertEquals(1, results.size());
        assertEquals(COUPON_ID, results.get(0).getCouponId());
    }

    @Test
    void warmupAllRushingFromDb_empty_returnsZero() {
        when(discountCouponMapper.selectList(any(DiscountCouponQuery.class))).thenReturn(null);

        assertEquals(0, couponRushStockService.warmupAllRushingFromDb());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void warmupAllRushingFromDb_warmsEach() {
        mockValueOps();
        when(discountCouponMapper.selectList(any(DiscountCouponQuery.class)))
                .thenReturn(List.of(coupon(100, 50)));

        assertEquals(1, couponRushStockService.warmupAllRushingFromDb());

        verify(valueOperations).set(eq("mall:rushing:stock:" + COUPON_ID), eq("50"));
    }
}
