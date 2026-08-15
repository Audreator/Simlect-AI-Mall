package com.simlect.controller.admin;

import com.simlect.biz.RagQuestionService;
import com.simlect.biz.RagSyncFailureService;
import com.simlect.biz.SearchHotKeywordService;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.po.RagQuestion;
import com.simlect.entity.query.RagSyncFailureQuery;
import com.simlect.entity.vo.PaginationResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RagQuestionControllerTest {

    @Mock
    private RagQuestionService ragQuestionService;

    @InjectMocks
    private RagQuestionController ragQuestionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ragQuestionController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadDataList_ok() throws Exception {
        PaginationResultVO<RagQuestion> page = new PaginationResultVO<>();
        page.setList(new ArrayList<>());
        when(ragQuestionService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/admin/ragQuestion/loadDataList").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void add_ok() throws Exception {
        mockMvc.perform(post("/admin/ragQuestion/add")
                        .param("question", "如何退款")
                        .param("answer", "联系客服"))
                .andExpect(status().isOk());

        verify(ragQuestionService).add(any(RagQuestion.class));
    }

    @Test
    void addBatch_ok() throws Exception {
        String body = "[{\"question\":\"q\",\"answer\":\"a\"}]";

        mockMvc.perform(post("/admin/ragQuestion/addBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(ragQuestionService).addBatch(any());
    }

    @Test
    void getByQuestionId_ok() throws Exception {
        when(ragQuestionService.getRagQuestionByQuestionId(1)).thenReturn(null);

        mockMvc.perform(post("/admin/ragQuestion/getRagQuestionByQuestionId").param("questionId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateByQuestionId_ok() throws Exception {
        mockMvc.perform(post("/admin/ragQuestion/updateRagQuestionByQuestionId")
                        .param("questionId", "1")
                        .param("question", "新问题"))
                .andExpect(status().isOk());

        verify(ragQuestionService).updateRagQuestionByQuestionId(any(RagQuestion.class), eq(1));
    }

    @Test
    void deleteByQuestionId_ok() throws Exception {
        mockMvc.perform(post("/admin/ragQuestion/deleteRagQuestionByQuestionId").param("questionId", "1"))
                .andExpect(status().isOk());

        verify(ragQuestionService).deleteRagQuestionByQuestionId(1);
    }
}
