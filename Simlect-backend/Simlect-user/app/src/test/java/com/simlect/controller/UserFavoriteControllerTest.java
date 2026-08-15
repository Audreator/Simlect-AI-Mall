package com.simlect.controller;

import com.simlect.biz.UserProductFavoriteService;
import com.simlect.component.RedisComponent;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserFavoriteControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private UserProductFavoriteService userProductFavoriteService;

    @InjectMocks
    private UserFavoriteController userFavoriteController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userFavoriteController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
    }

    @Test
    void loadFavorite_ok() throws Exception {
        PaginationResultVO result = new PaginationResultVO();
        result.setTotalCount(1);
        when(userProductFavoriteService.loadFavoritePage("U1", 1)).thenReturn(result);

        mockMvc.perform(post("/userFavorite/loadFavorite").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void toggleFavorite_ok() throws Exception {
        when(userProductFavoriteService.toggleFavorite("U1", "P1")).thenReturn(true);

        mockMvc.perform(post("/userFavorite/toggleFavorite").param("productId", "P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(userProductFavoriteService).toggleFavorite("U1", "P1");
    }

    @Test
    void isFavorite_ok() throws Exception {
        when(userProductFavoriteService.isFavorite("U1", "P1")).thenReturn(false);

        mockMvc.perform(post("/userFavorite/isFavorite").param("productId", "P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void removeFavorite_ok() throws Exception {
        mockMvc.perform(post("/userFavorite/removeFavorite").param("favoriteId", "FAV1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(userProductFavoriteService).removeFavorite("U1", "FAV1");
    }
}
