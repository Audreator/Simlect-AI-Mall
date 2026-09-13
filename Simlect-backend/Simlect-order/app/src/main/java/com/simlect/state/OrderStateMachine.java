package com.simlect.state;

import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.OrderInfoMapper;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机。状态规则与带旧状态条件的持久化更新都只能从这里发起。
 */
@Component
public class OrderStateMachine {

    private static final Map<OrderStatusEnum, Map<OrderStateEvent, OrderStatusEnum>> TRANSITIONS = buildTransitions();

    @Resource
    private OrderInfoMapper<OrderInfo, OrderInfoQuery> orderInfoMapper;

    public void initialize(OrderInfo orderInfo) {
        if (orderInfo == null) {
            throw new IllegalArgumentException("订单不能为空");
        }
        orderInfo.setOrderStatus(OrderStatusEnum.WAIT_PAYMENT.getStatus());
    }

    public boolean canTransition(Integer currentStatus, OrderStateEvent event) {
        OrderStatusEnum current = OrderStatusEnum.getByStatus(currentStatus);
        return current != null && event != null
                && TRANSITIONS.getOrDefault(current, Map.of()).containsKey(event);
    }

    public OrderStatusEnum target(Integer currentStatus, OrderStateEvent event) {
        OrderStatusEnum current = OrderStatusEnum.getByStatus(currentStatus);
        if (current == null || event == null) {
            throw new BusinessException("未知订单状态或状态事件");
        }
        OrderStatusEnum target = TRANSITIONS.getOrDefault(current, Map.of()).get(event);
        if (target == null) {
            throw new BusinessException("订单状态不允许执行该操作: " + current.getDesc() + " -> " + event);
        }
        return target;
    }

    public int transition(String orderId, OrderStatusEnum expectedState, OrderStateEvent event) {
        return transition(orderId, Set.of(expectedState), event, null);
    }

    public int transition(String orderId, OrderStatusEnum expectedState,
                          OrderStateEvent event, OrderInfo patch) {
        return transition(orderId, Set.of(expectedState), event, patch);
    }

    public int transition(String orderId, Collection<OrderStatusEnum> expectedStates,
                          OrderStateEvent event, OrderInfo patch) {
        if (StringTools.isEmpty(orderId) || expectedStates == null || expectedStates.isEmpty()) {
            throw new IllegalArgumentException("订单ID和前置状态不能为空");
        }
        OrderStatusEnum target = resolveCommonTarget(expectedStates, event);
        if (expectedStates.size() == 1 && expectedStates.iterator().next() == target && patch == null) {
            OrderInfo current = orderInfoMapper.selectByOrderId(orderId);
            return current != null && target.getStatus().equals(current.getOrderStatus()) ? 1 : 0;
        }
        OrderInfo update = patch == null ? new OrderInfo() : patch;
        update.setOrderStatus(target.getStatus());
        OrderInfoQuery query = new OrderInfoQuery();
        query.setOrderId(orderId);
        applyExpectedStates(query, expectedStates);
        return nullToZero(orderInfoMapper.updateByParam(update, query));
    }

    public int transitionPayOrder(String payOrderId, OrderStatusEnum expectedState, OrderStateEvent event) {
        if (StringTools.isEmpty(payOrderId)) {
            throw new IllegalArgumentException("支付订单ID不能为空");
        }
        OrderStatusEnum target = target(expectedState.getStatus(), event);
        OrderInfo update = new OrderInfo();
        update.setOrderStatus(target.getStatus());
        OrderInfoQuery query = new OrderInfoQuery();
        query.setPayOrderId(payOrderId);
        query.setOrderStatus(expectedState.getStatus());
        return nullToZero(orderInfoMapper.updateByParam(update, query));
    }

    private OrderStatusEnum resolveCommonTarget(Collection<OrderStatusEnum> expectedStates, OrderStateEvent event) {
        OrderStatusEnum commonTarget = null;
        for (OrderStatusEnum state : expectedStates) {
            if (state == null || state == OrderStatusEnum.WAIT_COMMENT) {
                throw new BusinessException("查询投影状态不能参与订单流转");
            }
            OrderStatusEnum candidate = target(state.getStatus(), event);
            if (commonTarget != null && commonTarget != candidate) {
                throw new IllegalArgumentException("多个前置状态没有共同目标状态");
            }
            commonTarget = candidate;
        }
        return commonTarget;
    }

    private void applyExpectedStates(OrderInfoQuery query, Collection<OrderStatusEnum> expectedStates) {
        if (expectedStates.size() == 1) {
            query.setOrderStatus(expectedStates.iterator().next().getStatus());
            return;
        }
        query.setOrderStatusList(expectedStates.stream().map(OrderStatusEnum::getStatus).toArray(Integer[]::new));
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static Map<OrderStatusEnum, Map<OrderStateEvent, OrderStatusEnum>> buildTransitions() {
        Map<OrderStatusEnum, Map<OrderStateEvent, OrderStatusEnum>> transitions =
                new EnumMap<>(OrderStatusEnum.class);
        register(transitions, OrderStatusEnum.WAIT_PAYMENT, OrderStateEvent.PAY_SUCCESS, OrderStatusEnum.PAID);
        register(transitions, OrderStatusEnum.WAIT_PAYMENT, OrderStateEvent.COUPON_RUSH_PAY_SUCCESS,
                OrderStatusEnum.COMPLETED);
        register(transitions, OrderStatusEnum.WAIT_PAYMENT, OrderStateEvent.USER_CANCEL, OrderStatusEnum.CANCELLED);
        register(transitions, OrderStatusEnum.WAIT_PAYMENT, OrderStateEvent.PAYMENT_TIMEOUT, OrderStatusEnum.CLOSED);
        register(transitions, OrderStatusEnum.PAID, OrderStateEvent.SHIP, OrderStatusEnum.SHIPPED);
        for (OrderStatusEnum refundable : Set.of(
                OrderStatusEnum.PAID, OrderStatusEnum.SHIPPED, OrderStatusEnum.PARTIALLY_REFUNDED)) {
            register(transitions, refundable, OrderStateEvent.PARTIAL_REFUND,
                    OrderStatusEnum.PARTIALLY_REFUNDED);
            register(transitions, refundable, OrderStateEvent.FULL_REFUND, OrderStatusEnum.REFUNDED);
        }
        for (OrderStatusEnum receivable : Set.of(
                OrderStatusEnum.SHIPPED, OrderStatusEnum.PARTIALLY_REFUNDED)) {
            register(transitions, receivable, OrderStateEvent.CONFIRM_RECEIPT, OrderStatusEnum.COMPLETED);
        }
        for (OrderStatusEnum removable : Set.of(
                OrderStatusEnum.COMPLETED, OrderStatusEnum.CANCELLED,
                OrderStatusEnum.CLOSED, OrderStatusEnum.REFUNDED)) {
            register(transitions, removable, OrderStateEvent.DELETE, OrderStatusEnum.DELETE);
        }
        return transitions;
    }

    private static void register(Map<OrderStatusEnum, Map<OrderStateEvent, OrderStatusEnum>> transitions,
                                 OrderStatusEnum from, OrderStateEvent event, OrderStatusEnum to) {
        transitions.computeIfAbsent(from, ignored -> new EnumMap<>(OrderStateEvent.class)).put(event, to);
    }
}
