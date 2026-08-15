package com.simlect.biz.impl;

import com.simlect.entity.po.SearchHotKeyword;
import com.simlect.entity.query.SearchHotKeywordQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.SearchHotKeywordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHotKeywordServiceImplTest {

    @Mock
    private SearchHotKeywordMapper<SearchHotKeyword, SearchHotKeywordQuery> searchHotKeywordMapper;

    @InjectMocks
    private SearchHotKeywordServiceImpl searchHotKeywordService;

    @Test
    void loadList_delegates() {
        when(searchHotKeywordMapper.selectList(any())).thenReturn(List.of(new SearchHotKeyword()));

        assertEquals(1, searchHotKeywordService.loadList().size());
    }

    @Test
    void save_emptyKeyword_throws() {
        assertThrows(BusinessException.class, () -> searchHotKeywordService.save("", 1, 1));
        assertThrows(BusinessException.class, () -> searchHotKeywordService.save(null, 1, 1));
    }

    @Test
    void save_trimsAndAppliesDefaults() {
        searchHotKeywordService.save(" 牛肉干 ", null, null);

        verify(searchHotKeywordMapper).insertOrUpdate(argThat(bean ->
                "牛肉干".equals(bean.getKeyword()) && 0 == bean.getSort() && 1 == bean.getStatus()
                        && bean.getUpdateTime() != null));
    }

    @Test
    void deleteByKeyword_empty_throws() {
        assertThrows(BusinessException.class, () -> searchHotKeywordService.deleteByKeyword(""));
    }

    @Test
    void deleteByKeyword_trims() {
        searchHotKeywordService.deleteByKeyword(" 手机 ");

        verify(searchHotKeywordMapper).deleteByKeyword("手机");
    }
}
