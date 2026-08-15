package com.simlect.controller;

import com.simlect.biz.UserNotificationService;
import com.simlect.component.RedisComponent;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.UserNotification;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserNotificationControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private UserNotificationService userNotificationService;

    @InjectMocks
    private UserNotificationController userNotificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userNotificationController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
    }

    @Test
    void loadNotification_ok() throws Exception {
        when(userNotificationService.loadPage("U1", 1, 0)).thenReturn(null);

        mockMvc.perform(post("/userNotification/loadNotification")
                        .param("pageNo", "1").param("readStatus", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(userNotificationService).loadPage("U1", 1, 0);
    }

    @Test
    void countUnread_ok() throws Exception {
        when(userNotificationService.countUnread("U1")).thenReturn(5);

        mockMvc.perform(get("/userNotification/countUnread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void markRead_ok() throws Exception {
        mockMvc.perform(post("/userNotification/markRead").param("notificationId", "N1"))
                .andExpect(status().isOk());

        verify(userNotificationService).markRead("U1", "N1");
    }

    @Test
    void markAllRead_ok() throws Exception {
        mockMvc.perform(post("/userNotification/markAllRead"))
                .andExpect(status().isOk());

        verify(userNotificationService).markAllRead("U1");
    }

    @Test
    void deleteNotification_ok() throws Exception {
        mockMvc.perform(post("/userNotification/deleteNotification").param("notificationId", "N1"))
                .andExpect(status().isOk());

        verify(userNotificationService).delete("U1", "N1");
    }

    @Test
    void clearAll_ok() throws Exception {
        mockMvc.perform(post("/userNotification/clearAll"))
                .andExpect(status().isOk());

        verify(userNotificationService).clearAll("U1");
    }

    @Test
    void getPopupNotification_ok() throws Exception {
        UserNotification notification = new UserNotification();
        notification.setNotificationId("N1");
        when(userNotificationService.getPopupNotification("U1")).thenReturn(notification);

        mockMvc.perform(get("/userNotification/getPopupNotification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value("N1"));
    }

    @Test
    void clearPopupNotification_ok() throws Exception {
        mockMvc.perform(post("/userNotification/clearPopupNotification").param("notificationId", "N1"))
                .andExpect(status().isOk());

        verify(userNotificationService).clearPopupNotification("U1", "N1");
    }
}
