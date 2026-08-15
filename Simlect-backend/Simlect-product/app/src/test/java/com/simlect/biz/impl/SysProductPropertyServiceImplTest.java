package com.simlect.biz.impl;

import com.simlect.entity.po.SysProductProperty;
import com.simlect.entity.query.SysProductPropertyQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.SysProductPropertyMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysProductPropertyServiceImplTest {

    @Mock
    private SysProductPropertyMapper<SysProductProperty, SysProductPropertyQuery> sysProductPropertyMapper;

    @InjectMocks
    private SysProductPropertyServiceImpl sysProductPropertyService;

    @Test
    void findListByPage_defaultPageSize() {
        when(sysProductPropertyMapper.selectCount(any())).thenReturn(8);
        when(sysProductPropertyMapper.selectList(any())).thenReturn(new ArrayList<>());
        SysProductPropertyQuery query = new SysProductPropertyQuery();
        query.setPageNo(1);

        PaginationResultVO<SysProductProperty> result = sysProductPropertyService.findListByPage(query);

        assertEquals(8, result.getTotalCount());
        assertEquals(15, result.getPageSize());
    }

    @Test
    void addBatch_empty_returnsZero() {
        assertEquals(0, sysProductPropertyService.addBatch(null));
        assertEquals(0, sysProductPropertyService.addBatch(List.of()));
    }

    @Test
    void saveProductProperty_delegates() {
        SysProductProperty bean = new SysProductProperty();
        when(sysProductPropertyMapper.insertOrUpdate(bean)).thenReturn(1);

        assertEquals(1, sysProductPropertyService.saveProductProperty(bean));
    }

    @Test
    void crudDelegates() {
        SysProductProperty bean = new SysProductProperty();
        when(sysProductPropertyMapper.selectByPropertyId("prop1")).thenReturn(bean);
        when(sysProductPropertyMapper.updateByPropertyId(bean, "prop1")).thenReturn(1);
        when(sysProductPropertyMapper.deleteByPropertyId("prop1")).thenReturn(1);

        assertSame(bean, sysProductPropertyService.getSysProductPropertyByPropertyId("prop1"));
        assertEquals(1, sysProductPropertyService.updateSysProductPropertyByPropertyId(bean, "prop1"));
        assertEquals(1, sysProductPropertyService.deleteSysProductPropertyByPropertyId("prop1"));
    }

    @Test
    void updateByParam_nullParam_throws() {
        assertThrows(BusinessException.class,
                () -> sysProductPropertyService.updateByParam(new SysProductProperty(), null));
    }
}
