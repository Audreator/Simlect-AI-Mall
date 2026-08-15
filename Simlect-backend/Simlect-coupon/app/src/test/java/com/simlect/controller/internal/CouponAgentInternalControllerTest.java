package com.simlect.controller.internal;

import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.po.UserCoupon;
import com.simlect.entity.query.DiscountCouponQuery;
import com.simlect.entity.query.UserCouponQuery;
import com.simlect.mappers.DiscountCouponMapper;
import com.simlect.mappers.UserCouponMapper;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CouponAgentInternalController Agent 用户券查询接口单元测试（MockMvc standalone）。
 */
@ExtendWith(MockitoExtension.class)
class CouponAgentInternalControllerTest {

    @Mock
    private UserCouponMapper<UserCoupon, UserCouponQuery> userCouponMapper;
    @Mock
    private DiscountCouponMapper<DiscountCoupon, DiscountCouponQuery> discountCouponMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CouponAgentInternalController controller = new CouponAgentInternalController();
        ReflectionTestUtils.setField(controller, "userCouponMapper", userCouponMapper);
        ReflectionTestUtils.setField(controller, "discountCouponMapper", discountCouponMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listUserCoupons_emptyUserId_returnsEmptyList() throws Exception {
        mockMvc.perform(post("/internal/coupon/agent/listUserCoupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(userCouponMapper, never()).selectList(any());
    }

    @Test
    void listUserCoupons_mergesTemplateDetails() throws Exception {
        UserCoupon uc1 = new UserCoupon();
        uc1.setUserCouponId("UC1");
        uc1.setUserId("U1");
        uc1.setCouponId("CP1");
        uc1.setStatus(0);
        UserCoupon uc2 = new UserCoupon();
        uc2.setUserCouponId("UC2");
        uc2.setUserId("U1");
        uc2.setCouponId("CP_MISSING");
        uc2.setStatus(1);
        when(userCouponMapper.selectList(any(UserCouponQuery.class))).thenReturn(List.of(uc1, uc2));

        DiscountCoupon dc = new DiscountCoupon();
        dc.setCouponId("CP1");
        dc.setCouponName("满减券");
        dc.setCouponType(1);
        dc.setDiscountAmount(new BigDecimal("20"));
        dc.setThresholdAmount(new BigDecimal("100"));
        when(discountCouponMapper.selectByCouponId("CP1")).thenReturn(dc);
        when(discountCouponMapper.selectByCouponId("CP_MISSING")).thenReturn(null);

        mockMvc.perform(post("/internal/coupon/agent/listUserCoupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"U1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userCouponId").value("UC1"))
                .andExpect(jsonPath("$.data[0].couponName").value("满减券"))
                .andExpect(jsonPath("$.data[0].minAmount").value(100))
                .andExpect(jsonPath("$.data[0].thresholdAmount").value(100))
                .andExpect(jsonPath("$.data[1].couponName").doesNotExist());

        verify(discountCouponMapper).selectByCouponId(eq("CP1"));
    }
}
