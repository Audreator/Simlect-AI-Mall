package com.simlect.controller;

import com.simlect.api.dto.CouponRushPrepareDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.enums.UserCouponStatusEnum;
import com.simlect.api.support.OrderFeignSupport;
import com.simlect.api.vo.UserCouponDetailVO;
import com.simlect.biz.DiscountCouponService;
import com.simlect.biz.UserCouponService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.po.UserCoupon;
import com.simlect.entity.query.UserCouponQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DiscountCouponController 用户端优惠券接口单元测试（MockMvc standalone）。
 */
@ExtendWith(MockitoExtension.class)
class DiscountCouponControllerTest {

    @Mock
    private DiscountCouponService discountCouponService;
    @Mock
    private UserCouponService userCouponService;
    @Mock
    private OrderFeignSupport orderFeignSupport;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;

    private MockMvc mockMvc;

    private static final String TOKEN = "token-coupon";
    private static final String USER_ID = "U001";
    private static final String COUPON_ID = "CP1";

    @BeforeEach
    void setUp() {
        DiscountCouponController controller = new DiscountCouponController();
        ReflectionTestUtils.setField(controller, "discountCouponService", discountCouponService);
        ReflectionTestUtils.setField(controller, "userCouponService", userCouponService);
        ReflectionTestUtils.setField(controller, "orderFeignSupport", orderFeignSupport);
        ReflectionTestUtils.setField(controller, "redisComponent", redisComponent);
        ReflectionTestUtils.setField(controller, "authCookieHelper", authCookieHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void mockLogin() {
        TokenUserInfoDTO user = new TokenUserInfoDTO();
        user.setUserId(USER_ID);
        doReturn(TOKEN).when(authCookieHelper).resolveWebToken(any());
        when(redisComponent.getTokenUserInfo(TOKEN)).thenReturn(user);
    }

    @Test
    void getDiscountCouponDetail_returnsCoupon() throws Exception {
        DiscountCoupon coupon = new DiscountCoupon();
        coupon.setCouponId(COUPON_ID);
        when(discountCouponService.getDiscountCouponByCouponId(COUPON_ID)).thenReturn(coupon);

        mockMvc.perform(post("/discountCoupon/getDiscountCouponDetail").param("couponId", COUPON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.couponId").value(COUPON_ID));
    }

    @Test
    void loadDiscountCoupon_withoutLogin_skipsHasBoughtFill() throws Exception {
        PaginationResultVO<DiscountCoupon> page = new PaginationResultVO<>(
                1, 15, 1, 1, List.of(new DiscountCoupon()));
        when(discountCouponService.loadDiscountCoupon(1, 15, "all", null)).thenReturn(page);

        mockMvc.perform(post("/discountCoupon/loadDiscountCoupon")
                        .param("pageNo", "1")
                        .param("pageSize", "15")
                        .param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(discountCouponService, never()).fillHasBoughtForPlaza(anyString(), any());
    }

    @Test
    void loadDiscountCoupon_withLogin_fillsHasBought() throws Exception {
        mockLogin();
        PaginationResultVO<DiscountCoupon> page = new PaginationResultVO<>(
                1, 15, 1, 1, List.of(new DiscountCoupon()));
        when(discountCouponService.loadDiscountCoupon(1, 15, "all", null)).thenReturn(page);

        mockMvc.perform(post("/discountCoupon/loadDiscountCoupon")
                        .param("pageNo", "1")
                        .param("pageSize", "15")
                        .param("status", "all"))
                .andExpect(status().isOk());

        verify(discountCouponService).fillHasBoughtForPlaza(USER_ID, page.getList());
    }

    @Test
    void rushCoupon_requiresLoginAndDelegates() throws Exception {
        mockLogin();
        CouponRushPrepareDTO prepare = new CouponRushPrepareDTO();
        prepare.setCouponId(COUPON_ID);
        prepare.setPayAmount(new BigDecimal("0.01"));
        when(discountCouponService.rushCoupon(USER_ID, COUPON_ID)).thenReturn(prepare);

        mockMvc.perform(post("/discountCoupon/rushCoupon").param("couponId", COUPON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.couponId").value(COUPON_ID));

        verify(discountCouponService).rushCoupon(USER_ID, COUPON_ID);
    }

    @Test
    void rushCoupon_withoutLogin_throws() {
        doReturn(null).when(authCookieHelper).resolveWebToken(any());

        assertThrows(jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(post("/discountCoupon/rushCoupon").param("couponId", COUPON_ID)));
    }

    @Test
    void buyDiscountCoupon_requiresLoginAndDelegates() throws Exception {
        mockLogin();
        PayInfoDTO payInfo = new PayInfoDTO("html", "PO1", new BigDecimal("0.01"));
        when(discountCouponService.buyDiscountCoupon(USER_ID, COUPON_ID, "alipay")).thenReturn(payInfo);

        mockMvc.perform(post("/discountCoupon/buyDiscountCoupon")
                        .param("couponId", COUPON_ID)
                        .param("payMethod", "alipay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payOrderId").value("PO1"));

        verify(discountCouponService).buyDiscountCoupon(USER_ID, COUPON_ID, "alipay");
    }

    @Test
    void loadUserCoupon_returnsDetailVOs() throws Exception {
        mockLogin();
        UserCoupon uc = new UserCoupon();
        uc.setUserCouponId("UC1");
        uc.setUserId(USER_ID);
        uc.setCouponId(COUPON_ID);
        uc.setStatus(UserCouponStatusEnum.NOUSE.getStatus());
        PaginationResultVO<UserCoupon> page = new PaginationResultVO<>(
                1, 15, 1, 1, List.of(uc));
        when(userCouponService.findListByPage(any(UserCouponQuery.class))).thenReturn(page);
        DiscountCoupon dc = new DiscountCoupon();
        dc.setCouponId(COUPON_ID);
        dc.setCouponName("满减券");
        dc.setCouponType(1);
        dc.setDiscountAmount(new BigDecimal("20"));
        dc.setThresholdAmount(new BigDecimal("100"));
        dc.setValidEndTime(new Date(System.currentTimeMillis() + 86_400_000L));
        when(discountCouponService.getDiscountCouponByCouponId(COUPON_ID)).thenReturn(dc);

        mockMvc.perform(post("/discountCoupon/loadUserCoupon").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.list[0].couponName").value("满减券"))
                .andExpect(jsonPath("$.data.list[0].status").value(0));

        verify(orderFeignSupport).syncPaidCouponRushUserCoupons(USER_ID);
    }

    @Test
    void loadUserCoupon_statusNouse_removesOverdueAfterFilter() throws Exception {
        mockLogin();
        UserCoupon usable = new UserCoupon();
        usable.setUserCouponId("UC1");
        usable.setCouponId("CP1");
        usable.setStatus(UserCouponStatusEnum.NOUSE.getStatus());
        UserCoupon overdue = new UserCoupon();
        overdue.setUserCouponId("UC2");
        overdue.setCouponId("CP2");
        overdue.setStatus(UserCouponStatusEnum.NOUSE.getStatus());
        PaginationResultVO<UserCoupon> page = new PaginationResultVO<>(
                2, 15, 1, 1, List.of(usable, overdue));
        when(userCouponService.findListByPage(any(UserCouponQuery.class))).thenReturn(page);

        DiscountCoupon usableDc = new DiscountCoupon();
        usableDc.setCouponId("CP1");
        usableDc.setValidEndTime(new Date(System.currentTimeMillis() + 86_400_000L));
        DiscountCoupon overdueDc = new DiscountCoupon();
        overdueDc.setCouponId("CP2");
        overdueDc.setValidEndTime(new Date(System.currentTimeMillis() - 3_600_000L));
        when(discountCouponService.getDiscountCouponByCouponId("CP1")).thenReturn(usableDc);
        when(discountCouponService.getDiscountCouponByCouponId("CP2")).thenReturn(overdueDc);

        mockMvc.perform(post("/discountCoupon/loadUserCoupon")
                        .param("pageNo", "1")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                // 过期券被动态置为 2（OVERDUE）后，在“未使用”过滤中被移除
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].userCouponId").value("UC1"));
    }

    @Test
    void loadUserCoupon_skipsMissingDetail() throws Exception {
        mockLogin();
        UserCoupon uc = new UserCoupon();
        uc.setUserCouponId("UC1");
        uc.setCouponId("CP_MISSING");
        uc.setStatus(UserCouponStatusEnum.NOUSE.getStatus());
        PaginationResultVO<UserCoupon> page = new PaginationResultVO<>(
                1, 15, 1, 1, List.of(uc));
        when(userCouponService.findListByPage(any(UserCouponQuery.class))).thenReturn(page);
        when(discountCouponService.getDiscountCouponByCouponId("CP_MISSING")).thenReturn(null);

        mockMvc.perform(post("/discountCoupon/loadUserCoupon").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(0));
    }
}
