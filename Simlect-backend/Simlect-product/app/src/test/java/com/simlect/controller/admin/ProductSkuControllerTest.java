package com.simlect.controller.admin;

import com.simlect.biz.ProductSkuService;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.po.ProductSku;
import com.simlect.entity.vo.PaginationResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductSkuControllerTest {

    @Mock
    private ProductSkuService productSkuService;

    @InjectMocks
    private ProductSkuController productSkuController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productSkuController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadDataList_ok() throws Exception {
        PaginationResultVO<ProductSku> page = new PaginationResultVO<>();
        page.setList(new ArrayList<>());
        when(productSkuService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/admin/productSku/loadDataList").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void add_ok() throws Exception {
        mockMvc.perform(post("/admin/productSku/add")
                        .param("productId", "P1")
                        .param("propertyValueIdHash", "h1")
                        .param("price", "19.9"))
                .andExpect(status().isOk());

        verify(productSkuService).add(any(ProductSku.class));
    }

    @Test
    void addBatch_ok() throws Exception {
        String body = "[{\"productId\":\"P1\",\"propertyValueIdHash\":\"h1\",\"price\":9.9}]";

        mockMvc.perform(post("/admin/productSku/addBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(productSkuService).addBatch(any());
    }

    @Test
    void getByKey_ok() throws Exception {
        when(productSkuService.getProductSkuByProductIdAndPropertyValueIdHash("P1", "h1"))
                .thenReturn(new ProductSku());

        mockMvc.perform(post("/admin/productSku/getProductSkuByProductIdAndPropertyValueIdHash")
                        .param("productId", "P1")
                        .param("propertyValueIdHash", "h1"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteByKey_ok() throws Exception {
        mockMvc.perform(post("/admin/productSku/deleteProductSkuByProductIdAndPropertyValueIdHash")
                        .param("productId", "P1")
                        .param("propertyValueIdHash", "h1"))
                .andExpect(status().isOk());

        verify(productSkuService).deleteProductSkuByProductIdAndPropertyValueIdHash("P1", "h1");
    }
}
