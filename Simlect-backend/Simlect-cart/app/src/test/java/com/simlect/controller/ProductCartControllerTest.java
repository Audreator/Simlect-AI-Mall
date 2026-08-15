package com.simlect.controller;

import com.simlect.biz.ProductCartService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.ProductCart;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.api.vo.ProductCartVO;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProductCartController 加购/列表/删除接口单元测试（MockMvc standalone）。
 */
@ExtendWith(MockitoExtension.class)
class ProductCartControllerTest {

    @Mock
    private ProductCartService productCartService;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;

    private MockMvc mockMvc;

    private static final String TOKEN = "token-abc";
    private static final String USER_ID = "U001";

    @BeforeEach
    void setUp() {
        ProductCartController controller = new ProductCartController();
        ReflectionTestUtils.setField(controller, "productCartService", productCartService);
        ReflectionTestUtils.setField(controller, "redisComponent", redisComponent);
        ReflectionTestUtils.setField(controller, "authCookieHelper", authCookieHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void mockLogin() {
        TokenUserInfoDTO user = new TokenUserInfoDTO();
        user.setUserId(USER_ID);
        doReturn(TOKEN).when(authCookieHelper).resolveWebToken(any());
        when(redisComponent.getTokenUserInfo(TOKEN)).thenReturn(user);
    }

    @Test
    void add2Cart_withoutLogin_throws() {
        doReturn(null).when(authCookieHelper).resolveWebToken(any());

        assertThrows(jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(post("/productCart/add2Cart")
                        .param("productId", "P1")
                        .param("propertyValueIds", "pv1")
                        .param("buyCount", "2")));

        verify(productCartService, never()).add2Cart(any(ProductCart.class));
    }

    @Test
    void add2Cart_withLogin_setsUserIdAndDelegates() throws Exception {
        mockLogin();

        mockMvc.perform(post("/productCart/add2Cart")
                        .param("productId", "P1")
                        .param("propertyValueIds", "pv1")
                        .param("buyCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).add2Cart(any(ProductCart.class));
    }

    @Test
    void loadProductCart_returnsPage() throws Exception {
        mockLogin();
        PaginationResultVO<ProductCartVO> page = new PaginationResultVO<>(0, 15, 1, 0, java.util.Collections.emptyList());
        when(productCartService.findListByPageAndUserId(any(), anyString())).thenReturn(page);

        mockMvc.perform(post("/productCart/loadProductCart").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalCount").value(0));

        verify(productCartService).findListByPageAndUserId(any(), anyString());
    }

    @Test
    void deleteCart_deletesByCartIdAndUserId() throws Exception {
        mockLogin();

        mockMvc.perform(post("/productCart/deleteCart").param("cartId", "cart1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productCartService).deleteByParam(any());
    }

    @Test
    void deleteCart_missingCartId_returnsClientError() throws Exception {
        mockMvc.perform(post("/productCart/deleteCart"))
                .andExpect(status().isBadRequest());
    }
}
