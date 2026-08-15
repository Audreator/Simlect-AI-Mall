package com.simlect.biz.impl;

import com.simlect.api.dto.NotificationMessageDTO;
import com.simlect.entity.enums.PageSize;
import com.simlect.entity.po.UserNotification;
import com.simlect.entity.query.SimplePage;
import com.simlect.entity.query.UserNotificationQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserNotificationMapper;
import com.simlect.component.NotifyPushPublisher;
import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.support.MqIdempotencyKeys;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.redis.RedisUtils;
import com.simlect.biz.UserNotificationService;
import com.simlect.utils.StringTools;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("userNotificationService")
@Slf4j
public class UserNotificationServiceImpl implements UserNotificationService {

    @Resource
    private UserNotificationMapper<UserNotification, UserNotificationQuery> userNotificationMapper;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private ReliableMessageSender reliableMessageSender;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private NotifyPushPublisher notifyPushPublisher;

    @Override
    public PaginationResultVO<UserNotification> loadPage(String userId, Integer pageNo, Integer readStatus) {
        UserNotificationQuery query = new UserNotificationQuery();
        query.setUserId(userId);
        query.setPageNo(pageNo);
        query.setOrderBy("create_time desc");
        if (readStatus != null) {
            query.setReadStatus(readStatus);
        }
        int count = userNotificationMapper.selectCount(query);
        int pageSize = PageSize.SIZE15.getSize();
        SimplePage page = new SimplePage(pageNo, count, pageSize);
        query.setSimplePage(page);
        List<UserNotification> list = userNotificationMapper.selectList(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    @Override
    public Integer countUnread(String userId) {
        return (int) getUnreadCountFromRedisOrSync(userId);
    }

    private String unreadCountKey(String userId) {
        return Constants.REDIS_KEY_USER_UNREAD_COUNT + userId;
    }

    private void incrUnread(String userId) {
        redisComponent.incr(unreadCountKey(userId));
    }

    private void decrUnread(String userId) {
        String key = unreadCountKey(userId);
        long after = redisComponent.decr(key);
        if (after < 0) {
            redisComponent.deleteCounter(key);
        }
    }

    private void clearUnread(String userId) {
        redisComponent.deleteCounter(unreadCountKey(userId));
    }

    private long getUnreadCountFromRedisOrSync(String userId) {
        String key = unreadCountKey(userId);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Math.max(0, Long.parseLong(cached));
        }
        UserNotificationQuery query = new UserNotificationQuery();
        query.setUserId(userId);
        query.setReadStatus(0);
        int dbCount = userNotificationMapper.selectCount(query);
        if (dbCount > 0) {
            redisComponent.setCounter(key, dbCount);
        }
        return dbCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(String userId, String notificationId) {
        UserNotification notification = userNotificationMapper.selectByNotificationId(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException("消息不存在");
        }
        if (Integer.valueOf(1).equals(notification.getReadStatus())) {
            return;
        }
        UserNotification update = new UserNotification();
        update.setReadStatus(1);
        userNotificationMapper.updateByNotificationId(update, notificationId);
        decrUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(String userId) {
        UserNotificationQuery query = new UserNotificationQuery();
        query.setUserId(userId);
        query.setReadStatus(0);
        UserNotification update = new UserNotification();
        update.setReadStatus(1);
        userNotificationMapper.updateByParam(update, query);
        clearUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String userId, String notificationId) {
        UserNotification notification = userNotificationMapper.selectByNotificationId(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException("消息不存在");
        }
        if (Integer.valueOf(0).equals(notification.getReadStatus())) {
            decrUnread(userId);
        }
        UserNotificationQuery query = new UserNotificationQuery();
        query.setUserId(userId);
        query.setNotificationId(notificationId);
        userNotificationMapper.deleteByParam(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll(String userId) {
        UserNotificationQuery query = new UserNotificationQuery();
        query.setUserId(userId);
        userNotificationMapper.deleteByParam(query);
        clearUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(String userId, String title, String content, String bizType, String bizId) {
        if (StringTools.isEmpty(userId) || StringTools.isEmpty(title)) {
            return;
        }
        String dedupKey = Constants.REDIS_KEY_NOTIFY_DEDUP + userId + ":" + (bizType == null ? "" : bizType) + ":"
                + (bizId == null ? "" : bizId) + ":" + title;
        if (!redisComponent.setIfAbsent(dedupKey, "1", 24, TimeUnit.HOURS)) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setNotificationId(StringTools.createNotificationId());
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBizType(bizType);
        notification.setBizId(bizId);
        notification.setReadStatus(0);
        notification.setCreateTime(new Date());
        try {
            userNotificationMapper.insert(notification);
            incrUnread(userId);
            notifyPushPublisher.push(notification);
        } catch (Exception e) {
            // 落库失败：释放去重键，避免 24 小时内不再通知
            redisComponent.deleteKey(dedupKey);
            throw e;
        }
    }

    @Override
    public void sendAsync(String userId, String title, String content, String bizType, String bizId) {
        if (StringTools.isEmpty(userId) || StringTools.isEmpty(title)) {
            return;
        }
        String dedupKey = Constants.REDIS_KEY_NOTIFY_DEDUP + userId + ":" + (bizType == null ? "" : bizType) + ":"
                + (bizId == null ? "" : bizId) + ":" + title;
        if (!redisComponent.setIfAbsent(dedupKey, "1", 24, TimeUnit.HOURS)) {
            return;
        }
        NotificationMessageDTO message = new NotificationMessageDTO(userId, title, content, bizType, bizId);
        reliableMessageSender.sendMessage(
                RabbitMQConfig.NOTIFY_EXCHANGE,
                RabbitMQConfig.NOTIFY_KEY,
                message,
                MqIdempotencyKeys.notification(userId, bizType, bizId),
                MessageReliabilityLevelEnum.HIGH);
        log.info("通知已通过RabbitMQ发送等待异步落库, userId={}, title={}", userId, title);
    }

    @Override
    public void batchInsert(List<UserNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        List<UserNotification> toInsert = new ArrayList<>();
        for (UserNotification notification : notifications) {
            if (isDuplicateBizNotification(notification)) {
                log.info("跳过重复通知 userId={}, bizType={}, bizId={}",
                        notification.getUserId(), notification.getBizType(), notification.getBizId());
                continue;
            }
            toInsert.add(notification);
        }
        if (toInsert.isEmpty()) {
            return;
        }
        userNotificationMapper.insertBatch(toInsert);
        for (UserNotification notification : toInsert) {
            incrUnread(notification.getUserId());
        }
    }

    private boolean isDuplicateBizNotification(UserNotification notification) {
        if (StringTools.isEmpty(notification.getUserId())
                || StringTools.isEmpty(notification.getBizType())
                || StringTools.isEmpty(notification.getBizId())) {
            return false;
        }
        UserNotificationQuery query = new UserNotificationQuery();
        query.setUserId(notification.getUserId());
        query.setBizType(notification.getBizType());
        query.setBizId(notification.getBizId());
        return userNotificationMapper.selectCount(query) > 0;
    }

    @Override
    public UserNotification getPopupNotification(String userId) {
        if (StringTools.isEmpty(userId)) {
            return null;
        }
        // 从数据库中获取最新的未读通知
        UserNotificationQuery query = new UserNotificationQuery();
        query.setUserId(userId);
        query.setReadStatus(0);
        query.setOrderBy("create_time desc");
        query.setPageNo(1);
        SimplePage page = new SimplePage(1, 1, 1);
        query.setSimplePage(page);
        List<UserNotification> list = userNotificationMapper.selectList(query);

        if (list == null || list.isEmpty()) {
            return null;
        }

        // 检查Redis中是否有未弹窗标记
        for (UserNotification notification : list) {
            String popupKey = Constants.REDIS_KEY_USER_POPUP_NOTIFY + userId + ":" + notification.getNotificationId();
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(popupKey))) {
                return notification;
            }
        }

        return null;
    }

    @Override
    public void clearPopupNotification(String userId, String notificationId) {
        if (StringTools.isEmpty(userId) || StringTools.isEmpty(notificationId)) {
            return;
        }
        String popupKey = Constants.REDIS_KEY_USER_POPUP_NOTIFY + userId + ":" + notificationId;
        redisUtils.delete(popupKey);
        log.info("清除未弹窗通知标记, userId={}, notificationId={}", userId, notificationId);
    }
}
