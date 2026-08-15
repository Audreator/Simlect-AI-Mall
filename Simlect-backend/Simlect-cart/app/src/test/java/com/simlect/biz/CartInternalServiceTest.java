package com.simlect.biz;

import com.simlect.api.dto.CartDeleteBatchDTO;
import com.simlect.api.dto.CartDeleteItemDTO;
import com.simlect.entity.po.ProductCart;
import com.simlect.entity.query.ProductCartQuery;
import com.simlect.mappers.ProductCartMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CartInternalService 批量删除购物车单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CartInternalServiceTest {

    @Mock
    private ProductCartMapper<ProductCart, ProductCartQuery> productCartMapper;

    @InjectMocks
    private CartInternalService cartInternalService;

    @Test
    void deleteBatch_nullDto_skips() {
        cartInternalService.deleteBatch(null);
        verify(productCartMapper, never()).deleteBatch(any());
    }

    @Test
    void deleteBatch_emptyItems_skips() {
        cartInternalService.deleteBatch(new CartDeleteBatchDTO());
        verify(productCartMapper, never()).deleteBatch(any());
    }

    @Test
    void deleteBatch_convertsItemsAndDeletes() {
        CartDeleteItemDTO item1 = new CartDeleteItemDTO("U1", "P1", "h1", "pv1");
        CartDeleteItemDTO item2 = new CartDeleteItemDTO("U1", "P2", "h2", "pv2");
        CartDeleteBatchDTO dto = new CartDeleteBatchDTO(Arrays.asList(item1, null, item2));

        cartInternalService.deleteBatch(dto);

        ArgumentCaptor<List<ProductCart>> captor = ArgumentCaptor.forClass(List.class);
        verify(productCartMapper).deleteBatch(captor.capture());
        List<ProductCart> list = captor.getValue();
        assertEquals(2, list.size());
        assertEquals("U1", list.get(0).getUserId());
        assertEquals("P1", list.get(0).getProductId());
        assertEquals("h1", list.get(0).getPropertyValueIdHash());
        assertEquals("pv1", list.get(0).getPropertyValueIds());
        assertEquals("P2", list.get(1).getProductId());
    }
}
