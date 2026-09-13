package com.simlect.component;

import com.rabbitmq.client.Channel;
import com.simlect.api.dto.PayOrderMessageDTO;
import com.simlect.api.enums.LogisticsStatusEnum;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderLogisticsInfoRecordService;
import com.simlect.biz.OrderLogisticsInfoService;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderLogisticsInfo;
import com.simlect.entity.po.OrderLogisticsInfoRecord;
import com.simlect.state.OrderStateEvent;
import com.simlect.state.OrderStateMachine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.aop.framework.AopContext;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RabbitMQPayOrderDeadListenerComponentTest {

    private static final String ORDER_ID = "O1";

    @Mock
    private OrderInfoService orderInfoService;
    @Mock
    private OrderLogisticsInfoService orderLogisticsInfoService;
    @Mock
    private OrderLogisticsInfoRecordService orderLogisticsInfoRecordService;
    @Mock
    private ReliableMessageSender reliableMessageSender;
    @Mock
    private AppConfig appConfig;
    @Mock
    private MqListenerHelper mqListenerHelper;
    @Mock
    private OrderStateMachine orderStateMachine;
    @Mock
    private Channel channel;

    @InjectMocks
    private RabbitMQPayOrderDeadListenerComponent component;

    private Message mqMessage(long deliveryTag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], props);
    }

    private PayOrderMessageDTO message(String orderId) {
        PayOrderMessageDTO dto = new PayOrderMessageDTO(orderId);
        dto.setLogisticsStep(0);
        return dto;
    }

    @BeforeEach
    void setUp() {
        when(appConfig.getLogisticsSimulateMaxStations()).thenReturn(5);
        when(mqListenerHelper.tryBeginConsume(any(), anyLong())).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // ==================== handlePayTimeoutOrder ====================

    @Test
    void handlePayTimeoutOrder_emptyMessage_acks() throws Exception {
        component.handlePayTimeoutOrder(null, channel, mqMessage(1L));
        verify(channel).basicAck(1L, false);
    }

    @Test
    void handlePayTimeoutOrder_emptyOrderId_acks() throws Exception {
        component.handlePayTimeoutOrder(new PayOrderMessageDTO(""), channel, mqMessage(2L));
        verify(channel).basicAck(2L, false);
    }

    @Test
    void handlePayTimeoutOrder_idempotent_acks() throws Exception {
        when(mqListenerHelper.tryBeginConsume(any(), anyLong())).thenReturn(false);
        component.handlePayTimeoutOrder(message(ORDER_ID), channel, mqMessage(3L));
        verify(channel).basicAck(3L, false);
        verify(orderInfoService, never()).cancelUnpaidOrderForPayTimeout(anyString());
    }

    @Test
    void handlePayTimeoutOrder_cancelSucceeds_acks() throws Exception {
        when(orderInfoService.cancelUnpaidOrderForPayTimeout(ORDER_ID)).thenReturn(true);
        component.handlePayTimeoutOrder(message(ORDER_ID), channel, mqMessage(4L));
        verify(mqListenerHelper).clearConsumeRetry(RabbitMQConfig.PAY_TIMEOUT_DEAD_QUEUE, mqMessage(4L));
        verify(channel).basicAck(4L, false);
    }

    @Test
    void handlePayTimeoutOrder_cancelIncomplete_nackWithRetry() throws Exception {
        when(orderInfoService.cancelUnpaidOrderForPayTimeout(ORDER_ID)).thenReturn(false);
        component.handlePayTimeoutOrder(message(ORDER_ID), channel, mqMessage(5L));
        verify(mqListenerHelper).nackWithRetryOrDlq(any(), anyLong(), any(), anyString(), any(), any(Exception.class));
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void handlePayTimeoutOrder_exception_nackWithRetry() throws Exception {
        when(orderInfoService.cancelUnpaidOrderForPayTimeout(ORDER_ID)).thenThrow(new RuntimeException("boom"));
        component.handlePayTimeoutOrder(message(ORDER_ID), channel, mqMessage(6L));
        verify(mqListenerHelper).nackWithRetryOrDlq(any(), anyLong(), any(), anyString(), any(), any(Exception.class));
    }

    // ==================== handleConfirmOrder ====================

    @Test
    void handleConfirmOrder_emptyMessage_acks() throws Exception {
        component.handleConfirmOrder(null, channel, mqMessage(1L));
        verify(channel).basicAck(1L, false);
    }

    @Test
    void handleConfirmOrder_idempotent_acks() throws Exception {
        when(mqListenerHelper.tryBeginConsume(any(), anyLong())).thenReturn(false);
        component.handleConfirmOrder(message(ORDER_ID), channel, mqMessage(2L));
        verify(channel).basicAck(2L, false);
    }

    @Test
    void handleConfirmOrder_confirmSucceeds_processOrderConfirm() throws Exception {
        when(orderInfoService.confirmOrderReceipt(null, ORDER_ID)).thenReturn(true);
        OrderInfo paid = new OrderInfo();
        paid.setUserId("U1");
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(paid);

        try (MockedStatic<AopContext> ctx = mockStatic(AopContext.class)) {
            ctx.when(AopContext::currentProxy).thenReturn(component);
            component.handleConfirmOrder(message(ORDER_ID), channel, mqMessage(3L));
        }

        verify(orderInfoService).confirmOrderReceipt(null, ORDER_ID);
        verify(orderInfoService).onOrderConfirmed("U1", ORDER_ID);
    }

    @Test
    void handleConfirmOrder_notConfirmed_noOnConfirmed() throws Exception {
        when(orderInfoService.confirmOrderReceipt(null, ORDER_ID)).thenReturn(false);
        try (MockedStatic<AopContext> ctx = mockStatic(AopContext.class)) {
            ctx.when(AopContext::currentProxy).thenReturn(component);
            component.handleConfirmOrder(message(ORDER_ID), channel, mqMessage(4L));
        }
        verify(orderInfoService, never()).onOrderConfirmed(anyString(), anyString());
    }

    // ==================== handleLogisticsOrder / processOrderLogistics ====================

    @Test
    void handleLogisticsOrder_emptyMessage_acks() throws Exception {
        component.handleLogisticsOrder(null, channel, mqMessage(1L));
        verify(channel).basicAck(1L, false);
    }

    @Test
    void handleLogisticsOrder_idempotent_acks() throws Exception {
        when(mqListenerHelper.tryBeginConsume(any(), anyLong())).thenReturn(false);
        component.handleLogisticsOrder(message(ORDER_ID), channel, mqMessage(2L));
        verify(channel).basicAck(2L, false);
    }

    @Test
    void processOrderLogistics_step0_paidShipments_andSendsConfirmOnCommit() throws Exception {
        OrderInfo paid = new OrderInfo();
        paid.setOrderId(ORDER_ID);
        paid.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(paid);
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        logistics.setSenderAddress("上海市浦东新区");
        logistics.setReceiverAddress("北京市朝阳区");
        when(orderLogisticsInfoService.getOrderLogisticsInfoByOrderId(ORDER_ID)).thenReturn(logistics);
        when(orderLogisticsInfoRecordService.findCountByParam(any())).thenReturn(0);
        when(orderStateMachine.transition(ORDER_ID, OrderStatusEnum.PAID, OrderStateEvent.SHIP)).thenReturn(1);
        when(orderLogisticsInfoService.updateByParam(any(), any())).thenReturn(1);
        when(orderLogisticsInfoRecordService.add(any())).thenReturn(1);

        component.processOrderLogistics(message(ORDER_ID), 10L, channel, mqMessage(10L));

        verify(orderStateMachine).transition(ORDER_ID, OrderStatusEnum.PAID, OrderStateEvent.SHIP);
        assertEquals(OrderStatusEnum.SHIPPED.getStatus(), paid.getOrderStatus());
        assertEquals(LogisticsStatusEnum.IN_TRANSIT.getStatus(), logistics.getLogisticsStatus());
        assertNotNull(logistics.getLogisticsNo());
        verify(orderLogisticsInfoRecordService).add(any(OrderLogisticsInfoRecord.class));

        runAfterCommit(10L);
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.PAY_EXCHANGE),
                eq(RabbitMQConfig.PAY_CONFIRM_DELAY_KEY), any(PayOrderMessageDTO.class), anyString(), any());
    }

    @Test
    void processOrderLogistics_step0_notPaid_noUpdate() throws Exception {
        OrderInfo wait = new OrderInfo();
        wait.setOrderId(ORDER_ID);
        wait.setOrderStatus(OrderStatusEnum.WAIT_PAYMENT.getStatus());
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(wait);
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        when(orderLogisticsInfoService.getOrderLogisticsInfoByOrderId(ORDER_ID)).thenReturn(logistics);
        when(orderLogisticsInfoRecordService.findCountByParam(any())).thenReturn(0);

        component.processOrderLogistics(message(ORDER_ID), 11L, channel, mqMessage(11L));

        verify(orderInfoService, never()).updateOrderInfoByOrderId(any(), anyString());
        verify(orderLogisticsInfoRecordService, never()).add(any());
    }

    @Test
    void processOrderLogistics_idempotentCountGreaterThanStep() throws Exception {
        OrderInfo paid = new OrderInfo();
        paid.setOrderId(ORDER_ID);
        paid.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(paid);
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        when(orderLogisticsInfoService.getOrderLogisticsInfoByOrderId(ORDER_ID)).thenReturn(logistics);
        when(orderLogisticsInfoRecordService.findCountByParam(any())).thenReturn(3);

        PayOrderMessageDTO dto = new PayOrderMessageDTO(ORDER_ID);
        dto.setLogisticsStep(0);
        component.processOrderLogistics(dto, 12L, channel, mqMessage(12L));

        verify(orderLogisticsInfoRecordService, never()).add(any());
    }

    @Test
    void processOrderLogistics_outOfOrderStep_skips() throws Exception {
        OrderInfo paid = new OrderInfo();
        paid.setOrderId(ORDER_ID);
        paid.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(paid);
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        when(orderLogisticsInfoService.getOrderLogisticsInfoByOrderId(ORDER_ID)).thenReturn(logistics);
        when(orderLogisticsInfoRecordService.findCountByParam(any())).thenReturn(1);

        PayOrderMessageDTO dto = new PayOrderMessageDTO(ORDER_ID);
        dto.setLogisticsStep(5);
        component.processOrderLogistics(dto, 13L, channel, mqMessage(13L));

        verify(orderLogisticsInfoRecordService, never()).add(any());
    }

    @Test
    void processOrderLogistics_shippedStep_schedulesNext() throws Exception {
        OrderInfo shipped = new OrderInfo();
        shipped.setOrderId(ORDER_ID);
        shipped.setOrderStatus(OrderStatusEnum.SHIPPED.getStatus());
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(shipped);
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        logistics.setReceiverAddress("北京市朝阳区");
        when(orderLogisticsInfoService.getOrderLogisticsInfoByOrderId(ORDER_ID)).thenReturn(logistics);
        when(orderLogisticsInfoRecordService.findCountByParam(any())).thenReturn(1);
        when(orderLogisticsInfoService.updateByParam(any(), any())).thenReturn(1);
        when(orderLogisticsInfoRecordService.add(any())).thenReturn(1);

        PayOrderMessageDTO dto = new PayOrderMessageDTO(ORDER_ID);
        dto.setLogisticsStep(1);
        component.processOrderLogistics(dto, 14L, channel, mqMessage(14L));

        verify(orderLogisticsInfoRecordService).add(any(OrderLogisticsInfoRecord.class));

        runAfterCommit(14L);
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.PAY_EXCHANGE),
                eq(RabbitMQConfig.PAY_LOGISTICS_DELAY_KEY), any(PayOrderMessageDTO.class), anyString(), any());
    }

    @Test
    void processOrderLogistics_lastStation_delivered_noNextStep() throws Exception {
        OrderInfo shipped = new OrderInfo();
        shipped.setOrderId(ORDER_ID);
        shipped.setOrderStatus(OrderStatusEnum.SHIPPED.getStatus());
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(shipped);
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        logistics.setReceiverAddress("北京市朝阳区");
        when(orderLogisticsInfoService.getOrderLogisticsInfoByOrderId(ORDER_ID)).thenReturn(logistics);
        when(orderLogisticsInfoRecordService.findCountByParam(any())).thenReturn(4);
        when(orderLogisticsInfoService.updateByParam(any(), any())).thenReturn(1);
        when(orderLogisticsInfoRecordService.add(any())).thenReturn(1);

        PayOrderMessageDTO dto = new PayOrderMessageDTO(ORDER_ID);
        dto.setLogisticsStep(4);
        component.processOrderLogistics(dto, 15L, channel, mqMessage(15L));

        assertEquals(LogisticsStatusEnum.DELIVERED.getStatus(), logistics.getLogisticsStatus());
        verify(orderLogisticsInfoRecordService).add(any(OrderLogisticsInfoRecord.class));
    }

    @Test
    void processOrderLogistics_noLogisticsInfo_skips() throws Exception {
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(new OrderInfo());
        when(orderLogisticsInfoService.getOrderLogisticsInfoByOrderId(ORDER_ID)).thenReturn(null);
        component.processOrderLogistics(message(ORDER_ID), 16L, channel, mqMessage(16L));
        verify(orderLogisticsInfoRecordService, never()).add(any());
    }

    private void runAfterCommit(long deliveryTag) {
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertNotNull(syncs);
        for (TransactionSynchronization s : syncs) {
            s.afterCommit();
        }
    }
}
