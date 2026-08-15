package com.simlect.controller;

import com.simlect.biz.SignService;
import com.simlect.api.vo.SignDataVO;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SignControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private SignService signService;

    @InjectMocks
    private SignController signController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(signController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
        lenient().when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        lenient().when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
    }

    @Test
    void getSignCalendar_returnsData() throws Exception {
        SignDataVO vo = new SignDataVO();
        vo.setContinuousDays(3);
        vo.setSignDays(List.of("20260801"));
        when(signService.getSignCalendar("U1", "202608")).thenReturn(vo);

        mockMvc.perform(post("/sign/getSignCalendar").param("yearMonth", "202608"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.continuousDays").value(3));

        verify(signService).getSignCalendar("U1", "202608");
    }

    @Test
    void sign_ok() throws Exception {
        doNothing().when(signService).sign("U1");

        mockMvc.perform(post("/sign/sign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(signService).sign("U1");
    }

    @Test
    void msign_ok() throws Exception {
        doNothing().when(signService).msign("U1", "20260801");

        mockMvc.perform(post("/sign/msign").param("date", "20260801"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(signService).msign("U1", "20260801");
    }

    @Test
    void msign_missingDate_rejectedByValidation() throws Exception {
        mockMvc.perform(post("/sign/msign"))
                .andExpect(status().isOk());

        verify(signService, never()).msign(anyString(), anyString());
    }
}
