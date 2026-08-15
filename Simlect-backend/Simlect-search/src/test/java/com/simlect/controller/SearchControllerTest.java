package com.simlect.controller;

import com.simlect.biz.SearchKeywordService;
import com.simlect.biz.SearchRecommendService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private SearchKeywordService searchKeywordService;
    @Mock
    private SearchRecommendService searchRecommendService;

    @InjectMocks
    private SearchController searchController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
        lenient().when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        lenient().when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
    }

    @Test
    void loadHotKeywords_public() throws Exception {
        when(searchKeywordService.loadHotKeywords()).thenReturn(List.of("牛肉干"));

        mockMvc.perform(get("/search/loadHotKeywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("牛肉干"));
    }

    @Test
    void loadRecentKeywords_ok() throws Exception {
        when(searchKeywordService.loadRecentKeywords("U1")).thenReturn(List.of("手机"));

        mockMvc.perform(get("/search/loadRecentKeywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("手机"));
    }

    @Test
    void saveKeyword_ok() throws Exception {
        mockMvc.perform(post("/search/saveKeyword").param("keyword", "手机"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(searchKeywordService).saveUserKeyword("U1", "手机");
    }

    @Test
    void clearRecentKeywords_ok() throws Exception {
        mockMvc.perform(post("/search/clearRecentKeywords"))
                .andExpect(status().isOk());

        verify(searchKeywordService).clearUserKeywords("U1");
    }

    @Test
    void removeRecentKeyword_ok() throws Exception {
        mockMvc.perform(post("/search/removeRecentKeyword").param("keyword", "手机"))
                .andExpect(status().isOk());

        verify(searchKeywordService).removeUserKeyword("U1", "手机");
    }

    @Test
    void loadGuessKeywords_ok() throws Exception {
        when(searchRecommendService.loadGuessKeywords("U1")).thenReturn(List.of("手机"));

        mockMvc.perform(get("/search/loadGuessKeywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("手机"));
    }

    @Test
    void loadRecommendProducts_ok() throws Exception {
        when(searchRecommendService.loadRecommendProducts("U1", 8)).thenReturn(List.of());

        mockMvc.perform(get("/search/loadRecommendProducts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(searchRecommendService).loadRecommendProducts(eq("U1"), eq(8));
    }
}
