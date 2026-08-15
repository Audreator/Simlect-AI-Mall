package com.simlect;

import com.simlect.constants.RabbitMQConfig;
import com.simlect.support.MqIdempotencyKeys;
import com.simlect.utils.OrderPayAmountUtil;
import com.simlect.utils.StringTools;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simlect-order 模块冒烟测试（位于根包，供 -Dtest=com.simlect.*Test 选中执行）。
 * 校验订单模块关键静态工具与 MQ 配置不缺失，不依赖 Spring 容器。
 */
class OrderModuleSmokeTest {

    @Test
    void orderIdAndPayOrderIdGenerators_work() {
        assertNotNull(StringTools.createOrderId());
        assertNotNull(StringTools.createPayOrderId());
        assertNotNull(StringTools.createUserCouponId());
        assertEquals(30, StringTools.getRandomNumber(30).length());
    }

    @Test
    void payAmountUtil_minPayNormalize() {
        assertEquals(new BigDecimal("0.01"), OrderPayAmountUtil.minOrderPayAmount());
        assertEquals(new BigDecimal("0.01"), OrderPayAmountUtil.normalizeChannelPayAmount(BigDecimal.ZERO));
    }

    @Test
    void mqIdempotencyKeys_buildCorrectly() {
        assertEquals("pay:timeout:O1", MqIdempotencyKeys.payTimeout("O1"));
        assertEquals("pay:logistics:O1:step:0", MqIdempotencyKeys.payLogistics("O1", 0));
        assertEquals("pay:confirm:O1", MqIdempotencyKeys.payConfirm("O1"));
    }

    @Test
    void rabbitMqConfig_queuesAndKeysPresent() {
        assertFalse(com.simlect.utils.StringTools.isEmpty(RabbitMQConfig.PAY_TIMEOUT_DEAD_QUEUE));
        assertFalse(com.simlect.utils.StringTools.isEmpty(RabbitMQConfig.PAY_CONFIRM_DEAD_QUEUE));
        assertFalse(com.simlect.utils.StringTools.isEmpty(RabbitMQConfig.PAY_LOGISTICS_DEAD_QUEUE));
        assertFalse(com.simlect.utils.StringTools.isEmpty(RabbitMQConfig.RUSHING_ORDER_QUEUE));
        assertFalse(com.simlect.utils.StringTools.isEmpty(RabbitMQConfig.PAY_EXCHANGE));
        assertTrue(com.simlect.utils.StringTools.isEmpty(""));
    }
}
