package com.simlect.state;

import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.OrderInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStateMachineTest {

    @Mock
    private OrderInfoMapper<OrderInfo, OrderInfoQuery> orderInfoMapper;

    @InjectMocks
    private OrderStateMachine stateMachine;

    @Test
    void initializeAlwaysSetsWaitPayment() {
        OrderInfo order = new OrderInfo();
        order.setOrderStatus(OrderStatusEnum.COMPLETED.getStatus());

        stateMachine.initialize(order);

        assertEquals(OrderStatusEnum.WAIT_PAYMENT.getStatus(), order.getOrderStatus());
        assertThrows(IllegalArgumentException.class, () -> stateMachine.initialize(null));
    }

    @Test
    void transitionRulesExposeValidAndInvalidEdges() {
        assertTrue(stateMachine.canTransition(OrderStatusEnum.WAIT_PAYMENT.getStatus(), OrderStateEvent.PAY_SUCCESS));
        assertEquals(OrderStatusEnum.PAID,
                stateMachine.target(OrderStatusEnum.WAIT_PAYMENT.getStatus(), OrderStateEvent.PAY_SUCCESS));
        assertEquals(OrderStatusEnum.COMPLETED,
                stateMachine.target(OrderStatusEnum.WAIT_PAYMENT.getStatus(),
                        OrderStateEvent.COUPON_RUSH_PAY_SUCCESS));
        assertTrue(stateMachine.canTransition(OrderStatusEnum.PAID.getStatus(), OrderStateEvent.SHIP));
        assertFalse(stateMachine.canTransition(OrderStatusEnum.COMPLETED.getStatus(), OrderStateEvent.SHIP));
        assertThrows(BusinessException.class,
                () -> stateMachine.target(OrderStatusEnum.COMPLETED.getStatus(), OrderStateEvent.PAY_SUCCESS));
        assertThrows(BusinessException.class, () -> stateMachine.target(999, OrderStateEvent.PAY_SUCCESS));
    }

    @Test
    void transitionWithPatchAtomicallyAppliesTargetAndExpectedState() {
        OrderInfo patch = new OrderInfo();
        patch.setChannelOrderId("CHANNEL-1");
        when(orderInfoMapper.updateByParam(any(), any())).thenReturn(1);

        int changed = stateMachine.transition("ORDER-1", OrderStatusEnum.WAIT_PAYMENT,
                OrderStateEvent.PAY_SUCCESS, patch);

        assertEquals(1, changed);
        ArgumentCaptor<OrderInfo> updateCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        ArgumentCaptor<OrderInfoQuery> queryCaptor = ArgumentCaptor.forClass(OrderInfoQuery.class);
        verify(orderInfoMapper).updateByParam(updateCaptor.capture(), queryCaptor.capture());
        assertEquals(OrderStatusEnum.PAID.getStatus(), updateCaptor.getValue().getOrderStatus());
        assertEquals("CHANNEL-1", updateCaptor.getValue().getChannelOrderId());
        assertEquals("ORDER-1", queryCaptor.getValue().getOrderId());
        assertEquals(OrderStatusEnum.WAIT_PAYMENT.getStatus(), queryCaptor.getValue().getOrderStatus());
    }

    @Test
    void transitionFromMultipleRefundableStatesUsesStatusList() {
        when(orderInfoMapper.updateByParam(any(), any())).thenReturn(1);

        int changed = stateMachine.transition("ORDER-2",
                Set.of(OrderStatusEnum.PAID, OrderStatusEnum.SHIPPED, OrderStatusEnum.PARTIALLY_REFUNDED),
                OrderStateEvent.FULL_REFUND, null);

        assertEquals(1, changed);
        ArgumentCaptor<OrderInfo> updateCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        ArgumentCaptor<OrderInfoQuery> queryCaptor = ArgumentCaptor.forClass(OrderInfoQuery.class);
        verify(orderInfoMapper).updateByParam(updateCaptor.capture(), queryCaptor.capture());
        assertEquals(OrderStatusEnum.REFUNDED.getStatus(), updateCaptor.getValue().getOrderStatus());
        assertArrayEquals(
                Set.of(OrderStatusEnum.PAID.getStatus(), OrderStatusEnum.SHIPPED.getStatus(),
                                OrderStatusEnum.PARTIALLY_REFUNDED.getStatus()).stream().sorted().toArray(Integer[]::new),
                java.util.Arrays.stream(queryCaptor.getValue().getOrderStatusList()).sorted().toArray(Integer[]::new));
    }

    @Test
    void transitionPayOrderUsesPayOrderIdAndExpectedState() {
        when(orderInfoMapper.updateByParam(any(), any())).thenReturn(1);

        int changed = stateMachine.transitionPayOrder("PAY-1", OrderStatusEnum.WAIT_PAYMENT,
                OrderStateEvent.PAYMENT_TIMEOUT);

        assertEquals(1, changed);
        ArgumentCaptor<OrderInfo> updateCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        ArgumentCaptor<OrderInfoQuery> queryCaptor = ArgumentCaptor.forClass(OrderInfoQuery.class);
        verify(orderInfoMapper).updateByParam(updateCaptor.capture(), queryCaptor.capture());
        assertEquals(OrderStatusEnum.CLOSED.getStatus(), updateCaptor.getValue().getOrderStatus());
        assertEquals("PAY-1", queryCaptor.getValue().getPayOrderId());
        assertEquals(OrderStatusEnum.WAIT_PAYMENT.getStatus(), queryCaptor.getValue().getOrderStatus());
    }

    @Test
    void projectionStateAndInvalidArgumentsNeverReachMapper() {
        assertThrows(BusinessException.class,
                () -> stateMachine.transition("ORDER-3", OrderStatusEnum.WAIT_COMMENT,
                        OrderStateEvent.DELETE));
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.transition("", OrderStatusEnum.COMPLETED, OrderStateEvent.DELETE));
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.transitionPayOrder("", OrderStatusEnum.WAIT_PAYMENT,
                        OrderStateEvent.PAY_SUCCESS));
        verify(orderInfoMapper, never()).updateByParam(any(), any());
    }

    @Test
    void idempotentPartialRefundChecksCurrentStateWithoutBlindWrite() {
        OrderInfo current = new OrderInfo();
        current.setOrderStatus(OrderStatusEnum.PARTIALLY_REFUNDED.getStatus());
        when(orderInfoMapper.selectByOrderId("ORDER-4")).thenReturn(current);

        int changed = stateMachine.transition("ORDER-4", OrderStatusEnum.PARTIALLY_REFUNDED,
                OrderStateEvent.PARTIAL_REFUND);

        assertEquals(1, changed);
        verify(orderInfoMapper, never()).updateByParam(any(), any());
    }
}
