package com.simlect.biz.impl;

import com.simlect.api.dto.PayOrderMessageDTO;
import com.simlect.api.enums.LogisticsStatusEnum;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.api.support.UserFeignSupport;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderLogisticsInfoRecordService;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderLogisticsInfo;
import com.simlect.entity.po.OrderLogisticsInfoRecord;
import com.simlect.entity.query.OrderLogisticsInfoQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.OrderLogisticsInfoMapper;
import com.simlect.state.OrderStateEvent;
import com.simlect.state.OrderStateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLogisticsInfoServiceImplTest {

    private static final String ORDER_ID = "O1";
    private static final String USER_ID = "U1";

    @Mock
    private OrderLogisticsInfoMapper<OrderLogisticsInfo, OrderLogisticsInfoQuery> orderLogisticsInfoMapper;
    @Mock
    private OrderLogisticsInfoRecordService orderLogisticsInfoRecordService;
    @Mock
    private OrderInfoService orderInfoService;
    @Mock
    private UserFeignSupport userFeignSupport;
    @Mock
    private ReliableMessageSender reliableMessageSender;
    @Mock
    private OrderStateMachine orderStateMachine;

    @InjectMocks
    private OrderLogisticsInfoServiceImpl service;

    @Test
    void getOrderLogisticsRecords_success_withRecords() {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        logistics.setUserId(USER_ID);
        when(orderLogisticsInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(logistics);
        when(orderLogisticsInfoRecordService.findListByParam(any()))
                .thenReturn(List.of(new OrderLogisticsInfoRecord()));

        OrderLogisticsInfo result = service.getOrderLogisticsRecords(USER_ID, ORDER_ID);

        assertSame(logistics, result);
        assertEquals(1, result.getRecordList().size());
    }

    @Test
    void getOrderLogisticsRecords_notExists_throws() {
        when(orderLogisticsInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getOrderLogisticsRecords(USER_ID, ORDER_ID));
    }

    @Test
    void getOrderLogisticsRecords_wrongUser_throws() {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        logistics.setUserId(USER_ID);
        when(orderLogisticsInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(logistics);
        assertThrows(BusinessException.class, () -> service.getOrderLogisticsRecords("U999", ORDER_ID));
    }

    @Test
    void delivery_success_sendsConfirmMessage_andNotify() {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        logistics.setSenderAddress("上海市浦东新区");
        logistics.setLogisticsNo("SF123");
        when(orderLogisticsInfoMapper.updateByParam(any(), any())).thenReturn(1);
        when(orderStateMachine.transition(ORDER_ID, OrderStatusEnum.PAID, OrderStateEvent.SHIP)).thenReturn(1);
        when(orderLogisticsInfoRecordService.add(any())).thenReturn(1);
        OrderInfo shipped = new OrderInfo();
        shipped.setUserId(USER_ID);
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(shipped);

        service.delivery(logistics);

        assertEquals(LogisticsStatusEnum.IN_TRANSIT.getStatus(), logistics.getLogisticsStatus());
        verify(orderLogisticsInfoRecordService).add(any(OrderLogisticsInfoRecord.class));
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.PAY_EXCHANGE),
                eq(RabbitMQConfig.PAY_CONFIRM_DELAY_KEY), any(PayOrderMessageDTO.class), anyString(),
                eq(MessageReliabilityLevelEnum.STANDARD));
        verify(userFeignSupport).sendNotifyAsync(eq(USER_ID), anyString(), anyString(), eq("logistics"), eq(ORDER_ID));
    }

    @Test
    void delivery_logisticsUpdateFails_throws() {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        when(orderLogisticsInfoMapper.updateByParam(any(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delivery(logistics));
        assertEquals("该订单已经发货过了", ex.getMessage());
        verify(orderStateMachine, never()).transition(anyString(), any(), any());
    }

    @Test
    void delivery_orderStatusUpdateFails_throws() {
        OrderLogisticsInfo logistics = new OrderLogisticsInfo();
        logistics.setOrderId(ORDER_ID);
        when(orderLogisticsInfoMapper.updateByParam(any(), any())).thenReturn(1);
        when(orderStateMachine.transition(ORDER_ID, OrderStatusEnum.PAID, OrderStateEvent.SHIP)).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.delivery(logistics));

        verify(reliableMessageSender, never()).sendMessage(anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void crud_delegates() {
        when(orderLogisticsInfoMapper.selectList(any(OrderLogisticsInfoQuery.class))).thenReturn(List.of(new OrderLogisticsInfo()));
        when(orderLogisticsInfoMapper.selectCount(any(OrderLogisticsInfoQuery.class))).thenReturn(2);
        when(orderLogisticsInfoMapper.insert(any())).thenReturn(1);
        when(orderLogisticsInfoMapper.insertBatch(any())).thenReturn(2);
        when(orderLogisticsInfoMapper.insertOrUpdateBatch(any())).thenReturn(3);
        when(orderLogisticsInfoMapper.updateByParam(any(), any())).thenReturn(4);
        when(orderLogisticsInfoMapper.deleteByParam(any())).thenReturn(5);
        when(orderLogisticsInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(new OrderLogisticsInfo());
        when(orderLogisticsInfoMapper.updateByOrderId(any(), anyString())).thenReturn(6);
        when(orderLogisticsInfoMapper.deleteByOrderId(anyString())).thenReturn(7);
        OrderLogisticsInfo byId = new OrderLogisticsInfo();
        when(orderLogisticsInfoMapper.selectByOrderId(ORDER_ID)).thenReturn(byId);

        assertEquals(1, service.findListByParam(new OrderLogisticsInfoQuery()).size());
        assertEquals(2, service.findCountByParam(new OrderLogisticsInfoQuery()));
        assertEquals(1, service.add(new OrderLogisticsInfo()));
        assertEquals(2, service.addBatch(List.of(new OrderLogisticsInfo())));
        assertEquals(0, service.addBatch(List.of()));
        assertEquals(3, service.addOrUpdateBatch(List.of(new OrderLogisticsInfo())));
        OrderLogisticsInfoQuery updateQuery = new OrderLogisticsInfoQuery();
        updateQuery.setOrderId(ORDER_ID);
        assertEquals(4, service.updateByParam(new OrderLogisticsInfo(), updateQuery));
        OrderLogisticsInfoQuery deleteQuery = new OrderLogisticsInfoQuery();
        deleteQuery.setOrderId(ORDER_ID);
        assertEquals(5, service.deleteByParam(deleteQuery));
        assertSame(byId, service.getOrderLogisticsInfoByOrderId(ORDER_ID));
        assertEquals(6, service.updateOrderLogisticsInfoByOrderId(new OrderLogisticsInfo(), ORDER_ID));
        assertEquals(7, service.deleteOrderLogisticsInfoByOrderId(ORDER_ID));
    }
}
