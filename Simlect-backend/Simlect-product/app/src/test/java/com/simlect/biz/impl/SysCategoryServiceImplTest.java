package com.simlect.biz.impl;

import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.entity.po.SysCategory;
import com.simlect.entity.po.SysProductProperty;
import com.simlect.entity.query.SysCategoryQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.mappers.SysCategoryMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysCategoryServiceImplTest {

    @Mock
    private SysCategoryMapper<SysCategory, SysCategoryQuery> sysCategoryMapper;
    @Mock
    private SysProductPropertyMapper<SysProductProperty, SysProductProperty> sysProductPropertyMapper;
    @Mock
    private SysProductPropertyServiceImpl sysProductPropertyService;
    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private SysCategoryServiceImpl sysCategoryService;

    private SysCategory category(String id, String pId) {
        SysCategory c = new SysCategory();
        c.setCategoryId(id);
        c.setpCategoryId(pId);
        return c;
    }

    @Test
    void findChildren_buildsTree() {
        List<SysCategory> flat = List.of(
                category("1", "0"),
                category("2", "0"),
                category("11", "1"));

        List<SysCategory> tree = sysCategoryService.findChildren(flat, Constants.ZERO_STR);

        assertEquals(2, tree.size());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("11", tree.get(0).getChildren().get(0).getCategoryId());
    }

    @Test
    void findListByParam_usesRedisCacheWhenPresent() {
        when(sysCategoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(redisComponent.getCategoryList()).thenAnswer(inv -> List.of(category("1", "0")));
        when(sysProductPropertyService.findListByParam(any())).thenReturn(new ArrayList<>());

        List<SysCategory> list = sysCategoryService.findListByParam(new SysCategoryQuery());

        assertEquals(1, list.size());
        // selectList 先回源 DB 一次，命中 Redis 非空缓存后不再重建
        verify(sysCategoryMapper, times(1)).selectList(any());
        verify(redisComponent, never()).saveCategory2Redis(any());
    }

    @Test
    void findListByParam_rebuildsRedisWhenEmptyCache() {
        when(sysCategoryMapper.selectList(any())).thenReturn(List.of(category("1", "0")));
        when(redisComponent.getCategoryList()).thenReturn(null);
        when(sysProductPropertyService.findListByParam(any())).thenReturn(new ArrayList<>());

        List<SysCategory> list = sysCategoryService.findListByParam(new SysCategoryQuery());

        assertEquals(1, list.size());
        verify(redisComponent).saveCategory2Redis(any());
    }

    @Test
    void findListByPage_delegates() {
        when(sysCategoryMapper.selectCount(any())).thenReturn(5);
        when(sysCategoryMapper.selectList(any())).thenReturn(new ArrayList<>());

        PaginationResultVO<SysCategory> result = sysCategoryService.findListByPage(new SysCategoryQuery());

        assertEquals(5, result.getTotalCount());
    }

    @Test
    void saveCategory_new_generatesIdAndMaxSort() {
        SysCategory bean = category(null, "0");
        when(sysCategoryMapper.selectMaxSort("0")).thenReturn(3);
        when(sysCategoryMapper.updateByCategoryId(eq(bean), anyString())).thenReturn(1);

        sysCategoryService.saveCategory(bean);

        assertNotNull(bean.getCategoryId());
        assertEquals(5, bean.getCategoryId().length());
        assertEquals(4, bean.getSort());
        verify(sysCategoryMapper).insert(bean);
        verify(redisComponent).saveCategory2Redis(any());
    }

    @Test
    void saveCategory_existing_updatesOnly() {
        SysCategory bean = category("1", "0");
        bean.setSort(1);
        when(sysCategoryMapper.updateByCategoryId(bean, "1")).thenReturn(1);

        sysCategoryService.saveCategory(bean);

        verify(sysCategoryMapper, never()).insert(any());
        verify(sysCategoryMapper, never()).selectMaxSort(anyString());
        verify(redisComponent).saveCategory2Redis(any());
    }

    @Test
    void deleteSysCategory_recursivelyDeletesChildren() {
        SysCategory parent = category("1", "0");
        SysCategory child = category("11", "1");
        when(sysCategoryMapper.selectByPCategoryId("1")).thenReturn(List.of(child));
        when(sysCategoryMapper.selectByPCategoryId("11")).thenReturn(List.of());
        when(sysCategoryMapper.deleteByCategoryId(anyString())).thenReturn(1);

        sysCategoryService.deleteSysCategory(parent);

        verify(sysCategoryMapper).deleteByCategoryId("11");
        verify(sysCategoryMapper).deleteByCategoryId("1");
        verify(redisComponent, times(2)).saveCategory2Redis(any());
    }

    @Test
    void changeCategorySort_reorders() {
        when(sysCategoryMapper.selectByCategoryId("2")).thenReturn(category("2", "0"));
        when(sysCategoryMapper.selectByCategoryId("1")).thenReturn(category("1", "0"));

        sysCategoryService.changeCategorySort("2,1");

        @SuppressWarnings({"unchecked", "rawtypes"})
        org.mockito.ArgumentCaptor<List<SysCategory>> captor =
                org.mockito.ArgumentCaptor.forClass((Class) List.class);
        verify(sysCategoryMapper).updateBatch(captor.capture());
        List<SysCategory> list = captor.getValue();
        assertEquals(2, list.size());
        assertEquals("2", list.get(0).getCategoryId());
        assertEquals(1, list.get(0).getSort());
        assertEquals("1", list.get(1).getCategoryId());
        assertEquals(2, list.get(1).getSort());
        verify(redisComponent).saveCategory2Redis(any());
    }

    @Test
    void saveProductProperty_newProperty_generatesIdAndSort() {
        SysProductProperty bean = new SysProductProperty();
        bean.setCategoryId("1");
        when(sysProductPropertyMapper.selectMaxPropertySort("1")).thenReturn(2);
        when(sysProductPropertyMapper.insertOrUpdate(bean)).thenReturn(1);

        Integer result = sysCategoryService.saveProductProperty(bean);

        assertEquals(1, result);
        assertNotNull(bean.getPropertyId());
        assertEquals(3, bean.getPropertySort());
    }

    @Test
    void getAllCategoryList_returnsRedisOrRebuild() {
        when(redisComponent.getCategoryList()).thenAnswer(inv -> List.of(category("1", "0")));

        List<SysCategory> list = sysCategoryService.getAllCategoryList();

        assertEquals(1, list.size());
        verify(sysCategoryMapper, never()).selectList(any());
    }
}
