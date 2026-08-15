package com.simlect.component;

import com.simlect.api.dto.UserTempBanDTO;
import com.simlect.api.enums.UserStatusEnum;
import com.simlect.biz.UserInfoService;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.po.UserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTempBanServiceTest {

    @Mock
    private UserInfoService userInfoService;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ReliableMessageSender reliableMessageSender;

    @InjectMocks
    private UserTempBanService userTempBanService;

    @Test
    void formatUnbanTime_rendersDateTime() {
        String formatted = UserTempBanService.formatUnbanTime(0L);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void buildTempBanMessage_containsUnbanTime() {
        assertTrue(userTempBanService.buildTempBanMessage(1000L).contains("解封时间"));
    }

    @Test
    void banUserHours_disablesUserSetsRedisAndSendsMq() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);

        long unbanAt = userTempBanService.banUserHours("U1", 2);

        assertTrue(unbanAt > System.currentTimeMillis());
        verify(userInfoService).updateUserInfoByUserId(argThat(p ->
                UserStatusEnum.DISABLE.getStatus().equals(p.getStatus())), eq("U1"));
        verify(redisComponent).cleanAllToken("U1");
        verify(ops).set(anyString(), eq(String.valueOf(unbanAt)), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.USER_TEMP_BAN_EXCHANGE),
                eq(RabbitMQConfig.USER_TEMP_BAN_DELAY_KEY), any(UserTempBanDTO.class),
                anyString(), any());
    }

    @Test
    void getUnbanAtMs_parsesCachedValue() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("1234567890");

        assertEquals(1234567890L, userTempBanService.getUnbanAtMs("U1"));
    }

    @Test
    void getUnbanAtMs_missing_returnsNull() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);

        assertNull(userTempBanService.getUnbanAtMs("U1"));
    }

    @Test
    void getUnbanAtMs_invalidValue_usesTtl() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("not-a-number");
        when(stringRedisTemplate.getExpire(anyString(), eq(TimeUnit.MILLISECONDS))).thenReturn(60000L);

        assertNotNull(userTempBanService.getUnbanAtMs("U1"));
    }

    @Test
    void isTempBanned_queriesKey() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        assertTrue(userTempBanService.isTempBanned("U1"));
    }

    @Test
    void tryAutoUnban_nullDto_returnsFalse() {
        assertFalse(userTempBanService.tryAutoUnban(null));
        assertFalse(userTempBanService.tryAutoUnban(new UserTempBanDTO(null, 0L)));
    }

    @Test
    void tryAutoUnban_noMark_returnsFalse() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);

        assertFalse(userTempBanService.tryAutoUnban(new UserTempBanDTO("U1", 1000L)));
    }

    @Test
    void tryAutoUnban_mismatchedUnbanAt_returnsFalse() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("999");

        assertFalse(userTempBanService.tryAutoUnban(new UserTempBanDTO("U1", 888L)));
    }

    @Test
    void tryAutoUnban_notTimeYet_returnsFalse() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        long future = System.currentTimeMillis() + 3600_000L;
        when(ops.get(anyString())).thenReturn(String.valueOf(future));

        assertFalse(userTempBanService.tryAutoUnban(new UserTempBanDTO("U1", future)));
    }

    @Test
    void tryAutoUnban_success_reenablesUser() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        long past = System.currentTimeMillis() - 1000L;
        when(ops.get(anyString())).thenReturn(String.valueOf(past));
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
        UserInfo user = new UserInfo();
        user.setStatus(UserStatusEnum.DISABLE.getStatus());
        when(userInfoService.getUserInfoByUserId("U1")).thenReturn(user);

        boolean result = userTempBanService.tryAutoUnban(new UserTempBanDTO("U1", past));

        assertTrue(result);
        verify(userInfoService).updateUserInfoByUserId(argThat(p ->
                UserStatusEnum.ENABLE.getStatus().equals(p.getStatus())), eq("U1"));
    }

    @Test
    void manualUnban_success() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        long past = System.currentTimeMillis() - 1000L;
        when(ops.get(anyString())).thenReturn(String.valueOf(past));
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
        UserInfo user = new UserInfo();
        user.setStatus(UserStatusEnum.DISABLE.getStatus());
        when(userInfoService.getUserInfoByUserId("U1")).thenReturn(user);

        assertTrue(userTempBanService.manualUnban("U1"));
        verify(userInfoService).updateUserInfoByUserId(argThat(p ->
                UserStatusEnum.ENABLE.getStatus().equals(p.getStatus())), eq("U1"));
    }

    @Test
    void banUserPermanent_clearsMarkAndDisables() {
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        userTempBanService.banUserPermanent("U1");

        verify(stringRedisTemplate).delete(anyString());
        verify(userInfoService).updateUserInfoByUserId(argThat(p ->
                UserStatusEnum.DISABLE.getStatus().equals(p.getStatus())), eq("U1"));
        verify(redisComponent).cleanAllToken("U1");
        verify(reliableMessageSender, never()).sendMessage(anyString(), anyString(), any(), anyString(), any());
    }
}
