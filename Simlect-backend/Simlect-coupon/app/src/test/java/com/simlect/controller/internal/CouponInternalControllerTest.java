package com.simlect.controller.internal;

import com.simlect.api.dto.CouponIdDTO;
import com.simlect.api.dto.CouponRushOpsDTO;
import com.simlect.api.dto.CouponValidateAndLockDTO;
import com.simlect.api.dto.UserCouponCreateDTO;
import com.simlect.api.dto.UserCouponIdDTO;
import com.simlect.api.dto.UserCouponStatusChangeDTO;
import com.simlect.api.vo.CouponBriefVO;
import com.simlect.api.vo.CouponLockResultVO;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.api.vo.StockChangeResultVO;
import com.simlect.api.vo.UserCouponVO;
import com.simlect.biz.CouponInternalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CouponInternalController 内部优惠券接口单元测试（MockMvc standalone）。
 */
@ExtendWith(MockitoExtension.class)
class CouponInternalControllerTest {

    @Mock
    private CouponInternalService couponInternalService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CouponInternalController controller = new CouponInternalController();
        ReflectionTestUtils.setField(controller, "couponInternalService", couponInternalService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void validateAndLock_returnsLockResult() throws Exception {
        CouponLockResultVO result = new CouponLockResultVO();
        result.setUserCouponId("UC1");
        result.setDiscountAmount(new BigDecimal("20"));
        when(couponInternalService.validateAndLock(any(CouponValidateAndLockDTO.class))).thenReturn(result);

        mockMvc.perform(post("/internal/coupon/validateAndLock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"U1\",\"userCouponId\":\"UC1\",\"orderAmount\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountAmount").value(20));

        verify(couponInternalService).validateAndLock(any(CouponValidateAndLockDTO.class));
    }

    @Test
    void getCoupon_returnsVo() throws Exception {
        DiscountCouponVO vo = new DiscountCouponVO();
        vo.setCouponId("CP1");
        when(couponInternalService.getCoupon("CP1")).thenReturn(vo);

        mockMvc.perform(post("/internal/coupon/getCoupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponId\":\"CP1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.couponId").value("CP1"));
    }

    @Test
    void getCouponBrief_returnsBrief() throws Exception {
        CouponBriefVO vo = new CouponBriefVO();
        vo.setCouponId("CP1");
        when(couponInternalService.getCouponBrief("CP1")).thenReturn(vo);

        mockMvc.perform(post("/internal/coupon/getCouponBrief")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponId\":\"CP1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.couponId").value("CP1"));
    }

    @Test
    void getUserCoupon_returnsVo() throws Exception {
        UserCouponVO vo = new UserCouponVO();
        vo.setUserCouponId("UC1");
        when(couponInternalService.getUserCoupon("UC1")).thenReturn(vo);

        mockMvc.perform(post("/internal/coupon/getUserCoupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userCouponId\":\"UC1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userCouponId").value("UC1"));
    }

    @Test
    void changeUserCouponStatus_delegates() throws Exception {
        mockMvc.perform(post("/internal/coupon/changeUserCouponStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userCouponId\":\"UC1\",\"userId\":\"U1\",\"fromStatus\":0,\"toStatus\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(couponInternalService).changeUserCouponStatus(any(UserCouponStatusChangeDTO.class));
    }

    @Test
    void createUserCoupon_delegates() throws Exception {
        mockMvc.perform(post("/internal/coupon/createUserCoupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userCouponId\":\"UC1\",\"userId\":\"U1\",\"couponId\":\"CP1\",\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(couponInternalService).createUserCoupon(any(UserCouponCreateDTO.class));
    }

    @Test
    void deductStock_returnsAffectedRows() throws Exception {
        when(couponInternalService.deductStock("CP1")).thenReturn(1);

        mockMvc.perform(post("/internal/coupon/deductStock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponId\":\"CP1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affectedRows").value(1));
    }

    @Test
    void rushOps_delegate() throws Exception {
        mockMvc.perform(post("/internal/coupon/rush/assertNotBlocked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponId\":\"CP1\"}"))
                .andExpect(status().isOk());
        verify(couponInternalService).assertRushNotBlocked("CP1");

        when(couponInternalService.hasAvailableRushStock("CP1")).thenReturn(true);
        mockMvc.perform(post("/internal/coupon/rush/hasAvailableStock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponId\":\"CP1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(post("/internal/coupon/rush/syncFromDbIfRedisZero")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponId\":\"CP1\"}"))
                .andExpect(status().isOk());
        verify(couponInternalService).syncRushStockFromDbIfRedisZero("CP1");

        mockMvc.perform(post("/internal/coupon/rush/releaseRedisReserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponId\":\"CP1\",\"userId\":\"U1\"}"))
                .andExpect(status().isOk());
        verify(couponInternalService).releaseRushRedisReserve("CP1", "U1");

        mockMvc.perform(post("/internal/coupon/rush/releaseCouponReserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponId\":\"CP1\",\"userId\":\"U1\"}"))
                .andExpect(status().isOk());
        verify(couponInternalService).releaseRushCouponReserve("CP1", "U1");

        mockMvc.perform(post("/internal/coupon/rush/invalidateCache")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponId\":\"CP1\"}"))
                .andExpect(status().isOk());
        verify(couponInternalService).invalidateCouponCache("CP1");
    }
}
