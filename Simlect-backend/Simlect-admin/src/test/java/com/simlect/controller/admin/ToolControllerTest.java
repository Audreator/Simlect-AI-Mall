package com.simlect.controller.admin;

import com.simlect.api.support.OrderFeignSupport;
import com.simlect.api.support.SearchToolFeignSupport;
import com.simlect.biz.StatisticsInfoService;
import com.simlect.component.RedisComponent;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.service.MqCompensationLogService;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ToolControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private StatisticsInfoService statisticsInfoService;
    @Mock
    private SearchToolFeignSupport searchToolFeignSupport;
    @Mock
    private OrderFeignSupport orderFeignSupport;

    @InjectMocks
    private ToolController toolController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(toolController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void statistics_ok() throws Exception {
        mockMvc.perform(post("/admin/tool/statistics"))
                .andExpect(status().isOk());

        verify(statisticsInfoService).statistics(null, null);
    }

    @Test
    void productData_ok() throws Exception {
        mockMvc.perform(post("/admin/tool/productData"))
                .andExpect(status().isOk());

        verify(searchToolFeignSupport).productData();
    }

    @Test
    void ragData_ok() throws Exception {
        mockMvc.perform(post("/admin/tool/ragData"))
                .andExpect(status().isOk());

        verify(searchToolFeignSupport).ragData();
    }

    @Test
    void addAllOrderToDelayQueue_ok() throws Exception {
        mockMvc.perform(post("/admin/tool/addAllOrderToDelayQueue"))
                .andExpect(status().isOk());

        verify(orderFeignSupport).addAllWaitPayToDelayQueue();
    }
}

@ExtendWith(MockitoExtension.class)
class MqCompensationLogControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private MqCompensationLogService mqCompensationLogService;

    @InjectMocks
    private MqCompensationLogController mqCompensationLogController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mqCompensationLogController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadDataList_appliesDefaultOrderBy() throws Exception {
        when(mqCompensationLogService.findListByPage(any())).thenReturn(null);

        mockMvc.perform(post("/admin/mqCompensationLog/loadDataList").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(mqCompensationLogService).findListByPage(org.mockito.ArgumentMatchers.argThat(q ->
                "create_time desc".equals(q.getOrderBy())));
    }

    @Test
    void getByLogId_ok() throws Exception {
        when(mqCompensationLogService.getByLogId(3)).thenReturn(null);

        mockMvc.perform(post("/admin/mqCompensationLog/getByLogId").param("logId", "3"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_ok() throws Exception {
        mockMvc.perform(post("/admin/mqCompensationLog/updateStatus")
                        .param("logId", "3")
                        .param("status", "2")
                        .param("handleRemark", "已重放"))
                .andExpect(status().isOk());

        verify(mqCompensationLogService).updateHandleStatus(3, 2, "已重放");
    }

    @Test
    void replay_ok() throws Exception {
        mockMvc.perform(post("/admin/mqCompensationLog/replay").param("logId", "3"))
                .andExpect(status().isOk());

        verify(mqCompensationLogService).replay(3);
    }
}

@ExtendWith(MockitoExtension.class)
class AdminAuditInternalControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private com.simlect.biz.AdminAuditLogService adminAuditLogService;

    @InjectMocks
    private com.simlect.controller.internal.AdminAuditInternalController adminAuditInternalController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAuditInternalController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void log_ok() throws Exception {
        String body = "{\"operator\":\"admin\",\"action\":\"BAN_USER\",\"targetUserId\":\"U1\",\"detail\":\"封禁\"}";

        mockMvc.perform(post("/internal/admin/audit/log")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(adminAuditLogService).log("admin", "BAN_USER", "U1", "封禁");
    }

    @Test
    void log_emptyBody_rejectedAsBadRequest() throws Exception {
        // @RequestBody 必填：空请求体触发 HttpMessageNotReadableException → 全局异常处理返回 error
        mockMvc.perform(post("/internal/admin/audit/log")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
