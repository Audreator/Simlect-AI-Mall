package com.simlect.biz.impl;

import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.po.RagQuestion;
import com.simlect.entity.query.RagQuestionQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.RagQuestionMapper;
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
class RagQuestionServiceImplTest {

    @Mock
    private RagQuestionMapper<RagQuestion, RagQuestionQuery> ragQuestionMapper;
    @Mock
    private ReliableMessageSender reliableMessageSender;

    @InjectMocks
    private RagQuestionServiceImpl ragQuestionService;

    private RagQuestion buildQuestion() {
        RagQuestion question = new RagQuestion();
        question.setQuestionId(1);
        question.setQuestion("如何退款");
        question.setAnswer("联系客服");
        return question;
    }

    @Test
    void findListByPage_defaultPageSize() {
        when(ragQuestionMapper.selectCount(any())).thenReturn(20);
        when(ragQuestionMapper.selectList(any())).thenReturn(new ArrayList<>());
        RagQuestionQuery query = new RagQuestionQuery();
        query.setPageNo(1);

        PaginationResultVO<RagQuestion> result = ragQuestionService.findListByPage(query);

        assertEquals(20, result.getTotalCount());
        assertEquals(15, result.getPageSize());
    }

    @Test
    void deleteRagQuestionByQuestionId_success_queuesVectorDeletion() {
        when(ragQuestionMapper.deleteByQuestionId(1)).thenReturn(1);

        assertEquals(1, ragQuestionService.deleteRagQuestionByQuestionId(1));

        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.RAG_EXCHANGE),
                eq(RabbitMQConfig.RAG_QUEUE_KEY), any(), anyString(), any());
    }

    @Test
    void deleteRagQuestionByQuestionId_zeroRows_noMq() {
        when(ragQuestionMapper.deleteByQuestionId(1)).thenReturn(0);

        assertEquals(0, ragQuestionService.deleteRagQuestionByQuestionId(1));
        verifyNoInteractions(reliableMessageSender);
    }

    @Test
    void saveRagQuestion_new_createsAndQueues() {
        // 模拟数据库回填自增主键
        when(ragQuestionMapper.insert(any())).thenAnswer(inv -> {
            RagQuestion bean = inv.getArgument(0);
            bean.setQuestionId(10);
            return 1;
        });

        ragQuestionService.saveRagQuestion(null, "相似问题", "如何退款", "联系客服");

        verify(ragQuestionMapper).insert(argThat(q ->
                "如何退款".equals(q.getQuestion()) && q.getCreateTime() != null));
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.RAG_EXCHANGE),
                eq(RabbitMQConfig.RAG_QUEUE_KEY), any(), anyString(), any());
    }

    @Test
    void saveRagQuestion_updateMissing_throws() {
        when(ragQuestionMapper.selectByQuestionId(99)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> ragQuestionService.saveRagQuestion(99, "s", "q", "a"));
        assertTrue(e.getMessage().contains("不存在"));
    }

    @Test
    void saveRagQuestion_update_queues() {
        when(ragQuestionMapper.selectByQuestionId(1)).thenReturn(buildQuestion());
        when(ragQuestionMapper.updateByQuestionId(any(), any())).thenReturn(1);

        ragQuestionService.saveRagQuestion(1, "新相似", "新问题", "新答案");

        verify(ragQuestionMapper).updateByQuestionId(argThat(q ->
                "新问题".equals(q.getQuestion())), eq(1));
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.RAG_EXCHANGE),
                eq(RabbitMQConfig.RAG_QUEUE_KEY), any(), anyString(), any());
    }
}
