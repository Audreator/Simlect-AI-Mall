package com.simlect.task;

import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.api.enums.PayChannelEnum;
import com.simlect.api.support.PayFeignSupport;
import com.simlect.biz.OrderInfoService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.entity.vo.PaginationResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayOrderTaskTest {

    private static final String PAY_ORDER_ID = "PO1";

    @Mock
    private AppConfig appConfig;
    @Mock
    private OrderInfoService orderInfoService;
    @Mock
    private PayFeignSupport payFeignSupport;
    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private PayOrderTask task;

    private OrderInfo waitPayOrder() {
        OrderInfo o = new OrderInfo();
        o.setOrderId("O1");
        o.setPayOrderId(PAY_ORDER_ID);
        o.setOrderStatus(OrderStatusEnum.WAIT_PAYMENT.getStatus());
        o.setPayChannel(PayChannelEnum.ALIPAY_PC.getPayChannel());
        return o;
    }

    private void lockAcquired() {
        when(redisComponent.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    /** 第 1 页返回给定订单，第 2/3 页返回空 — 模拟真实分页终止循环 */
    private void pageOne(List<OrderInfo> firstPage) {
        when(orderInfoService.findListByPage(any(OrderInfoQuery.class)))
                .thenReturn(pageResult(firstPage))
                .thenReturn(pageResult(List.of()));
    }

    @Test
    void pollPayOrders_autoCheckpayDisabled_shortCircuit() {
        when(appConfig.getAutoCheckpay()).thenReturn(false);
        task.pollPayOrders();
        verify(orderInfoService, never()).findListByPage(any(OrderInfoQuery.class));
        verify(redisComponent, never()).setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void pollPayOrders_lockBusy_shortCircuit() {
        when(appConfig.getAutoCheckpay()).thenReturn(true);
        when(redisComponent.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);
        task.pollPayOrders();
        verify(orderInfoService, never()).findListByPage(any(OrderInfoQuery.class));
    }

    @Test
    void pollPayOrders_noOrders_nothingHappens() {
        when(appConfig.getAutoCheckpay()).thenReturn(true);
        lockAcquired();
        when(orderInfoService.findListByPage(any(OrderInfoQuery.class))).thenReturn(pageResult(List.of()));
        task.pollPayOrders();
        verify(redisComponent, never()).isPayTradeInitiated(anyString());
    }

    @Test
    void pollPayOrders_emptyPayOrderId_skipped() {
        when(appConfig.getAutoCheckpay()).thenReturn(true);
        lockAcquired();
        OrderInfo o = waitPayOrder();
        o.setPayOrderId("");
        pageOne(List.of(o));
        task.pollPayOrders();
        verify(redisComponent, never()).isPayTradeInitiated(anyString());
    }

    @Test
    void pollPayOrders_notInitiated_skipped() {
        when(appConfig.getAutoCheckpay()).thenReturn(true);
        lockAcquired();
        pageOne(List.of(waitPayOrder()));
        when(redisComponent.isPayTradeInitiated(PAY_ORDER_ID)).thenReturn(false);
        task.pollPayOrders();
        verify(payFeignSupport, never()).queryOrder(anyString(), anyString());
    }

    @Test
    void pollPayOrders_unresolvedChannel_skipped() {
        when(appConfig.getAutoCheckpay()).thenReturn(true);
        lockAcquired();
        OrderInfo o = waitPayOrder();
        o.setPayChannel("wechat");
        pageOne(List.of(o));
        when(redisComponent.isPayTradeInitiated(PAY_ORDER_ID)).thenReturn(true);
        task.pollPayOrders();
        verify(payFeignSupport, never()).queryOrder(anyString(), anyString());
    }

    @Test
    void pollPayOrders_queryReturnsNull_skipped() {
        when(appConfig.getAutoCheckpay()).thenReturn(true);
        lockAcquired();
        pageOne(List.of(waitPayOrder()));
        when(redisComponent.isPayTradeInitiated(PAY_ORDER_ID)).thenReturn(true);
        when(payFeignSupport.queryOrder(PAY_ORDER_ID, PayChannelEnum.ALIPAY_PC.getPayScene())).thenReturn(null);
        task.pollPayOrders();
        verify(orderInfoService, never()).paySuccess(any(PayOrderNotifyDTO.class));
    }

    @Test
    void pollPayOrders_querySuccess_callsPaySuccess() {
        when(appConfig.getAutoCheckpay()).thenReturn(true);
        lockAcquired();
        pageOne(List.of(waitPayOrder()));
        when(redisComponent.isPayTradeInitiated(PAY_ORDER_ID)).thenReturn(true);
        PayOrderNotifyDTO dto = new PayOrderNotifyDTO(PAY_ORDER_ID, "CH1");
        when(payFeignSupport.queryOrder(PAY_ORDER_ID, PayChannelEnum.ALIPAY_PC.getPayScene())).thenReturn(dto);
        task.pollPayOrders();
        verify(orderInfoService).paySuccess(dto);
    }

    @Test
    void pollPayOrders_multipleOrders_eachPolled() {
        when(appConfig.getAutoCheckpay()).thenReturn(true);
        lockAcquired();
        OrderInfo o1 = waitPayOrder();
        OrderInfo o2 = waitPayOrder();
        o2.setOrderId("O2");
        pageOne(List.of(o1, o2));
        when(redisComponent.isPayTradeInitiated(PAY_ORDER_ID)).thenReturn(true);
        when(payFeignSupport.queryOrder(PAY_ORDER_ID, PayChannelEnum.ALIPAY_PC.getPayScene()))
                .thenReturn(new PayOrderNotifyDTO(PAY_ORDER_ID, "CH1"));
        task.pollPayOrders();
        verify(orderInfoService, times(2)).paySuccess(any(PayOrderNotifyDTO.class));
    }

    @Test
    void pollPayOrders_perItemException_isIsolated() {
        when(appConfig.getAutoCheckpay()).thenReturn(true);
        lockAcquired();
        pageOne(List.of(waitPayOrder(), waitPayOrder()));
        when(redisComponent.isPayTradeInitiated(anyString()))
                .thenThrow(new RuntimeException("redis down"))
                .thenReturn(true);
        when(payFeignSupport.queryOrder(anyString(), anyString()))
                .thenReturn(new PayOrderNotifyDTO("PO-1", "CH-1"));

        task.pollPayOrders();

        verify(orderInfoService, org.mockito.Mockito.times(1)).paySuccess(any(PayOrderNotifyDTO.class));
    }
    private PaginationResultVO<com.simlect.entity.po.OrderInfo> pageResult(
            java.util.List<com.simlect.entity.po.OrderInfo> list) {
        return new PaginationResultVO<>(list.size(), list.size(), 1, 1, list);
    }
}
