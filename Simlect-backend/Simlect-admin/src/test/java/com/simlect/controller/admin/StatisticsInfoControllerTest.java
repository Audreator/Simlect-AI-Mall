package com.simlect.controller.admin;

import com.simlect.api.support.ProductFeignSupport;
import com.simlect.api.support.SearchToolFeignSupport;
import com.simlect.biz.StatisticsInfoService;
import com.simlect.biz.impl.StatisticsInfoServiceImpl;
import com.simlect.component.RedisComponent;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.dto.LogisticsSendDTO;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.StatisticsDataVO;
import com.simlect.entity.vo.TodayDataVO;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StatisticsInfoControllerTest {

    @Mock
    private StatisticsInfoService statisticsInfoService;

    @InjectMocks
    private StatisticsInfoController statisticsInfoController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(statisticsInfoController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadDataList_ok() throws Exception {
        when(statisticsInfoService.findListByPage(any())).thenReturn(null);

        mockMvc.perform(post("/admin/statisticsInfo/loadDataList").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(statisticsInfoService).findListByPage(org.mockito.ArgumentMatchers.argThat(q ->
                "statistics_date desc, data_type asc".equals(q.getOrderBy())));
    }
}

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private StatisticsInfoServiceImpl statisticsInfoService;
    @Mock
    private ProductFeignSupport productFeignSupport;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(homeController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadLessStockProduct_ok() throws Exception {
        when(productFeignSupport.lessStockSkuPage(1, 15, 10)).thenReturn(null);

        mockMvc.perform(post("/admin/home/loadLessStockProduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productFeignSupport).lessStockSkuPage(1, 15, 10);
    }

    @Test
    void getTodayData_ok() throws Exception {
        when(statisticsInfoService.getTodayData())
                .thenReturn(List.of(new TodayDataVO("orderAmount", BigDecimal.ONE, BigDecimal.ZERO)));

        mockMvc.perform(post("/admin/home/getTodayData"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("orderAmount"));
    }

    @Test
    void getTodayData_degradeOnError() throws Exception {
        when(statisticsInfoService.getTodayData()).thenThrow(new RuntimeException("feign down"));

        mockMvc.perform(post("/admin/home/getTodayData"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].todayValue").value(0));
    }

    @Test
    void loadWeeklyStatisticsData_degradeOnError() throws Exception {
        when(statisticsInfoService.loadWeeklyStatisticsData())
                .thenThrow(new RuntimeException("feign down"));

        mockMvc.perform(post("/admin/home/loadWeeklyStatisticsData"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}

@ExtendWith(MockitoExtension.class)
class AgentMessageControllerTest {

    @Mock
    private com.simlect.biz.AgentMessageService agentMessageService;

    @InjectMocks
    private AgentMessageController agentMessageController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(agentMessageController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadDataList_ok() throws Exception {
        when(agentMessageService.findListByPage(any())).thenReturn(null);

        mockMvc.perform(post("/admin/agentMessage/loadDataList").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void deleteByMessageId_ok() throws Exception {
        mockMvc.perform(post("/admin/agentMessage/deleteAgentMessageByMessageId").param("messageId", "3"))
                .andExpect(status().isOk());

        verify(agentMessageService).deleteAgentMessageByMessageId(3);
    }
}

@ExtendWith(MockitoExtension.class)
class SensitiveWordControllerTest {

    @Mock
    private com.simlect.biz.SensitiveWordService sensitiveWordService;

    @InjectMocks
    private SensitiveWordController sensitiveWordController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sensitiveWordController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void list_ok() throws Exception {
        when(sensitiveWordService.findListByPage(any())).thenReturn(null);

        mockMvc.perform(post("/admin/sensitiveWord/list").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void save_ok() throws Exception {
        mockMvc.perform(post("/admin/sensitiveWord/save")
                        .param("word", "广告")
                        .param("replaceWord", "***")
                        .param("status", "1"))
                .andExpect(status().isOk());

        verify(sensitiveWordService).save(null, "广告", "***", 1);
    }

    @Test
    void delete_ok() throws Exception {
        mockMvc.perform(post("/admin/sensitiveWord/delete").param("id", "9"))
                .andExpect(status().isOk());

        verify(sensitiveWordService).delete(9L);
    }

    @Test
    void refresh_ok() throws Exception {
        mockMvc.perform(post("/admin/sensitiveWord/refresh"))
                .andExpect(status().isOk());

        verify(sensitiveWordService).refreshCache();
    }
}

@ExtendWith(MockitoExtension.class)
class SettingControllerTest {

    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private SettingController settingController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(settingController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void saveLogistics_ok() throws Exception {
        mockMvc.perform(post("/admin/setting/saveLogistics")
                        .param("province", "广东省")
                        .param("city", "深圳市"))
                .andExpect(status().isOk());

        verify(redisComponent).saveLogistics(any(LogisticsSendDTO.class));
    }

    @Test
    void getLogistics_ok() throws Exception {
        mockMvc.perform(post("/admin/setting/getLogistics"))
                .andExpect(status().isOk());

        verify(redisComponent).getLogisticsInfo();
    }

    @Test
    void loadPromptList_ok() throws Exception {
        mockMvc.perform(post("/admin/setting/loadPromptList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void getPromptDetail_redisHit_returnsCachedPrompt() throws Exception {
        when(redisComponent.getPrompt("chat")).thenReturn("自定义提示词");

        mockMvc.perform(post("/admin/setting/getPromptDetail").param("key", "chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("自定义提示词"));
    }

    @Test
    void getPromptDetail_emptyRedis_fallsBackToEnum() throws Exception {
        when(redisComponent.getPrompt("chat")).thenReturn("");

        mockMvc.perform(post("/admin/setting/getPromptDetail").param("key", "chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void savePrompt_ok() throws Exception {
        mockMvc.perform(post("/admin/setting/savePrompt")
                        .param("key", "ORDER_SERVICE")
                        .param("prompt", "请回答"))
                .andExpect(status().isOk());

        verify(redisComponent).savePrompt("ORDER_SERVICE", "请回答");
    }

    @Test
    void cleanPromptCache_ok() throws Exception {
        mockMvc.perform(post("/admin/setting/cleanPromptCache").param("key", "ORDER_SERVICE"))
                .andExpect(status().isOk());

        verify(redisComponent).cleanPrompt("ORDER_SERVICE");
    }
}
