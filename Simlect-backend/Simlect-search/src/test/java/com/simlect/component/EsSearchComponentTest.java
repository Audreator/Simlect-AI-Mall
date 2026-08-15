package com.simlect.component;

import com.simlect.api.dto.ProductInfoDTO;
import com.simlect.api.enums.ProductStatusEnum;
import com.simlect.api.support.ProductFeignSupport;
import com.simlect.api.vo.ProductSearchIndexVO;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EsSearchComponentTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Mock
    private ProductFeignSupport productFeignSupport;

    @InjectMocks
    private EsSearchComponent esSearchComponent;

    @Test
    void createIndexWithIK_indexExists_skipsCreation() {
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(elasticsearchOperations.indexOps(ProductInfoDTO.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);

        esSearchComponent.createIndexWithIK();

        verify(indexOperations, never()).createWithMapping();
    }

    @Test
    void createIndexWithIK_createsMappingWhenMissing() {
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(elasticsearchOperations.indexOps(ProductInfoDTO.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);
        when(indexOperations.createWithMapping()).thenReturn(true);

        esSearchComponent.createIndexWithIK();

        verify(indexOperations).createWithMapping();
    }

    @Test
    void createIndexWithIK_swallowsEsErrors() {
        when(elasticsearchOperations.indexOps(ProductInfoDTO.class))
                .thenThrow(new RuntimeException("ES not installed"));

        assertDoesNotThrow(() -> esSearchComponent.createIndexWithIK());
    }

    @Test
    void saveIndex_productGone_deletesIndex() {
        when(productFeignSupport.getSearchIndex("P1")).thenReturn(null);

        esSearchComponent.saveIndex("P1");

        verify(elasticsearchOperations).delete(any(ProductInfoDTO.class));
    }

    @Test
    void saveIndex_onSale_savesDocument() {
        ProductSearchIndexVO vo = new ProductSearchIndexVO();
        vo.setProductId("P1");
        vo.setProductName("牛肉干");
        vo.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        when(productFeignSupport.getSearchIndex("P1")).thenReturn(vo);

        esSearchComponent.saveIndex("P1");

        verify(elasticsearchOperations).save(argThat((ProductInfoDTO dto) ->
                "牛肉干".equals(dto.getProductName())));
        verify(elasticsearchOperations, never()).delete(any());
    }

    @Test
    void saveIndex_offSale_deletesDocument() {
        ProductSearchIndexVO vo = new ProductSearchIndexVO();
        vo.setProductId("P1");
        vo.setStatus(ProductStatusEnum.OFF_SALE.getStatus());
        when(productFeignSupport.getSearchIndex("P1")).thenReturn(vo);

        esSearchComponent.saveIndex("P1");

        verify(elasticsearchOperations).delete(any(ProductInfoDTO.class));
    }

    @Test
    void searchProducts_mapsHitsAndTotal() {
        @SuppressWarnings("unchecked")
        SearchHits<ProductInfoDTO> hits = mock(SearchHits.class);
        when(hits.getTotalHits()).thenReturn(2L);
        ProductInfoDTO dto = new ProductInfoDTO();
        dto.setProductId("P1");
        @SuppressWarnings("unchecked")
        SearchHit<ProductInfoDTO> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(dto);
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(ProductInfoDTO.class)))
                .thenReturn(hits);

        PaginationResultVO<ProductInfoDTO> result =
                esSearchComponent.searchProducts("牛肉干", null, null, "asc", "price", 1);

        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getList().size());
        assertEquals("P1", result.getList().get(0).getProductId());
        assertEquals(1, result.getPageNo());
    }

    @Test
    void searchProducts_nullPageNo_defaultsTo1() {
        @SuppressWarnings("unchecked")
        SearchHits<ProductInfoDTO> hits = mock(SearchHits.class);
        when(hits.getTotalHits()).thenReturn(0L);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(ProductInfoDTO.class)))
                .thenReturn(hits);

        PaginationResultVO<ProductInfoDTO> result =
                esSearchComponent.searchProducts("短", null, null, null, null, null);

        assertEquals(1, result.getPageNo());
        // 短词（<=2 字）走精准+匹配+通配三路 OR 条件
        verify(elasticsearchOperations).search(any(CriteriaQuery.class), eq(ProductInfoDTO.class));
    }

    @Test
    void searchProducts_priceRange_combinesCriteria() {
        @SuppressWarnings("unchecked")
        SearchHits<ProductInfoDTO> hits = mock(SearchHits.class);
        when(hits.getTotalHits()).thenReturn(0L);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(ProductInfoDTO.class)))
                .thenReturn(hits);

        esSearchComponent.searchProducts("牛肉干", new java.math.BigDecimal("10"),
                new java.math.BigDecimal("100"), "desc", "sale", 2);

        // 价格区间会同时构造 minPrice >= from 与 maxPrice <= to
        verify(elasticsearchOperations).search(org.mockito.Mockito.<CriteriaQuery>argThat(query ->
                        query.getCriteria().getCriteriaChain().size() >= 3),
                eq(ProductInfoDTO.class));
    }

    @Test
    void searchProducts_esFailure_wrapsBusinessException() {
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(ProductInfoDTO.class)))
                .thenThrow(new RuntimeException("ES down"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> esSearchComponent.searchProducts("牛肉干", null, null, null, null, 1));
        assertTrue(e.getMessage().contains("搜索服务暂时不可用"));
    }

    @Test
    void searchProducts_emptyKeyword_matchAll() {
        @SuppressWarnings("unchecked")
        SearchHits<ProductInfoDTO> hits = mock(SearchHits.class);
        when(hits.getTotalHits()).thenReturn(0L);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(ProductInfoDTO.class)))
                .thenReturn(hits);

        PaginationResultVO<ProductInfoDTO> result = esSearchComponent.searchProducts("", null, null, null, null, 1);

        assertEquals(1, result.getPageNo());
    }
}
