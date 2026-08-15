package com.simlect.biz.impl;

import com.simlect.api.dto.SkuStockDTO;
import com.simlect.api.enums.ProductStatusEnum;
import com.simlect.api.support.StockFeignSupport;
import com.simlect.api.vo.ProductSkuListVO;
import com.simlect.biz.ProductInfoService;
import com.simlect.entity.po.ProductInfo;
import com.simlect.entity.po.ProductPropertyValue;
import com.simlect.entity.po.ProductSku;
import com.simlect.entity.query.ProductSkuQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.ProductInfoMapper;
import com.simlect.mappers.ProductPropertyValueMapper;
import com.simlect.mappers.ProductSkuMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSkuServiceImplTest {

    @Mock
    private ProductSkuMapper<ProductSku, ProductSkuQuery> productSkuMapper;
    @Mock
    private StockFeignSupport stockFeignSupport;
    @Mock
    private ProductInfoService productInfoService;
    @Mock
    private ProductInfoMapper productInfoMapper;
    @Mock
    private ProductPropertyValueMapper productPropertyValueMapper;

    @InjectMocks
    private ProductSkuServiceImpl productSkuService;

    private ProductSku buildSku() {
        ProductSku sku = new ProductSku();
        sku.setProductId("P1");
        sku.setPropertyValueIdHash("hash1");
        sku.setPropertyValueIds("v1-v2");
        sku.setPrice(new BigDecimal("19.90"));
        return sku;
    }

    @Test
    void findListByPage_usesDefaultPageSize() {
        when(productSkuMapper.selectCount(any())).thenReturn(30);
        when(productSkuMapper.selectList(any())).thenReturn(new ArrayList<>());
        ProductSkuQuery query = new ProductSkuQuery();
        query.setPageNo(1);

        PaginationResultVO<ProductSku> result = productSkuService.findListByPage(query);

        assertEquals(30, result.getTotalCount());
        assertEquals(15, result.getPageSize());
    }

    @Test
    void updateStock_skuMissing_throws() {
        when(productSkuMapper.selectByProductIdAndPropertyValueIdHash("P1", "hash1")).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> productSkuService.updateStock("P1", "hash1", 5));
        verify(stockFeignSupport, never()).changeStock(anyString(), anyString(), anyInt());
    }

    @Test
    void updateStock_success_delegatesToStockService() {
        when(productSkuMapper.selectByProductIdAndPropertyValueIdHash("P1", "hash1")).thenReturn(buildSku());

        productSkuService.updateStock("P1", "hash1", 5);

        verify(stockFeignSupport).changeStock("P1", "hash1", 5);
    }

    @Test
    void findListByPage4ListVO_enrichesSkus() {
        ProductSku sku = buildSku();
        when(productSkuMapper.selectCount(any())).thenReturn(1);
        when(productSkuMapper.selectList(any())).thenReturn(List.of(sku));
        ProductInfo product = new ProductInfo();
        product.setProductId("P1");
        product.setProductName("测试商品");
        product.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        product.setCover("a.jpg,b.jpg");
        when(productInfoService.getProductInfoByProductId("P1")).thenReturn(product);
        when(stockFeignSupport.getAvailable("P1", "hash1")).thenReturn(8);
        ProductPropertyValue pv1 = new ProductPropertyValue();
        pv1.setPropertyName("颜色");
        pv1.setPropertyValue("红色");
        when(productPropertyValueMapper.selectByProductIdAndPropertyValueId("P1", "v1")).thenReturn(pv1);
        when(productPropertyValueMapper.selectByProductIdAndPropertyValueId("P1", "v2")).thenReturn(null);

        PaginationResultVO<ProductSkuListVO> result = productSkuService.findListByPage4ListVO(new ProductSkuQuery());

        ProductSkuListVO vo = result.getList().get(0);
        assertEquals("测试商品", vo.getProductName());
        assertTrue(vo.getProductOnsale());
        assertEquals(8, vo.getStock());
        assertEquals("a.jpg", vo.getProductCover());
        assertEquals(1, vo.getPropertyData().size());
    }

    @Test
    void lessStockSkuPage_filtersOrphanSkusAndPaginates() {
        SkuStockDTO stock = new SkuStockDTO();
        stock.setProductId("P1");
        stock.setPropertyValueIdHash("hash1");
        stock.setStock(3);
        PaginationResultVO<SkuStockDTO> stockPage = new PaginationResultVO<>();
        stockPage.setTotalCount(1);
        stockPage.setPageSize(100);
        stockPage.setPageNo(1);
        stockPage.setPageTotal(1);
        stockPage.setList(List.of(stock));
        when(stockFeignSupport.listLessThan(1, 100, 10)).thenReturn(stockPage);
        when(productSkuMapper.selectByProductIdAndPropertyValueIdHash("P1", "hash1")).thenReturn(buildSku());
        ProductInfo product = new ProductInfo();
        product.setProductId("P1");
        product.setProductName("低库存商品");
        product.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        when(productInfoService.getProductInfoByProductId("P1")).thenReturn(product);

        PaginationResultVO<ProductSkuListVO> result = productSkuService.lessStockSkuPage(1, 15, 10);

        assertEquals(1, result.getTotalCount());
        assertEquals("低库存商品", result.getList().get(0).getProductName());
        assertEquals(3, result.getList().get(0).getStock());
    }

    @Test
    void lessStockSkuPage_orphanSku_excluded() {
        SkuStockDTO stock = new SkuStockDTO();
        stock.setProductId("P1");
        stock.setPropertyValueIdHash("hash1");
        stock.setStock(3);
        PaginationResultVO<SkuStockDTO> stockPage = new PaginationResultVO<>();
        stockPage.setTotalCount(1);
        stockPage.setPageSize(100);
        stockPage.setPageNo(1);
        stockPage.setPageTotal(1);
        stockPage.setList(List.of(stock));
        when(stockFeignSupport.listLessThan(1, 100, 10)).thenReturn(stockPage);
        when(productSkuMapper.selectByProductIdAndPropertyValueIdHash("P1", "hash1")).thenReturn(null);

        PaginationResultVO<ProductSkuListVO> result = productSkuService.lessStockSkuPage(1, 15, 10);

        assertEquals(0, result.getTotalCount());
    }
}
