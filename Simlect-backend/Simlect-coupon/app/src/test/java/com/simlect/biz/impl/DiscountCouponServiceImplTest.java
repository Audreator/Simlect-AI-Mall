package com.simlect.biz.impl;

import com.simlect.api.dto.CouponRushPrepareDTO;
import com.simlect.api.dto.DiscountCouponDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.enums.CouponTypeEnum;
import com.simlect.api.enums.RushingCouponStatusEnum;
import com.simlect.api.enums.UserCouponStatusEnum;
import com.simlect.api.support.OrderFeignSupport;
import com.simlect.api.support.UserFeignSupport;
import com.simlect.component.CouponRushStockService;
import com.simlect.component.DiscountCouponCacheComponent;
import com.simlect.component.RedisComponent;
import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.po.UserCoupon;
import com.simlect.entity.query.DiscountCouponQuery;
import com.simlect.entity.query.UserCouponQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.DiscountCouponMapper;
import com.simlect.mappers.UserCouponMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DiscountCouponServiceImpl 优惠券管理/领券/秒杀预占释放/有效期校验单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DiscountCouponServiceImplTest {

    @Mock
    private DiscountCouponMapper<DiscountCoupon, DiscountCouponQuery> discountCouponMapper;
    @Mock
    private UserCouponMapper<UserCoupon, UserCouponQuery> userCouponMapper;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private OrderFeignSupport orderFeignSupport;
    @Mock
    private DiscountCouponCacheComponent discountCouponCacheComponent;
    @Mock
    private CouponRushStockService couponRushStockService;
    @Mock
    private UserFeignSupport userFeignSupport;

    @InjectMocks
    private DiscountCouponServiceImpl discountCouponService;

    private static final String COUPON_ID = "CP20260815001";

    private DiscountCoupon coupon(int type, BigDecimal threshold, BigDecimal discountAmount, BigDecimal rate) {
        DiscountCoupon coupon = new DiscountCoupon();
        coupon.setCouponId(COUPON_ID);
        coupon.setCouponName("测试券");
        coupon.setCouponType(type);
        coupon.setThresholdAmount(threshold);
        coupon.setDiscountAmount(discountAmount);
        coupon.setDiscountRate(rate);
        return coupon;
    }

    private DiscountCouponDTO dto(boolean newCoupon) {
        DiscountCouponDTO dto = new DiscountCouponDTO();
        if (!newCoupon) {
            dto.setCouponId(COUPON_ID);
        }
        dto.setCouponName("618满减券");
        dto.setCouponType(CouponTypeEnum.FULL.getStatus());
        dto.setThresholdAmount(new BigDecimal("100"));
        dto.setDiscountAmount(new BigDecimal("20"));
        dto.setTotalCount(200);
        dto.setValidStartTime("2026-08-01 00:00:00");
        dto.setValidEndTime("2026-08-31 23:59:59");
        dto.setRushingstatus(RushingCouponStatusEnum.YES.getStatus());
        dto.setRushingStartTime("2026-08-15 10:00:00");
        dto.setRushingEndTime("2026-08-15 12:00:00");
        return dto;
    }

    // ==================== 分页/增删改 ====================

    @Test
    void findListByPage_buildsPagination() {
        DiscountCouponQuery query = new DiscountCouponQuery();
        query.setPageNo(1);
        when(discountCouponMapper.selectCount(query)).thenReturn(30);
        when(discountCouponMapper.selectList(query)).thenReturn(List.of(new DiscountCoupon()));

        PaginationResultVO<DiscountCoupon> page = discountCouponService.findListByPage(query);

        assertEquals(30, page.getTotalCount());
        assertEquals(15, page.getPageSize());
        assertEquals(1, page.getList().size());
    }

    @Test
    void addBatch_nullOrEmpty_returnsZero() {
        assertEquals(0, discountCouponService.addBatch(null));
        assertEquals(0, discountCouponService.addBatch(Collections.emptyList()));
        verify(discountCouponMapper, never()).insertBatch(any());
    }

    @Test
    void updateDiscountCouponByCouponId_rowsUpdated_invalidatesCache() {
        when(discountCouponMapper.updateByCouponId(any(), eq(COUPON_ID))).thenReturn(1);

        Integer rows = discountCouponService.updateDiscountCouponByCouponId(new DiscountCoupon(), COUPON_ID);

        assertEquals(1, rows);
        verify(discountCouponCacheComponent).invalidateAfterWrite(COUPON_ID);
    }

    @Test
    void updateDiscountCouponByCouponId_noRows_noInvalidate() {
        when(discountCouponMapper.updateByCouponId(any(), eq(COUPON_ID))).thenReturn(0);

        discountCouponService.updateDiscountCouponByCouponId(new DiscountCoupon(), COUPON_ID);

        verify(discountCouponCacheComponent, never()).invalidateAfterWrite(any());
    }

    @Test
    void deleteDiscountCouponByCouponId_rowsDeleted_invalidatesCache() {
        when(discountCouponMapper.deleteByCouponId(COUPON_ID)).thenReturn(1);

        assertEquals(1, discountCouponService.deleteDiscountCouponByCouponId(COUPON_ID));

        verify(discountCouponCacheComponent).invalidateAfterWrite(COUPON_ID);
    }

    @Test
    void getDiscountCouponByCouponId_readsThroughCache() {
        DiscountCoupon expected = new DiscountCoupon();
        when(discountCouponCacheComponent.getDetail(eq(COUPON_ID), any())).thenReturn(expected);

        assertSame(expected, discountCouponService.getDiscountCouponByCouponId(COUPON_ID));
    }

    // ==================== 广场 hasBought 标记 ====================

    @Test
    void fillHasBoughtForPlaza_emptyUser_skips() {
        discountCouponService.fillHasBoughtForPlaza("", List.of(new DiscountCoupon()));
        discountCouponService.fillHasBoughtForPlaza(null, List.of(new DiscountCoupon()));
        discountCouponService.fillHasBoughtForPlaza("U1", null);
        discountCouponService.fillHasBoughtForPlaza("U1", Collections.emptyList());

        verify(userCouponMapper, never()).selectList(any());
    }

    @Test
    void fillHasBoughtForPlaza_userHoldsValidCoupon_marksBought() {
        DiscountCoupon coupon = coupon(CouponTypeEnum.FULL.getStatus(), BigDecimal.ZERO, new BigDecimal("10"), null);
        UserCoupon uc = new UserCoupon();
        uc.setCouponId(COUPON_ID);
        uc.setStatus(UserCouponStatusEnum.NOUSE.getStatus());
        when(userCouponMapper.selectList(any(UserCouponQuery.class))).thenReturn(List.of(uc));

        discountCouponService.fillHasBoughtForPlaza("U1", List.of(coupon));

        assertEquals(Boolean.TRUE, coupon.getHasBought());
        verify(redisComponent, never()).isUserRushCouponParticipant(any(), any());
    }

    @Test
    void fillHasBoughtForPlaza_onlyCantStatus_fallsBackToRedis() {
        DiscountCoupon coupon = coupon(CouponTypeEnum.FULL.getStatus(), BigDecimal.ZERO, new BigDecimal("10"), null);
        UserCoupon uc = new UserCoupon();
        uc.setCouponId(COUPON_ID);
        uc.setStatus(UserCouponStatusEnum.CANT.getStatus());
        when(userCouponMapper.selectList(any(UserCouponQuery.class))).thenReturn(List.of(uc));
        when(redisComponent.isUserRushCouponParticipant("U1", COUPON_ID)).thenReturn(true);

        discountCouponService.fillHasBoughtForPlaza("U1", List.of(coupon));

        assertEquals(Boolean.TRUE, coupon.getHasBought());
    }

    @Test
    void fillHasBoughtForPlaza_noOwnershipAndNotParticipant_marksNotBought() {
        DiscountCoupon coupon = coupon(CouponTypeEnum.FULL.getStatus(), BigDecimal.ZERO, new BigDecimal("10"), null);
        when(userCouponMapper.selectList(any(UserCouponQuery.class))).thenReturn(null);
        when(redisComponent.isUserRushCouponParticipant("U1", COUPON_ID)).thenReturn(false);

        discountCouponService.fillHasBoughtForPlaza("U1", List.of(coupon));

        assertEquals(Boolean.FALSE, coupon.getHasBought());
    }

    // ==================== saveDiscountCoupon：新增 ====================

    @Test
    void saveDiscountCoupon_newCoupon_insertsAndWarmsUp() {
        when(discountCouponMapper.insertOrUpdate(any(DiscountCoupon.class))).thenReturn(1);
        when(userFeignSupport.listUserIdsByPage(1, 500)).thenReturn(List.of("U1", "U2"));

        discountCouponService.saveDiscountCoupon(dto(true));

        ArgumentCaptor<DiscountCoupon> captor = ArgumentCaptor.forClass(DiscountCoupon.class);
        verify(discountCouponMapper).insertOrUpdate(captor.capture());
        DiscountCoupon coupon = captor.getValue();
        assertNotNull(coupon.getCouponId());
        assertTrue(coupon.getCouponId().startsWith("CP"));
        assertEquals(new BigDecimal("20"), coupon.getDiscountAmount());
        assertEquals(200, coupon.getRemainCount());
        verify(couponRushStockService).warmupStock(coupon.getCouponId(), 200, 200);
        verify(discountCouponCacheComponent).invalidateAfterWrite(coupon.getCouponId());
        verify(discountCouponCacheComponent).warmPlazaListCache(any());
        verify(userFeignSupport).sendNotifyAsync("U1", "秒杀券上线", "618满减券 已上线，快去抢购！", "rush_coupon", coupon.getCouponId());
        verify(userFeignSupport).sendNotifyAsync("U2", "秒杀券上线", "618满减券 已上线，快去抢购！", "rush_coupon", coupon.getCouponId());
    }

    @Test
    void saveDiscountCoupon_newNonRushingCoupon_noWarmupNoBroadcast() {
        DiscountCouponDTO dto = dto(true);
        dto.setRushingstatus(RushingCouponStatusEnum.NO.getStatus());
        when(discountCouponMapper.insertOrUpdate(any(DiscountCoupon.class))).thenReturn(1);

        discountCouponService.saveDiscountCoupon(dto);

        ArgumentCaptor<DiscountCoupon> captor = ArgumentCaptor.forClass(DiscountCoupon.class);
        verify(discountCouponMapper).insertOrUpdate(captor.capture());
        assertEquals(RushingCouponStatusEnum.NO.getStatus(), captor.getValue().getRushingstatus());
        verify(couponRushStockService, never()).warmupStock(any(), any());
        verify(discountCouponCacheComponent, never()).warmPlazaListCache(any());
        verify(userFeignSupport, never()).listAllUserIds();
    }

    // ==================== saveDiscountCoupon：编辑（保留已售份数） ====================

    @Test
    void saveDiscountCoupon_edit_preservesSoldCountInRemain() {
        DiscountCoupon existing = coupon(CouponTypeEnum.FULL.getStatus(), new BigDecimal("100"), new BigDecimal("20"), null);
        existing.setTotalCount(100);
        existing.setRemainCount(60);
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(existing);
        when(discountCouponMapper.insertOrUpdate(any(DiscountCoupon.class))).thenReturn(1);

        DiscountCouponDTO editDto = dto(false);
        editDto.setTotalCount(200);
        discountCouponService.saveDiscountCoupon(editDto);

        ArgumentCaptor<DiscountCoupon> captor = ArgumentCaptor.forClass(DiscountCoupon.class);
        verify(discountCouponMapper).insertOrUpdate(captor.capture());
        DiscountCoupon saved = captor.getValue();
        // 已售 40，新总量 200 → remain 160
        assertEquals(160, saved.getRemainCount());
        assertEquals(COUPON_ID, saved.getCouponId());
    }

    @Test
    void saveDiscountCoupon_editCouponNotExists_throws() {
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> discountCouponService.saveDiscountCoupon(dto(false)));

        assertEquals("优惠卷不存在", e.getMessage());
        verify(discountCouponMapper, never()).insertOrUpdate(any());
    }

    // ==================== calcDiscountAmount：有效期与门槛 ====================

    @Test
    void calcDiscountAmount_nullCouponOrNonPositiveAmount_returnsZero() {
        assertEquals(BigDecimal.ZERO, discountCouponService.calcDiscountAmount(null, new BigDecimal("100")));
        assertEquals(BigDecimal.ZERO, discountCouponService.calcDiscountAmount(new DiscountCoupon(), BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO, discountCouponService.calcDiscountAmount(new DiscountCoupon(), null));
    }

    @Test
    void calcDiscountAmount_notStarted_throws() {
        DiscountCoupon c = coupon(CouponTypeEnum.FULL.getStatus(), BigDecimal.ZERO, new BigDecimal("10"), null);
        c.setValidStartTime(new Date(System.currentTimeMillis() + 3_600_000L));

        assertThrows(BusinessException.class, () -> discountCouponService.calcDiscountAmount(c, new BigDecimal("100")));
    }

    @Test
    void calcDiscountAmount_expired_throws() {
        DiscountCoupon c = coupon(CouponTypeEnum.FULL.getStatus(), BigDecimal.ZERO, new BigDecimal("10"), null);
        c.setValidEndTime(new Date(System.currentTimeMillis() - 3_600_000L));

        assertThrows(BusinessException.class, () -> discountCouponService.calcDiscountAmount(c, new BigDecimal("100")));
    }

    @Test
    void calcDiscountAmount_fullBelowThreshold_throws() {
        DiscountCoupon c = coupon(CouponTypeEnum.FULL.getStatus(), new BigDecimal("100"), new BigDecimal("20"), null);

        assertThrows(BusinessException.class, () -> discountCouponService.calcDiscountAmount(c, new BigDecimal("99")));
    }

    @Test
    void calcDiscountAmount_fullAboveThreshold_returnsDiscount() {
        DiscountCoupon c = coupon(CouponTypeEnum.FULL.getStatus(), new BigDecimal("100"), new BigDecimal("20"), null);

        assertEquals(new BigDecimal("20"), discountCouponService.calcDiscountAmount(c, new BigDecimal("150")));
    }

    @Test
    void calcDiscountAmount_discountType_computesAmount() {
        DiscountCoupon c = coupon(CouponTypeEnum.DISCOUNT.getStatus(), new BigDecimal("50"), null, new BigDecimal("0.85"));

        BigDecimal discount = discountCouponService.calcDiscountAmount(c, new BigDecimal("100"));

        assertEquals(new BigDecimal("15.00"), discount);
    }

    @Test
    void calcDiscountAmount_discountRateInvalid_throws() {
        DiscountCoupon c = coupon(CouponTypeEnum.DISCOUNT.getStatus(), BigDecimal.ZERO, null, new BigDecimal("1.5"));

        assertThrows(BusinessException.class, () -> discountCouponService.calcDiscountAmount(c, new BigDecimal("100")));
    }

    @Test
    void calcDiscountAmount_noThreshold_returnsDiscount() {
        DiscountCoupon c = coupon(CouponTypeEnum.NOTHRESHOLD.getStatus(), null, new BigDecimal("5"), null);

        assertEquals(new BigDecimal("5"), discountCouponService.calcDiscountAmount(c, new BigDecimal("20")));
    }

    @Test
    void calcDiscountAmount_unknownType_throws() {
        DiscountCoupon c = coupon(99, BigDecimal.ZERO, null, null);

        assertThrows(BusinessException.class, () -> discountCouponService.calcDiscountAmount(c, new BigDecimal("20")));
    }

    // ==================== 秒杀预占/释放 ====================

    @Test
    void rushCoupon_delegatesToOrderFeign() {
        CouponRushPrepareDTO expected = new CouponRushPrepareDTO();
        when(orderFeignSupport.prepareCouponRush("U1", COUPON_ID)).thenReturn(expected);

        assertSame(expected, discountCouponService.rushCoupon("U1", COUPON_ID));
    }

    @Test
    void buyDiscountCoupon_delegatesToOrderFeign() {
        PayInfoDTO expected = new PayInfoDTO("html", "PO1", new BigDecimal("0.01"));
        when(orderFeignSupport.postCouponRushOrder("U1", COUPON_ID, "alipay")).thenReturn(expected);

        assertSame(expected, discountCouponService.buyDiscountCoupon("U1", COUPON_ID, "alipay"));
    }

    @Test
    void releaseRushRedisReserve_rollsBackRedis() {
        discountCouponService.releaseRushRedisReserve(COUPON_ID, "U1");
        verify(redisComponent).rollbackRushRedisReserve(COUPON_ID, "U1");
    }

    @Test
    void releaseRushCouponReserve_releasesDbStockAndInvalidates() {
        discountCouponService.releaseRushCouponReserve(COUPON_ID, "U1");

        verify(couponRushStockService).releaseStockAfterDbRefund(COUPON_ID, "U1");
        verify(discountCouponCacheComponent).invalidateAfterWrite(COUPON_ID);
    }

    // ==================== 管理端加载 ====================

    @Test
    void loadDiscountCoupon4Admin_buildsQueryAndPages() {
        when(discountCouponMapper.selectCount(any(DiscountCouponQuery.class))).thenReturn(3);
        when(discountCouponMapper.selectList(any(DiscountCouponQuery.class))).thenReturn(List.of(new DiscountCoupon()));

        PaginationResultVO<DiscountCoupon> page =
                discountCouponService.loadDiscountCoupon4Admin(1, 15, "满减", CouponTypeEnum.FULL.getStatus(), 1);

        assertEquals(3, page.getTotalCount());
        assertEquals(1, page.getList().size());
    }

    @Test
    void loadDiscountCoupon_goesThroughPlazaCache() {
        PaginationResultVO<DiscountCoupon> expected = new PaginationResultVO<>();
        when(discountCouponCacheComponent.getPlazaList(isNull(), eq(1), eq(15), isNull(), any())).thenReturn(expected);

        assertSame(expected, discountCouponService.loadDiscountCoupon(1, 15, null, null));
    }
}
