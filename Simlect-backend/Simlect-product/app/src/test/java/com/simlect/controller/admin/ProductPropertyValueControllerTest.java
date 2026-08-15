package com.simlect.controller.admin;

import com.simlect.biz.ProductPropertyValueService;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.po.ProductPropertyValue;
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
class ProductPropertyValueControllerTest {

    @Mock
    private ProductPropertyValueService productPropertyValueService;

    @InjectMocks
    private ProductPropertyValueController productPropertyValueController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productPropertyValueController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadDataList_ok() throws Exception {
        PaginationResultVO<ProductPropertyValue> page = new PaginationResultVO<>();
        page.setList(new ArrayList<>());
        when(productPropertyValueService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/admin/productPropertyValue/loadDataList").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void add_ok() throws Exception {
        mockMvc.perform(post("/admin/productPropertyValue/add")
                        .param("productId", "P1")
                        .param("propertyValueId", "v1")
                        .param("propertyValue", "红色"))
                .andExpect(status().isOk());

        verify(productPropertyValueService).add(any(ProductPropertyValue.class));
    }

    @Test
    void addBatch_ok() throws Exception {
        String body = "[{\"productId\":\"P1\",\"propertyValueId\":\"v1\",\"propertyValue\":\"红色\"}]";

        mockMvc.perform(post("/admin/productPropertyValue/addBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(productPropertyValueService).addBatch(any());
    }

    @Test
    void getByKey_ok() throws Exception {
        when(productPropertyValueService.getProductPropertyValueByProductIdAndPropertyValueId("P1", "v1"))
                .thenReturn(null);

        mockMvc.perform(post("/admin/productPropertyValue/getProductPropertyValueByProductIdAndPropertyValueId")
                        .param("productId", "P1")
                        .param("propertyValueId", "v1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateByKey_ok() throws Exception {
        mockMvc.perform(post("/admin/productPropertyValue/updateProductPropertyValueByProductIdAndPropertyValueId")
                        .param("productId", "P1")
                        .param("propertyValueId", "v1")
                        .param("propertyValue", "蓝色"))
                .andExpect(status().isOk());

        verify(productPropertyValueService).updateProductPropertyValueByProductIdAndPropertyValueId(
                any(ProductPropertyValue.class), eqOrNull("P1"), eqOrNull("v1"));
    }

    @Test
    void deleteByKey_ok() throws Exception {
        mockMvc.perform(post("/admin/productPropertyValue/deleteProductPropertyValueByProductIdAndPropertyValueId")
                        .param("productId", "P1")
                        .param("propertyValueId", "v1"))
                .andExpect(status().isOk());

        verify(productPropertyValueService).deleteProductPropertyValueByProductIdAndPropertyValueId("P1", "v1");
    }

    private static String eqOrNull(String value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
