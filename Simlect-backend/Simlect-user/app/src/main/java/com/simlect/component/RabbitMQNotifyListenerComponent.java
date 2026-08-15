package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.api.dto.NotificationMessageDTO;
import com.simlect.entity.po.UserNotification;
import com.simlect.redis.RedisUtils;
import com.simlect.biz.UserNotificationService;
import com.simlect.utils.StringTools;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RabbitMQNotifyListenerComponent {

    @Resource
    private UserNotificationService userNotificationService;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private NotifyPushPublisher notifyPushPublisher;
    @Resource
    private MqListenerHelper mqListenerHelper;

    private static final int BATCH_SIZE = 100;
    private static final int FLUSH_INTERVAL_SECONDS = 2;
    private final List<PendingNotify> batchList = new ArrayList<>(BATCH_SIZE);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final class PendingNotify {
        private final UserNotification notification;
        private final Message mqMessage;
        private final long deliveryTag;
        private final Channel channel;

        private PendingNotify(UserNotification notification, Message mqMessage, long deliveryTag, Channel channel) {
            this.notification = notification;
            this.mqMessage = mqMessage;
            this.deliveryTag = deliveryTag;
            this.channel = channel;
        }
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::flushRemaining, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("通知刷新定时任务已启动，间隔: {}秒", FLUSH_INTERVAL_SECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        flushRemaining();
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFY_QUEUE, ackMode = "MANUAL")
    public void handleNotify(NotificationMessageDTO message, Channel channel, Message mqMessage) {
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        try {
            if (!mqListenerHelper.tryBeginConsume(mqMessage, MqListenerHelper.CONSUME_IDEMPOTENCY_TTL_HIGH_SECONDS)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            UserNotification notification = new UserNotification();
            notification.setNotificationId(StringTools.createNotificationId());
            notification.setUserId(message.getUserId());
            notification.setTitle(message.getTitle());
            notification.setContent(message.getContent());
            notification.setBizType(message.getBizType());
            notification.setBizId(message.getBizId());
            notification.setReadStatus(0);
            notification.setCreateTime(new Date());

            synchronized (batchList) {
                batchList.add(new PendingNotify(notification, mqMessage, deliveryTag, channel));
                if (batchList.size() >= BATCH_SIZE) {
                    flushBatch();
                }
            }
        } catch (Exception e) {
            log.error("处理通知消息失败", e);
            mqListenerHelper.releaseConsume(mqMessage);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("通知 NACK 失败", ex);
            }
        }
    }

    private void flushRemaining() {
        synchronized (batchList) {
            if (!batchList.isEmpty()) {
                flushBatch();
            }
        }
    }

    private void flushBatch() {
        List<PendingNotify> toSave = new ArrayList<>(batchList);
        batchList.clear();
        List<UserNotification> notifications = new ArrayList<>(toSave.size());
        for (PendingNotify pending : toSave) {
            notifications.add(pending.notification);
        }
        try {
            userNotificationService.batchInsert(notifications);
            for (UserNotification notification : notifications) {
                markPopupAndPush(notification);
            }
            for (PendingNotify pending : toSave) {
                pending.channel.basicAck(pending.deliveryTag, false);
                mqListenerHelper.clearConsumeRetry(RabbitMQConfig.NOTIFY_QUEUE, pending.mqMessage);
            }
            log.info("批量写入通知完成，数量: {}", toSave.size());
        } catch (Exception e) {
            log.error("批量写入通知失败", e);
            // 失败路径：requeue 重投（MQ 层保证重试）+ 释放幂等键允许再次消费；
            // 不回批（避免跨线程/失效 deliveryTag 重复 ACK 与重复通知）
            for (PendingNotify pending : toSave) {
                mqListenerHelper.releaseConsume(pending.mqMessage);
                try {
                    pending.channel.basicNack(pending.deliveryTag, false, true);
                } catch (IOException ex) {
                    log.error("通知失败 NACK 异常", ex);
                }
            }
        }
    }

    private void markPopupAndPush(UserNotification notification) {
        try {
            String bizType = notification.getBizType();
            if ("rush_coupon".equals(bizType) || "logistics".equals(bizType)
                    || "coupon_expire".equals(bizType) || "sign_reward".equals(bizType)) {
                String popupKey = Constants.REDIS_KEY_USER_POPUP_NOTIFY + notification.getUserId()
                        + ":" + notification.getNotificationId();
                redisUtils.setex(popupKey, "1", 7 * 24 * 3600);
            }
        } catch (Exception e) {
            log.warn("通知弹窗标记失败 notificationId={}", notification.getNotificationId(), e);
        }
        try {
            notifyPushPublisher.push(notification);
        } catch (Exception e) {
            log.warn("通知 WS 推送失败 notificationId={}", notification.getNotificationId(), e);
        }
    }
}
