package com.simlect.controller.admin;

import com.simlect.biz.ProductCartService;
import com.simlect.entity.po.ProductCart;
import com.simlect.entity.query.ProductCartQuery;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端 ProductCartController 接口单元测试（MockMvc standalone）。
 */
@ExtendWith(MockitoExtension.class)
class ProductCartControllerTest {

    @Mock
    private ProductCartService productCartService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        com.simlect.controller.admin.ProductCartController controller =
                new com.simlect.controller.admin.ProductCartController();
        ReflectionTestUtils.setField(controller, "productCartService", productCartService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void loadDataList_returnsPage() throws Exception {
        when(productCartService.findListByPage(any(ProductCartQuery.class)))
                .thenReturn(new PaginationResultVO<>(1, 15, 1, 1, List.of(new ProductCart())));

        mockMvc.perform(post("/admin/productCart/loadDataList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void add_delegates() throws Exception {
        mockMvc.perform(post("/admin/productCart/add")
                        .param("productId", "P1")
                        .param("propertyValueIds", "pv1")
                        .param("buyCount", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).add(any(ProductCart.class));
    }

    @Test
    void addBatch_delegates() throws Exception {
        mockMvc.perform(post("/admin/productCart/addBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"productId\":\"P1\",\"propertyValueIds\":\"pv1\",\"buyCount\":1}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).addBatch(anyList());
    }

    @Test
    void addOrUpdateBatch_delegates() throws Exception {
        mockMvc.perform(post("/admin/productCart/addOrUpdateBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"productId\":\"P1\",\"propertyValueIds\":\"pv1\",\"buyCount\":1}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).addBatch(anyList());
    }

    @Test
    void getProductCartByCartId_returnsCart() throws Exception {
        ProductCart cart = new ProductCart();
        cart.setCartId("c1");
        when(productCartService.getProductCartByCartId("c1")).thenReturn(cart);

        mockMvc.perform(post("/admin/productCart/getProductCartByCartId").param("cartId", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId").value("c1"));
    }

    @Test
    void updateProductCartByCartId_delegates() throws Exception {
        mockMvc.perform(post("/admin/productCart/updateProductCartByCartId")
                        .param("cartId", "c1")
                        .param("buyCount", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).updateProductCartByCartId(any(ProductCart.class), any());
    }

    @Test
    void deleteProductCartByCartId_delegates() throws Exception {
        mockMvc.perform(post("/admin/productCart/deleteProductCartByCartId").param("cartId", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).deleteProductCartByCartId("c1");
    }

    @Test
    void getByProductAndHashAndUser_delegates() throws Exception {
        mockMvc.perform(post("/admin/productCart/getProductCartByProductIdAndPropertyValueIdHashAndUserId")
                        .param("productId", "P1")
                        .param("propertyValueIdHash", "h1")
                        .param("userId", "U1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).getProductCartByProductIdAndPropertyValueIdHashAndUserId("P1", "h1", "U1");
    }

    @Test
    void updateByProductAndHashAndUser_delegates() throws Exception {
        mockMvc.perform(post("/admin/productCart/updateProductCartByProductIdAndPropertyValueIdHashAndUserId")
                        .param("productId", "P1")
                        .param("propertyValueIdHash", "h1")
                        .param("userId", "U1")
                        .param("buyCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).updateProductCartByProductIdAndPropertyValueIdHashAndUserId(
                any(ProductCart.class), any(), any(), any());
    }

    @Test
    void deleteByProductAndHashAndUser_delegates() throws Exception {
        mockMvc.perform(post("/admin/productCart/deleteProductCartByProductIdAndPropertyValueIdHashAndUserId")
                        .param("productId", "P1")
                        .param("propertyValueIdHash", "h1")
                        .param("userId", "U1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).deleteProductCartByProductIdAndPropertyValueIdHashAndUserId("P1", "h1", "U1");
    }
}
