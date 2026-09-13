package com.simlect.component;

import com.rabbitmq.client.Channel;
import com.simlect.api.dto.SignStreakCouponMessageDTO;
import com.simlect.biz.CouponInternalService;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class SignStreakCouponListenerComponent {

    @Resource
    private CouponInternalService couponInternalService;
    @Resource
    private MqListenerHelper mqListenerHelper;

    @RabbitListener(queues = RabbitMQConfig.SIGN_STREAK_COUPON_QUEUE, ackMode = "MANUAL")
    public void handle(SignStreakCouponMessageDTO payload, Channel channel, Message mqMessage) throws IOException {
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        if (!mqListenerHelper.tryBeginConsume(
                mqMessage, MqListenerHelper.CONSUME_IDEMPOTENCY_TTL_HIGH_SECONDS)) {
            channel.basicAck(deliveryTag, false);
            return;
        }
        if (payload == null || StringTools.isEmpty(payload.getUserId())
                || StringTools.isEmpty(payload.getCouponId())
                || payload.getStreakDays() == null || payload.getStreakDays() < 1) {
            log.error("丢弃非法连续签到奖励消息: {}", payload);
            mqListenerHelper.clearConsumeRetry(RabbitMQConfig.SIGN_STREAK_COUPON_QUEUE, mqMessage);
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            CouponInternalService.StreakCouponGrantResult result =
                    couponInternalService.grantSignStreakCoupon(payload);
            log.info("连续签到奖励处理完成 userId={}, couponId={}, streakDays={}, outcome={}",
                    payload.getUserId(), payload.getCouponId(), payload.getStreakDays(), result.outcome());
            mqListenerHelper.clearConsumeRetry(RabbitMQConfig.SIGN_STREAK_COUPON_QUEUE, mqMessage);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("连续签到奖励消费失败 payload={}", payload, e);
            mqListenerHelper.nackWithRetryOrDlq(channel, deliveryTag, mqMessage,
                    RabbitMQConfig.SIGN_STREAK_COUPON_QUEUE, payload, e);
        }
    }
}
