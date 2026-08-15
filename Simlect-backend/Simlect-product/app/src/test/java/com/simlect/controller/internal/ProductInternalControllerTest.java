package com.simlect.controller.internal;

import com.simlect.api.dto.LessStockPageDTO;
import com.simlect.api.dto.ProductIdDTO;
import com.simlect.api.dto.ProductIdListDTO;
import com.simlect.api.dto.ProductSalesIncreaseDTO;
import com.simlect.api.dto.ProductSnapshotBatchVO;
import com.simlect.api.vo.ProductRagIndexVO;
import com.simlect.api.vo.ProductSearchIndexVO;
import com.simlect.api.vo.ProductSkuSnapshotVO;
import com.simlect.biz.ProductInternalService;
import com.simlect.biz.ProductSkuService;
import com.simlect.component.RedisComponent;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductInternalControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private ProductInternalService productInternalService;
    @Mock
    private ProductSkuService productSkuService;

    @InjectMocks
    private ProductInternalController productInternalController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productInternalController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void snapshotBatch_ok() throws Exception {
        when(productInternalService.snapshotBatch(List.of("P1"))).thenReturn(new ProductSnapshotBatchVO());

        mockMvc.perform(post("/internal/product/snapshotBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productIds\":[\"P1\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productInternalService).snapshotBatch(List.of("P1"));
    }

    @Test
    void defaultSku_ok() throws Exception {
        when(productInternalService.defaultSku("P1")).thenReturn(new ProductSkuSnapshotVO());

        mockMvc.perform(post("/internal/product/defaultSku")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void increaseSales_ok() throws Exception {
        mockMvc.perform(post("/internal/product/increaseSales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\",\"qty\":3}"))
                .andExpect(status().isOk());

        verify(productInternalService).increaseSales("P1", 3);
    }

    @Test
    void searchIndex_ok() throws Exception {
        when(productInternalService.getSearchIndex("P1")).thenReturn(new ProductSearchIndexVO());

        mockMvc.perform(post("/internal/product/searchIndex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void ragIndex_ok() throws Exception {
        when(productInternalService.getRagIndex("P1")).thenReturn(new ProductRagIndexVO());

        mockMvc.perform(post("/internal/product/ragIndex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void lessStockSkuPage_ok() throws Exception {
        when(productSkuService.lessStockSkuPage(1, 15, 10)).thenReturn(new PaginationResultVO<>());

        mockMvc.perform(post("/internal/product/lessStockSkuPage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNo\":1,\"pageSize\":15,\"threshold\":10}"))
                .andExpect(status().isOk());

        verify(productSkuService).lessStockSkuPage(1, 15, 10);
    }

    @Test
    void listOnSaleProductIds_ok() throws Exception {
        when(productInternalService.listOnSaleProductIds()).thenReturn(List.of("P1"));

        mockMvc.perform(post("/internal/product/listOnSaleProductIds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("P1"));
    }
}
