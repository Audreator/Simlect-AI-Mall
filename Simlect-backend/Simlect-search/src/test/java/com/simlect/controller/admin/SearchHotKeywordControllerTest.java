package com.simlect.controller.admin;

import com.simlect.biz.RagQuestionService;
import com.simlect.biz.RagSyncFailureService;
import com.simlect.biz.SearchHotKeywordService;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.query.RagSyncFailureQuery;
import com.simlect.entity.vo.PaginationResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchHotKeywordControllerTest {

    @Mock
    private SearchHotKeywordService searchHotKeywordService;

    @InjectMocks
    private SearchHotKeywordController searchHotKeywordController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchHotKeywordController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadList_ok() throws Exception {
        when(searchHotKeywordService.loadList()).thenReturn(List.of());

        mockMvc.perform(post("/admin/searchHotKeyword/loadList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void save_ok() throws Exception {
        mockMvc.perform(post("/admin/searchHotKeyword/save")
                        .param("keyword", "牛肉干")
                        .param("sort", "1")
                        .param("status", "1"))
                .andExpect(status().isOk());

        verify(searchHotKeywordService).save("牛肉干", 1, 1);
    }

    @Test
    void del_ok() throws Exception {
        mockMvc.perform(post("/admin/searchHotKeyword/del").param("keyword", "牛肉干"))
                .andExpect(status().isOk());

        verify(searchHotKeywordService).deleteByKeyword("牛肉干");
    }
}

@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    @Mock
    private RagQuestionService ragQuestionService;

    @InjectMocks
    private RagController ragController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ragController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadRagQuestion_ok() throws Exception {
        when(ragQuestionService.findListByPage(any())).thenReturn(new PaginationResultVO<>());

        mockMvc.perform(post("/admin/rag/loadRagQuestion")
                        .param("pageNo", "1")
                        .param("questionFuzzy", "退款"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(ragQuestionService).findListByPage(org.mockito.ArgumentMatchers.argThat(q ->
                "退款".equals(q.getQuestionFuzzy())));
    }

    @Test
    void saveRagQuestion_ok() throws Exception {
        mockMvc.perform(post("/admin/rag/saveRagQuestion")
                        .param("question", "如何退款")
                        .param("answer", "联系客服"))
                .andExpect(status().isOk());

        verify(ragQuestionService).saveRagQuestion(null, null, "如何退款", "联系客服");
    }

    @Test
    void delRagQuestion_ok() throws Exception {
        mockMvc.perform(post("/admin/rag/delRagQuestion").param("questionId", "5"))
                .andExpect(status().isOk());

        verify(ragQuestionService).deleteRagQuestionByQuestionId(5);
    }
}

@ExtendWith(MockitoExtension.class)
class RagSyncFailureControllerTest {

    @Mock
    private RagSyncFailureService ragSyncFailureService;

    @InjectMocks
    private RagSyncFailureController ragSyncFailureController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ragSyncFailureController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadDataList_ok() throws Exception {
        when(ragSyncFailureService.loadList(any())).thenReturn(new PaginationResultVO<>());

        mockMvc.perform(post("/admin/ragSyncFailure/loadDataList").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(ragSyncFailureService).loadList(any(RagSyncFailureQuery.class));
    }

    @Test
    void replay_ok() throws Exception {
        mockMvc.perform(post("/admin/ragSyncFailure/replay").param("logId", "3"))
                .andExpect(status().isOk());

        verify(ragSyncFailureService).replay(3);
    }

    @Test
    void updateStatus_ok() throws Exception {
        mockMvc.perform(post("/admin/ragSyncFailure/updateStatus")
                        .param("logId", "3")
                        .param("status", "2")
                        .param("handleRemark", "已重放"))
                .andExpect(status().isOk());

        verify(ragSyncFailureService).updateStatus(3, 2, "已重放");
    }

    @Test
    void dismissRedisSnapshot_ok() throws Exception {
        mockMvc.perform(post("/admin/ragSyncFailure/dismissRedisSnapshot")
                        .param("dataId", "P1")
                        .param("dataType", "PRODUCT"))
                .andExpect(status().isOk());

        verify(ragSyncFailureService).dismissRedisSnapshot("P1", "PRODUCT");
    }
}
