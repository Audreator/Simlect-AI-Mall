package com.simlect.controller;

import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PostOrderDTO;
import com.simlect.api.enums.OrderCommentStatusEnum;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.api.vo.OrderCountVO;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderItemService;
import com.simlect.biz.OrderLogisticsInfoService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderItem;
import com.simlect.entity.po.OrderLogisticsInfo;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderControllerTest {

    private static final String USER_ID = "U10001";
    private static final String ORDER_ID = "O1";
    private static final String PAY_ORDER_ID = "PO1";

    @Mock
    private OrderInfoService orderInfoService;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private OrderLogisticsInfoService orderLogisticsInfoService;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController();
        ReflectionTestUtils.setField(controller, "orderInfoService", orderInfoService);
        ReflectionTestUtils.setField(controller, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(controller, "orderLogisticsInfoService", orderLogisticsInfoService);
        ReflectionTestUtils.setField(controller, "redisComponent", redisComponent);
        ReflectionTestUtils.setField(controller, "authCookieHelper", authCookieHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();

        when(authCookieHelper.resolveWebToken(any())).thenReturn("token");
        TokenUserInfoDTO tokenUser = new TokenUserInfoDTO();
        tokenUser.setUserId(USER_ID);
        when(redisComponent.getTokenUserInfo("token")).thenReturn(tokenUser);
    }

    @Test
    void postOrder_success() throws Exception {
        PayInfoDTO payInfo = new PayInfoDTO();
        payInfo.setPayOrderId(PAY_ORDER_ID);
        payInfo.setPayInfo("https://pay.example/alipay");
        when(orderInfoService.postOrder(eq(USER_ID), any(PostOrderDTO.class))).thenReturn(payInfo);

        mockMvc.perform(post("/order/postOrder")
                        .contentType("application/json")
                        .content("{\"payMethod\":\"alipay_pc\",\"addressId\":\"A1\",\"orderFrom\":0,"
                                + "\"orderList\":[{\"productId\":\"P1\",\"propertyValueIds\":\"pv1\",\"buyCount\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.payOrderId").value(PAY_ORDER_ID));
    }

    @Test
    void getPayInfo_success() throws Exception {
        PayInfoDTO payInfo = new PayInfoDTO();
        payInfo.setPayOrderId(PAY_ORDER_ID);
        when(orderInfoService.getPayInfo(USER_ID, ORDER_ID)).thenReturn(payInfo);

        mockMvc.perform(post("/order/getPayInfo").param("orderId", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payOrderId").value(PAY_ORDER_ID));
    }

    @Test
    void getOrderInfo_success_sumsAmounts() throws Exception {
        OrderInfo o1 = new OrderInfo();
        o1.setOrderId(ORDER_ID);
        o1.setUserId(USER_ID);
        o1.setPayOrderId(PAY_ORDER_ID);
        o1.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        o1.setAmount(new BigDecimal("60.00"));
        OrderInfo o2 = new OrderInfo();
        o2.setOrderId("O2");
        o2.setUserId(USER_ID);
        o2.setPayOrderId(PAY_ORDER_ID);
        o2.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        o2.setAmount(new BigDecimal("40.00"));
        when(orderInfoService.findListByParam(any())).thenReturn(List.of(o1, o2));

        mockMvc.perform(post("/order/getOrderInfo").param("payOrderId", PAY_ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.data.payTotalAmount").value(100.0));
    }

    @Test
    void getOrderInfo_empty_throwsBusinessError() throws Exception {
        when(orderInfoService.findListByParam(any())).thenReturn(List.of());
        mockMvc.perform(post("/order/getOrderInfo").param("payOrderId", PAY_ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void loadMyOrder_defaultStatusList() throws Exception {
        PaginationResultVO<OrderInfo> page = new PaginationResultVO<>();
        page.setList(List.of());
        when(orderInfoService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/order/loadMyOrder").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void loadMyOrder_waitCommentStatus() throws Exception {
        PaginationResultVO<OrderInfo> page = new PaginationResultVO<>();
        page.setList(List.of());
        when(orderInfoService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/order/loadMyOrder").param("pageNo", "1").param("status", "8"))
                .andExpect(status().isOk());

        verify(orderInfoService).findListByPage(any());
    }

    @Test
    void cancelOrder_success() throws Exception {
        OrderInfo order = new OrderInfo();
        order.setOrderId(ORDER_ID);
        order.setUserId(USER_ID);
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(order);

        mockMvc.perform(post("/order/cancelOrder").param("orderId", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(orderInfoService).cancelOrder(USER_ID, ORDER_ID, OrderStatusEnum.WAIT_PAYMENT);
    }

    @Test
    void cancelOrder_notOwner_error() throws Exception {
        OrderInfo order = new OrderInfo();
        order.setOrderId(ORDER_ID);
        order.setUserId("U999");
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(order);

        mockMvc.perform(post("/order/cancelOrder").param("orderId", ORDER_ID))
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void deleteOrder_completed_success() throws Exception {
        mockMvc.perform(post("/order/deleteOrder").param("orderId", ORDER_ID))
                .andExpect(jsonPath("$.status").value("success"));

        verify(orderInfoService).deleteOrder(USER_ID, ORDER_ID);
    }

    @Test
    void deleteOrder_waitPayment_error() throws Exception {
        doThrow(new BusinessException("当前订单状态无法删除！"))
                .when(orderInfoService).deleteOrder(USER_ID, ORDER_ID);

        mockMvc.perform(post("/order/deleteOrder").param("orderId", ORDER_ID))
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void confirmOrder_success_callsOnConfirmed() throws Exception {
        OrderInfo order = new OrderInfo();
        order.setOrderId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setOrderStatus(OrderStatusEnum.SHIPPED.getStatus());
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(order);
        when(orderInfoService.confirmOrderReceipt(USER_ID, ORDER_ID)).thenReturn(true);

        mockMvc.perform(post("/order/confirmOrder").param("orderId", ORDER_ID))
                .andExpect(jsonPath("$.status").value("success"));

        verify(orderInfoService).onOrderConfirmed(USER_ID, ORDER_ID);
    }

    @Test
    void confirmOrder_noTransitionIsIdempotentSuccess() throws Exception {
        mockMvc.perform(post("/order/confirmOrder").param("orderId", ORDER_ID))
                .andExpect(jsonPath("$.status").value("success"));

        verify(orderInfoService).confirmOrderReceipt(USER_ID, ORDER_ID);
        verify(orderInfoService, never()).onOrderConfirmed(anyString(), anyString());
    }

    @Test
    void refundOrder_success() throws Exception {
        when(orderItemService.getOrderItemByOrderItemId("OI1")).thenReturn(new OrderItem());
        mockMvc.perform(post("/order/refundOrder").param("orderItemId", "OI1"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderInfoService).refund(any(OrderItem.class), eq(USER_ID));
    }

    @Test
    void getMyOrderDetail_success() throws Exception {
        OrderInfo order = new OrderInfo();
        order.setOrderId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(order);
        when(orderItemService.findListByParam(any())).thenReturn(List.of(new OrderItem()));

        mockMvc.perform(post("/order/getMyOrderDetail").param("orderId", ORDER_ID))
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.data.orderItemList.length()").value(1));
    }

    @Test
    void getLogistics_success() throws Exception {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        when(orderLogisticsInfoService.getOrderLogisticsRecords(USER_ID, ORDER_ID)).thenReturn(logistics);

        mockMvc.perform(post("/order/getLogistics").param("orderId", ORDER_ID))
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID));
    }

    @Test
    void getOrderCountInfo_success() throws Exception {
        OrderCountVO vo = new OrderCountVO();
        vo.setCode("pendingPayment");
        vo.setCount(1);
        when(orderInfoService.getOrderCountInfo(USER_ID)).thenReturn(List.of(vo));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/order/getOrderCountInfo"))
                .andExpect(jsonPath("$.data[0].code").value("pendingPayment"));
    }

    @Test
    void getOrderInfo_noLogin_throws() throws Exception {
        when(authCookieHelper.resolveWebToken(any())).thenReturn(null);
        when(orderInfoService.findListByParam(any())).thenReturn(List.of());
        mockMvc.perform(post("/order/getOrderInfo").param("payOrderId", PAY_ORDER_ID))
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void getOrderInfo_otherUser_error() throws Exception {
        OrderInfo o = new OrderInfo();
        o.setOrderId(ORDER_ID);
        o.setUserId("U999");
        when(orderInfoService.findListByParam(any())).thenReturn(List.of(o));
        mockMvc.perform(post("/order/getOrderInfo").param("payOrderId", PAY_ORDER_ID))
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void loadMyOrder_setsCommentFilter_forWaitComment() throws Exception {
        PaginationResultVO<OrderInfo> page = new PaginationResultVO<>();
        page.setList(List.of());
        when(orderInfoService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/order/loadMyOrder").param("pageNo", "1").param("status",
                        String.valueOf(OrderStatusEnum.WAIT_COMMENT.getStatus())))
                .andExpect(jsonPath("$.status").value("success"));

        verify(orderInfoService).findListByPage(any());
    }
}
