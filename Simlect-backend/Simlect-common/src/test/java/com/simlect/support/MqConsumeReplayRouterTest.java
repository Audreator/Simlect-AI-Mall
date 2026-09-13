package com.simlect.support;

import com.simlect.constants.Constants;
import com.simlect.constants.RabbitMQConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqConsumeReplayRouterTest {

    @Test
    void resolve_ragQueue_mapsToRagExchange() {
        MqConsumeReplayRouter.Target target = MqConsumeReplayRouter.resolve(RabbitMQConfig.RAG_QUEUE);
        assertEquals(RabbitMQConfig.RAG_EXCHANGE, target.exchange());
        assertEquals(RabbitMQConfig.RAG_QUEUE_KEY, target.routingKey());
    }

    @Test
    void resolve_ragDeadQueue_mapsToRagExchange() {
        MqConsumeReplayRouter.Target target = MqConsumeReplayRouter.resolve(RabbitMQConfig.RAG_DEAD_QUEUE);
        assertEquals(RabbitMQConfig.RAG_EXCHANGE, target.exchange());
        assertEquals(RabbitMQConfig.RAG_QUEUE_KEY, target.routingKey());
    }

    @Test
    void resolve_browseQueue_mapsToBrowseExchange() {
        MqConsumeReplayRouter.Target target = MqConsumeReplayRouter.resolve(RabbitMQConfig.BROWSE_RECORD_QUEUE);
        assertEquals(RabbitMQConfig.BROWSE_EXCHANGE, target.exchange());
        assertEquals(RabbitMQConfig.BROWSE_RECORD_KEY, target.routingKey());
    }

    @Test
    void resolve_signStreakCouponQueue_mapsToRewardExchange() {
        MqConsumeReplayRouter.Target target =
                MqConsumeReplayRouter.resolve(RabbitMQConfig.SIGN_STREAK_COUPON_QUEUE);
        assertEquals(RabbitMQConfig.SIGN_STREAK_COUPON_EXCHANGE, target.exchange());
        assertEquals(RabbitMQConfig.SIGN_STREAK_COUPON_KEY, target.routingKey());
    }

    @Test
    void resolve_notifyQueue_mapsToNotifyExchange() {
        MqConsumeReplayRouter.Target target = MqConsumeReplayRouter.resolve(RabbitMQConfig.NOTIFY_QUEUE);
        assertEquals(RabbitMQConfig.NOTIFY_EXCHANGE, target.exchange());
        assertEquals(RabbitMQConfig.NOTIFY_KEY, target.routingKey());
    }

    @Test
    void resolve_payDeadQueues_mapBackToPayExchange() {
        assertEquals(RabbitMQConfig.PAY_TIMEOUT_DEAD_KEY,
                MqConsumeReplayRouter.resolve(RabbitMQConfig.PAY_TIMEOUT_DEAD_QUEUE).routingKey());
        assertEquals(RabbitMQConfig.PAY_LOGISTICS_DEAD_KEY,
                MqConsumeReplayRouter.resolve(RabbitMQConfig.PAY_LOGISTICS_DEAD_QUEUE).routingKey());
        assertEquals(RabbitMQConfig.PAY_CONFIRM_DEAD_KEY,
                MqConsumeReplayRouter.resolve(RabbitMQConfig.PAY_CONFIRM_DEAD_QUEUE).routingKey());
    }

    @Test
    void resolve_rushingDeadQueue_mapsToRushingExchange() {
        MqConsumeReplayRouter.Target target = MqConsumeReplayRouter.resolve(RabbitMQConfig.RUSHING_DEAD_QUEUE);
        assertEquals(RabbitMQConfig.RUSHING_EXCHANGE, target.exchange());
        assertEquals(RabbitMQConfig.RUSHING_DEAD_KEY, target.routingKey());
    }

    @Test
    void resolve_userTempBanDeadQueue_mapsToTempBanExchange() {
        MqConsumeReplayRouter.Target target = MqConsumeReplayRouter.resolve(RabbitMQConfig.USER_TEMP_BAN_DEAD_QUEUE);
        assertEquals(RabbitMQConfig.USER_TEMP_BAN_EXCHANGE, target.exchange());
        assertEquals(RabbitMQConfig.USER_TEMP_BAN_DEAD_KEY, target.routingKey());
    }

    @Test
    void resolve_unknownOrNull_returnsNull() {
        assertNull(MqConsumeReplayRouter.resolve("unknown.queue"));
        assertNull(MqConsumeReplayRouter.resolve(null));
    }

    @Test
    void isConsumeFailure_matchesConsumeExchange() {
        assertTrue(MqConsumeReplayRouter.isConsumeFailure(Constants.MQ_CONSUME_FAILURE_EXCHANGE));
        assertFalse(MqConsumeReplayRouter.isConsumeFailure("pay.exchange"));
        assertFalse(MqConsumeReplayRouter.isConsumeFailure(null));
    }
}
