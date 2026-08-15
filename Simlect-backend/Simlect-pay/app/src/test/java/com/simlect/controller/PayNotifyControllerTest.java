package com.simlect.controller;

import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.support.OrderFeignSupport;
import com.simlect.biz.PayChannel;
import com.simlect.component.RedisComponent;
import com.simlect.component.SpringContext;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PayNotifyController 支付宝异步回调单元测试（MockMvc standalone + mock SpringContext 渠道解析）。
 * 覆盖：验签失败返回 failure、重复回调返回 success、业务异常返回 failure、成功回调入账。
 */
@ExtendWith(MockitoExtension.class)
class PayNotifyControllerTest {

    private static final long LOCK_WAIT_MS = 3_000L;
    private static final String PAY_ORDER_ID = "PO20260815001";

    @Mock
    private OrderFeignSupport orderFeignSupport;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private PayChannel payChannel;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PayNotifyController controller = new PayNotifyController();
        ReflectionTestUtils.setField(controller, "orderFeignSupport", orderFeignSupport);
        ReflectionTestUtils.setField(controller, "redisComponent", redisComponent);
        ReflectionTestUtils.setField(controller, "redissonClient", redissonClient);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown() {
        Thread.interrupted();
    }

    @Test
    void alipayNotify_signCheckFailure_returnsFailure() throws Exception {
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(payChannel);
            when(payChannel.payNotify(anyMap(), isNull())).thenThrow(new BusinessException("支付宝回调校验失败"));

            mockMvc.perform(post("/notify/alipayNotify")
                            .param("out_trade_no", PAY_ORDER_ID)
                            .param("trade_status", "TRADE_SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("failure"));

            verify(redissonClient, never()).getLock(anyString());
            verify(orderFeignSupport, never()).paySuccess(any());
        }
    }

    @Test
    void alipayNotify_notifyNull_returnsSuccess() throws Exception {
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(payChannel);
            when(payChannel.payNotify(anyMap(), isNull())).thenReturn(null);

            mockMvc.perform(post("/notify/alipayNotify")
                            .param("out_trade_no", PAY_ORDER_ID)
                            .param("trade_status", "WAIT_BUYER_PAY"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("success"));

            verify(redissonClient, never()).getLock(anyString());
            verify(orderFeignSupport, never()).paySuccess(any());
        }
    }

    @Test
    void alipayNotify_payOrderIdNull_returnsSuccess() throws Exception {
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(payChannel);
            when(payChannel.payNotify(anyMap(), isNull())).thenReturn(new PayOrderNotifyDTO(null, "CH1"));

            mockMvc.perform(post("/notify/alipayNotify")
                            .param("out_trade_no", PAY_ORDER_ID)
                            .param("trade_status", "TRADE_SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("success"));

            verify(redissonClient, never()).getLock(anyString());
        }
    }

    @Test
    void alipayNotify_duplicateCallback_lockBusy_returnsSuccessAndIgnores() throws Exception {
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(payChannel);
            when(payChannel.payNotify(anyMap(), isNull()))
                    .thenReturn(new PayOrderNotifyDTO(PAY_ORDER_ID, "CH1"));
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS)).thenReturn(false);

            mockMvc.perform(post("/notify/alipayNotify")
                            .param("out_trade_no", PAY_ORDER_ID)
                            .param("trade_status", "TRADE_SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("success"));

            verify(orderFeignSupport, never()).paySuccess(any());
            verify(lock, never()).unlock();
        }
    }

    @Test
    void alipayNotify_success_paysOrderAndUnlocks() throws Exception {
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(payChannel);
            when(payChannel.payNotify(anyMap(), isNull()))
                    .thenReturn(new PayOrderNotifyDTO(PAY_ORDER_ID, "CH1"));
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS)).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(true);

            mockMvc.perform(post("/notify/alipayNotify")
                            .param("out_trade_no", PAY_ORDER_ID)
                            .param("trade_status", "TRADE_SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("success"));

            verify(orderFeignSupport).paySuccess(any(PayOrderNotifyDTO.class));
            verify(lock).unlock();
        }
    }

    @Test
    void alipayNotify_businessException_returnsFailureAndUnlocks() throws Exception {
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(payChannel);
            when(payChannel.payNotify(anyMap(), isNull()))
                    .thenReturn(new PayOrderNotifyDTO(PAY_ORDER_ID, "CH1"));
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS)).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(true);
            doThrow(new BusinessException("订单状态更新失败"))
                    .when(orderFeignSupport).paySuccess(any());

            mockMvc.perform(post("/notify/alipayNotify")
                            .param("out_trade_no", PAY_ORDER_ID)
                            .param("trade_status", "TRADE_SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("failure"));

            verify(lock).unlock();
        }
    }

    @Test
    void alipayNotify_genericException_returnsFailure() throws Exception {
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(payChannel);
            when(payChannel.payNotify(anyMap(), isNull()))
                    .thenReturn(new PayOrderNotifyDTO(PAY_ORDER_ID, "CH1"));
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS)).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(true);
            doThrow(new IllegalStateException("boom")).when(orderFeignSupport).paySuccess(any());

            mockMvc.perform(post("/notify/alipayNotify")
                            .param("out_trade_no", PAY_ORDER_ID)
                            .param("trade_status", "TRADE_SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("failure"));

            verify(lock).unlock();
        }
    }

    @Test
    void alipayNotify_interrupted_returnsFailureAndRestoresInterruptFlag() throws Exception {
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(payChannel);
            when(payChannel.payNotify(anyMap(), isNull()))
                    .thenReturn(new PayOrderNotifyDTO(PAY_ORDER_ID, "CH1"));
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS))
                    .thenThrow(new InterruptedException());

            mockMvc.perform(post("/notify/alipayNotify")
                            .param("out_trade_no", PAY_ORDER_ID)
                            .param("trade_status", "TRADE_SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("failure"));

            org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted());
            verify(lock, never()).unlock();
        }
    }

    @Test
    void alipayNotify_lockLostBeforeUnlock_skipsUnlock() throws Exception {
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(payChannel);
            when(payChannel.payNotify(anyMap(), isNull()))
                    .thenReturn(new PayOrderNotifyDTO(PAY_ORDER_ID, "CH1"));
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS)).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(false);

            mockMvc.perform(post("/notify/alipayNotify")
                            .param("out_trade_no", PAY_ORDER_ID)
                            .param("trade_status", "TRADE_SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("success"));

            verify(orderFeignSupport).paySuccess(any());
            verify(lock, never()).unlock();
        }
    }
}
