package com.simlect.controller;

import com.simlect.biz.UserMemberProfileService;
import com.simlect.component.RedisComponent;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.UserMemberProfile;
import com.simlect.entity.vo.MemberCenterVO;
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
class UserMemberControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private UserMemberProfileService userMemberProfileService;

    @InjectMocks
    private UserMemberController userMemberController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userMemberController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
        lenient().when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        lenient().when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
    }

    @Test
    void getProfile_returnsProfile() throws Exception {
        UserMemberProfile profile = new UserMemberProfile();
        profile.setUserId("U1");
        profile.setLevelCode(1);
        when(userMemberProfileService.getOrInitProfile("U1")).thenReturn(profile);

        mockMvc.perform(get("/userMember/getProfile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.levelCode").value(1));
    }

    @Test
    void getProfile_centerRequest_returnsCenter() throws Exception {
        when(userMemberProfileService.getMemberCenter("U1")).thenReturn(new MemberCenterVO());

        mockMvc.perform(get("/userMember/getProfile").param("center", "true"))
                .andExpect(status().isOk());

        verify(userMemberProfileService).getMemberCenter("U1");
    }

    @Test
    void getMemberCenter_ok() throws Exception {
        when(userMemberProfileService.getMemberCenter("U1")).thenReturn(new MemberCenterVO());

        mockMvc.perform(get("/userMember/getMemberCenter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void claimLevelReward_ok() throws Exception {
        doNothing().when(userMemberProfileService).claimLevelReward("U1", 2);
        when(userMemberProfileService.getMemberCenter("U1")).thenReturn(new MemberCenterVO());

        mockMvc.perform(post("/userMember/claimLevelReward").param("levelCode", "2"))
                .andExpect(status().isOk());

        verify(userMemberProfileService).claimLevelReward("U1", 2);
        verify(userMemberProfileService).getMemberCenter("U1");
    }

    @Test
    void getLevelBadge_ok() throws Exception {
        UserMemberProfile profile = new UserMemberProfile();
        profile.setLevelCode(3);
        profile.setLevelName("金卡会员");
        when(userMemberProfileService.getOrInitProfile("U1")).thenReturn(profile);

        mockMvc.perform(get("/userMember/getLevelBadge").param("userId", "U1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.levelName").value("金卡会员"));
    }
}
