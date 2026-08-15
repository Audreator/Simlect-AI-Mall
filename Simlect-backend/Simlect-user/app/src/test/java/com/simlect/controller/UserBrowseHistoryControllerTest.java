package com.simlect.controller;

import com.simlect.biz.UserBrowseHistoryService;
import com.simlect.component.RedisComponent;
import com.simlect.controller.AGlobalExceptionHandlerController;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserBrowseHistoryControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private UserBrowseHistoryService userBrowseHistoryService;

    @InjectMocks
    private UserBrowseHistoryController userBrowseHistoryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userBrowseHistoryController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
    }

    @Test
    void loadBrowse_ok() throws Exception {
        when(userBrowseHistoryService.loadBrowsePage("U1", 1)).thenReturn(null);

        mockMvc.perform(post("/browseHistory/loadBrowse").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(userBrowseHistoryService).loadBrowsePage("U1", 1);
    }

    @Test
    void clearBrowse_ok() throws Exception {
        mockMvc.perform(post("/browseHistory/clearBrowse"))
                .andExpect(status().isOk());

        verify(userBrowseHistoryService).clearBrowse("U1");
    }

    @Test
    void removeBrowse_ok() throws Exception {
        mockMvc.perform(post("/browseHistory/removeBrowse").param("historyId", "9"))
                .andExpect(status().isOk());

        verify(userBrowseHistoryService).removeBrowse("U1", 9L);
    }
}
