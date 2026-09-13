package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.config.AppConfig;
import com.simlect.api.dto.PayOrderMessageDTO;
import com.simlect.api.enums.LogisticsStatusEnum;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderLogisticsInfo;
import com.simlect.entity.po.OrderLogisticsInfoRecord;
import com.simlect.entity.query.OrderLogisticsInfoQuery;
import com.simlect.entity.query.OrderLogisticsInfoRecordQuery;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderLogisticsInfoRecordService;
import com.simlect.biz.OrderLogisticsInfoService;
import com.simlect.support.MqIdempotencyKeys;
import com.simlect.state.OrderStateEvent;
import com.simlect.state.OrderStateMachine;
import com.simlect.utils.StringTools;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.Objects;
import java.util.Random;

@Component
@Slf4j
public class RabbitMQPayOrderDeadListenerComponent {

    private static final String[] RECORD_ADDRESSES = {
            "上海市浦东新区中转站",
            "杭州市萧山区转运中心",
            "南京市江宁区中转场",
            "无锡市滨湖区分拨中心",
            "苏州市工业园区集散站",
            "合肥市蜀山区转运场",
            "武汉市东西湖区中转站",
            "郑州市郑东新区分拨中心",
            "济南市历下区转运站",
            "天津市滨海新区中转场",
            "北京市大兴区集散中心",
            "广州市白云区转运站",
            "深圳市宝安区中转场",
            "成都市双流区分拨站",
            "重庆市渝北区转运中心",
            "西安市未央区集散场",
            "福州市仓山区中转站",
            "青岛市崂山区分拨中心",
            "大连市甘井子区转运站",
            "沈阳市铁西区集散场",
            "长沙市岳麓区中转站",
            "南昌市东湖区转运中心",
            "昆明市官渡区分拨站",
            "贵阳市观山湖区集散场",
            "太原市小店区中转站",
            "石家庄市长安区转运中心",
            "哈尔滨市南岗区分拨场",
            "长春市朝阳区集散站",
            "兰州市城关区中转场",
            "呼和浩特市新城区转运站"
    };

    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private OrderStateMachine orderStateMachine;
    @Resource
    private OrderLogisticsInfoService orderLogisticsInfoService;
    @Resource
    private OrderLogisticsInfoRecordService orderLogisticsInfoRecordService;
    @Resource
    private ReliableMessageSender reliableMessageSender;
    @Resource
    private AppConfig appConfig;
    @Resource
    private MqListenerHelper mqListenerHelper;

