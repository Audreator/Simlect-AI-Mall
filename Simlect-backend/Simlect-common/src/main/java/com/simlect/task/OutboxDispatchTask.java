package com.simlect.task;

import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.service.OutboxMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@ConditionalOnProperty(name = "mq.outbox.dispatch-enabled", havingValue = "true")
@ConditionalOnProperty(name = "app.common-scheduling.enabled", havingValue = "true")
/**
 * 【功能】Outbox 定时补投：扫描 PENDING/FAILED 消息再次 dispatch。
 * <p>
 * 【角色】可靠投递的扫表重试；与 TransactionalMqSender.afterCommit 即时投递互补。
 * Redis 锁防止多实例重复扫。
 * <p>
 * 【调用链】@Scheduled → Redis lock → OutboxMessageService.dispatchPendingBatch → RabbitMQ。
 */
public class OutboxDispatchTask {

    @Resource
    private OutboxMessageService outboxMessageService;
    @Resource
    private RedisComponent redisComponent;

    @Value("${mq.outbox.dispatch-batch-size:30}")
    private int batchSize;

    @Value("${mq.outbox.dispatch-max-retries:10}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${mq.outbox.dispatch-interval-ms:5000}")
    public void dispatch() {
        String lockKey = Constants.REDIS_KEY_MQ_COMPENSATE + "outbox:dispatch:lock";
        if (!redisComponent.setIfAbsent(lockKey, "1", 4, TimeUnit.SECONDS)) {
            return;
        }
        try {
            int count = outboxMessageService.dispatchPendingBatch(batchSize, maxRetries);
            if (count > 0) {
                log.info("Outbox 定时投递成功 {} 条", count);
            }
        } catch (Exception e) {
            // 表不存在（非 order/admin 库）时安静跳过
            log.debug("Outbox 定时投递跳过/失败: {}", e.getMessage());
        }
    }
}
