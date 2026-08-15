package com.simlect.controller;

import com.simlect.biz.impl.AliEmailServiceImpl;
import com.simlect.biz.impl.UserInfoServiceImpl;
import com.simlect.component.RedisComponent;
import com.simlect.component.UserLoginLockService;
import com.simlect.component.UserTempBanService;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.UserInfo;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.exception.BusinessException;
import com.simlect.service.PasswordService;
import com.simlect.service.SlideCaptchaVerifier;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private UserInfoServiceImpl userInfoService;
    @Mock
    private AliEmailServiceImpl aliEmailServiceImpl;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private UserTempBanService userTempBanService;
    @Mock
    private UserLoginLockService userLoginLockService;
    @Mock
    private SlideCaptchaVerifier slideCaptchaVerifier;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setUserId("U1");
        user.setEmail("a@b.com");
        user.setNickName("nick");
        user.setPassword("enc");
        user.setStatus(1);
        return user;
    }

    @Test
    void autoLogin_noToken_returnsNull() throws Exception {
        when(authCookieHelper.resolveWebToken(any())).thenReturn(null);

        mockMvc.perform(get("/account/autoLogin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void autoLogin_validToken_refreshesToken() throws Exception {
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
        when(userInfoService.getUserInfoByUserId("U1")).thenReturn(buildUser());
        when(redisComponent.saveTokenUserInfo(tokenInfo)).thenReturn("new-tok");

        MvcResult result = mockMvc.perform(get("/account/autoLogin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("\"userId\":\"U1\""));
        verify(authCookieHelper).writeWebTokenCookie(any(), any(), eq("new-tok"));
    }

    @Test
    void autoLogin_disabledUser_returnsNull() throws Exception {
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
        UserInfo user = buildUser();
        user.setStatus(0);
        when(userInfoService.getUserInfoByUserId("U1")).thenReturn(user);

        mockMvc.perform(get("/account/autoLogin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(redisComponent, never()).saveTokenUserInfo(any());
    }

    @Test
    void checkCode_returnsCaptcha() throws Exception {
        when(redisComponent.saveCheckCode(anyString())).thenReturn("code-key");

        mockMvc.perform(get("/account/checkCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.checkCodeKey").value("code-key"));
    }

    @Test
    void register_success() throws Exception {
        doNothing().when(userInfoService).register(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/account/register")
                        .param("email", "a@b.com")
                        .param("nickName", "nick")
                        .param("registerPassword", "Passw0rd1")
                        .param("checkCode", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(userInfoService).register("a@b.com", "nick", "Passw0rd1", "1234");
    }

    @Test
    void login_success_returnsUserAndSetsCookie() throws Exception {
        when(redisComponent.getCheckCode("key")).thenReturn("1234");
        when(userInfoService.getUserInfoByEmail("a@b.com")).thenReturn(buildUser());
        when(passwordService.matches("pass", "enc")).thenReturn(true);
        when(passwordService.isBcrypt("enc")).thenReturn(true);
        when(redisComponent.saveTokenUserInfo(any())).thenReturn("tok");

        mockMvc.perform(post("/account/login")
                        .param("email", "a@b.com")
                        .param("password", "pass")
                        .param("checkCodeKey", "key")
                        .param("checkCode", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").value("U1"));

        verify(authCookieHelper).writeWebTokenCookie(any(), any(), eq("tok"));
        verify(redisComponent).cleanCheckCode("key");
        verify(userInfoService).updateByParam(any(), any());
    }

    @Test
    void login_wrongCheckCode_fails() throws Exception {
        when(redisComponent.getCheckCode("key")).thenReturn("9999");

        mockMvc.perform(post("/account/login")
                        .param("email", "a@b.com")
                        .param("password", "pass")
                        .param("checkCodeKey", "key")
                        .param("checkCode", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.info").value("验证码错误！"));

        verify(redisComponent).cleanCheckCode("key");
        verify(userInfoService, never()).getUserInfoByEmail(anyString());
    }

    @Test
    void login_wrongPassword_fails() throws Exception {
        when(redisComponent.getCheckCode("key")).thenReturn("1234");
        when(userInfoService.getUserInfoByEmail("a@b.com")).thenReturn(buildUser());
        when(passwordService.matches("bad", "enc")).thenReturn(false);

        mockMvc.perform(post("/account/login")
                        .param("email", "a@b.com")
                        .param("password", "bad")
                        .param("checkCodeKey", "key")
                        .param("checkCode", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.info").value("账号或密码错误！"));
    }

    @Test
    void login_disabledUser_throwsTempBan() throws Exception {
        when(redisComponent.getCheckCode("key")).thenReturn("1234");
        UserInfo user = buildUser();
        user.setStatus(0);
        when(userInfoService.getUserInfoByEmail("a@b.com")).thenReturn(user);
        when(passwordService.matches("pass", "enc")).thenReturn(true);
        when(passwordService.isBcrypt("enc")).thenReturn(true);
        when(userTempBanService.getUnbanAtMs("U1")).thenReturn(1000L);
        when(userTempBanService.buildTempBanMessage(1000L)).thenReturn("账号因违规被临时封禁");

        mockMvc.perform(post("/account/login")
                        .param("email", "a@b.com")
                        .param("password", "pass")
                        .param("checkCodeKey", "key")
                        .param("checkCode", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.data.errorType").value("ACCOUNT_TEMP_BANNED"));
    }

    @Test
    void login_legacyPassword_upgradedToBcrypt() throws Exception {
        when(redisComponent.getCheckCode("key")).thenReturn("1234");
        when(userInfoService.getUserInfoByEmail("a@b.com")).thenReturn(buildUser());
        when(passwordService.matches("pass", "enc")).thenReturn(true);
        when(passwordService.isBcrypt("enc")).thenReturn(false);
        when(passwordService.encode("pass")).thenReturn("enc-bcrypt");
        when(redisComponent.saveTokenUserInfo(any())).thenReturn("tok");

        mockMvc.perform(post("/account/login")
                        .param("email", "a@b.com")
                        .param("password", "pass")
                        .param("checkCodeKey", "key")
                        .param("checkCode", "1234"))
                .andExpect(status().isOk());

        verify(userInfoService).updateByParam(argThat(u -> "enc-bcrypt".equals(u.getPassword())), any());
    }

    @Test
    void logout_cleansTokenAndCookie() throws Exception {
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");

        mockMvc.perform(post("/account/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(redisComponent).cleanTokenUserInfo("tok");
        verify(authCookieHelper).clearWebTokenCookie(any(), any());
    }

    @Test
    void getUserInfo_returnsProfile() throws Exception {
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
        when(userInfoService.getUserInfoByUserId("U1")).thenReturn(buildUser());

        mockMvc.perform(get("/account/getUserInfo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("a@b.com"));
    }

    @Test
    void updateUserInfo_ok() throws Exception {
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);

        mockMvc.perform(post("/account/updateUserInfo")
                        .param("avatar", "a.png")
                        .param("nickName", "新昵称")
                        .param("sex", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(userInfoService).updateUserInfo(eq("U1"), eq("a.png"), eq("新昵称"), eq(1));
    }

    @Test
    void updatePassword_ok() throws Exception {
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);

        mockMvc.perform(post("/account/updatePassword")
                        .param("oldPassword", "old")
                        .param("password", "new"))
                .andExpect(status().isOk());

        verify(userInfoService).updatePassword("U1", "old", "new");
    }

    @Test
    void forgetPassword_ok() throws Exception {
        doNothing().when(userInfoService).forgetPassword(anyString(), anyString(), anyString());

        mockMvc.perform(post("/account/forgetPassword")
                        .param("email", "a@b.com")
                        .param("newPassword", "NewPassw0rd1")
                        .param("checkCode", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    private void stubEmailRateLimitFirstHit() {
        // email 维度限流：首次计数返回 1（放行）
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
    }

    @Test
    void getEmailCode_resendsThrottled() throws Exception {
        stubEmailRateLimitFirstHit();
        doNothing().when(slideCaptchaVerifier).verify(anyString());
        when(redisComponent.getEmailCode("a@b.com")).thenReturn("existing");
        when(stringRedisTemplate.getExpire(anyString(), any())).thenReturn(290_000L);

        mockMvc.perform(post("/account/getEmailCode")
                        .param("email", "a@b.com")
                        .param("captchaVerification", "v"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.info").value(org.hamcrest.Matchers.containsString("验证码已发送")));

        verify(aliEmailServiceImpl, never()).sendVerificationCode(anyString());
    }

    @Test
    void getEmailCode_secondHitWithinWindow_rejected() throws Exception {
        doNothing().when(slideCaptchaVerifier).verify(anyString());
        // 同一邮箱 60s 内第二次：increment 返回 2 → 拒绝（换 IP 不可绕过）
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(2L);

        mockMvc.perform(post("/account/getEmailCode")
                        .param("email", "a@b.com")
                        .param("captchaVerification", "v"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.info").value(org.hamcrest.Matchers.containsString("频繁")));

        verify(aliEmailServiceImpl, never()).sendVerificationCode(anyString());
    }

    @Test
    void getEmailCode_sendsNewCode() throws Exception {
        stubEmailRateLimitFirstHit();
        doNothing().when(slideCaptchaVerifier).verify(anyString());
        when(redisComponent.getEmailCode("a@b.com")).thenReturn(null);
        when(aliEmailServiceImpl.sendVerificationCode("a@b.com")).thenReturn("654321");

        mockMvc.perform(post("/account/getEmailCode")
                        .param("email", "a@b.com")
                        .param("captchaVerification", "v"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(redisComponent).saveEmailCode("a@b.com", "654321");
    }
}
