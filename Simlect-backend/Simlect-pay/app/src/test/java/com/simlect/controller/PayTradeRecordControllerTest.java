package com.simlect.controller;

import com.simlect.biz.PayTradeRecordService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.PayTradeRecord;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PayTradeRecordController 我的交易记录接口单元测试（MockMvc standalone）。
 */
@ExtendWith(MockitoExtension.class)
class PayTradeRecordControllerTest {

    @Mock
    private PayTradeRecordService payTradeRecordService;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;

    private MockMvc mockMvc;

    private static final String TOKEN = "token-pay";
    private static final String USER_ID = "U001";

    @BeforeEach
    void setUp() {
        PayTradeRecordController controller = new PayTradeRecordController();
        ReflectionTestUtils.setField(controller, "payTradeRecordService", payTradeRecordService);
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
    void loadMyTrades_withLogin_usesUserIdAndPageNo() throws Exception {
        mockLogin();
        PaginationResultVO<PayTradeRecord> page = new PaginationResultVO<>(
                1, 15, 1, 1, List.of(new PayTradeRecord()));
        when(payTradeRecordService.loadUserTrades(USER_ID, 2)).thenReturn(page);

        mockMvc.perform(post("/payTrade/loadMyTrades").param("pageNo", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        verify(payTradeRecordService).loadUserTrades(USER_ID, 2);
    }

    @Test
    void loadMyTrades_withoutPageNo_defaultsToOne() throws Exception {
        mockLogin();
        PaginationResultVO<PayTradeRecord> page = new PaginationResultVO<>(
                0, 15, 1, 0, List.of());
        when(payTradeRecordService.loadUserTrades(USER_ID, 1)).thenReturn(page);

        mockMvc.perform(post("/payTrade/loadMyTrades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(payTradeRecordService).loadUserTrades(USER_ID, 1);
    }

    @Test
    void loadMyTrades_withoutLogin_throws() {
        doReturn(null).when(authCookieHelper).resolveWebToken(any());

        assertThrows(jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(post("/payTrade/loadMyTrades")));
    }
}
