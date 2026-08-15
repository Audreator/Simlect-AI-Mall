package com.simlect.biz.impl;

import com.simlect.api.enums.CommendTypeEnum;
import com.simlect.api.enums.ProductStatusEnum;
import com.simlect.api.support.StockFeignSupport;
import com.simlect.component.ProductBloomFilterComponent;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.dto.ProductSaveDTO;
import com.simlect.entity.po.ProductInfo;
import com.simlect.entity.po.ProductPropertyValue;
import com.simlect.entity.po.ProductSku;
import com.simlect.entity.query.ProductInfoQuery;
import com.simlect.entity.query.ProductPropertyValueQuery;
import com.simlect.entity.query.ProductSkuQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.Product4VO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.ProductInfoMapper;
import com.simlect.mappers.ProductPropertyValueMapper;
import com.simlect.mappers.ProductSkuMapper;
import com.simlect.mappers.SysCategoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductInfoServiceImplTest {

    @Mock
    private ProductInfoMapper<ProductInfo, ProductInfoQuery> productInfoMapper;
    @Mock
    private ProductPropertyValueMapper productPropertyValueMapper;
    @Mock
    private ProductSkuMapper productSkuMapper;
    @Mock
    private SysCategoryMapper sysCategoryMapper;
    @Mock
    private ReliableMessageSender reliableMessageSender;
    @Mock
    private ProductBloomFilterComponent productBloomFilterComponent;
    @Mock
    private StockFeignSupport stockFeignSupport;

    @InjectMocks
    private ProductInfoServiceImpl productInfoService;

    @BeforeEach
    void setUpTransaction() {
        // saveProduct/deleteProduct 内部 registerSynchronization(afterCommit)，
        // 需要模拟一个激活的事务上下文
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void tearDownTransaction() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private ProductInfo buildProduct() {
        ProductInfo product = new ProductInfo();
        product.setProductId("P1");
        product.setProductName("测试商品");
        product.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        return product;
    }

    private ProductSku buildSku(String hash, String price) {
        ProductSku sku = new ProductSku();
        sku.setPropertyValueIdHash(hash);
        sku.setPrice(new BigDecimal(price));
        return sku;
    }

    @Test
    void findListByPage_normalQuery_usesStandardPaging() {
        when(productInfoMapper.selectCount(any())).thenReturn(20);
        when(productInfoMapper.selectList(any())).thenReturn(new ArrayList<>());
        ProductInfoQuery query = new ProductInfoQuery();
        query.setPageNo(1);

        PaginationResultVO<ProductInfo> result = productInfoService.findListByPage(query);

        assertEquals(20, result.getTotalCount());
        assertEquals(15, result.getPageSize());
        verify(productInfoMapper, never()).selectCountByCategoryUnion(any());
    }

    @Test
    void findListByPage_categoryUnion_preparesAndStripsP() {
        when(productInfoMapper.selectCountByCategoryUnion(any())).thenReturn(5);
        when(productInfoMapper.selectListByCategoryUnion(any())).thenReturn(new ArrayList<>());
        ProductInfoQuery query = new ProductInfoQuery();
        query.setCategoryIdOrPCategoryId("C1");
        query.setOrderBy("p.total_sale desc");

        PaginationResultVO<ProductInfo> result = productInfoService.findListByPage(query);

        assertEquals(5, result.getTotalCount());
        assertTrue(query.getCategoryUnionQuery());
        assertEquals("total_sale desc", query.getOrderBy());
        verify(productInfoMapper, never()).selectCount(any());
    }

    @Test
    void getProductInfoByProductId_bloomMiss_returnsNull() {
        when(productBloomFilterComponent.mightExist("P1")).thenReturn(false);

        assertNull(productInfoService.getProductInfoByProductId("P1"));
        verify(productInfoMapper, never()).selectByProductId(anyString());
    }

    @Test
    void getProductInfoByProductId_bloomHit_addsToBloom() {
        when(productBloomFilterComponent.mightExist("P1")).thenReturn(true);
        when(productInfoMapper.selectByProductId("P1")).thenReturn(buildProduct());

        assertEquals("测试商品", productInfoService.getProductInfoByProductId("P1").getProductName());
        verify(productBloomFilterComponent).add("P1");
    }

    @Test
    void saveProduct_add_generatesIdAndInsertsAll() {
        ProductSaveDTO dto = new ProductSaveDTO();
        ProductInfo product = buildProduct();
        product.setProductId(null);
        product.setCommendType(CommendTypeEnum.COMMEND.getType());
        product.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        dto.setProductInfo(product);
        ProductSku skuLow = buildSku("h1", "10.00");
        ProductSku skuHigh = buildSku("h2", "99.00");
        dto.setSkuList(new ArrayList<>(List.of(skuLow, skuHigh)));
        ProductPropertyValue property = new ProductPropertyValue();
        dto.setProductPropertyList(new ArrayList<>(List.of(property)));

        productInfoService.saveProduct(dto);

        assertNotNull(product.getProductId());
        assertEquals(ProductStatusEnum.OFF_SALE.getStatus(), product.getStatus());
        assertNull(product.getCommendType());
        assertEquals(0, new BigDecimal("10.00").compareTo(product.getMinPrice()));
        assertEquals(0, new BigDecimal("99.00").compareTo(product.getMaxPrice()));
        assertEquals(product.getProductId(), skuLow.getProductId());
        verify(productInfoMapper).insert(product);
        verify(productPropertyValueMapper).insertBatch(List.of(property));
        verify(productSkuMapper).insertBatch(anyList());
        verify(productBloomFilterComponent).add(product.getProductId());
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.RAG_EXCHANGE),
                eq(RabbitMQConfig.RAG_QUEUE_KEY), any(), anyString(), any());
    }

    @Test
    void saveProduct_update_onlyUpdatesMainInfo() {
        ProductSaveDTO dto = new ProductSaveDTO();
        ProductInfo product = buildProduct();
        dto.setProductInfo(product);
        dto.setSkuList(new ArrayList<>());
        dto.setProductPropertyList(new ArrayList<>());
        when(productPropertyValueMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(productSkuMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(productInfoMapper.updateByProductId(any(), anyString())).thenReturn(1);

        productInfoService.saveProduct(dto);

        verify(productInfoMapper).updateByProductId(product, "P1");
        verify(productPropertyValueMapper, never()).insertBatch(anyList());
        verify(productSkuMapper, never()).insertBatch(anyList());
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.RAG_EXCHANGE),
                eq(RabbitMQConfig.RAG_QUEUE_KEY), any(), anyString(), any());
    }

    @Test
    void updateProductStatus_invalidStatus_throws() {
        assertThrows(BusinessException.class, () -> productInfoService.updateProductStatus("P1", 99));
        assertThrows(BusinessException.class, () -> productInfoService.updateProductStatus("P1", ProductStatusEnum.DELETE.getStatus()));
    }

    @Test
    void updateProductStatus_offSaleWithCommend_throws() {
        when(productInfoMapper.selectByProductId("P1")).thenReturn(buildProduct());
        ProductInfo product = buildProduct();
        product.setCommendType(CommendTypeEnum.COMMEND.getType());
        when(productInfoMapper.selectByProductId("P1")).thenReturn(product);

        BusinessException e = assertThrows(BusinessException.class,
                () -> productInfoService.updateProductStatus("P1", ProductStatusEnum.OFF_SALE.getStatus()));
        assertTrue(e.getMessage().contains("请先取消推荐"));
    }

    @Test
    void updateProductStatus_success() {
        when(productInfoMapper.selectByProductId("P1")).thenReturn(buildProduct());

        productInfoService.updateProductStatus("P1", ProductStatusEnum.OFF_SALE.getStatus());

        verify(productInfoMapper).updateByProductId(argThat(p ->
                ProductStatusEnum.OFF_SALE.getStatus().equals(p.getStatus())), eq("P1"));
        verify(reliableMessageSender, atLeastOnce()).sendMessage(anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void deleteProduct_notFound_throws() {
        when(productInfoMapper.selectByProductId("P1")).thenReturn(null);

        assertThrows(BusinessException.class, () -> productInfoService.deleteProduct("P1"));
    }

    @Test
    void deleteProduct_onSale_throws() {
        when(productInfoMapper.selectByProductId("P1")).thenReturn(buildProduct());

        BusinessException e = assertThrows(BusinessException.class,
                () -> productInfoService.deleteProduct("P1"));
        assertTrue(e.getMessage().contains("请先下架"));
    }

    @Test
    void deleteProduct_success_marksDelete() {
        ProductInfo product = buildProduct();
        product.setStatus(ProductStatusEnum.OFF_SALE.getStatus());
        when(productInfoMapper.selectByProductId("P1")).thenReturn(product);

        productInfoService.deleteProduct("P1");

        verify(productInfoMapper).updateByProductId(argThat(p ->
                ProductStatusEnum.DELETE.getStatus().equals(p.getStatus())), eq("P1"));
    }

    @Test
    void commendProduct_notOnSale_throws() {
        ProductInfo product = buildProduct();
        product.setStatus(ProductStatusEnum.OFF_SALE.getStatus());
        when(productInfoMapper.selectByProductId("P1")).thenReturn(product);

        BusinessException e = assertThrows(BusinessException.class,
                () -> productInfoService.commendProduct("P1", CommendTypeEnum.COMMEND.getType()));
        assertTrue(e.getMessage().contains("仅已上架商品可设为推荐"));
    }

    @Test
    void commendProduct_success_addsBloom() {
        ProductInfo product = buildProduct();
        product.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        when(productInfoMapper.selectByProductId("P1")).thenReturn(product);

        productInfoService.commendProduct("P1", CommendTypeEnum.COMMEND.getType());

        verify(productInfoMapper).updateByProductId(argThat(p ->
                CommendTypeEnum.COMMEND.getType().equals(p.getCommendType())), eq("P1"));
        verify(productBloomFilterComponent).add("P1");
    }

    @Test
    void getProduct4VOByProductId_bloomMiss_throws() {
        when(productBloomFilterComponent.mightExist("P1")).thenReturn(false);

        assertThrows(BusinessException.class, () -> productInfoService.getProduct4VOByProductId("P1"));
    }

    @Test
    void getProduct4VOByProductId_groupsPropertiesAndStocks() {
        when(productBloomFilterComponent.mightExist("P1")).thenReturn(true);
        when(productInfoMapper.selectByProductId("P1")).thenReturn(buildProduct());
        ProductPropertyValue pv1 = new ProductPropertyValue();
        pv1.setPropertyId("prop1");
        pv1.setPropertyValueId("v1");
        pv1.setPropertyName("颜色");
        pv1.setPropertyValue("红色");
        ProductPropertyValue pv2 = new ProductPropertyValue();
        pv2.setPropertyId("prop1");
        pv2.setPropertyValueId("v2");
        pv2.setPropertyName("颜色");
        pv2.setPropertyValue("蓝色");
        when(productPropertyValueMapper.selectList(any())).thenReturn(List.of(pv1, pv2));
        ProductSku sku = buildSku("h1", "10.00");
        sku.setProductId("P1");
        when(productSkuMapper.selectList(any())).thenReturn(List.of(sku));
        when(stockFeignSupport.getAvailable("P1", "h1")).thenReturn(5);

        Product4VO vo = productInfoService.getProduct4VOByProductId("P1");

        assertEquals(1, vo.getProductPropertyList().size());
        assertEquals(2, vo.getProductPropertyList().get(0).getPropertyValues().size());
        assertEquals(5, vo.getSkuList().get(0).getStock());
        verify(productBloomFilterComponent).add("P1");
    }

    @Test
    void findListByPage4ListVO_enrichesLoadData() {
        when(productInfoMapper.selectCount(any())).thenReturn(1);
        when(productInfoMapper.selectList(any())).thenReturn(List.of(buildProduct()));
        when(stockFeignSupport.totalByProduct("P1")).thenReturn(10);
        when(productSkuMapper.selectCountByProductId("P1")).thenReturn(3);
        when(sysCategoryMapper.selectNameByCategoryId(any())).thenReturn("零食");

        PaginationResultVO result = productInfoService.findListByPage4ListVO(new ProductInfoQuery());

        assertEquals(1, result.getTotalCount());
    }

    @Test
    void updateTotalSaleByCount_emptyMap_ignored() {
        productInfoService.updateTotalSaleByCount(null);
        productInfoService.updateTotalSaleByCount(Map.of());

        verifyNoInteractions(productInfoMapper, reliableMessageSender);
    }

    @Test
    void updateTotalSaleByCount_updatesAndNotifiesEs() {
        productInfoService.updateTotalSaleByCount(Map.of("P1", 5));

        verify(productInfoMapper).updateTotalSaleByCount(Map.of("P1", 5));
        verify(reliableMessageSender, times(1)).sendMessage(anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void updateSkuStock_isDeprecated() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> productInfoService.updateSkuStock("S1", 10));
        assertTrue(e.getMessage().contains("propertyValueIdHash"));
    }
}
