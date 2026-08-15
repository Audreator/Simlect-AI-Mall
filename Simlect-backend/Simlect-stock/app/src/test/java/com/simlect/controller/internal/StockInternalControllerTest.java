package com.simlect.controller.internal;

import com.simlect.api.dto.SkuStockBatchChangeDTO;
import com.simlect.api.dto.SkuStockChangeDTO;
import com.simlect.api.dto.SkuStockDTO;
import com.simlect.biz.SkuStockService;
import com.simlect.entity.vo.PaginationResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * StockInternalController 内部接口单元测试（MockMvc standalone + mock SkuStockService）。
 */
@ExtendWith(MockitoExtension.class)
class StockInternalControllerTest {

    @Mock
    private SkuStockService skuStockService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StockInternalController controller = new StockInternalController();
        ReflectionTestUtils.setField(controller, "skuStockService", skuStockService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getStock_returnsDto() throws Exception {
        when(skuStockService.getStock("P1", "hash1")).thenReturn(new SkuStockDTO("P1", "hash1", 9));

        mockMvc.perform(post("/internal/stock/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\",\"propertyValueIdHash\":\"hash1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.stock").value(9));
    }

    @Test
    void changeStock_returnsAffectedRows() throws Exception {
        when(skuStockService.changeStock(any(SkuStockChangeDTO.class))).thenReturn(1);

        mockMvc.perform(post("/internal/stock/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\",\"propertyValueIdHash\":\"hash1\",\"changeAmount\":-2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affectedRows").value(1));
    }

    @Test
    void changeStockBatch_returnsAffectedRows() throws Exception {
        when(skuStockService.changeStockBatch(any(SkuStockBatchChangeDTO.class))).thenReturn(3);

        mockMvc.perform(post("/internal/stock/changeBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"P1\",\"propertyValueIdHash\":\"h1\",\"changeAmount\":-1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affectedRows").value(3));
    }

    @Test
    void lockAndVerify_delegates() throws Exception {
        mockMvc.perform(post("/internal/stock/lockAndVerify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"P1\",\"propertyValueIdHash\":\"h1\",\"changeAmount\":-1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(skuStockService).lockAndVerify(any(SkuStockBatchChangeDTO.class));
    }

    @Test
    void setStock_delegates() throws Exception {
        mockMvc.perform(post("/internal/stock/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\",\"propertyValueIdHash\":\"hash1\",\"stock\":66}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(skuStockService).setStock("P1", "hash1", 66);
    }

    @Test
    void totalByProduct_returnsTotal() throws Exception {
        when(skuStockService.totalByProductId("P1")).thenReturn(120);

        mockMvc.perform(post("/internal/stock/totalByProduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value("P1"))
                .andExpect(jsonPath("$.data.totalStock").value(120));
    }

    @Test
    void listLessThan_returnsPage() throws Exception {
        PaginationResultVO<SkuStockDTO> page = new PaginationResultVO<>(
                1, 15, 1, 1, List.of(new SkuStockDTO("P1", "h1", 2)));
        when(skuStockService.listLessThan(eq(1), eq(15), eq(10))).thenReturn(page);

        mockMvc.perform(post("/internal/stock/listLessThan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNo\":1,\"pageSize\":15,\"threshold\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.list[0].stock").value(2));
    }

    @Test
    void listLessThan_emptyBody_usesDefaults() throws Exception {
        when(skuStockService.listLessThan(nullable(Integer.class), nullable(Integer.class), nullable(Integer.class)))
                .thenReturn(new PaginationResultVO<>(0, 15, 1, 0, List.of()));

        mockMvc.perform(post("/internal/stock/listLessThan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(skuStockService).listLessThan(eq(null), eq(null), eq(null));
    }
}
