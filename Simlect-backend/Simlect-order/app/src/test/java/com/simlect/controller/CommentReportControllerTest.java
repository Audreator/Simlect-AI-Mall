package com.simlect.controller;

import com.simlect.biz.CommentReportService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.CommentReport;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentReportControllerTest {

    private static final String USER_ID = "U1";

    @Mock
    private CommentReportService commentReportService;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CommentReportController controller = new CommentReportController();
        ReflectionTestUtils.setField(controller, "commentReportService", commentReportService);
        ReflectionTestUtils.setField(controller, "redisComponent", redisComponent);
        ReflectionTestUtils.setField(controller, "authCookieHelper", authCookieHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(authCookieHelper.resolveWebToken(any())).thenReturn("token");
        TokenUserInfoDTO tokenUser = new TokenUserInfoDTO();
        tokenUser.setUserId(USER_ID);
        when(redisComponent.getTokenUserInfo("token")).thenReturn(tokenUser);
    }

    @Test
    void submitReport_success() throws Exception {
        CommentReport report = new CommentReport();
        report.setOrderId("O1");
        when(commentReportService.submitReport(eq(USER_ID), eq("O1"), eq("P1"), eq("广告"), any(), any()))
                .thenReturn(report);

        mockMvc.perform(post("/commentReport/submitReport")
                        .param("orderId", "O1")
                        .param("productId", "P1")
                        .param("reason", "广告")
                        .param("detail", "虚假宣传"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.orderId").value("O1"));

        verify(commentReportService).submitReport(eq(USER_ID), eq("O1"), eq("P1"), eq("广告"),
                eq("虚假宣传"), any());
    }
}
