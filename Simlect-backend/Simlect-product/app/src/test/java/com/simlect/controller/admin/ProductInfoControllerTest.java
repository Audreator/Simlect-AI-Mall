package com.simlect.controller.admin;

import com.simlect.biz.ProductInfoService;
import com.simlect.biz.ProductSkuService;
import com.simlect.entity.dto.ProductSaveDTO;
import com.simlect.entity.po.ProductInfo;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.Product4VO;
import com.simlect.controller.AGlobalExceptionHandlerController;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductInfoControllerTest {

    @Mock
    private ProductInfoService productInfoService;
    @Mock
    private ProductSkuService productSkuService;

    @InjectMocks
    private ProductInfoController productInfoController;

    private MockMvc mockMvc;

    private static final String VALID_PRODUCT_JSON =
            "{\"productInfo\":{\"productName\":\"测试商品\",\"productDesc\":\"描述\","
                    + "\"cover\":\"a.jpg\",\"categoryId\":\"C1\",\"pCategoryId\":\"0\"},"
                    + "\"productPropertyList\":[{\"propertyId\":\"prop1\",\"propertyValue\":\"红色\"}],"
                    + "\"skuList\":[{\"propertyValueIdHash\":\"h1\",\"price\":9.9}]}";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productInfoController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadDataList_ok() throws Exception {
        PaginationResultVO<ProductInfo> page = new PaginationResultVO<>();
        page.setList(new ArrayList<>());
        when(productInfoService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/admin/productInfo/loadDataList").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void addProduct_saves() throws Exception {
        mockMvc.perform(post("/admin/productInfo/addProduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productInfoService).saveProduct(any(ProductSaveDTO.class));
    }

    @Test
    void addProduct_emptyLists_validationGroupAllowsAndSaves() throws Exception {
        // @Size(min=1) 属于默认组，而控制器使用 @Validated(Create.class) 分组校验，
        // 默认组约束不会在 Create 分组下触发，请求会正常到达 Service
        String body = "{\"productInfo\":{\"productName\":\"x\",\"productDesc\":\"d\","
                + "\"cover\":\"a.jpg\",\"categoryId\":\"C1\",\"pCategoryId\":\"0\"},"
                + "\"productPropertyList\":[],\"skuList\":[]}";

        mockMvc.perform(post("/admin/productInfo/addProduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productInfoService).saveProduct(any(ProductSaveDTO.class));
    }

    @Test
    void updateProduct_saves() throws Exception {
        String body = "{\"productInfo\":{\"productId\":\"P1\",\"productName\":\"测试商品\","
                + "\"productDesc\":\"描述\",\"cover\":\"a.jpg\",\"categoryId\":\"C1\",\"pCategoryId\":\"0\"},"
                + "\"productPropertyList\":[{\"propertyId\":\"prop1\",\"propertyValue\":\"红色\"}],"
                + "\"skuList\":[{\"propertyValueIdHash\":\"h1\",\"price\":9.9}]}";

        mockMvc.perform(post("/admin/productInfo/updateProduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(productInfoService).saveProduct(any(ProductSaveDTO.class));
    }

    @Test
    void getProductInfoByProductId_ok() throws Exception {
        when(productInfoService.getProductInfoByProductId("P1")).thenReturn(null);

        mockMvc.perform(post("/admin/productInfo/getProductInfoByProductId").param("productId", "P1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProductStatus_ok() throws Exception {
        mockMvc.perform(post("/admin/productInfo/updateProductStatus")
                        .param("productId", "P1")
                        .param("status", "0"))
                .andExpect(status().isOk());

        verify(productInfoService).updateProductStatus("P1", 0);
    }

    @Test
    void deleteProduct_ok() throws Exception {
        mockMvc.perform(post("/admin/productInfo/deleteProduct").param("productId", "P1"))
                .andExpect(status().isOk());

        verify(productInfoService).deleteProduct("P1");
    }

    @Test
    void commendProduct_ok() throws Exception {
        mockMvc.perform(post("/admin/productInfo/commendProduct")
                        .param("productId", "P1")
                        .param("commendType", "1"))
                .andExpect(status().isOk());

        verify(productInfoService).commendProduct("P1", 1);
    }

    @Test
    void updateSkuStock_ok() throws Exception {
        mockMvc.perform(post("/admin/productInfo/updateSkuStock")
                        .param("productId", "P1")
                        .param("propertyValueIdHash", "h1")
                        .param("changeStock", "5"))
                .andExpect(status().isOk());

        verify(productSkuService).updateStock("P1", "h1", 5);
    }

    @Test
    void loadProduct_ok() throws Exception {
        PaginationResultVO page = new PaginationResultVO<>();
        page.setList(new ArrayList<>());
        when(productInfoService.findListByPage4ListVO(any())).thenReturn(page);

        mockMvc.perform(post("/admin/productInfo/loadProduct")
                        .param("productNameFuzzy", "手机")
                        .param("pageNo", "1"))
                .andExpect(status().isOk());

        verify(productInfoService).findListByPage4ListVO(argThat(q ->
                "手机".equals(q.getProductNameFuzzy())));
    }

    @Test
    void getProductInfo_ok() throws Exception {
        when(productInfoService.getProduct4VOByProductId("P1")).thenReturn(new Product4VO());

        mockMvc.perform(post("/admin/productInfo/getProductInfo").param("productId", "P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
