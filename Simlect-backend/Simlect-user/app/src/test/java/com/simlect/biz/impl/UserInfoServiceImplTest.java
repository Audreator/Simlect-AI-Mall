package com.simlect.biz.impl;

import com.simlect.api.enums.UserSexEnum;
import com.simlect.api.enums.UserStatusEnum;
import com.simlect.component.RedisComponent;
import com.simlect.entity.po.UserInfo;
import com.simlect.entity.query.UserInfoQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserInfoMapper;
import com.simlect.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceImplTest {

    @Mock
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private UserInfoServiceImpl userInfoService;

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setUserId("U000000001");
        user.setEmail("a@b.com");
        user.setPassword("enc-old");
        user.setNickName("nick");
        return user;
    }

    @Test
    void findListByPage_usesDefaultPageSize15_whenNull() {
        when(userInfoMapper.selectCount(any())).thenReturn(30);
        when(userInfoMapper.selectList(any())).thenReturn(new ArrayList<>());
        UserInfoQuery query = new UserInfoQuery();
        query.setPageNo(2);

        PaginationResultVO<UserInfo> result = userInfoService.findListByPage(query);

        assertEquals(30, result.getTotalCount());
        assertEquals(2, result.getPageNo());
        assertEquals(15, result.getPageSize());
        assertEquals(2, result.getPageTotal());
        assertNotNull(query.getSimplePage());
    }

    @Test
    void addBatch_emptyList_returnsZero() {
        assertEquals(0, userInfoService.addBatch(null));
        assertEquals(0, userInfoService.addBatch(List.of()));
        verifyNoInteractions(userInfoMapper);
    }

    @Test
    void updateByParam_nullParam_throws() {
        assertThrows(BusinessException.class, () -> userInfoService.updateByParam(new UserInfo(), null));
        assertThrows(BusinessException.class, () -> userInfoService.deleteByParam(null));
    }

    @Test
    void getAndUpdateByUserId_delegates() {
        when(userInfoMapper.selectByUserId("U1")).thenReturn(buildUser());
        when(userInfoMapper.updateByUserId(any(), anyString())).thenReturn(1);
        when(userInfoMapper.deleteByUserId("U1")).thenReturn(1);

        assertEquals("a@b.com", userInfoService.getUserInfoByUserId("U1").getEmail());
        assertEquals(1, userInfoService.updateUserInfoByUserId(new UserInfo(), "U1"));
        assertEquals(1, userInfoService.deleteUserInfoByUserId("U1"));
    }

    @Test
    void register_success_encodesPasswordAndInserts() {
        when(redisComponent.consumeEmailCode("a@b.com")).thenReturn("123456");
        when(userInfoMapper.selectByEmail("a@b.com")).thenReturn(null);
        when(passwordService.encode("pwd123")).thenReturn("enc");

        userInfoService.register("a@b.com", "nick", "pwd123", "123456");

        verify(userInfoMapper).insert(any(UserInfo.class));
        verify(redisComponent).consumeEmailCode("a@b.com");
    }

    @Test
    void register_codeExpired_throws() {
        when(redisComponent.consumeEmailCode("a@b.com")).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> userInfoService.register("a@b.com", "nick", "pwd123", "123456"));
        assertTrue(e.getMessage().contains("验证码已过期"));
        verify(userInfoMapper, never()).insert(any());
    }

    @Test
    void register_wrongCode_throws() {
        when(redisComponent.consumeEmailCode("a@b.com")).thenReturn("111111");

        BusinessException e = assertThrows(BusinessException.class,
                () -> userInfoService.register("a@b.com", "nick", "pwd123", "999999"));
        assertTrue(e.getMessage().contains("验证码错误"));
    }

    @Test
    void register_userExists_throws() {
        when(redisComponent.consumeEmailCode("a@b.com")).thenReturn("123456");
        when(userInfoMapper.selectByEmail("a@b.com")).thenReturn(buildUser());

        BusinessException e = assertThrows(BusinessException.class,
                () -> userInfoService.register("a@b.com", "nick", "pwd123", "123456"));
        assertTrue(e.getMessage().contains("注册失败"));
    }

    @Test
    void updateUserInfo_updatesDbAndRefreshesToken() {
        when(userInfoMapper.updateByUserId(any(), anyString())).thenReturn(1);

        userInfoService.updateUserInfo("U1", "avatar.png", "新昵称", 1);

        verify(userInfoMapper).updateByUserId(argThat(bean ->
                "新昵称".equals(bean.getNickName()) && 1 == bean.getSex()), eq("U1"));
        verify(redisComponent).updateUser("U1", "avatar.png", "新昵称");
    }

    @Test
    void updatePassword_wrongOldPassword_throws() {
        when(userInfoMapper.selectByUserId("U1")).thenReturn(buildUser());
        when(passwordService.matches("bad", "enc-old")).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> userInfoService.updatePassword("U1", "bad", "new-pwd"));
        assertTrue(e.getMessage().contains("旧密码输入错误"));
    }

    @Test
    void updatePassword_sameAsOld_throws() {
        when(userInfoMapper.selectByUserId("U1")).thenReturn(buildUser());
        when(passwordService.matches("enc-old", "enc-old")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> userInfoService.updatePassword("U1", "enc-old", "enc-old"));
    }

    @Test
    void updatePassword_success_encodesAndCleansToken() {
        when(userInfoMapper.selectByUserId("U1")).thenReturn(buildUser());
        when(passwordService.matches("old", "enc-old")).thenReturn(true);
        when(passwordService.matches("new-pwd", "enc-old")).thenReturn(false);
        when(passwordService.encode("new-pwd")).thenReturn("enc-new");

        userInfoService.updatePassword("U1", "old", "new-pwd");

        verify(userInfoMapper).updateByUserId(argThat(bean -> "enc-new".equals(bean.getPassword())), eq("U1"));
        verify(redisComponent).cleanAllToken("U1");
    }

    @Test
    void forgetPassword_userNotRegistered_throws() {
        when(redisComponent.consumeEmailCode("a@b.com")).thenReturn("123456");
        when(userInfoMapper.selectByEmail("a@b.com")).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> userInfoService.forgetPassword("a@b.com", "new-pwd", "123456"));
    }

    @Test
    void forgetPassword_success_updatesAndCleans() {
        when(userInfoMapper.selectByEmail("a@b.com")).thenReturn(buildUser());
        when(redisComponent.consumeEmailCode("a@b.com")).thenReturn("123456");
        when(passwordService.encode("new-pwd")).thenReturn("enc-new");

        userInfoService.forgetPassword("a@b.com", "new-pwd", "123456");

        verify(userInfoMapper).updateByEmail(argThat(bean -> "enc-new".equals(bean.getPassword())), eq("a@b.com"));
        verify(redisComponent).consumeEmailCode("a@b.com");
        verify(redisComponent).cleanAllToken("U000000001");
    }
}
