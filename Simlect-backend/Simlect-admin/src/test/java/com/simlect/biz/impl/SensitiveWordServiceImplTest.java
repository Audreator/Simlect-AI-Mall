package com.simlect.biz.impl;

import com.simlect.component.SensitiveWordCacheComponent;
import com.simlect.entity.po.SensitiveWord;
import com.simlect.entity.query.SensitiveWordQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.SensitiveWordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensitiveWordServiceImplTest {

    @Mock
    private SensitiveWordMapper<SensitiveWord, SensitiveWordQuery> sensitiveWordMapper;
    @Mock
    private SensitiveWordCacheComponent sensitiveWordCacheComponent;

    @InjectMocks
    private SensitiveWordServiceImpl sensitiveWordService;

    @Test
    void init_redisCacheMissing_syncsFromDb() {
        when(sensitiveWordCacheComponent.loadFromRedis()).thenReturn(null);
        when(sensitiveWordMapper.selectList(any())).thenReturn(List.of(new SensitiveWord()));

        sensitiveWordService.init();

        verify(sensitiveWordCacheComponent).saveToRedis(any());
    }

    @Test
    void init_redisCachePresent_skipsSync() {
        when(sensitiveWordCacheComponent.loadFromRedis()).thenReturn(new ArrayList<>());

        sensitiveWordService.init();

        verify(sensitiveWordMapper, never()).selectList(any());
    }

    @Test
    void findListByPage_appliesDefaults() {
        when(sensitiveWordMapper.selectCount(any())).thenReturn(20);
        when(sensitiveWordMapper.selectList(any())).thenReturn(new ArrayList<>());
        SensitiveWordQuery query = new SensitiveWordQuery();

        PaginationResultVO<SensitiveWord> result = sensitiveWordService.findListByPage(query);

        assertEquals(20, result.getTotalCount());
        assertEquals(15, result.getPageSize());
        assertEquals(1, result.getPageNo());
        assertEquals(2, result.getPageTotal());
    }

    @Test
    void save_emptyWord_throws() {
        assertThrows(BusinessException.class, () -> sensitiveWordService.save(null, "", "***", 1));
    }

    @Test
    void save_newWord_trimsAndDefaultsReplaceWord() {
        when(sensitiveWordMapper.insert(any())).thenReturn(1);
        when(sensitiveWordMapper.selectList(any())).thenReturn(List.of());

        sensitiveWordService.save(null, " 广告 ", null, null);

        verify(sensitiveWordMapper).insert(argThat(bean ->
                "广告".equals(bean.getWord()) && "***".equals(bean.getReplaceWord())
                        && 1 == bean.getStatus() && bean.getCreateTime() != null));
        verify(sensitiveWordCacheComponent).saveToRedis(any());
    }

    @Test
    void save_updateById() {
        when(sensitiveWordMapper.updateByParam(any(), any())).thenReturn(1);
        when(sensitiveWordMapper.selectList(any())).thenReturn(List.of());

        sensitiveWordService.save(5L, "广告", "广而告之", 0);

        verify(sensitiveWordMapper).updateByParam(argThat(bean -> 5L == bean.getId()
                && "广而告之".equals(bean.getReplaceWord()) && 0 == bean.getStatus()), any());
        verify(sensitiveWordCacheComponent).saveToRedis(any());
    }

    @Test
    void delete_nullId_throws() {
        assertThrows(BusinessException.class, () -> sensitiveWordService.delete(null));
    }

    @Test
    void delete_syncsCache() {
        when(sensitiveWordMapper.deleteById(1L)).thenReturn(1);
        when(sensitiveWordMapper.selectList(any())).thenReturn(List.of());

        sensitiveWordService.delete(1L);

        verify(sensitiveWordMapper).deleteById(1L);
        verify(sensitiveWordCacheComponent).saveToRedis(any());
    }

    @Test
    void refreshCache_syncsFromDb() {
        when(sensitiveWordMapper.selectList(any())).thenReturn(List.of(new SensitiveWord(), new SensitiveWord()));

        sensitiveWordService.refreshCache();

        verify(sensitiveWordCacheComponent).saveToRedis(any());
    }

    @Test
    void syncFromDbToRedis_filtersEnabledOnly() {
        when(sensitiveWordMapper.selectList(any())).thenReturn(List.of(new SensitiveWord()));

        sensitiveWordService.syncFromDbToRedis();

        verify(sensitiveWordMapper).selectList(argThat(q -> 1 == q.getStatus()));
        verify(sensitiveWordCacheComponent).saveToRedis(any());
    }
}
