package com.simlect.controller.admin;

import com.simlect.biz.OrderCommentService;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderLogisticsInfoService;
import com.simlect.entity.po.OrderComment;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderLogisticsInfo;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderInfoService orderInfoService;
    @Mock
    private OrderCommentService orderCommentService;
    @Mock
    private OrderLogisticsInfoService orderLogisticsInfoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        com.simlect.controller.admin.OrderController controller = new com.simlect.controller.admin.OrderController();
        ReflectionTestUtils.setField(controller, "orderInfoService", orderInfoService);
        ReflectionTestUtils.setField(controller, "orderCommentService", orderCommentService);
        ReflectionTestUtils.setField(controller, "orderLogisticsInfoService", orderLogisticsInfoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private PaginationResultVO<OrderInfo> orderPage() {
        OrderInfo order = new OrderInfo();
        order.setOrderId("O1");
        order.setOrderStatus(com.simlect.api.enums.OrderStatusEnum.PAID.getStatus());
        PaginationResultVO<OrderInfo> page = new PaginationResultVO<>();
        page.setList(List.of(order));
        return page;
    }

    @Test
    void loadOrder_success() throws Exception {
        when(orderInfoService.findListByPage(any())).thenReturn(orderPage());

        mockMvc.perform(post("/admin/order/loadOrder").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void loadOrder_withProductNameFuzzy() throws Exception {
        when(orderInfoService.findListByPage(any())).thenReturn(orderPage());
        when(orderInfoService.findByProductNameFuzzy(any(), any())).thenReturn(orderPage());

        mockMvc.perform(post("/admin/order/loadOrder").param("pageNo", "1").param("productNameFuzzy", "苹果"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void loadOrderStatus_returnsAllStatuses() throws Exception {
        mockMvc.perform(post("/admin/order/loadOrderStatus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(com.simlect.api.enums.OrderStatusEnum.values().length));
    }

    @Test
    void loadComment_success() throws Exception {
        PaginationResultVO<OrderComment> page = new PaginationResultVO<>();
        page.setList(List.of());
        when(orderCommentService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/admin/order/loadComment").param("pageNo", "1"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void loadComment_withFuzzy() throws Exception {
        PaginationResultVO<OrderComment> page = new PaginationResultVO<>();
        page.setList(List.of());
        when(orderCommentService.findListByPage(any())).thenReturn(page);
        when(orderCommentService.findByNickNameFuzzy(any(), any())).thenReturn(page);
        when(orderCommentService.findByProductNameFuzzy(any(), any())).thenReturn(page);

        mockMvc.perform(post("/admin/order/loadComment")
                        .param("pageNo", "1")
                        .param("nickNameFuzzy", "小")
                        .param("productNameFuzzy", "苹"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void getComment_success() throws Exception {
        OrderComment comment = new OrderComment();
        comment.setOrderId("O1");
        when(orderCommentService.getComment(null, "O1")).thenReturn(comment);

        mockMvc.perform(post("/admin/order/getComment").param("orderId", "O1"))
                .andExpect(jsonPath("$.data.orderId").value("O1"));
    }

    @Test
    void bizComment_success() throws Exception {
        mockMvc.perform(post("/admin/order/bizComment")
                        .param("orderId", "O1")
                        .param("commentBizReply", "感谢购买"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderCommentService).bizComment("O1", "感谢购买", null);
    }

    @Test
    void delComment_success() throws Exception {
        mockMvc.perform(post("/admin/order/delComment").param("orderId", "O1"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderCommentService).delMyComment(null, "O1");
    }

    @Test
    void getLogistics_success() throws Exception {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId("O1");
        when(orderLogisticsInfoService.getOrderLogisticsRecords(null, "O1")).thenReturn(logistics);

        mockMvc.perform(post("/admin/order/getLogistics").param("orderId", "O1"))
                .andExpect(jsonPath("$.data.orderId").value("O1"));
    }

    @Test
    void delivery_success() throws Exception {
        mockMvc.perform(post("/admin/order/delivery")
                        .param("orderId", "O1")
                        .param("senderAddress", "上海"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderLogisticsInfoService).delivery(any(OrderLogisticsInfo.class));
    }
}
