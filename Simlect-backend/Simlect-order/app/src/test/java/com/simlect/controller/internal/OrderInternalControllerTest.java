package com.simlect.controller.internal;

import com.simlect.api.dto.CouponRushPayRequestDTO;
import com.simlect.api.dto.CouponRushPrepareDTO;
import com.simlect.api.dto.OrderIdDTO;
import com.simlect.api.dto.OrderStatsRangeDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.dto.UserIdDTO;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.api.vo.OrderBriefVO;
import com.simlect.api.vo.OrderRangeStatsVO;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderInternalService;
import com.simlect.entity.po.OrderInfo;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderInternalControllerTest {

    @Mock
    private OrderInternalService orderInternalService;
    @Mock
    private OrderInfoService orderInfoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderInternalController controller = new OrderInternalController();
        ReflectionTestUtils.setField(controller, "orderInternalService", orderInternalService);
        ReflectionTestUtils.setField(controller, "orderInfoService", orderInfoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getOrder_success() throws Exception {
        OrderBriefVO vo = new OrderBriefVO();
        vo.setOrderId("O1");
        when(orderInternalService.getOrder(any(OrderIdDTO.class))).thenReturn(vo);

        mockMvc.perform(post("/internal/order/get")
                        .contentType("application/json")
                        .content("{\"orderId\":\"O1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value("O1"));
    }

    @Test
    void cancelUnpaidForPayTimeout_success() throws Exception {
        when(orderInternalService.cancelUnpaidForPayTimeout(any(OrderIdDTO.class))).thenReturn(true);
        mockMvc.perform(post("/internal/order/cancelUnpaidForPayTimeout")
                        .contentType("application/json")
                        .content("{\"orderId\":\"O1\"}"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void confirmReceipt_success() throws Exception {
        when(orderInternalService.confirmReceipt(any(OrderIdDTO.class))).thenReturn(true);
        mockMvc.perform(post("/internal/order/confirmReceipt")
                        .contentType("application/json")
                        .content("{\"orderId\":\"O1\",\"userId\":\"U1\"}"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void onConfirmed_delegates() throws Exception {
        mockMvc.perform(post("/internal/order/onConfirmed")
                        .contentType("application/json")
                        .content("{\"orderId\":\"O1\",\"userId\":\"U1\"}"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderInternalService).onConfirmed(any(OrderIdDTO.class));
    }

    @Test
    void paySuccess_delegates() throws Exception {
        mockMvc.perform(post("/internal/order/paySuccess")
                        .contentType("application/json")
                        .content("{\"payOrderId\":\"PO1\",\"channelOrderId\":\"CH1\"}"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderInternalService).paySuccess(any(PayOrderNotifyDTO.class));
    }

    @Test
    void prepareCouponRush_success() throws Exception {
        CouponRushPrepareDTO dto = new CouponRushPrepareDTO();
        dto.setOrderId("O1");
        when(orderInternalService.prepareCouponRush(any())).thenReturn(dto);

        mockMvc.perform(post("/internal/order/prepareCouponRush")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\",\"couponId\":\"C1\"}"))
                .andExpect(jsonPath("$.data.orderId").value("O1"));
    }

    @Test
    void postCouponRushOrder_success() throws Exception {
        PayInfoDTO payInfo = new PayInfoDTO();
        payInfo.setOrderId("O1");
        when(orderInternalService.postCouponRushOrder(any(CouponRushPayRequestDTO.class))).thenReturn(payInfo);

        mockMvc.perform(post("/internal/order/postCouponRushOrder")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\",\"couponId\":\"C1\",\"payMethod\":\"alipay_pc\"}"))
                .andExpect(jsonPath("$.data.orderId").value("O1"));
    }

    @Test
    void syncPaidCouponRushUserCoupons_delegates() throws Exception {
        mockMvc.perform(post("/internal/order/syncPaidCouponRushUserCoupons")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\"}"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderInternalService).syncPaidCouponRushUserCoupons(any(UserIdDTO.class));
    }

    @Test
    void cancelOrder_delegates() throws Exception {
        mockMvc.perform(post("/internal/order/cancelOrder")
                        .contentType("application/json")
                        .content("{\"orderId\":\"O1\",\"userId\":\"U1\"}"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderInternalService).cancelOrder(any(OrderIdDTO.class));
    }

    @Test
    void aggregateRange_success() throws Exception {
        OrderRangeStatsVO vo = new OrderRangeStatsVO();
        when(orderInternalService.aggregateRange(any(OrderStatsRangeDTO.class))).thenReturn(vo);

        mockMvc.perform(post("/internal/order/stats/range")
                        .contentType("application/json")
                        .content("{\"startTime\":\"2026-08-01 00:00:00\",\"endTime\":\"2026-08-31 23:59:59\"}"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void aggregateDaily_success() throws Exception {
        when(orderInternalService.aggregateDaily(any(OrderStatsRangeDTO.class))).thenReturn(List.of());

        mockMvc.perform(post("/internal/order/stats/daily")
                        .contentType("application/json")
                        .content("{\"startTime\":\"2026-08-01 00:00:00\",\"endTime\":\"2026-08-31 23:59:59\"}"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void addAllWaitPayToDelayQueue_queriesWaitPayAndSends() throws Exception {
        OrderInfo order = new OrderInfo();
        order.setOrderId("O1");
        order.setOrderStatus(OrderStatusEnum.WAIT_PAYMENT.getStatus());
        when(orderInfoService.findListByParam(any())).thenReturn(List.of(order));

        mockMvc.perform(post("/internal/order/tool/addAllWaitPayToDelayQueue"))
                .andExpect(jsonPath("$.status").value("success"));

        verify(orderInfoService).addAllOrderToDelayQueue(any());
    }
}
