package com.simlect.controller.internal;

import com.simlect.biz.PayInternalService;
import com.simlect.api.dto.PayCloseDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.dto.PayQueryDTO;
import com.simlect.api.dto.PayRefundDTO;
import com.simlect.api.dto.PayTradeCreateDTO;
import com.simlect.api.dto.PayTradeStatusDTO;
import com.simlect.api.dto.PayUrlRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PayInternalController 内部支付接口单元测试（MockMvc standalone）。
 */
@ExtendWith(MockitoExtension.class)
class PayInternalControllerTest {

    @Mock
    private PayInternalService payInternalService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PayInternalController controller = new PayInternalController();
        ReflectionTestUtils.setField(controller, "payInternalService", payInternalService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createPending_delegates() throws Exception {
        mockMvc.perform(post("/internal/pay/trade/createPending")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"U1\",\"payOrderId\":\"PO1\",\"orderId\":\"O1\",\"payAmount\":10.00,\"payChannel\":\"alipay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(payInternalService).createPending(any(PayTradeCreateDTO.class));
    }

    @Test
    void markSuccess_delegates() throws Exception {
        mockMvc.perform(post("/internal/pay/trade/markSuccess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payOrderId\":\"PO1\",\"channelOrderId\":\"CH1\"}"))
                .andExpect(status().isOk());

        verify(payInternalService).markSuccess(any(PayTradeStatusDTO.class));
    }

    @Test
    void markClosed_delegates() throws Exception {
        mockMvc.perform(post("/internal/pay/trade/markClosed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payOrderId\":\"PO1\"}"))
                .andExpect(status().isOk());

        verify(payInternalService).markClosed(any(PayTradeStatusDTO.class));
    }

    @Test
    void markRefunded_delegates() throws Exception {
        mockMvc.perform(post("/internal/pay/trade/markRefunded")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payOrderId\":\"PO1\"}"))
                .andExpect(status().isOk());

        verify(payInternalService).markRefunded(any(PayTradeStatusDTO.class));
    }

    @Test
    void getPayUrl_returnsPayInfo() throws Exception {
        when(payInternalService.getPayUrl(any(PayUrlRequestDTO.class)))
                .thenReturn(new PayInfoDTO("html", "PO1", new BigDecimal("0.01")));

        mockMvc.perform(post("/internal/pay/channel/getPayUrl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payChannel\":\"alipay\",\"payOrderId\":\"PO1\",\"subject\":\"s\",\"amount\":0.01}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payOrderId").value("PO1"))
                .andExpect(jsonPath("$.data.payInfo").value("html"));
    }

    @Test
    void refund_delegates() throws Exception {
        mockMvc.perform(post("/internal/pay/channel/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourcePayOrderId\":\"SRC1\",\"refundOrderId\":\"R1\",\"refundAmount\":5.00,\"payChannel\":\"alipay\"}"))
                .andExpect(status().isOk());

        verify(payInternalService).refund(any(PayRefundDTO.class));
    }

    @Test
    void closeOrder_delegates() throws Exception {
        mockMvc.perform(post("/internal/pay/channel/closeOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payOrderId\":\"PO1\",\"payChannel\":\"alipay\"}"))
                .andExpect(status().isOk());

        verify(payInternalService).closeOrder(any(PayCloseDTO.class));
    }

    @Test
    void queryOrder_returnsNotifyDto() throws Exception {
        when(payInternalService.queryOrder(any(PayQueryDTO.class)))
                .thenReturn(new PayOrderNotifyDTO("PO1", "CH1"));

        mockMvc.perform(post("/internal/pay/channel/queryOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payOrderId\":\"PO1\",\"payChannel\":\"alipay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelOrderId").value("CH1"));
    }
}
