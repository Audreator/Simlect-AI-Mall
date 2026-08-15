package com.simlect.controller.admin;

import com.simlect.biz.CommentReportService;
import com.simlect.biz.OrderCommentService;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderItemService;
import com.simlect.biz.OrderLogisticsInfoRecordService;
import com.simlect.biz.OrderLogisticsInfoService;
import com.simlect.entity.po.CommentReport;
import com.simlect.entity.po.OrderComment;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderItem;
import com.simlect.entity.po.OrderLogisticsInfo;
import com.simlect.entity.po.OrderLogisticsInfoRecord;
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

/**
 * 五个 admin 通用 CRUD 控制器（OrderInfo/OrderItem/OrderLogisticsInfo/
 * OrderLogisticsInfoRecord/OrderComment/CommentReport）的 standalone 覆盖。
 */
@ExtendWith(MockitoExtension.class)
class AdminCrudControllersTest {

    @Mock
    private OrderInfoService orderInfoService;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private OrderLogisticsInfoService orderLogisticsInfoService;
    @Mock
    private OrderLogisticsInfoRecordService orderLogisticsInfoRecordService;
    @Mock
    private OrderCommentService orderCommentService;
    @Mock
    private CommentReportService commentReportService;

    private MockMvc orderInfoMvc;
    private MockMvc orderItemMvc;
    private MockMvc logisticsMvc;
    private MockMvc recordMvc;
    private MockMvc commentMvc;
    private MockMvc reportMvc;

    @BeforeEach
    void setUp() {
        OrderInfoController infoController = new OrderInfoController();
        ReflectionTestUtils.setField(infoController, "orderInfoService", orderInfoService);
        orderInfoMvc = MockMvcBuilders.standaloneSetup(infoController).build();

        OrderItemController itemController = new OrderItemController();
        ReflectionTestUtils.setField(itemController, "orderItemService", orderItemService);
        orderItemMvc = MockMvcBuilders.standaloneSetup(itemController).build();

        OrderLogisticsInfoController logisticsController = new OrderLogisticsInfoController();
        ReflectionTestUtils.setField(logisticsController, "orderLogisticsInfoService", orderLogisticsInfoService);
        logisticsMvc = MockMvcBuilders.standaloneSetup(logisticsController).build();

        OrderLogisticsInfoRecordController recordController = new OrderLogisticsInfoRecordController();
        ReflectionTestUtils.setField(recordController, "orderLogisticsInfoRecordService", orderLogisticsInfoRecordService);
        recordMvc = MockMvcBuilders.standaloneSetup(recordController).build();

        OrderCommentController commentController = new OrderCommentController();
        ReflectionTestUtils.setField(commentController, "orderCommentService", orderCommentService);
        commentMvc = MockMvcBuilders.standaloneSetup(commentController).build();

        com.simlect.controller.admin.CommentReportController reportController =
                new com.simlect.controller.admin.CommentReportController();
        ReflectionTestUtils.setField(reportController, "commentReportService", commentReportService);
        reportMvc = MockMvcBuilders.standaloneSetup(reportController).build();
    }

