package com.simlect.biz.impl;

import com.simlect.api.dto.ProductSnapshotBatchVO;
import com.simlect.api.support.ProductFeignSupport;
import com.simlect.api.support.StockFeignSupport;
import com.simlect.api.enums.ProductStatusEnum;
import com.simlect.api.vo.ProductCartVO;
import com.simlect.api.vo.ProductInfoSnapshotVO;
import com.simlect.api.vo.ProductPropertyValueSnapshotVO;
import com.simlect.api.vo.ProductSkuSnapshotVO;
import com.simlect.entity.po.ProductCart;
import com.simlect.entity.query.ProductCartQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.ProductCartMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductCartServiceImpl 加购/改数量/删除/合并/金额汇总单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ProductCartServiceImplTest {

    @Mock
    private ProductCartMapper<ProductCart, ProductCartQuery> productCartMapper;
    @Mock
    private StockFeignSupport stockFeignSupport;
    @Mock
    private ProductFeignSupport productFeignSupport;

    @InjectMocks
    private ProductCartServiceImpl productCartService;

    private static final String PRODUCT_ID = "P001";
    private static final String USER_ID = "U001";
    private static final String PROPERTY_IDS = "pv1-pv2";
    private static final String PROPERTY_HASH = com.simlect.utils.StringTools.encodeByMD5(PROPERTY_IDS);

    private ProductCart newCart(String productId, String propertyValueIds, Integer buyCount) {
        ProductCart cart = new ProductCart();
        cart.setProductId(productId);
        cart.setPropertyValueIds(propertyValueIds);
        cart.setBuyCount(buyCount);
        cart.setUserId(USER_ID);
        return cart;
    }

    private ProductSkuSnapshotVO sku(String productId, String propertyValueIds, String price) {
        ProductSkuSnapshotVO sku = new ProductSkuSnapshotVO();
        sku.setProductId(productId);
        sku.setPropertyValueIds(propertyValueIds);
        sku.setPropertyValueIdHash("hash-" + propertyValueIds);
        sku.setPrice(new BigDecimal(price));
        return sku;
    }

    // ==================== add2Cart：新增 ====================

    @Test
    void add2Cart_notInCart_insertsWithSnapshotPrice() {
        ProductCart cart = newCart(PRODUCT_ID, PROPERTY_IDS, 2);
        when(productCartMapper.selectByProductIdAndPropertyValueIdHashAndUserId(
                PRODUCT_ID, PROPERTY_HASH, USER_ID)).thenReturn(null);
        ProductSnapshotBatchVO snapshot = new ProductSnapshotBatchVO();
        snapshot.setSkus(List.of(sku(PRODUCT_ID, PROPERTY_IDS, "99.90")));
        when(productFeignSupport.snapshotBatch(Collections.singletonList(PRODUCT_ID))).thenReturn(snapshot);
        when(productFeignSupport.toSkuMapByPropertyValueIds(snapshot))
                .thenReturn(Map.of(PRODUCT_ID + PROPERTY_IDS, sku(PRODUCT_ID, PROPERTY_IDS, "99.90")));
        when(productCartMapper.insert(any(ProductCart.class))).thenReturn(1);

        productCartService.add2Cart(cart);

        assertEquals(PROPERTY_IDS, cart.getPropertyValueIds());
        assertEquals(PROPERTY_HASH, cart.getPropertyValueIdHash());
        assertEquals(new BigDecimal("99.90"), cart.getAddPrice());
        assertNotNull(cart.getCartId());
        assertEquals(15, cart.getCartId().length());
        verify(productCartMapper).insert(cart);
    }

    // ==================== add2Cart：已存在则改数量（保留首次单价） ====================

    @Test
    void add2Cart_exists_updateCountAndLastUpdateOnly() {
        ProductCart cart = newCart(PRODUCT_ID, PROPERTY_IDS, 5);
        ProductCart existing = newCart(PRODUCT_ID, PROPERTY_IDS, 1);
        existing.setAddPrice(new BigDecimal("10.00"));
        when(productCartMapper.selectByProductIdAndPropertyValueIdHashAndUserId(
                PRODUCT_ID, PROPERTY_HASH, USER_ID)).thenReturn(existing);

        productCartService.add2Cart(cart);

        verify(productCartMapper).setBuyCountByProductIdAndPropertyValueIdHashAndUserId(
                5, PRODUCT_ID, PROPERTY_HASH, USER_ID);
        verify(productCartMapper).updateByProductIdAndPropertyValueIdHashAndUserId(
                cart, PRODUCT_ID, PROPERTY_HASH, USER_ID);
        // addPrice 已存在则只更新一次（不打价格补丁）
        verify(productCartMapper, times(1))
                .updateByProductIdAndPropertyValueIdHashAndUserId(any(ProductCart.class), eq(PRODUCT_ID), eq(PROPERTY_HASH), eq(USER_ID));
        verify(productCartMapper, never()).insert(any());
    }

    @Test
    void add2Cart_existsButAddPriceMissing_patchesPrice() {
        ProductCart cart = newCart(PRODUCT_ID, PROPERTY_IDS, 5);
        ProductCart existing = newCart(PRODUCT_ID, PROPERTY_IDS, 1);
        existing.setAddPrice(null);
        when(productCartMapper.selectByProductIdAndPropertyValueIdHashAndUserId(
                PRODUCT_ID, PROPERTY_HASH, USER_ID)).thenReturn(existing);
        ProductSnapshotBatchVO snapshot = new ProductSnapshotBatchVO();
        snapshot.setSkus(List.of(sku(PRODUCT_ID, PROPERTY_IDS, "66.00")));
        when(productFeignSupport.snapshotBatch(Collections.singletonList(PRODUCT_ID))).thenReturn(snapshot);
        when(productFeignSupport.toSkuMapByPropertyValueIds(snapshot))
                .thenReturn(Map.of(PRODUCT_ID + PROPERTY_IDS, sku(PRODUCT_ID, PROPERTY_IDS, "66.00")));

        productCartService.add2Cart(cart);

        verify(productCartMapper).setBuyCountByProductIdAndPropertyValueIdHashAndUserId(
                5, PRODUCT_ID, PROPERTY_HASH, USER_ID);
        // 补价 update 调用 2 次（一次常规更新 + 一次价格补丁）
        verify(productCartMapper, times(2))
                .updateByProductIdAndPropertyValueIdHashAndUserId(any(ProductCart.class), eq(PRODUCT_ID), eq(PROPERTY_HASH), eq(USER_ID));
        verify(productCartMapper, never()).insert(any());
    }

    // ==================== add2Cart：默认 SKU 解析 ====================

    @Test
    void add2Cart_emptyPropertyValueIds_resolvesDefaultSku() {
        ProductCart cart = newCart(PRODUCT_ID, "", 1);
        ProductSkuSnapshotVO defaultSku = sku(PRODUCT_ID, "d1-d2", "50.00");
        when(productFeignSupport.defaultSku(PRODUCT_ID)).thenReturn(defaultSku);
        when(productCartMapper.selectByProductIdAndPropertyValueIdHashAndUserId(
                eq(PRODUCT_ID), anyString(), eq(USER_ID))).thenReturn(null);
        ProductSnapshotBatchVO snapshot = new ProductSnapshotBatchVO();
        snapshot.setSkus(List.of(defaultSku));
        when(productFeignSupport.snapshotBatch(Collections.singletonList(PRODUCT_ID))).thenReturn(snapshot);
        when(productFeignSupport.toSkuMapByPropertyValueIds(snapshot)).thenReturn(Map.of());
        when(productFeignSupport.toDefaultSkuByProductId(snapshot))
                .thenReturn(Map.of(PRODUCT_ID, defaultSku));
        when(productCartMapper.insert(any(ProductCart.class))).thenReturn(1);

        productCartService.add2Cart(cart);

        assertEquals("d1-d2", cart.getPropertyValueIds());
        assertEquals(new BigDecimal("50.00"), cart.getAddPrice());
    }

    @Test
    void add2Cart_noDefaultSku_throws() {
        ProductCart cart = newCart(PRODUCT_ID, "", 1);
        when(productFeignSupport.defaultSku(PRODUCT_ID)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class, () -> productCartService.add2Cart(cart));

        assertEquals("该商品暂不可加入购物车", e.getMessage());
        verify(productCartMapper, never()).insert(any());
    }

    @Test
    void add2Cart_skuPriceUnresolvable_throws() {
        ProductCart cart = newCart(PRODUCT_ID, PROPERTY_IDS, 1);
        when(productCartMapper.selectByProductIdAndPropertyValueIdHashAndUserId(
                PRODUCT_ID, PROPERTY_HASH, USER_ID)).thenReturn(null);
        ProductSnapshotBatchVO snapshot = new ProductSnapshotBatchVO();
        snapshot.setSkus(Collections.emptyList());
        when(productFeignSupport.snapshotBatch(Collections.singletonList(PRODUCT_ID))).thenReturn(snapshot);
        when(productFeignSupport.toSkuMapByPropertyValueIds(snapshot)).thenReturn(Map.of());
        when(productFeignSupport.toDefaultSkuByProductId(snapshot)).thenReturn(Map.of());

        assertThrows(BusinessException.class, () -> productCartService.add2Cart(cart));
    }

    // ==================== findListByPageAndUserId ====================

    @Test
    void findListByPageAndUserId_nullUser_returnsEmpty() {
        PaginationResultVO<ProductCartVO> page = productCartService.findListByPageAndUserId(new ProductCartQuery(), null);

        assertTrue(page.getList().isEmpty());
        verifyNoInteractions(productCartMapper);
    }

    @Test
    void findListByPageAndUserId_emptyList_returnsEmpty() {
        ProductCartQuery query = new ProductCartQuery();
        when(productCartMapper.selectCount(query)).thenReturn(0);
        when(productCartMapper.selectList(query)).thenReturn(Collections.emptyList());

        PaginationResultVO<ProductCartVO> page = productCartService.findListByPageAndUserId(query, USER_ID);

        assertTrue(page.getList().isEmpty());
        verify(productFeignSupport, never()).snapshotBatch(any());
    }

    @Test
    void findListByPageAndUserId_enrichesSnapshotAndStock() {
        ProductCart cart = newCart(PRODUCT_ID, PROPERTY_IDS, 2);
        cart.setCartId("cart1");
        cart.setPropertyValueIdHash(PROPERTY_HASH);
        ProductCartQuery query = new ProductCartQuery();
        when(productCartMapper.selectCount(query)).thenReturn(1);
        when(productCartMapper.selectList(query)).thenReturn(List.of(cart));

        ProductSnapshotBatchVO snapshot = new ProductSnapshotBatchVO();
        ProductInfoSnapshotVO info = new ProductInfoSnapshotVO();
        info.setProductId(PRODUCT_ID);
        info.setProductName("测试商品");
        info.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        info.setCover("cover1.jpg,cover2.jpg");
        snapshot.setProducts(List.of(info));

        ProductPropertyValueSnapshotVO pv = new ProductPropertyValueSnapshotVO();
        pv.setProductId(PRODUCT_ID);
        pv.setPropertyValueId("pv1");
        pv.setPropertyName("颜色");
        pv.setPropertyValue("红色");
        pv.setPropertyCover("pv-cover.jpg");
        snapshot.setPropertyValues(List.of(pv));

        ProductSkuSnapshotVO sku = sku(PRODUCT_ID, PROPERTY_IDS, "88.00");
        sku.setPropertyValueIdHash("skuhash1");
        snapshot.setSkus(List.of(sku));

        when(productFeignSupport.snapshotBatch(List.of(PRODUCT_ID))).thenReturn(snapshot);
        when(productFeignSupport.toProductInfoMap(snapshot)).thenReturn(Map.of(PRODUCT_ID, info));
        when(productFeignSupport.toPropertyValueMap(snapshot)).thenReturn(Map.of(PRODUCT_ID + "pv1", pv));
        when(productFeignSupport.toSkuMapByPropertyValueIds(snapshot)).thenReturn(Map.of(PRODUCT_ID + PROPERTY_IDS, sku));
        when(productFeignSupport.toDefaultSkuByProductId(snapshot)).thenReturn(Map.of());
        when(stockFeignSupport.getAvailable(PRODUCT_ID, "skuhash1")).thenReturn(7);

        PaginationResultVO<ProductCartVO> page = productCartService.findListByPageAndUserId(query, USER_ID);

        assertEquals(1, page.getList().size());
        ProductCartVO vo = page.getList().get(0);
        assertEquals("测试商品", vo.getProductName());
        assertEquals("pv-cover.jpg", vo.getProductCover());
        assertEquals(Boolean.TRUE, vo.getProductOnSale());
        assertEquals(new BigDecimal("88.00"), vo.getPrice());
        assertEquals(new BigDecimal("88.00"), vo.getAddPrice());
        assertEquals(7, vo.getStock());
        assertEquals(1, vo.getPropertyData().size());
        assertEquals("颜色", vo.getPropertyData().get(0).getPropertyName());
    }

    @Test
    void findListByPageAndUserId_coverFromPropertyValue() {
        ProductCart cart = newCart(PRODUCT_ID, PROPERTY_IDS, 1);
        cart.setCartId("cart1");
        ProductCartQuery query = new ProductCartQuery();
        when(productCartMapper.selectCount(query)).thenReturn(1);
        when(productCartMapper.selectList(query)).thenReturn(List.of(cart));

        ProductSnapshotBatchVO snapshot = new ProductSnapshotBatchVO();
        ProductInfoSnapshotVO info = new ProductInfoSnapshotVO();
        info.setProductId(PRODUCT_ID);
        info.setCover("main-cover.jpg");
        snapshot.setProducts(List.of(info));
        ProductPropertyValueSnapshotVO pv = new ProductPropertyValueSnapshotVO();
        pv.setProductId(PRODUCT_ID);
        pv.setPropertyValueId("pv1");
        pv.setPropertyCover("pv-cover.jpg");
        snapshot.setPropertyValues(List.of(pv));

        when(productFeignSupport.snapshotBatch(any())).thenReturn(snapshot);
        when(productFeignSupport.toProductInfoMap(snapshot)).thenReturn(Map.of(PRODUCT_ID, info));
        when(productFeignSupport.toPropertyValueMap(snapshot)).thenReturn(Map.of(PRODUCT_ID + "pv1", pv));
        when(productFeignSupport.toSkuMapByPropertyValueIds(snapshot)).thenReturn(Map.of());
        when(productFeignSupport.toDefaultSkuByProductId(snapshot)).thenReturn(Map.of());

        ProductCartVO vo = productCartService.findListByPageAndUserId(query, USER_ID).getList().get(0);

        assertEquals("pv-cover.jpg", vo.getProductCover());
    }

    // ==================== CRUD 委托 ====================

    @Test
    void crudDelegations_passThrough() {
        ProductCart cart = new ProductCart();
        ProductCartQuery query = new ProductCartQuery();
        query.setCartId("c1");
        when(productCartMapper.insert(cart)).thenReturn(1);
        when(productCartMapper.insertBatch(List.of(cart))).thenReturn(2);
        when(productCartMapper.insertOrUpdateBatch(List.of(cart))).thenReturn(3);
        when(productCartMapper.updateByParam(cart, query)).thenReturn(4);
        when(productCartMapper.deleteByParam(query)).thenReturn(5);
        when(productCartMapper.selectByCartId("c1")).thenReturn(cart);
        when(productCartMapper.updateByCartId(cart, "c1")).thenReturn(6);
        when(productCartMapper.deleteByCartId("c1")).thenReturn(7);
        when(productCartMapper.selectByProductIdAndPropertyValueIdHashAndUserId("p", "h", "u")).thenReturn(cart);
        when(productCartMapper.updateByProductIdAndPropertyValueIdHashAndUserId(cart, "p", "h", "u")).thenReturn(8);
        when(productCartMapper.deleteByProductIdAndPropertyValueIdHashAndUserId("p", "h", "u")).thenReturn(9);
        when(productCartMapper.selectList(query)).thenReturn(List.of(cart));
        when(productCartMapper.selectCount(query)).thenReturn(10);

        assertEquals(1, productCartService.add(cart));
        assertEquals(2, productCartService.addBatch(List.of(cart)));
        assertEquals(3, productCartService.addOrUpdateBatch(List.of(cart)));
        assertEquals(4, productCartService.updateByParam(cart, query));
        assertEquals(5, productCartService.deleteByParam(query));
        assertSame(cart, productCartService.getProductCartByCartId("c1"));
        assertEquals(6, productCartService.updateProductCartByCartId(cart, "c1"));
        assertEquals(7, productCartService.deleteProductCartByCartId("c1"));
        assertSame(cart, productCartService.getProductCartByProductIdAndPropertyValueIdHashAndUserId("p", "h", "u"));
        assertEquals(8, productCartService.updateProductCartByProductIdAndPropertyValueIdHashAndUserId(cart, "p", "h", "u"));
        assertEquals(9, productCartService.deleteProductCartByProductIdAndPropertyValueIdHashAndUserId("p", "h", "u"));
        assertEquals(0, productCartService.addBatch(null));
        assertEquals(0, productCartService.addOrUpdateBatch(null));
        assertNotNull(productCartService.findListByPage(query));
    }

    @Test
    void updateByParam_emptyQuery_throws() {
        ProductCartQuery empty = new ProductCartQuery();
        assertThrows(com.simlect.exception.BusinessException.class,
                () -> productCartService.updateByParam(new ProductCart(), empty));
        verify(productCartMapper, never()).updateByParam(any(), any());
    }
}
