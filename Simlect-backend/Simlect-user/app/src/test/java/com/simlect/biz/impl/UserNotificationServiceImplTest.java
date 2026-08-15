package com.simlect.biz.impl;

import com.simlect.api.dto.NotificationMessageDTO;
import com.simlect.biz.UserNotificationService;
import com.simlect.component.NotifyPushPublisher;
import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.po.UserNotification;
import com.simlect.entity.query.UserNotificationQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserNotificationMapper;
import com.simlect.redis.RedisUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceImplTest {

    @Mock
    private UserNotificationMapper<UserNotification, UserNotificationQuery> userNotificationMapper;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private ReliableMessageSender reliableMessageSender;
    @Mock
    private RedisUtils redisUtils;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private NotifyPushPublisher notifyPushPublisher;

    @InjectMocks
    private UserNotificationServiceImpl userNotificationService;

    private UserNotification buildNotification() {
        UserNotification notification = new UserNotification();
        notification.setNotificationId("N1");
        notification.setUserId("U1");
        notification.setTitle("标题");
        notification.setContent("内容");
        notification.setReadStatus(0);
        notification.setBizType("order");
        notification.setBizId("O1");
        return notification;
    }

    @Test
    void loadPage_appliesFiltersAndPaging() {
        when(userNotificationMapper.selectCount(any())).thenReturn(30);
        when(userNotificationMapper.selectList(any())).thenReturn(new ArrayList<>());

        PaginationResultVO<UserNotification> result =
                userNotificationService.loadPage("U1", 2, 0);

        assertEquals(30, result.getTotalCount());
        assertEquals(15, result.getPageSize());
        assertEquals(2, result.getPageNo());
    }

    @Test
    void countUnread_usesCachedRedisValue() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(Constants.REDIS_KEY_USER_UNREAD_COUNT + "U1")).thenReturn("7");

        assertEquals(7, userNotificationService.countUnread("U1"));
        verify(userNotificationMapper, never()).selectCount(any());
    }

    @Test
    void countUnread_fallsBackToDb_andWarmsCache() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(Constants.REDIS_KEY_USER_UNREAD_COUNT + "U1")).thenReturn(null);
        when(userNotificationMapper.selectCount(any())).thenReturn(3);

        assertEquals(3, userNotificationService.countUnread("U1"));

        verify(redisComponent).setCounter(Constants.REDIS_KEY_USER_UNREAD_COUNT + "U1", 3);
    }

    @Test
    void markRead_notFound_throws() {
        when(userNotificationMapper.selectByNotificationId("N1")).thenReturn(null);

        assertThrows(BusinessException.class, () -> userNotificationService.markRead("U1", "N1"));
    }

    @Test
    void markRead_notOwned_throws() {
        UserNotification notification = buildNotification();
        notification.setUserId("U2");
        when(userNotificationMapper.selectByNotificationId("N1")).thenReturn(notification);

        assertThrows(BusinessException.class, () -> userNotificationService.markRead("U1", "N1"));
    }

    @Test
    void markRead_alreadyRead_noUpdate() {
        UserNotification notification = buildNotification();
        notification.setReadStatus(1);
        when(userNotificationMapper.selectByNotificationId("N1")).thenReturn(notification);

        userNotificationService.markRead("U1", "N1");

        verify(userNotificationMapper, never()).updateByNotificationId(any(), anyString());
        verify(redisComponent, never()).decr(anyString());
    }

    @Test
    void markRead_success_decrementsUnread() {
        when(userNotificationMapper.selectByNotificationId("N1")).thenReturn(buildNotification());
        when(redisComponent.decr(anyString())).thenReturn(5L);

        userNotificationService.markRead("U1", "N1");

        verify(userNotificationMapper).updateByNotificationId(argThat(u -> 1 == u.getReadStatus()), eq("N1"));
        verify(redisComponent).decr(Constants.REDIS_KEY_USER_UNREAD_COUNT + "U1");
        verify(redisComponent, never()).deleteCounter(anyString());
    }

    @Test
    void markAllRead_updatesAndClears() {
        userNotificationService.markAllRead("U1");

        verify(userNotificationMapper).updateByParam(any(), any());
        verify(redisComponent).deleteCounter(Constants.REDIS_KEY_USER_UNREAD_COUNT + "U1");
    }

    @Test
    void delete_unreadNotification_decrementsThenDeletes() {
        when(userNotificationMapper.selectByNotificationId("N1")).thenReturn(buildNotification());
        when(redisComponent.decr(anyString())).thenReturn(2L);

        userNotificationService.delete("U1", "N1");

        verify(userNotificationMapper).deleteByParam(any());
        verify(redisComponent).decr(Constants.REDIS_KEY_USER_UNREAD_COUNT + "U1");
    }

    @Test
    void clearAll_deletesAllAndClearsCounter() {
        userNotificationService.clearAll("U1");

        verify(userNotificationMapper).deleteByParam(any());
        verify(redisComponent).deleteCounter(Constants.REDIS_KEY_USER_UNREAD_COUNT + "U1");
    }

    @Test
    void send_emptyArgs_ignored() {
        userNotificationService.send("", "标题", "内容", null, null);
        userNotificationService.send("U1", "", "内容", null, null);

        verifyNoInteractions(userNotificationMapper, redisComponent);
    }

    @Test
    void send_duplicateDedup_skipped() {
        when(redisComponent.setIfAbsent(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(false);

        userNotificationService.send("U1", "标题", "内容", "order", "O1");

        verify(userNotificationMapper, never()).insert(any());
        verify(notifyPushPublisher, never()).push(any());
    }

    @Test
    void send_success_insertsIncrementsAndPushes() {
        when(redisComponent.setIfAbsent(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        userNotificationService.send("U1", "标题", "内容", "order", "O1");

        verify(userNotificationMapper).insert(argThat(n ->
                "U1".equals(n.getUserId()) && 0 == n.getReadStatus() && n.getCreateTime() != null));
        verify(redisComponent).incr(Constants.REDIS_KEY_USER_UNREAD_COUNT + "U1");
        verify(notifyPushPublisher).push(any(UserNotification.class));
    }

    @Test
    void sendAsync_success_sendsReliableMessage() {
        when(redisComponent.setIfAbsent(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        userNotificationService.sendAsync("U1", "标题", "内容", "sign", "20260101");

        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.NOTIFY_EXCHANGE),
                eq(RabbitMQConfig.NOTIFY_KEY), any(NotificationMessageDTO.class), anyString(), any());
    }

    @Test
    void batchInsert_emptyList_ignored() {
        userNotificationService.batchInsert(null);
        userNotificationService.batchInsert(List.of());

        verifyNoInteractions(userNotificationMapper);
    }

    @Test
    void batchInsert_skipsDuplicateAndInsertsRest() {
        UserNotification dup = buildNotification();
        dup.setNotificationId("N-dup");
        UserNotification fresh = buildNotification();
        fresh.setNotificationId("N-fresh");
        when(userNotificationMapper.selectCount(any())).thenReturn(1).thenReturn(0);

        userNotificationService.batchInsert(List.of(dup, fresh));

        verify(userNotificationMapper).insertBatch(argThat(list -> list.size() == 1));
        verify(redisComponent).incr(Constants.REDIS_KEY_USER_UNREAD_COUNT + "U1");
    }

    @Test
    void getPopupNotification_returnsNewestUnreadWithPopupFlag() {
        UserNotification notification = buildNotification();
        when(userNotificationMapper.selectList(any())).thenReturn(List.of(notification));
        when(stringRedisTemplate.hasKey(Constants.REDIS_KEY_USER_POPUP_NOTIFY + "U1:" + "N1"))
                .thenReturn(true);

        assertEquals("N1", userNotificationService.getPopupNotification("U1").getNotificationId());
    }

    @Test
    void getPopupNotification_noUnread_returnsNull() {
        when(userNotificationMapper.selectList(any())).thenReturn(List.of());

        assertNull(userNotificationService.getPopupNotification("U1"));
    }

    @Test
    void clearPopupNotification_deletesRedisKey() {
        userNotificationService.clearPopupNotification("U1", "N1");

        verify(redisUtils).delete(Constants.REDIS_KEY_USER_POPUP_NOTIFY + "U1:N1");
    }
}
