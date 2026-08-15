package com.simlect.controller.admin;

import com.simlect.api.dto.CouponRushStockReconcileDTO;
import com.simlect.api.dto.DiscountCouponDTO;
import com.simlect.biz.DiscountCouponService;
import com.simlect.component.CouponRushStockService;
import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.vo.PaginationResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端 DiscountCouponController 接口单元测试（MockMvc standalone）。
 */
@ExtendWith(MockitoExtension.class)
class DiscountCouponControllerTest {

    @Mock
    private DiscountCouponService discountCouponService;
    @Mock
    private CouponRushStockService couponRushStockService;

    private MockMvc mockMvc;

    private static final String COUPON_ID = "CP1";

    @BeforeEach
    void setUp() {
        com.simlect.controller.admin.DiscountCouponController controller =
                new com.simlect.controller.admin.DiscountCouponController();
        ReflectionTestUtils.setField(controller, "discountCouponService", discountCouponService);
        ReflectionTestUtils.setField(controller, "couponRushStockService", couponRushStockService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void saveDiscountCoupon_delegates() throws Exception {
        mockMvc.perform(post("/admin/discountCoupon/saveDiscountCoupon")
                        .param("couponName", "满减券")
                        .param("couponType", "1")
                        .param("thresholdAmount", "100")
                        .param("discountAmount", "20")
                        .param("totalCount", "200")
                        .param("validStartTime", "2026-08-01 00:00:00")
                        .param("validEndTime", "2026-08-31 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(discountCouponService).saveDiscountCoupon(any(DiscountCouponDTO.class));
    }

    @Test
    void loadDiscountCoupon_returnsPage() throws Exception {
        PaginationResultVO<DiscountCoupon> page = new PaginationResultVO<>(
                2, 15, 1, 1, List.of(new DiscountCoupon(), new DiscountCoupon()));
        when(discountCouponService.loadDiscountCoupon4Admin(1, 15, "满减", 1, 1)).thenReturn(page);

        mockMvc.perform(post("/admin/discountCoupon/loadDiscountCoupon")
                        .param("pageNo", "1")
                        .param("pageSize", "15")
                        .param("couponNameFuzzy", "满减")
                        .param("couponType", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2));
    }

    @Test
    void getDiscountCouponInfo_returnsCoupon() throws Exception {
        DiscountCoupon coupon = new DiscountCoupon();
        coupon.setCouponId(COUPON_ID);
        when(discountCouponService.getDiscountCouponByCouponId(COUPON_ID)).thenReturn(coupon);

        mockMvc.perform(post("/admin/discountCoupon/getDiscountCouponInfo").param("couponId", COUPON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.couponId").value(COUPON_ID));
    }

    @Test
    void updateDiscountCouponStatus_updatesStatus() throws Exception {
        DiscountCoupon coupon = new DiscountCoupon();
        coupon.setCouponId(COUPON_ID);
        coupon.setStatus(1);
        when(discountCouponService.getDiscountCouponByCouponId(COUPON_ID)).thenReturn(coupon);

        mockMvc.perform(post("/admin/discountCoupon/updateDiscountCouponStatus")
                        .param("couponId", COUPON_ID)
                        .param("status", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(discountCouponService).updateDiscountCouponByCouponId(any(DiscountCoupon.class), eq(COUPON_ID));
    }

    @Test
    void warmupRushStock_withCouponId_warmsAndReturnsRemain() throws Exception {
        DiscountCoupon coupon = new DiscountCoupon();
        coupon.setCouponId(COUPON_ID);
        coupon.setRemainCount(30);
        when(discountCouponService.getDiscountCouponByCouponId(COUPON_ID)).thenReturn(coupon);

        mockMvc.perform(post("/admin/discountCoupon/warmupRushStock").param("couponId", COUPON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(30));

        verify(couponRushStockService).warmupStock(COUPON_ID, 30);
    }

    @Test
    void warmupRushStock_withoutCouponId_warmsAll() throws Exception {
        when(couponRushStockService.warmupAllRushingFromDb()).thenReturn(3);

        mockMvc.perform(post("/admin/discountCoupon/warmupRushStock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void warmupRushStock_couponMissing_returnsNullData() throws Exception {
        when(discountCouponService.getDiscountCouponByCouponId(COUPON_ID)).thenReturn(null);

        mockMvc.perform(post("/admin/discountCoupon/warmupRushStock").param("couponId", COUPON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(couponRushStockService, org.mockito.Mockito.never()).warmupStock(any(), any());
    }

    @Test
    void reconcileRushStock_withCouponId_reconcilesOne() throws Exception {
        CouponRushStockReconcileDTO dto = new CouponRushStockReconcileDTO();
        dto.setCouponId(COUPON_ID);
        dto.setDbRemainCount(50);
        when(couponRushStockService.reconcileOne(COUPON_ID)).thenReturn(dto);

        mockMvc.perform(post("/admin/discountCoupon/reconcileRushStock").param("couponId", COUPON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dbRemainCount").value(50));
    }

    @Test
    void reconcileRushStock_withoutCouponId_reconcilesAll() throws Exception {
        when(couponRushStockService.reconcileAllRushing()).thenReturn(List.of(new CouponRushStockReconcileDTO()));

        mockMvc.perform(post("/admin/discountCoupon/reconcileRushStock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
