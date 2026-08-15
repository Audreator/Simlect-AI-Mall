package com.simlect.biz.impl;

import com.simlect.api.dto.ProductSnapshotBatchVO;
import com.simlect.api.enums.ProductStatusEnum;
import com.simlect.api.support.ProductFeignSupport;
import com.simlect.api.vo.ProductInfoSnapshotVO;
import com.simlect.api.vo.UserFavoriteProductVO;
import com.simlect.entity.po.UserProductFavorite;
import com.simlect.entity.query.UserProductFavoriteQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserProductFavoriteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProductFavoriteServiceImplTest {

    @Mock
    private UserProductFavoriteMapper<UserProductFavorite, UserProductFavoriteQuery> userProductFavoriteMapper;
    @Mock
    private ProductFeignSupport productFeignSupport;

    @InjectMocks
    private UserProductFavoriteServiceImpl userProductFavoriteService;

    private ProductInfoSnapshotVO buildProduct(String productId) {
        ProductInfoSnapshotVO product = new ProductInfoSnapshotVO();
        product.setProductId(productId);
        product.setProductName("商品" + productId);
        product.setCover("cover1.jpg,cover2.jpg");
        product.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        product.setMinPrice(new BigDecimal("99.00"));
        return product;
    }

    @Test
    void loadFavoritePage_joinsProductSnapshot() {
        UserProductFavorite favorite = new UserProductFavorite();
        favorite.setFavoriteId("FAV1");
        favorite.setProductId("P1");
        favorite.setUserId("U1");
        favorite.setCreateTime(new Date());

        when(userProductFavoriteMapper.selectCount(any())).thenReturn(1);
        when(userProductFavoriteMapper.selectList(any())).thenReturn(List.of(favorite));
        when(productFeignSupport.snapshotBatch(List.of("P1")))
                .thenReturn(new ProductSnapshotBatchVO());
        when(productFeignSupport.toProductInfoMap(any()))
                .thenReturn(Map.of("P1", buildProduct("P1")));

        PaginationResultVO<UserFavoriteProductVO> result =
                userProductFavoriteService.loadFavoritePage("U1", 1);

        assertEquals(1, result.getTotalCount());
        UserFavoriteProductVO vo = result.getList().get(0);
        assertEquals("商品P1", vo.getProductName());
        assertEquals("cover1.jpg", vo.getCover());
        assertEquals(ProductStatusEnum.ON_SALE.getStatus(), vo.getStatus());
        assertEquals(0, new BigDecimal("99.00").compareTo(vo.getMinPrice()));
    }

    @Test
    void toggleFavorite_productNotOnSale_throws() {
        ProductInfoSnapshotVO product = buildProduct("P1");
        product.setStatus(ProductStatusEnum.OFF_SALE.getStatus());
        when(productFeignSupport.toProductInfoMap(any())).thenReturn(Map.of("P1", product));

        BusinessException e = assertThrows(BusinessException.class,
                () -> userProductFavoriteService.toggleFavorite("U1", "P1"));
        assertTrue(e.getMessage().contains("商品不存在或已下架"));
        verify(userProductFavoriteMapper, never()).insert(any());
    }

    @Test
    void toggleFavorite_alreadyFavorite_removesAndReturnsFalse() {
        when(productFeignSupport.toProductInfoMap(any())).thenReturn(Map.of("P1", buildProduct("P1")));
        UserProductFavorite favorite = new UserProductFavorite();
        favorite.setFavoriteId("FAV1");
        when(userProductFavoriteMapper.selectList(any())).thenReturn(List.of(favorite));

        boolean result = userProductFavoriteService.toggleFavorite("U1", "P1");

        assertFalse(result);
        verify(userProductFavoriteMapper).deleteByFavoriteId("FAV1");
    }

    @Test
    void toggleFavorite_newFavorite_insertsAndReturnsTrue() {
        when(productFeignSupport.toProductInfoMap(any())).thenReturn(Map.of("P1", buildProduct("P1")));
        when(userProductFavoriteMapper.selectList(any())).thenReturn(List.of());

        boolean result = userProductFavoriteService.toggleFavorite("U1", "P1");

        assertTrue(result);
        verify(userProductFavoriteMapper).insert(argThat(fav ->
                "U1".equals(fav.getUserId()) && "P1".equals(fav.getProductId())
                        && fav.getFavoriteId().startsWith("FAV")));
    }

    @Test
    void isFavorite_countsRecords() {
        when(userProductFavoriteMapper.selectCount(any())).thenReturn(1);
        assertTrue(userProductFavoriteService.isFavorite("U1", "P1"));

        when(userProductFavoriteMapper.selectCount(any())).thenReturn(0);
        assertFalse(userProductFavoriteService.isFavorite("U1", "P1"));
    }

    @Test
    void removeFavorite_notOwned_throws() {
        UserProductFavorite favorite = new UserProductFavorite();
        favorite.setFavoriteId("FAV1");
        favorite.setUserId("U2");
        when(userProductFavoriteMapper.selectByFavoriteId("FAV1")).thenReturn(favorite);

        assertThrows(BusinessException.class, () -> userProductFavoriteService.removeFavorite("U1", "FAV1"));
        verify(userProductFavoriteMapper, never()).deleteByFavoriteId(anyString());
    }

    @Test
    void removeFavorite_owned_deletes() {
        UserProductFavorite favorite = new UserProductFavorite();
        favorite.setFavoriteId("FAV1");
        favorite.setUserId("U1");
        when(userProductFavoriteMapper.selectByFavoriteId("FAV1")).thenReturn(favorite);

        userProductFavoriteService.removeFavorite("U1", "FAV1");

        verify(userProductFavoriteMapper).deleteByFavoriteId("FAV1");
    }
}