    @Test
    void orderInfo_loadDataList() throws Exception {
        OrderInfo order = new OrderInfo();
        order.setOrderId("O1");
        order.setOrderStatus(com.simlect.api.enums.OrderStatusEnum.PAID.getStatus());
        PaginationResultVO<OrderInfo> page = new PaginationResultVO<>();
        page.setList(List.of(order));
        when(orderInfoService.findListByPage(any())).thenReturn(page);

        orderInfoMvc.perform(post("/admin/orderInfo/loadDataList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void orderInfo_getByOrderId() throws Exception {
        OrderInfo order = new OrderInfo();
        order.setOrderId("O1");
        order.setOrderStatus(com.simlect.api.enums.OrderStatusEnum.PAID.getStatus());
        when(orderInfoService.getOrderInfoByOrderId("O1")).thenReturn(order);

        orderInfoMvc.perform(post("/admin/orderInfo/getOrderInfoByOrderId").param("orderId", "O1"))
                .andExpect(jsonPath("$.data.orderId").value("O1"));
    }

    @Test
    void orderInfo_addAndBatch() throws Exception {
        orderInfoMvc.perform(post("/admin/orderInfo/add").param("orderId", "O1"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderInfoService).add(any(OrderInfo.class));

        orderInfoMvc.perform(post("/admin/orderInfo/addBatch")
                        .contentType("application/json")
                        .content("[{\"orderId\":\"O1\"}]"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderInfoService).addBatch(any());
    }

    @Test
    void orderInfo_updateAndDelete() throws Exception {
        orderInfoMvc.perform(post("/admin/orderInfo/updateOrderInfoByOrderId")
                        .param("orderId", "O1").param("userId", "U1"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderInfoService).updateOrderInfoByOrderId(any(OrderInfo.class), org.mockito.ArgumentMatchers.eq("O1"));

        orderInfoMvc.perform(post("/admin/orderInfo/deleteOrderInfoByOrderId").param("orderId", "O1"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderInfoService).deleteOrderInfoByOrderId("O1");
    }

    @Test
    void orderItem_loadDataListAndGet() throws Exception {
        PaginationResultVO<OrderItem> page = new PaginationResultVO<>();
        page.setList(List.of(new OrderItem()));
        when(orderItemService.findListByPage(any())).thenReturn(page);

        orderItemMvc.perform(post("/admin/orderItem/loadDataList"))
                .andExpect(jsonPath("$.status").value("success"));

        OrderItem item = new OrderItem();
        item.setOrderItemId("OI1");
        when(orderItemService.getOrderItemByOrderItemId("OI1")).thenReturn(item);
        orderItemMvc.perform(post("/admin/orderItem/getOrderItemByOrderItemId").param("orderItemId", "OI1"))
                .andExpect(jsonPath("$.data.orderItemId").value("OI1"));
    }

    @Test
    void orderItem_addBatch() throws Exception {
        orderItemMvc.perform(post("/admin/orderItem/addBatch")
                        .contentType("application/json")
                        .content("[{\"orderItemId\":\"OI1\"}]"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderItemService).addBatch(any());
    }

    @Test
    void logistics_loadDataList() throws Exception {
        PaginationResultVO<OrderLogisticsInfo> page = new PaginationResultVO<>();
        page.setList(List.of(new OrderLogisticsInfo()));
        when(orderLogisticsInfoService.findListByPage(any())).thenReturn(page);

        logisticsMvc.perform(post("/admin/orderLogisticsInfo/loadDataList"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void logistics_getByOrderId() throws Exception {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId("O1");
        when(orderLogisticsInfoService.getOrderLogisticsInfoByOrderId("O1")).thenReturn(logistics);

        logisticsMvc.perform(post("/admin/orderLogisticsInfo/getOrderLogisticsInfoByOrderId").param("orderId", "O1"))
                .andExpect(jsonPath("$.data.orderId").value("O1"));
    }

    @Test
    void record_loadDataList() throws Exception {
        PaginationResultVO<OrderLogisticsInfoRecord> page = new PaginationResultVO<>();
        page.setList(List.of(new OrderLogisticsInfoRecord()));
        when(orderLogisticsInfoRecordService.findListByPage(any())).thenReturn(page);

        recordMvc.perform(post("/admin/orderLogisticsInfoRecord/loadDataList"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void record_getByRecordId() throws Exception {
        OrderLogisticsInfoRecord record = new OrderLogisticsInfoRecord();
        record.setRecordId(1);
        when(orderLogisticsInfoRecordService.getOrderLogisticsInfoRecordByRecordId(1)).thenReturn(record);

        recordMvc.perform(post("/admin/orderLogisticsInfoRecord/getOrderLogisticsInfoRecordByRecordId")
                        .param("recordId", "1"))
                .andExpect(jsonPath("$.data.recordId").value(1));
    }

    @Test
    void comment_loadDataListAndGet() throws Exception {
        PaginationResultVO<OrderComment> page = new PaginationResultVO<>();
        page.setList(List.of(new OrderComment()));
        when(orderCommentService.findListByPage(any())).thenReturn(page);

        commentMvc.perform(post("/admin/orderComment/loadDataList"))
                .andExpect(jsonPath("$.status").value("success"));

        OrderComment comment = new OrderComment();
        comment.setOrderId("O1");
        when(orderCommentService.getOrderCommentByOrderId("O1")).thenReturn(comment);
        commentMvc.perform(post("/admin/orderComment/getOrderCommentByOrderId").param("orderId", "O1"))
                .andExpect(jsonPath("$.data.orderId").value("O1"));
    }

    @Test
    void comment_updateByOrderId() throws Exception {
        commentMvc.perform(post("/admin/orderComment/updateOrderCommentByOrderId")
                        .param("orderId", "O1").param("star", "5"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderCommentService).updateOrderCommentByOrderId(any(OrderComment.class),
                org.mockito.ArgumentMatchers.eq("O1"));
    }

    @Test
    void report_loadDataList_setsDefaultOrderBy() throws Exception {
        PaginationResultVO<CommentReport> page = new PaginationResultVO<>();
        page.setList(List.of(new CommentReport()));
        when(commentReportService.findListByPage(any())).thenReturn(page);

        reportMvc.perform(post("/admin/commentReport/loadDataList"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void report_getAndHandleAndDelete() throws Exception {
        CommentReport report = new CommentReport();
        report.setReportId(1);
        when(commentReportService.getByReportId(1)).thenReturn(report);

        reportMvc.perform(post("/admin/commentReport/getCommentReportByReportId").param("reportId", "1"))
                .andExpect(jsonPath("$.data.reportId").value(1));

        reportMvc.perform(post("/admin/commentReport/handleReport")
                        .param("reportId", "1").param("status", "2").param("handleRemark", "删除"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(commentReportService).handleReport(1, 2, "删除");

        reportMvc.perform(post("/admin/commentReport/deleteCommentReportByReportId").param("reportId", "1"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(commentReportService).deleteByReportId(1);
    }
}
