package com.simlect.biz.impl;

import com.simlect.api.dto.ProductInfoDTO;
import com.simlect.biz.SearchKeywordService;
import com.simlect.component.EsSearchComponent;
import com.simlect.entity.vo.PaginationResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchRecommendServiceImplTest {

    @Mock
    private SearchKeywordService searchKeywordService;
    @Mock
    private EsSearchComponent esSearchComponent;

    @InjectMocks
    private SearchRecommendServiceImpl searchRecommendService;

    @Test
    void loadGuessKeywords_mergesRecentAndHot() {
        when(searchKeywordService.loadRecentKeywords("U1")).thenReturn(List.of("手机", "零食"));
        when(searchKeywordService.loadHotKeywords()).thenReturn(List.of("牛肉干", "女装", "手机"));

        List<String> keywords = searchRecommendService.loadGuessKeywords("U1");

        assertTrue(keywords.contains("手机"));
        assertTrue(keywords.contains("零食"));
        assertTrue(keywords.contains("牛肉干"));
        assertTrue(keywords.size() <= 10);
    }

    @Test
    void loadGuessKeywords_anonymous_usesHotOnly() {
        when(searchKeywordService.loadHotKeywords()).thenReturn(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"));

        List<String> keywords = searchRecommendService.loadGuessKeywords("");

        assertEquals(10, keywords.size());
        verify(searchKeywordService, never()).loadRecentKeywords(anyString());
    }

    @Test
    void loadRecommendProducts_usesFirstRecentKeyword() {
        when(searchKeywordService.loadRecentKeywords("U1")).thenReturn(List.of("手机"));
        ProductInfoDTO dto = new ProductInfoDTO();
        PaginationResultVO<ProductInfoDTO> page = new PaginationResultVO<>();
        page.setTotalCount(1);
        page.setList(List.of(dto));
        when(esSearchComponent.searchProducts(eq("手机"), isNull(), isNull(), eq("desc"),
                eq("totalSale"), eq(1))).thenReturn(page);

        List<ProductInfoDTO> result = searchRecommendService.loadRecommendProducts("U1", 8);

        assertEquals(1, result.size());
    }

    @Test
    void loadRecommendProducts_noRecent_usesFirstHotKeyword() {
        when(searchKeywordService.loadRecentKeywords("U1")).thenReturn(List.of());
        when(searchKeywordService.loadHotKeywords()).thenReturn(List.of("牛肉干"));
        when(esSearchComponent.searchProducts(eq("牛肉干"), isNull(), isNull(), eq("desc"),
                eq("totalSale"), eq(1))).thenReturn(null);

        List<ProductInfoDTO> result = searchRecommendService.loadRecommendProducts("U1", 8);

        assertTrue(result.isEmpty());
    }

    @Test
    void loadRecommendProducts_invalidLimit_usesDefault8() {
        when(searchKeywordService.loadRecentKeywords("U1")).thenReturn(List.of());
        when(searchKeywordService.loadHotKeywords()).thenReturn(List.of());
        PaginationResultVO<ProductInfoDTO> page = new PaginationResultVO<>();
        page.setList(List.of());
        when(esSearchComponent.searchProducts(eq(" "), isNull(), isNull(), eq("desc"),
                eq("totalSale"), eq(1))).thenReturn(page);

        List<ProductInfoDTO> result = searchRecommendService.loadRecommendProducts("U1", -1);

        assertTrue(result.isEmpty());
        // 无任何关键词兜底为空格搜索
        verify(esSearchComponent).searchProducts(eq(" "), isNull(), isNull(), eq("desc"),
                eq("totalSale"), eq(1));
    }

    @Test
    void loadRecommendProducts_limitsResultsTo20() {
        when(searchKeywordService.loadRecentKeywords("U1")).thenReturn(List.of("手机"));
        PaginationResultVO<ProductInfoDTO> page = new PaginationResultVO<>();
        java.util.List<ProductInfoDTO> many = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            many.add(new ProductInfoDTO());
        }
        page.setList(many);
        when(esSearchComponent.searchProducts(anyString(), isNull(), isNull(), eq("desc"),
                eq("totalSale"), eq(1))).thenReturn(page);

        List<ProductInfoDTO> result = searchRecommendService.loadRecommendProducts("U1", 100);

        assertEquals(20, result.size());
    }
}
