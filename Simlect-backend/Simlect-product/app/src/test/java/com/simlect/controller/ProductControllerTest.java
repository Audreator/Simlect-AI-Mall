package com.simlect.controller;

import com.simlect.api.dto.BrowseHistoryMessageDTO;
import com.simlect.biz.ProductInfoService;
import com.simlect.biz.SysCategoryService;
import com.simlect.component.RedisComponent;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.ProductInfo;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.Product4VO;
import com.simlect.exception.BusinessException;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private SysCategoryService sysCategoryService;
    @Mock
    private ProductInfoService productInfoService;
    @Mock
    private ReliableMessageSender reliableMessageSender;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadCategory_ok() throws Exception {
        when(sysCategoryService.findListByParam(any())).thenReturn(List.of());

        mockMvc.perform(get("/product/loadCategory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void loadCommendProduct_ok() throws Exception {
        PaginationResultVO<ProductInfo> page = new PaginationResultVO<>();
        page.setList(new ArrayList<>());
        when(productInfoService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(get("/product/loadCommendProduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(productInfoService).findListByPage(argThat(q ->
                1 == q.getCommendType() && 1 == q.getStatus()));
    }

    @Test
    void loadProduct_noCategory_setsDefaultOrder() throws Exception {
        PaginationResultVO<ProductInfo> page = new PaginationResultVO<>();
        page.setList(new ArrayList<>());
        when(productInfoService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/product/loadProduct")
                        .param("pageNo", "1")
                        .param("priceFrom", "10")
                        .param("sortField", "price")
                        .param("sortType", "asc"))
                .andExpect(status().isOk());

        verify(productInfoService).findListByPage(argThat(q ->
                "min_price asc, total_sale desc".equals(q.getOrderBy())
                        && q.getCategoryIdOrPCategoryId() == null));
    }

    @Test
    void loadProduct_withCategory_usesCategoryUnion() throws Exception {
        PaginationResultVO<ProductInfo> page = new PaginationResultVO<>();
        page.setList(new ArrayList<>());
        when(productInfoService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/product/loadProduct")
                        .param("pageNo", "2")
                        .param("categoryId", "C1")
                        .param("sortField", "sale"))
                .andExpect(status().isOk());

        verify(productInfoService).findListByPage(argThat(q ->
                "total_sale desc, min_price asc".equals(q.getOrderBy())
                        && "C1".equals(q.getCategoryIdOrPCategoryId())));
    }

    @Test
    void getProduct_withToken_sendsBrowseMq() throws Exception {
        when(productInfoService.getProduct4VOByProductId("P1")).thenReturn(new Product4VO());
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);

        mockMvc.perform(post("/product/getProduct").param("productId", "P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.BROWSE_EXCHANGE),
                eq(RabbitMQConfig.BROWSE_RECORD_KEY), any(BrowseHistoryMessageDTO.class), anyString(), any());
    }

    @Test
    void getProduct_anonymous_skipsBrowseMq() throws Exception {
        when(productInfoService.getProduct4VOByProductId("P1")).thenReturn(new Product4VO());
        when(authCookieHelper.resolveWebToken(any())).thenReturn(null);

        mockMvc.perform(post("/product/getProduct").param("productId", "P1"))
                .andExpect(status().isOk());

        verifyNoInteractions(reliableMessageSender);
    }

    @Test
    void search_isUnsupported() throws Exception {
        mockMvc.perform(post("/product/search").param("keyWords", "手机"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.info").value("请使用搜索服务接口 /api/search 完成商品搜索"));
    }
}
