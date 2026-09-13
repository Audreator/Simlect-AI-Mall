package com.simlect.biz.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.AlipayRequest;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.enums.PayChannelEnum;
import com.simlect.component.RedisComponent;
import com.simlect.entity.config.AppConfig;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PayChannel4AliPay 支付宝渠道实现单元测试：mock 支付宝 SDK（DefaultAlipayClient
 * 构造拦截 + AlipaySignature 静态验签），覆盖验签/回调参数解析/退款/关单。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayChannel4AliPayTest {

    private static final String PAY_ORDER_ID = "PO20260815001";
    private static final String CHANNEL_ORDER_ID = "202608152200100001";
    private static final String SUBJECT = "测试订单";
    private static final String CONFIG_BASE = "/data/certs/";

    @Mock
    private AppConfig appConfig;
    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private PayChannel4AliPay payChannel4AliPay;

    private void mockAppConfig() {
        when(appConfig.getProjectDomain()).thenReturn("https://shop.example.com");
        when(appConfig.getOrderExpireMinute()).thenReturn(15);
        when(appConfig.getProjectFolder()).thenReturn(CONFIG_BASE);
        when(appConfig.getAlipayAppCertPath()).thenReturn("appCert.crt");
        when(appConfig.getAlipayPublicCertPath()).thenReturn("alipayPublicCert.crt");
        when(appConfig.getAlipayRootCertPath()).thenReturn("rootCert.crt");
        when(appConfig.getAlipayServerUrl()).thenReturn("https://openapi.alipay.com/gateway.do");
        when(appConfig.getAlipayAppid()).thenReturn("20210001");
        when(appConfig.getAlipayAppPrivateKey()).thenReturn("private-key");
    }

    /**
     * 构造 DefaultAlipayClient 被替换为 mock，并按请求类型分发响应。
     */
    private MockedConstruction<DefaultAlipayClient> mockAlipayClient(
            AlipayTradePagePayResponse pageResp,
            AlipayTradeWapPayResponse wapResp,
            AlipayTradeQueryResponse queryResp,
            AlipayTradeRefundResponse refundResp,
            AlipayTradeCloseResponse closeResp) {
        return mockConstruction(DefaultAlipayClient.class, (mock, context) -> {
            try {
                doAnswer(inv -> {
                    Object req = inv.getArgument(0);
                    if (req instanceof AlipayTradeWapPayRequest) {
                        return wapResp;
                    }
                    return pageResp;
                }).when(mock).pageExecute(any(AlipayRequest.class));
                doAnswer(inv -> {
                    Object req = inv.getArgument(0);
                    if (req instanceof AlipayTradeQueryRequest) {
                        return queryResp;
                    }
                    if (req instanceof AlipayTradeRefundRequest) {
                        return refundResp;
                    }
                    return closeResp;
                }).when(mock).certificateExecute(any(AlipayRequest.class));
            } catch (AlipayApiException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ==================== getPayUrl：PC ====================

    @Test
    void getPayUrl_localMock_returnsPayOrderWithoutCallingAlipayOrMarkingInitiated() {
        ReflectionTestUtils.setField(payChannel4AliPay, "localMockEnabled", true);

        PayInfoDTO result = payChannel4AliPay.getPayUrl(
                PayChannelEnum.ALIPAY_PC, PAY_ORDER_ID, SUBJECT, new BigDecimal("33.9"));

        assertEquals(PAY_ORDER_ID, result.getPayOrderId());
        assertEquals(new BigDecimal("33.90"), result.getAmount());
        assertNull(result.getPayInfo());
        verify(redisComponent, org.mockito.Mockito.never()).markPayTradeInitiated(anyString());
    }

    @Test
    void localMock_queryRefundAndCloseNeverConstructAlipayClient() {
        ReflectionTestUtils.setField(payChannel4AliPay, "localMockEnabled", true);

        try (MockedConstruction<DefaultAlipayClient> construction = mockConstruction(DefaultAlipayClient.class)) {
            assertNull(payChannel4AliPay.queryOrder(PAY_ORDER_ID));
            assertDoesNotThrow(() -> payChannel4AliPay.refund(
                    PAY_ORDER_ID, "R20260815001", new BigDecimal("10.00")));
            assertDoesNotThrow(() -> payChannel4AliPay.closeOrder(PAY_ORDER_ID));

            assertTrue(construction.constructed().isEmpty());
            verify(redisComponent, org.mockito.Mockito.never())
                    .setIfAbsent(anyString(), anyString(), anyLong(), any());
        }
    }

    @Test
    void getPayUrl_pc_success_returnsPayInfoAndMarksInitiated() {
        mockAppConfig();
        AlipayTradePagePayResponse pageResp = mock(AlipayTradePagePayResponse.class);
        when(pageResp.isSuccess()).thenReturn(true);
        when(pageResp.getBody()).thenReturn("<form>alipay-form</form>");
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(pageResp, mock(AlipayTradeWapPayResponse.class),
                             mock(AlipayTradeQueryResponse.class), mock(AlipayTradeRefundResponse.class),
                             mock(AlipayTradeCloseResponse.class))) {

            PayInfoDTO dto = payChannel4AliPay.getPayUrl(
                    PayChannelEnum.ALIPAY_PC, PAY_ORDER_ID, SUBJECT, new BigDecimal("100.00"));

            assertNotNull(dto);
            assertEquals("<form>alipay-form</form>", dto.getPayInfo());
            assertEquals(PAY_ORDER_ID, dto.getPayOrderId());
            assertEquals(new BigDecimal("100.00"), dto.getAmount());
            assertEquals(1, construction.constructed().size());
            verify(redisComponent).markPayTradeInitiated(PAY_ORDER_ID);
        }
    }

    @Test
    void getPayUrl_pc_sdkFailure_throwsBusinessException() {
        mockAppConfig();
        AlipayTradePagePayResponse pageResp = mock(AlipayTradePagePayResponse.class);
        when(pageResp.isSuccess()).thenReturn(false);
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(pageResp, mock(AlipayTradeWapPayResponse.class),
                             mock(AlipayTradeQueryResponse.class), mock(AlipayTradeRefundResponse.class),
                             mock(AlipayTradeCloseResponse.class))) {

            BusinessException e = assertThrows(BusinessException.class,
                    () -> payChannel4AliPay.getPayUrl(PayChannelEnum.ALIPAY_PC, PAY_ORDER_ID, SUBJECT, new BigDecimal("1.00")));

            assertEquals("获取支付信息失败", e.getMessage());
            verify(redisComponent, org.mockito.Mockito.never()).markPayTradeInitiated(anyString());
        }
    }

    @Test
    void getPayUrl_pc_exception_throwsBusinessException() {
        mockAppConfig();
        AlipayTradePagePayResponse pageResp = mock(AlipayTradePagePayResponse.class);
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockConstruction(DefaultAlipayClient.class, (mock, context) -> {
                         try {
                             when(mock.pageExecute(any(AlipayRequest.class))).thenThrow(new AlipayApiException("net err"));
                         } catch (AlipayApiException ignored) {
                         }
                     })) {

            BusinessException e = assertThrows(BusinessException.class,
                    () -> payChannel4AliPay.getPayUrl(PayChannelEnum.ALIPAY_PC, PAY_ORDER_ID, SUBJECT, new BigDecimal("10.00")));

            assertEquals("获取支付信息失败", e.getMessage());
        }
    }

    // ==================== getPayUrl：WAP ====================

    @Test
    void getPayUrl_wap_success_returnsPayInfoAndMarksInitiated() {
        mockAppConfig();
        AlipayTradeWapPayResponse wapResp = mock(AlipayTradeWapPayResponse.class);
        when(wapResp.isSuccess()).thenReturn(true);
        when(wapResp.getBody()).thenReturn("<html>wap</html>");
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), wapResp,
                             mock(AlipayTradeQueryResponse.class), mock(AlipayTradeRefundResponse.class),
                             mock(AlipayTradeCloseResponse.class))) {

            PayInfoDTO dto = payChannel4AliPay.getPayUrl(
                    PayChannelEnum.ALIPAY_WAP, PAY_ORDER_ID, SUBJECT, new BigDecimal("0.50"));

            assertEquals("<html>wap</html>", dto.getPayInfo());
            // 金额低于最小支付额时归一化到 0.01
            assertEquals(new BigDecimal("0.50"), dto.getAmount());
            verify(redisComponent).markPayTradeInitiated(PAY_ORDER_ID);
        }
    }

    // ==================== payNotify：验签 ====================

    private Map<String, String> notifyParams(String status) {
        Map<String, String> params = new HashMap<>();
        params.put("sign_type", "RSA2");
        params.put("out_trade_no", PAY_ORDER_ID);
        params.put("trade_no", CHANNEL_ORDER_ID);
        params.put("trade_status", status);
        return params;
    }

    @Test
    void payNotify_validSign_successStatus_parsesParamsAndRemovesSignType() {
        mockAppConfig();
        try (MockedStatic<AlipaySignature> signature = mockStatic(AlipaySignature.class)) {
            signature.when(() -> AlipaySignature.rsaCertCheckV2(anyMap(), anyString(), anyString(), anyString()))
                    .thenReturn(true);

            Map<String, String> params = notifyParams("TRADE_SUCCESS");
            PayOrderNotifyDTO dto = payChannel4AliPay.payNotify(params, null);

            assertNotNull(dto);
            assertEquals(PAY_ORDER_ID, dto.getPayOrderId());
            assertEquals(CHANNEL_ORDER_ID, dto.getChannelOrderId());
            assertFalse(params.containsKey("sign_type"));
            signature.verify(() -> AlipaySignature.rsaCertCheckV2(
                    params, CONFIG_BASE + "alipayPublicCert.crt", "UTF-8", "RSA2"));
        }
    }

    @Test
    void payNotify_signCheckFalse_throwsBusinessException() {
        mockAppConfig();
        try (MockedStatic<AlipaySignature> signature = mockStatic(AlipaySignature.class)) {
            signature.when(() -> AlipaySignature.rsaCertCheckV2(anyMap(), anyString(), anyString(), anyString()))
                    .thenReturn(false);

            BusinessException e = assertThrows(BusinessException.class,
                    () -> payChannel4AliPay.payNotify(notifyParams("TRADE_SUCCESS"), null));

            assertEquals("支付宝回调校验失败", e.getMessage());
        }
    }

    @Test
    void payNotify_signCheckThrowsApiException_throwsBusinessException() {
        mockAppConfig();
        try (MockedStatic<AlipaySignature> signature = mockStatic(AlipaySignature.class)) {
            signature.when(() -> AlipaySignature.rsaCertCheckV2(anyMap(), anyString(), anyString(), anyString()))
                    .thenThrow(new AlipayApiException("bad cert"));

            BusinessException e = assertThrows(BusinessException.class,
                    () -> payChannel4AliPay.payNotify(notifyParams("TRADE_SUCCESS"), null));

            assertEquals("支付宝回调校验失败", e.getMessage());
        }
    }

    @Test
    void payNotify_nonSuccessStatus_returnsNull() {
        mockAppConfig();
        try (MockedStatic<AlipaySignature> signature = mockStatic(AlipaySignature.class)) {
            signature.when(() -> AlipaySignature.rsaCertCheckV2(anyMap(), anyString(), anyString(), anyString()))
                    .thenReturn(true);

            assertNull(payChannel4AliPay.payNotify(notifyParams("WAIT_BUYER_PAY"), null));
        }
    }

    // ==================== queryOrder：查单 ====================

    @Test
    void queryOrder_tradeNotExist_returnsNull() {
        mockAppConfig();
        AlipayTradeQueryResponse queryResp = mock(AlipayTradeQueryResponse.class);
        when(queryResp.isSuccess()).thenReturn(false);
        when(queryResp.getSubCode()).thenReturn("ACQ.TRADE_NOT_EXIST");
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), mock(AlipayTradeWapPayResponse.class),
                             queryResp, mock(AlipayTradeRefundResponse.class), mock(AlipayTradeCloseResponse.class))) {

            assertNull(payChannel4AliPay.queryOrder(PAY_ORDER_ID));
        }
    }

    @Test
    void queryOrder_otherFailure_returnsNull() {
        mockAppConfig();
        AlipayTradeQueryResponse queryResp = mock(AlipayTradeQueryResponse.class);
        when(queryResp.isSuccess()).thenReturn(false);
        when(queryResp.getSubCode()).thenReturn("ACQ.SYSTEM_ERROR");
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), mock(AlipayTradeWapPayResponse.class),
                             queryResp, mock(AlipayTradeRefundResponse.class), mock(AlipayTradeCloseResponse.class))) {

            assertNull(payChannel4AliPay.queryOrder(PAY_ORDER_ID));
        }
    }

    @Test
    void queryOrder_paid_returnsNotifyDto() {
        mockAppConfig();
        AlipayTradeQueryResponse queryResp = mock(AlipayTradeQueryResponse.class);
        when(queryResp.isSuccess()).thenReturn(true);
        when(queryResp.getTradeStatus()).thenReturn("TRADE_SUCCESS");
        when(queryResp.getTradeNo()).thenReturn(CHANNEL_ORDER_ID);
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), mock(AlipayTradeWapPayResponse.class),
                             queryResp, mock(AlipayTradeRefundResponse.class), mock(AlipayTradeCloseResponse.class))) {

            PayOrderNotifyDTO dto = payChannel4AliPay.queryOrder(PAY_ORDER_ID);

            assertEquals(PAY_ORDER_ID, dto.getPayOrderId());
            assertEquals(CHANNEL_ORDER_ID, dto.getChannelOrderId());
        }
    }

    @Test
    void queryOrder_unpaid_returnsNull() {
        mockAppConfig();
        AlipayTradeQueryResponse queryResp = mock(AlipayTradeQueryResponse.class);
        when(queryResp.isSuccess()).thenReturn(true);
        when(queryResp.getTradeStatus()).thenReturn("WAIT_BUYER_PAY");
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), mock(AlipayTradeWapPayResponse.class),
                             queryResp, mock(AlipayTradeRefundResponse.class), mock(AlipayTradeCloseResponse.class))) {

            assertNull(payChannel4AliPay.queryOrder(PAY_ORDER_ID));
        }
    }

    // ==================== refund：退款 ====================

    @Test
    void refund_success_doesNotThrow() {
        mockAppConfig();
        AlipayTradeRefundResponse refundResp = mock(AlipayTradeRefundResponse.class);
        when(refundResp.isSuccess()).thenReturn(true);
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), mock(AlipayTradeWapPayResponse.class),
                             mock(AlipayTradeQueryResponse.class), refundResp, mock(AlipayTradeCloseResponse.class))) {

            assertDoesNotThrow(() -> payChannel4AliPay.refund(
                    PAY_ORDER_ID, "R20260815001", new BigDecimal("10.00")));
        }
    }

    @Test
    void refund_sdkRejects_throwsBusinessException() {
        mockAppConfig();
        when(redisComponent.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        AlipayTradeRefundResponse refundResp = mock(AlipayTradeRefundResponse.class);
        when(refundResp.isSuccess()).thenReturn(false);
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), mock(AlipayTradeWapPayResponse.class),
                             mock(AlipayTradeQueryResponse.class), refundResp, mock(AlipayTradeCloseResponse.class))) {

            BusinessException e = assertThrows(BusinessException.class,
                    () -> payChannel4AliPay.refund(PAY_ORDER_ID, "R1", new BigDecimal("1.00")));

            assertEquals("退款失败", e.getMessage());
        }
    }

    @Test
    void refund_sdkException_throwsBusinessException() {
        mockAppConfig();
        when(redisComponent.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockConstruction(DefaultAlipayClient.class, (mock, context) -> {
                         try {
                             when(mock.certificateExecute(any(AlipayRequest.class))).thenThrow(new AlipayApiException("boom"));
                         } catch (AlipayApiException ignored) {
                         }
                     })) {

            BusinessException e = assertThrows(BusinessException.class,
                    () -> payChannel4AliPay.refund(PAY_ORDER_ID, "R1", new BigDecimal("1.00")));

            assertEquals("支付宝退款失败", e.getMessage());
        }
    }

    // ==================== closeOrder：关单 ====================

    @Test
    void closeOrder_success_doesNotThrow() {
        mockAppConfig();
        AlipayTradeCloseResponse closeResp = mock(AlipayTradeCloseResponse.class);
        when(closeResp.isSuccess()).thenReturn(true);
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), mock(AlipayTradeWapPayResponse.class),
                             mock(AlipayTradeQueryResponse.class), mock(AlipayTradeRefundResponse.class), closeResp)) {

            assertDoesNotThrow(() -> payChannel4AliPay.closeOrder(PAY_ORDER_ID));
        }
    }

    @Test
    void closeOrder_tradeNotExist_ignored() {
        mockAppConfig();
        AlipayTradeCloseResponse closeResp = mock(AlipayTradeCloseResponse.class);
        when(closeResp.isSuccess()).thenReturn(false);
        when(closeResp.getSubCode()).thenReturn("ACQ.TRADE_NOT_EXIST");
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), mock(AlipayTradeWapPayResponse.class),
                             mock(AlipayTradeQueryResponse.class), mock(AlipayTradeRefundResponse.class), closeResp)) {

            assertDoesNotThrow(() -> payChannel4AliPay.closeOrder(PAY_ORDER_ID));
        }
    }

    @Test
    void closeOrder_otherSubCode_throwsBusinessException() {
        mockAppConfig();
        AlipayTradeCloseResponse closeResp = mock(AlipayTradeCloseResponse.class);
        when(closeResp.isSuccess()).thenReturn(false);
        when(closeResp.getSubCode()).thenReturn("ACQ.SYSTEM_ERROR");
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockAlipayClient(mock(AlipayTradePagePayResponse.class), mock(AlipayTradeWapPayResponse.class),
                             mock(AlipayTradeQueryResponse.class), mock(AlipayTradeRefundResponse.class), closeResp)) {

            BusinessException e = assertThrows(BusinessException.class,
                    () -> payChannel4AliPay.closeOrder(PAY_ORDER_ID));

            assertEquals("订单关闭失败", e.getMessage());
        }
    }

    @Test
    void closeOrder_sdkException_swallowed() {
        mockAppConfig();
        try (MockedConstruction<DefaultAlipayClient> construction =
                     mockConstruction(DefaultAlipayClient.class, (mock, context) -> {
                         try {
                             when(mock.certificateExecute(any(AlipayRequest.class))).thenThrow(new AlipayApiException("boom"));
                         } catch (AlipayApiException ignored) {
                         }
                     })) {

            assertDoesNotThrow(() -> payChannel4AliPay.closeOrder(PAY_ORDER_ID));
        }
    }
}
