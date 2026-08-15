package com.simlect.biz;

import com.simlect.api.dto.PayCloseDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.dto.PayQueryDTO;
import com.simlect.api.dto.PayRefundDTO;
import com.simlect.api.dto.PayTradeCreateDTO;
import com.simlect.api.dto.PayTradeStatusDTO;
import com.simlect.api.dto.PayUrlRequestDTO;
import com.simlect.component.SpringContext;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PayInternalService 内部支付编排单元测试（mock SpringContext 静态渠道解析）。
 */
@ExtendWith(MockitoExtension.class)
class PayInternalServiceTest {

    @Mock
    private PayTradeRecordService payTradeRecordService;

    @InjectMocks
    private PayInternalService payInternalService;

    @Test
    void createPending_delegates() {
        PayTradeCreateDTO dto = new PayTradeCreateDTO("U1", "PO1", "O1", new BigDecimal("10.00"), "alipay");

        payInternalService.createPending(dto);

        verify(payTradeRecordService).createPending("U1", "PO1", "O1", new BigDecimal("10.00"), "alipay");
    }

    @Test
    void markSuccess_delegates() {
        PayTradeStatusDTO dto = new PayTradeStatusDTO("PO1", "CH1");
        payInternalService.markSuccess(dto);
        verify(payTradeRecordService).markSuccess("PO1", "CH1");

        PayTradeStatusDTO close = new PayTradeStatusDTO("PO2");
        payInternalService.markClosed(close);
        verify(payTradeRecordService).markClosed("PO2");

        payInternalService.markRefunded(close);
        verify(payTradeRecordService).markRefunded("PO2");
    }

    @Test
    void getPayUrl_resolvesChannelAndDelegates() {
        PayChannel channel = mock(PayChannel.class);
        PayInfoDTO expected = new PayInfoDTO("html", "PO1", new BigDecimal("0.01"));
        PayUrlRequestDTO dto = new PayUrlRequestDTO("alipay", "PO1", "subject", new BigDecimal("0.01"));
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(channel);
            when(channel.getPayUrl(any(), any(), any(), any())).thenReturn(expected);

            PayInfoDTO result = payInternalService.getPayUrl(dto);

            assertSame(expected, result);
            verify(channel).getPayUrl(any(), any(), any(), any());
        }
    }

    @Test
    void getPayUrl_sceneValue_resolvesToPcBean() {
        PayChannel channel = mock(PayChannel.class);
        PayUrlRequestDTO dto = new PayUrlRequestDTO("alipay_wap", "PO1", "subject", new BigDecimal("0.01"));
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(channel);
            when(channel.getPayUrl(any(), any(), any(), any()))
                    .thenReturn(new PayInfoDTO("html", "PO1", new BigDecimal("0.01")));

            payInternalService.getPayUrl(dto);

            verify(channel).getPayUrl(any(), any(), any(), any());
        }
    }

    @Test
    void getPayUrl_emptyChannel_throws() {
        PayUrlRequestDTO dto = new PayUrlRequestDTO("", "PO1", "s", new BigDecimal("1"));

        BusinessException e = assertThrows(BusinessException.class, () -> payInternalService.getPayUrl(dto));

        assertEquals("支付渠道为空", e.getMessage());
    }

    @Test
    void getPayUrl_unsupportedChannel_throws() {
        PayUrlRequestDTO dto = new PayUrlRequestDTO("wechat", "PO1", "s", new BigDecimal("1"));

        BusinessException e = assertThrows(BusinessException.class, () -> payInternalService.getPayUrl(dto));

        assertEquals("不支持的支付渠道", e.getMessage());
    }

    @Test
    void refund_delegatesToResolvedChannel() {
        PayChannel channel = mock(PayChannel.class);
        PayRefundDTO dto = new PayRefundDTO("SRC1", "R1", new BigDecimal("5.00"), "alipay");
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(channel);

            payInternalService.refund(dto);

            verify(channel).refund("SRC1", "R1", new BigDecimal("5.00"));
        }
    }

    @Test
    void closeOrder_delegatesToResolvedChannel() {
        PayChannel channel = mock(PayChannel.class);
        PayCloseDTO dto = new PayCloseDTO("PO1", "alipay");
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(channel);

            payInternalService.closeOrder(dto);

            verify(channel).closeOrder("PO1");
        }
    }

    @Test
    void queryOrder_delegatesToResolvedChannel() {
        PayChannel channel = mock(PayChannel.class);
        PayOrderNotifyDTO expected = new PayOrderNotifyDTO("PO1", "CH1");
        PayQueryDTO dto = new PayQueryDTO("PO1", "alipay");
        try (MockedStatic<SpringContext> ctx = mockStatic(SpringContext.class)) {
            ctx.when(() -> SpringContext.getBean("payChannel4Alipay")).thenReturn(channel);
            when(channel.queryOrder("PO1")).thenReturn(expected);

            PayOrderNotifyDTO result = payInternalService.queryOrder(dto);

            assertSame(expected, result);
        }
    }
}