    @RabbitListener(queues = RabbitMQConfig.PAY_TIMEOUT_DEAD_QUEUE, ackMode = "MANUAL")
    public void handlePayTimeoutOrder(PayOrderMessageDTO message, Channel channel, Message mqMessage) throws IOException {
        Long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        if (message == null || StringTools.isEmpty(message.getOrderId())) {
            log.error("支付超时死信队列收到订单为空");
            channel.basicAck(deliveryTag, false);
            return;
        }
        if (!mqListenerHelper.tryBeginConsume(mqMessage, MqListenerHelper.CONSUME_IDEMPOTENCY_TTL_STANDARD_SECONDS)) {
            channel.basicAck(deliveryTag, false);
            return;
        }
        log.info("支付超时死信队列收到订单: {}", message.getOrderId());
        try {
            if (!orderInfoService.cancelUnpaidOrderForPayTimeout(message.getOrderId())) {
                mqListenerHelper.nackWithRetryOrDlq(channel, deliveryTag, mqMessage,
                        RabbitMQConfig.PAY_TIMEOUT_DEAD_QUEUE, message,
                        new IllegalStateException("关单未生效，稍后重试"));
                return;
            }
            mqListenerHelper.clearConsumeRetry(RabbitMQConfig.PAY_TIMEOUT_DEAD_QUEUE, mqMessage);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("支付超时关单失败 orderId={}", message.getOrderId(), e);
            mqListenerHelper.nackWithRetryOrDlq(channel, deliveryTag, mqMessage,
                    RabbitMQConfig.PAY_TIMEOUT_DEAD_QUEUE, message, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAY_CONFIRM_DEAD_QUEUE, ackMode = "MANUAL")
    public void handleConfirmOrder(PayOrderMessageDTO message, Channel channel, Message mqMessage) {
        Long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        try {
            if (message == null || StringTools.isEmpty(message.getOrderId())) {
                log.error("确认收货死信队列收到订单为空");
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (!mqListenerHelper.tryBeginConsume(mqMessage, MqListenerHelper.CONSUME_IDEMPOTENCY_TTL_STANDARD_SECONDS)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("确认收货死信队列收到订单: {}", message.getOrderId());
            RabbitMQPayOrderDeadListenerComponent proxy =
                    (RabbitMQPayOrderDeadListenerComponent) AopContext.currentProxy();
            proxy.processOrderConfirm(message, deliveryTag, channel, mqMessage);
        } catch (Exception e) {
            log.error("自动确认收货处理失败", e);
            try {
                if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                    mqListenerHelper.releaseConsume(mqMessage);
                    channel.basicNack(deliveryTag, false, false);
                }
            } catch (Exception ex) {
                log.error("兜底NACK失败", ex);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processOrderConfirm(PayOrderMessageDTO message, Long deliveryTag, Channel channel, Message mqMessage) {
        registerAckSync(deliveryTag, channel, mqMessage);
        boolean confirmed = orderInfoService.confirmOrderReceipt(null, message.getOrderId());
        if (confirmed) {
            OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderId(message.getOrderId());
            if (orderInfo != null && !StringTools.isEmpty(orderInfo.getUserId())) {
                orderInfoService.onOrderConfirmed(orderInfo.getUserId(), message.getOrderId());
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAY_LOGISTICS_DEAD_QUEUE, ackMode = "MANUAL")
    public void handleLogisticsOrder(PayOrderMessageDTO message, Channel channel, Message mqMessage) {
        Long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        try {
            if (message == null || StringTools.isEmpty(message.getOrderId())) {
                log.error("模拟物流死信队列收到订单为空");
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (!mqListenerHelper.tryBeginConsume(mqMessage, MqListenerHelper.CONSUME_IDEMPOTENCY_TTL_STANDARD_SECONDS)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("模拟物流死信队列收到订单: {}, step={}", message.getOrderId(), message.getLogisticsStep());
            RabbitMQPayOrderDeadListenerComponent proxy =
                    (RabbitMQPayOrderDeadListenerComponent) AopContext.currentProxy();
            proxy.processOrderLogistics(message, deliveryTag, channel, mqMessage);
        } catch (Exception e) {
            log.error("模拟物流处理失败", e);
            try {
                if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                    mqListenerHelper.releaseConsume(mqMessage);
                    channel.basicNack(deliveryTag, false, false);
                }
            } catch (Exception ex) {
                log.error("兜底NACK失败", ex);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processOrderLogistics(PayOrderMessageDTO message, Long deliveryTag, Channel channel, Message mqMessage) {
        final boolean[] sendConfirm = {false};
        final Integer[] nextLogisticsStep = {null};
        registerAckSync(deliveryTag, channel, mqMessage, () -> {
            if (sendConfirm[0]) {
                PayOrderMessageDTO confirmDto = new PayOrderMessageDTO(message.getOrderId());
                reliableMessageSender.sendMessage(
                        RabbitMQConfig.PAY_EXCHANGE,
                        RabbitMQConfig.PAY_CONFIRM_DELAY_KEY,
                        confirmDto,
                        MqIdempotencyKeys.payConfirm(message.getOrderId()),
                        MessageReliabilityLevelEnum.STANDARD);
            }
            if (nextLogisticsStep[0] != null) {
                PayOrderMessageDTO nextDto = new PayOrderMessageDTO(message.getOrderId());
                nextDto.setLogisticsStep(nextLogisticsStep[0]);
                reliableMessageSender.sendMessage(
                        RabbitMQConfig.PAY_EXCHANGE,
                        RabbitMQConfig.PAY_LOGISTICS_DELAY_KEY,
                        nextDto,
                        MqIdempotencyKeys.payLogistics(message.getOrderId(), nextLogisticsStep[0]),
                        MessageReliabilityLevelEnum.STANDARD);
            }
        });

        int step = message.getLogisticsStep() == null ? 0 : message.getLogisticsStep();
        String orderId = message.getOrderId();
        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderId(orderId);
        if (orderInfo == null) {
            return;
        }
        OrderLogisticsInfo orderLogisticsInfo = orderLogisticsInfoService.getOrderLogisticsInfoByOrderId(orderId);
        if (orderLogisticsInfo == null) {
            log.warn("物流信息不存在，orderId: {}", orderId);
            return;
        }

        OrderLogisticsInfoRecordQuery recordQuery = new OrderLogisticsInfoRecordQuery();
        recordQuery.setOrderId(orderId);
        Integer recordCount = orderLogisticsInfoRecordService.findCountByParam(recordQuery);
        int count = recordCount == null ? 0 : recordCount;
        if (count > step) {
            log.info("物流步骤幂等跳过 orderId={}, step={}, recordCount={}", orderId, step, count);
            scheduleNextIfNeeded(orderInfo, orderLogisticsInfo, count, sendConfirm, nextLogisticsStep);
            return;
        }
        if (count < step) {
            log.warn("物流步骤乱序 orderId={}, step={}, recordCount={}", orderId, step, count);
            return;
        }

        OrderLogisticsInfoRecord orderLogisticsInfoRecord = new OrderLogisticsInfoRecord();
        orderLogisticsInfoRecord.setOrderId(orderId);
        orderLogisticsInfoRecord.setRecordTime(StringTools.getCurrentDate());

        if (step == 0) {
            if (!Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PAID.getStatus())) {
                if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.SHIPPED.getStatus())
                        || Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PARTIALLY_REFUNDED.getStatus())
                        || Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.COMPLETED.getStatus())) {
                    scheduleNextIfNeeded(orderInfo, orderLogisticsInfo, count, sendConfirm, nextLogisticsStep);
                }
                return;
            }
            orderLogisticsInfoRecord.setRecordAddress(orderLogisticsInfo.getSenderAddress());
            orderLogisticsInfo.setLogisticsStatus(LogisticsStatusEnum.IN_TRANSIT.getStatus());
            orderLogisticsInfo.setLogisticsCompany("顺丰");
            orderLogisticsInfo.setLogisticsNo("SF" + StringTools.getRandomNumber(Constants.LENGTH_30));
            int transitioned = orderStateMachine.transition(
                    orderId, OrderStatusEnum.PAID, OrderStateEvent.SHIP);
            if (transitioned != 1) {
                return;
            }
            orderInfo.setOrderStatus(OrderStatusEnum.SHIPPED.getStatus());
            OrderLogisticsInfoQuery logisticsQuery = new OrderLogisticsInfoQuery();
            logisticsQuery.setOrderId(orderId);
            orderLogisticsInfoService.updateByParam(orderLogisticsInfo, logisticsQuery);
            sendConfirm[0] = true;
        } else if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.SHIPPED.getStatus())
                || Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PARTIALLY_REFUNDED.getStatus())
                || Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.COMPLETED.getStatus())) {
            orderLogisticsInfoRecord.setRecordAddress(RECORD_ADDRESSES[new Random().nextInt(RECORD_ADDRESSES.length)]);
        } else {
            return;
        }

        int maxStations = appConfig.getLogisticsSimulateMaxStations();
        int countAfter = count + 1;
        if (countAfter >= maxStations) {
            orderLogisticsInfo.setLogisticsStatus(LogisticsStatusEnum.DELIVERED.getStatus());
            orderLogisticsInfoRecord.setRecordAddress(orderLogisticsInfo.getReceiverAddress());
        } else {
            nextLogisticsStep[0] = step + 1;
        }

        OrderLogisticsInfoQuery updateQuery = new OrderLogisticsInfoQuery();
        updateQuery.setOrderId(orderId);
        orderLogisticsInfoService.updateByParam(orderLogisticsInfo, updateQuery);
        orderLogisticsInfoRecordService.add(orderLogisticsInfoRecord);
    }

    private void scheduleNextIfNeeded(OrderInfo orderInfo, OrderLogisticsInfo orderLogisticsInfo, int recordCount,
                                      boolean[] sendConfirm, Integer[] nextLogisticsStep) {
        if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PAID.getStatus())) {
            return;
        }
        int maxStations = appConfig.getLogisticsSimulateMaxStations();
        if (recordCount == 0) {
            return;
        }
        if (recordCount == 1 && Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.SHIPPED.getStatus())) {
            sendConfirm[0] = true;
        }
        if (recordCount < maxStations
                && !Objects.equals(orderLogisticsInfo.getLogisticsStatus(), LogisticsStatusEnum.DELIVERED.getStatus())) {
            nextLogisticsStep[0] = recordCount;
        }
    }

    private void registerAckSync(Long deliveryTag, Channel channel, Message mqMessage) {
        registerAckSync(deliveryTag, channel, mqMessage, null);
    }

    private void registerAckSync(Long deliveryTag, Channel channel, Message mqMessage, Runnable afterCommitExtra) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    if (afterCommitExtra != null) {
                        afterCommitExtra.run();
                    }
                    channel.basicAck(deliveryTag, false);
                } catch (IOException e) {
                    log.error("ACK失败", e);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try {
                        mqListenerHelper.releaseConsume(mqMessage);
                        channel.basicNack(deliveryTag, false, false);
                    } catch (IOException e) {
                        log.error("NACK失败", e);
                    }
                }
            }
        });
    }
}
