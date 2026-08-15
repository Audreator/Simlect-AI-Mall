package com.simlect.controller.internal;

import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.biz.OrderCommentService;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderItemService;
import com.simlect.biz.OrderLogisticsInfoService;
import com.simlect.entity.po.OrderComment;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderItem;
import com.simlect.entity.po.OrderLogisticsInfo;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderAgentInternalControllerTest {

    private static final String USER_ID = "U1";
    private static final String ORDER_ID = "O1";

    @Mock
    private OrderInfoService orderInfoService;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private OrderLogisticsInfoService orderLogisticsInfoService;
    @Mock
    private OrderCommentService orderCommentService;

    private MockMvc mockMvc;

    private OrderInfo order() {
        OrderInfo o = new OrderInfo();
        o.setOrderId(ORDER_ID);
        o.setUserId(USER_ID);
        o.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        o.setAmount(new BigDecimal("100.00"));
        o.setPayScene("0");
        o.setPayChannel("alipay");
        o.setPayOrderId("PO1");
        o.setSubject("测试商品");
        o.setCommentStatus(0);
        o.setOrderTime(new Date());
        return o;
    }

    @BeforeEach
    void setUp() {
        OrderAgentInternalController controller = new OrderAgentInternalController();
        ReflectionTestUtils.setField(controller, "orderInfoService", orderInfoService);
        ReflectionTestUtils.setField(controller, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(controller, "orderLogisticsInfoService", orderLogisticsInfoService);
        ReflectionTestUtils.setField(controller, "orderCommentService", orderCommentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listOrders_emptyUserId_returnsEmptyList() throws Exception {
        mockMvc.perform(post("/internal/order/agent/listOrders")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void listOrders_withOrders_andTimeFilter() throws Exception {
        when(orderInfoService.findListByParam(any())).thenReturn(List.of(order()));

        mockMvc.perform(post("/internal/order/agent/listOrders")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\",\"orderId\":\"O1\",\"limit\":10,"
                                + "\"timeStart\":\"2000-01-01 00:00:00\",\"timeEnd\":\"2099-12-31 23:59:59\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.data[0].items.length()").value(0));
    }

    @Test
    void listOrders_outOfTimeRange_filtered() throws Exception {
        when(orderInfoService.findListByParam(any())).thenReturn(List.of(order()));
        mockMvc.perform(post("/internal/order/agent/listOrders")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\",\"timeStart\":\"2020-01-01 00:00:00\",\"timeEnd\":\"2020-12-31 23:59:59\"}"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getOrder_emptyOrderId_returnsNull() throws Exception {
        mockMvc.perform(post("/internal/order/agent/getOrder")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getOrder_success_withItems() throws Exception {
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(order());
        OrderItem item = new OrderItem();
        item.setOrderItemId(ORDER_ID + "_1");
        item.setOrderId(ORDER_ID);
        item.setProductId("P1");
        item.setProductName("测试商品");
        item.setBuyCount(2);
        when(orderItemService.findListByParam(any())).thenReturn(List.of(item));

        mockMvc.perform(post("/internal/order/agent/getOrder")
                        .contentType("application/json")
                        .content("{\"orderId\":\"O1\"}"))
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void getOrder_notExists_returnsNull() throws Exception {
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(null);
        mockMvc.perform(post("/internal/order/agent/getOrder")
                        .contentType("application/json")
                        .content("{\"orderId\":\"O1\"}"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getOrderItem_emptyId_returnsNull() throws Exception {
        mockMvc.perform(post("/internal/order/agent/getOrderItem")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getOrderItem_success() throws Exception {
        OrderItem item = new OrderItem();
        item.setOrderItemId("OI1");
        when(orderItemService.getOrderItemByOrderItemId("OI1")).thenReturn(item);

        mockMvc.perform(post("/internal/order/agent/getOrderItem")
                        .contentType("application/json")
                        .content("{\"orderItemId\":\"OI1\"}"))
                .andExpect(jsonPath("$.data.orderItemId").value("OI1"));
    }

    @Test
    void listOrderItems_emptyOrderId_returnsEmpty() throws Exception {
        mockMvc.perform(post("/internal/order/agent/listOrderItems")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void listOrderItems_success() throws Exception {
        OrderItem item = new OrderItem();
        item.setOrderItemId("OI1");
        when(orderItemService.findListByParam(any())).thenReturn(List.of(item));

        mockMvc.perform(post("/internal/order/agent/listOrderItems")
                        .contentType("application/json")
                        .content("{\"orderId\":\"O1\"}"))
                .andExpect(jsonPath("$.data[0].orderItemId").value("OI1"));
    }

    @Test
    void getLogistics_missingParams_returnsNull() throws Exception {
        mockMvc.perform(post("/internal/order/agent/getLogistics")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\"}"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getLogistics_success() throws Exception {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        logistics.setUserId(USER_ID);
        logistics.setLogisticsNo("SF123");
        when(orderLogisticsInfoService.getOrderLogisticsRecords(USER_ID, ORDER_ID)).thenReturn(logistics);

        mockMvc.perform(post("/internal/order/agent/getLogistics")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\",\"orderId\":\"O1\"}"))
                .andExpect(jsonPath("$.data.logisticsNo").value("SF123"));
    }

    @Test
    void getLogistics_serviceThrows_returnsNull() throws Exception {
        when(orderLogisticsInfoService.getOrderLogisticsRecords(USER_ID, ORDER_ID))
                .thenThrow(new RuntimeException("no"));
        mockMvc.perform(post("/internal/order/agent/getLogistics")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\",\"orderId\":\"O1\"}"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getComment_missingParams_returnsNull() throws Exception {
        mockMvc.perform(post("/internal/order/agent/getComment")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getComment_success() throws Exception {
        OrderComment comment = new OrderComment();
        comment.setOrderId(ORDER_ID);
        comment.setUserId(USER_ID);
        comment.setProductId("P1");
        comment.setCommentContent("很好");
        comment.setStar(5);
        comment.setCommentTime(new Date());
        when(orderCommentService.findListByParam(any())).thenReturn(List.of(comment));

        mockMvc.perform(post("/internal/order/agent/getComment")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\",\"orderId\":\"O1\"}"))
                .andExpect(jsonPath("$.data.commentContent").value("很好"));
    }

    @Test
    void getComment_noComment_returnsNull() throws Exception {
        when(orderCommentService.findListByParam(any())).thenReturn(List.of());
        mockMvc.perform(post("/internal/order/agent/getComment")
                        .contentType("application/json")
                        .content("{\"userId\":\"U1\",\"orderId\":\"O1\"}"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }
}
