package com.simlect.constants;

import com.simlect.component.MqCompensationStore;
import com.simlect.component.MqConsumerIdempotencyHelper;
import com.simlect.component.MqIdempotencyGuard;
import com.simlect.component.MqPublisherConfirmHelper;
import com.simlect.entity.dto.MqCompensationRecord;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.service.MqCompensationLogService;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Component
@Slf4j
public class ReliableMessageSender {

    private static final int RETRY_MAX_COUNT = 3;
    private static final long HIGH_IDEMPOTENCY_TTL_SECONDS = 30L;
    private static final long STANDARD_IDEMPOTENCY_TTL_SECONDS = 24 * 3600L;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MqIdempotencyGuard mqIdempotencyGuard;

    @Resource
    private MqCompensationStore mqCompensationStore;

    @Resource
    private MqCompensationLogService mqCompensationLogService;

    @Resource
    private MqPublisherConfirmHelper mqPublisherConfirmHelper;

    @Resource
    @Qualifier("mqAsyncExecutor")
    private Executor mqAsyncExecutor;

    public void sendMessage(String exchange, String routingKey, Object message,
                            String idempotencyKey, MessageReliabilityLevelEnum reliabilityLevel) {
        if (StringTools.isEmpty(idempotencyKey)) {
            throw new IllegalArgumentException("MQ 发送必须指定 idempotencyKey");
        }
        if (reliabilityLevel == MessageReliabilityLevelEnum.HIGH) {
            sendHighConcurrency(exchange, routingKey, message, idempotencyKey);
        } else {
            sendStandard(exchange, routingKey, message, idempotencyKey);
        }
    }

    public void replaySend(String exchange, String routingKey, Object message, String idempotencyKey) {
        if (StringTools.isEmpty(idempotencyKey)) {
            throw new IllegalArgumentException("MQ 重放必须指定 idempotencyKey");
        }
        if (!mqIdempotencyGuard.tryAcquireSend(idempotencyKey, STANDARD_IDEMPOTENCY_TTL_SECONDS)) {
            log.info("MQ 重放幂等跳过, key={}", idempotencyKey);
            return;
        }
        sendStandardInternal(exchange, routingKey, message, idempotencyKey);
    }

    private void sendHighConcurrency(String exchange, String routingKey, Object message, String idempotencyKey) {
        if (!mqIdempotencyGuard.tryAcquireSend(idempotencyKey, HIGH_IDEMPOTENCY_TTL_SECONDS)) {
            return;
        }
        try {
            // 发送前先写补偿哨兵（防「取幂等闸后、异步发送前」崩溃导致消息无痕丢失；
            // 成功后清除，失败/崩溃残留由补偿流程兜底可见）
            MqCompensationRecord pending = buildCompensationRecord(
                    exchange, routingKey, message, idempotencyKey,
                    new IllegalStateException("发送前哨兵（待发送）"));
            mqCompensationStore.saveToRedis(pending);
            mqAsyncExecutor.execute(() -> {
                try {
                    doSendWithConfirm(exchange, routingKey, message, idempotencyKey);
                    mqCompensationStore.remove(idempotencyKey);
                    log.debug("高并发 MQ 已异步发送并 Confirm, exchange={}, routingKey={}, key={}",
                            exchange, routingKey, idempotencyKey);
                } catch (Exception e) {
                    log.error("高并发 MQ 异步发送失败，写入补偿, exchange={}, routingKey={}, key={}",
                            exchange, routingKey, idempotencyKey, e);
                    mqIdempotencyGuard.releaseSend(idempotencyKey);
                    saveCompensation(exchange, routingKey, message, idempotencyKey, e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("高并发 MQ 线程池已满，提交被拒绝，写入补偿, exchange={}, routingKey={}, key={}",
                    exchange, routingKey, idempotencyKey, e);
            mqIdempotencyGuard.releaseSend(idempotencyKey);
            saveCompensation(exchange, routingKey, message, idempotencyKey, e);
        }
    }

    private void sendStandard(String exchange, String routingKey, Object message, String idempotencyKey) {
        if (!mqIdempotencyGuard.tryAcquireSend(idempotencyKey, STANDARD_IDEMPOTENCY_TTL_SECONDS)) {
            return;
        }
        sendStandardInternal(exchange, routingKey, message, idempotencyKey);
    }

    private void sendStandardInternal(String exchange, String routingKey, Object message, String idempotencyKey) {
        Exception last = null;
        for (int i = 0; i < RETRY_MAX_COUNT; i++) {
            try {
                if (i > 0) {
                    Thread.sleep(1000L * i);
                }
                doSendWithConfirm(exchange, routingKey, message, idempotencyKey);
                log.info("MQ 同步发送成功, exchange={}, routingKey={}, key={}, retry={}",
                        exchange, routingKey, idempotencyKey, i);
                return;
            } catch (Exception e) {
                last = e;
                log.warn("MQ 同步发送失败, key={}, retry={}/{}", idempotencyKey, i + 1, RETRY_MAX_COUNT, e);
            }
        }
        mqIdempotencyGuard.releaseSend(idempotencyKey);
        log.error("MQ 同步发送重试耗尽, key={}", idempotencyKey, last);
        throw new AmqpException("消息发送失败，已重试 " + RETRY_MAX_COUNT + " 次", last);
    }

    private void doSendWithConfirm(String exchange, String routingKey, Object message, String idempotencyKey)
            throws Exception {
        mqPublisherConfirmHelper.sendAndAwaitConfirm(
                exchange,
                routingKey,
                message,
                msg -> {
                    MessageProperties props = msg.getMessageProperties();
                    props.setMessageId(idempotencyKey);
                    props.setHeader(MqConsumerIdempotencyHelper.HEADER_IDEMPOTENCY_KEY, idempotencyKey);
                    // 消息持久化：与 durable 队列配合，Broker 落盘后 Confirm 才算成功
                    props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                },
                idempotencyKey);
    }

    private void saveCompensation(String exchange, String routingKey, Object message,
                                  String idempotencyKey, Exception error) {
        MqCompensationRecord record = buildCompensationRecord(exchange, routingKey, message, idempotencyKey, error);
        try {
            mqCompensationLogService.saveFromFailure(record);
            mqCompensationStore.saveToRedis(record);
        } catch (Exception e) {
            log.error("MQ 补偿记录写入失败, key={}", idempotencyKey, e);
        }
    }

    private MqCompensationRecord buildCompensationRecord(String exchange, String routingKey, Object message,
                                                         String idempotencyKey, Exception error) {
        MqCompensationRecord record = new MqCompensationRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setExchange(exchange);
        record.setRoutingKey(routingKey);
        record.setPayload(message);
        record.setReliabilityLevel(MessageReliabilityLevelEnum.HIGH);
        record.setFailedAt(System.currentTimeMillis());
        record.setRetryCount(0);
        record.setErrorMessage(error == null ? null : error.getMessage());
        return record;
    }
}
