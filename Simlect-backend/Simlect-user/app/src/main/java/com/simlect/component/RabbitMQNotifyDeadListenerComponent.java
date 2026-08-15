package com.simlect.component;

import com.simlect.api.dto.NotificationMessageDTO;
import com.simlect.constants.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 通知死信队列消费者：转存到 notify.dead.queue 的消息（TTL 超期/积压）在此处理。
 *
 * 策略：有限退避重投 —— 把消息按原 body+header 重投回 notify 队列（保留计数 header），
 * 重投超过上限（MAX_RECYCLE 次）则丢弃并打告警审计日志。
 * 防止「积压 → 重投 → 再次 TTL 超期 → 再死信」无限循环，同时避免死信队列无消费者导致的消息黑洞。
 */
@Slf4j
@Component
public class RabbitMQNotifyDeadListenerComponent {

    /** 重投计数 header 名 */
    public static final String HEADER_RECYCLE_COUNT = "x-simlect-recycle-count";

    /** 最大重投次数：0→1→2 三次重投机会，第 4 次到达死信队列时丢弃（约 15+ 分钟缓冲窗口） */
    private static final int MAX_RECYCLE_COUNT = 3;

    @Resource
    private MqPublisherConfirmHelper mqPublisherConfirmHelper;

    @RabbitListener(queues = RabbitMQConfig.NOTIFY_DEAD_QUEUE, ackMode = "MANUAL")
    public void handleDeadNotify(NotificationMessageDTO message, Channel channel, Message mqMessage) {
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        try {
            int recycleCount = recycleCountOf(mqMessage);
            String userId = message == null ? "unknown" : message.getUserId();
            String bizType = message == null ? "unknown" : message.getBizType();
            if (recycleCount >= MAX_RECYCLE_COUNT) {
                // 重投次数已达上限：丢弃 + 审计告警（通知已过期，继续重投无意义）
                log.error("通知死信重投超限丢弃 notifyDead, userId={}, bizType={}, recycleCount={}, reason={}",
                        userId, bizType, recycleCount, deadReason(mqMessage));
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 有限重投回 notify 队列：保留原始 headers（含 __TypeId__/幂等键），计数 +1
            Message recycled = MessageBuilder.fromMessage(mqMessage)
                    .setHeader(HEADER_RECYCLE_COUNT, recycleCount + 1)
                    .build();
            mqPublisherConfirmHelper.sendAndAwaitConfirm(
                    RabbitMQConfig.NOTIFY_EXCHANGE,
                    RabbitMQConfig.NOTIFY_KEY,
                    recycled,
                    msg -> msg,
                    "notify.dead.recycle:" + mqMessage.getMessageProperties().getMessageId()
                            + ":" + recycleCount);
            log.warn("通知死信重投 notify.queue userId={}, bizType={}, recycleCount={}",
                    userId, bizType, recycleCount + 1);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 死信处理失败：ack 丢弃（防死信再入队死循环），仅告警审计 —— 消息已由原链路补偿/幂等兜底
            log.error("通知死信处理失败，ack 丢弃告警", e);
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("死信 ack 失败", ex);
            }
        }
    }

    private int recycleCountOf(Message mqMessage) {
        Object v = mqMessage.getMessageProperties().getHeader(HEADER_RECYCLE_COUNT);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            try {
                return Integer.parseInt((String) v);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private String deadReason(Message mqMessage) {
        Object death = mqMessage.getMessageProperties().getHeader("x-death");
        return death == null ? "unknown" : death.toString();
    }
}
