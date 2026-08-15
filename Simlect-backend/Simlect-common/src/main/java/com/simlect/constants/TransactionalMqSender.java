package com.simlect.constants;

import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.service.OutboxMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 【功能】事务消息发送门面：解决「DB 事务与 MQ 双写」不一致。
 * <p>
 * 【角色】业务（如下单）应调用本类 {@link #sendAfterCommit}，而不是在事务里直接 convertAndSend。
 * 有事务：先写 Outbox 表，afterCommit 再 dispatch；无事务：走 ReliableMessageSender 直发。
 * 补投靠 OutboxDispatchTask 扫 PENDING/FAILED。
 * <p>
 * 【典型调用链】OrderInfoServiceImpl.postOrder 成功路径 → sendAfterCommit(pay.timeout.delay)
 * → Outbox → RabbitMQ TTL → DLX → 超时关单消费。
 */
@Component
@Slf4j
public class TransactionalMqSender {

    @Resource
    private OutboxMessageService outboxMessageService;
    @Resource
    private ReliableMessageSender reliableMessageSender;

    public void sendAfterCommit(String exchange, String routingKey, Object message,
                                String idempotencyKey, MessageReliabilityLevelEnum reliabilityLevel) {
        MessageReliabilityLevelEnum level = reliabilityLevel == null
                ? MessageReliabilityLevelEnum.STANDARD : reliabilityLevel;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            Long id = outboxMessageService.savePending(exchange, routingKey, message, idempotencyKey, level);
            if (id == null) {
                return;
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        outboxMessageService.tryDispatch(id);
                    } catch (Exception e) {
                        log.error("Outbox afterCommit 投递失败，等待定时重试 id={}", id, e);
                    }
                }
            });
            return;
        }
        reliableMessageSender.sendMessage(exchange, routingKey, message, idempotencyKey, level);
    }

    public void sendAfterCommit(Runnable sendAction) {
        if (sendAction == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendAction.run();
                }
            });
            return;
        }
        sendAction.run();
    }
}
