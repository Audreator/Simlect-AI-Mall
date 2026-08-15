package com.simlect.biz.impl;

import com.simlect.entity.po.SearchHotKeyword;
import com.simlect.entity.po.UserSearchKeyword;
import com.simlect.entity.query.SearchHotKeywordQuery;
import com.simlect.entity.query.UserSearchKeywordQuery;
import com.simlect.mappers.SearchHotKeywordMapper;
import com.simlect.mappers.UserSearchKeywordMapper;
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
class SearchKeywordServiceImplTest {

    @Mock
    private SearchHotKeywordMapper<SearchHotKeyword, SearchHotKeywordQuery> searchHotKeywordMapper;
    @Mock
    private UserSearchKeywordMapper<UserSearchKeyword, UserSearchKeywordQuery> userSearchKeywordMapper;

    @InjectMocks
    private SearchKeywordServiceImpl searchKeywordService;

    @Test
    void loadHotKeywords_emptyDb_returnsDefaults() {
        when(searchHotKeywordMapper.selectList(any())).thenReturn(null);

        List<String> keywords = searchKeywordService.loadHotKeywords();

        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("牛肉干"));
    }

    @Test
    void loadHotKeywords_returnsEnabledSorted() {
        SearchHotKeyword hot = new SearchHotKeyword();
        hot.setKeyword("手机");
        when(searchHotKeywordMapper.selectList(any())).thenReturn(List.of(hot));

        List<String> keywords = searchKeywordService.loadHotKeywords();

        assertEquals(List.of("手机"), keywords);
        verify(searchHotKeywordMapper).selectList(argThat(q -> 1 == q.getStatus()));
    }

    @Test
    void loadRecentKeywords_dedupsAndLimits10() {
        List<UserSearchKeyword> list = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            UserSearchKeyword kw = new UserSearchKeyword();
            kw.setKeyword("kw" + (i % 3));
            list.add(kw);
        }
        when(userSearchKeywordMapper.selectList(any())).thenReturn(list);

        List<String> keywords = searchKeywordService.loadRecentKeywords("U1");

        assertEquals(3, keywords.size());
        assertEquals("kw0", keywords.get(0));
    }

    @Test
    void saveUserKeyword_empty_ignored() {
        searchKeywordService.saveUserKeyword("U1", "");
        searchKeywordService.saveUserKeyword("U1", null);

        verifyNoInteractions(userSearchKeywordMapper);
    }

    @Test
    void saveUserKeyword_deletesDuplicateAndInserts() {
        when(userSearchKeywordMapper.selectList(any())).thenReturn(List.of());

        searchKeywordService.saveUserKeyword("U1", " 手机 ");

        verify(userSearchKeywordMapper).deleteByParam(argThat(q ->
                "U1".equals(q.getUserId()) && "手机".equals(q.getKeyword())));
        verify(userSearchKeywordMapper).insert(argThat(r ->
                "U1".equals(r.getUserId()) && "手机".equals(r.getKeyword()) && r.getSearchTime() != null));
    }

    @Test
    void saveUserKeyword_trimsOverflow() {
        List<UserSearchKeyword> existing = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            UserSearchKeyword kw = new UserSearchKeyword();
            kw.setId((long) i);
            kw.setKeyword("k" + i);
            existing.add(kw);
        }
        when(userSearchKeywordMapper.selectList(any())).thenReturn(existing);

        searchKeywordService.saveUserKeyword("U1", "新词");

        // 13 条已有 + 1 条新增 = 14 条，删除索引 10..12 的 3 条
        verify(userSearchKeywordMapper).deleteById(10L);
        verify(userSearchKeywordMapper).deleteById(11L);
        verify(userSearchKeywordMapper).deleteById(12L);
    }

    @Test
    void clearAndRemoveUserKeywords_delegate() {
        searchKeywordService.clearUserKeywords("U1");
        verify(userSearchKeywordMapper).deleteByParam(any());

        searchKeywordService.removeUserKeyword("U1", "手机");
        verify(userSearchKeywordMapper).deleteByParam(argThat(q ->
                "手机".equals(q.getKeyword())));
    }
}
