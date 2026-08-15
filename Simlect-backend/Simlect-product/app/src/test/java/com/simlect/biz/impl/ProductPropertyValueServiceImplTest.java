package com.simlect.biz.impl;

import com.simlect.entity.po.ProductPropertyValue;
import com.simlect.entity.query.ProductPropertyValueQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.ProductPropertyValueMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductPropertyValueServiceImplTest {

    @Mock
    private ProductPropertyValueMapper<ProductPropertyValue, ProductPropertyValueQuery> productPropertyValueMapper;

    @InjectMocks
    private ProductPropertyValueServiceImpl productPropertyValueService;

    @Test
    void findListByPage_defaultPageSize() {
        when(productPropertyValueMapper.selectCount(any())).thenReturn(10);
        when(productPropertyValueMapper.selectList(any())).thenReturn(new ArrayList<>());
        ProductPropertyValueQuery query = new ProductPropertyValueQuery();
        query.setPageNo(1);

        PaginationResultVO<ProductPropertyValue> result = productPropertyValueService.findListByPage(query);

        assertEquals(10, result.getTotalCount());
        assertEquals(15, result.getPageSize());
    }

    @Test
    void addBatch_empty_returnsZero() {
        assertEquals(0, productPropertyValueService.addBatch(null));
        assertEquals(0, productPropertyValueService.addBatch(List.of()));
        verifyNoInteractions(productPropertyValueMapper);
    }

    @Test
    void crudDelegates() {
        ProductPropertyValue bean = new ProductPropertyValue();
        when(productPropertyValueMapper.insert(bean)).thenReturn(1);
        when(productPropertyValueMapper.selectByProductIdAndPropertyValueId("P1", "v1")).thenReturn(bean);
        when(productPropertyValueMapper.updateByProductIdAndPropertyValueId(bean, "P1", "v1")).thenReturn(1);
        when(productPropertyValueMapper.deleteByProductIdAndPropertyValueId("P1", "v1")).thenReturn(1);

        assertEquals(1, productPropertyValueService.add(bean));
        assertSame(bean, productPropertyValueService.getProductPropertyValueByProductIdAndPropertyValueId("P1", "v1"));
        assertEquals(1, productPropertyValueService.updateProductPropertyValueByProductIdAndPropertyValueId(bean, "P1", "v1"));
        assertEquals(1, productPropertyValueService.deleteProductPropertyValueByProductIdAndPropertyValueId("P1", "v1"));
    }

    @Test
    void updateByParam_nullParam_throws() {
        assertThrows(BusinessException.class,
                () -> productPropertyValueService.updateByParam(new ProductPropertyValue(), null));
    }
}
