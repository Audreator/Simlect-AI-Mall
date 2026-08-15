package com.simlect.controller.internal;

import com.simlect.component.RedisComponent;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.po.ProductInfo;
import com.simlect.entity.po.ProductPropertyValue;
import com.simlect.entity.po.ProductSku;
import com.simlect.entity.query.ProductInfoQuery;
import com.simlect.entity.query.ProductPropertyValueQuery;
import com.simlect.entity.query.ProductSkuQuery;
import com.simlect.mappers.ProductInfoMapper;
import com.simlect.mappers.ProductPropertyValueMapper;
import com.simlect.mappers.ProductSkuMapper;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductAgentInternalControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private ProductInfoMapper<ProductInfo, ProductInfoQuery> productInfoMapper;
    @Mock
    private ProductSkuMapper<ProductSku, ProductSkuQuery> productSkuMapper;
    @Mock
    private ProductPropertyValueMapper<ProductPropertyValue, ProductPropertyValueQuery> productPropertyValueMapper;

    @InjectMocks
    private ProductAgentInternalController productAgentInternalController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productAgentInternalController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void searchOnSale_filtersAndReturnsCards() throws Exception {
        ProductInfo product = new ProductInfo();
        product.setProductId("P1");
        product.setProductName("牛肉干");
        product.setCover("a.jpg");
        product.setMinPrice(new java.math.BigDecimal("10"));
        when(productInfoMapper.selectList(any())).thenReturn(List.of(product));

        mockMvc.perform(post("/internal/product/agent/searchOnSale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"牛肉\",\"hotSale\":true,\"limit\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].productId").value("P1"))
                .andExpect(jsonPath("$.data[0].productName").value("牛肉干"))
                .andExpect(jsonPath("$.data[0].totalSale").isEmpty());

        verify(productInfoMapper).selectList(argThat(q ->
                q.getProductNameFuzzy() != null && q.getOrderBy().contains("total_sale")));
    }

    @Test
    void searchOnSale_categoryKeyword_parsesCategoryId() throws Exception {
        when(productInfoMapper.selectList(any())).thenReturn(List.of());

        mockMvc.perform(post("/internal/product/agent/searchOnSale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"category:C1\",\"limit\":5}"))
                .andExpect(status().isOk());

        verify(productInfoMapper).selectList(argThat(q ->
                "C1".equals(q.getCategoryId()) && q.getProductNameFuzzy() == null));
    }

    @Test
    void getDetail_returnsSkusAndProperties() throws Exception {
        ProductInfo product = new ProductInfo();
        product.setProductId("P1");
        product.setProductName("牛肉干");
        product.setProductDesc("好吃的");
        when(productInfoMapper.selectByProductId("P1")).thenReturn(product);
        ProductSku sku = new ProductSku();
        when(productSkuMapper.selectList(any())).thenReturn(List.of(sku));
        ProductPropertyValue pv = new ProductPropertyValue();
        when(productPropertyValueMapper.selectList(any())).thenReturn(List.of(pv));

        mockMvc.perform(post("/internal/product/agent/getDetail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("牛肉干"))
                .andExpect(jsonPath("$.data.description").value("好吃的"));
    }

    @Test
    void getDetail_missingProductId_returnsNull() throws Exception {
        mockMvc.perform(post("/internal/product/agent/getDetail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getDetail_notFound_returnsNull() throws Exception {
        when(productInfoMapper.selectByProductId("P1")).thenReturn(null);

        mockMvc.perform(post("/internal/product/agent/getDetail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
