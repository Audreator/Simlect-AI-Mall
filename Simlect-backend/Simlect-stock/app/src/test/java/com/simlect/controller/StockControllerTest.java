package com.simlect.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * StockController 健康检查接口单元测试（MockMvc standalone）。
 */
class StockControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StockController()).build();

    @Test
    void health_returnsUp() throws Exception {
        mockMvc.perform(post("/stock/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.serviceName").value("simlect-stock"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
