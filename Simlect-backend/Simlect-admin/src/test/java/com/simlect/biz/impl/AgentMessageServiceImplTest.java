package com.simlect.biz.impl;

import com.simlect.biz.AgentMessageService;
import com.simlect.constants.InternalApiHeaders;
import com.simlect.entity.po.AgentMessage;
import com.simlect.entity.query.AgentMessageQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentMessageServiceImplTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @InjectMocks
    private AgentMessageServiceImpl agentMessageService;

    private RestClient.RequestBodyUriSpec spec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUpConfig() {
        // @Value 字段在单测中为空，手动注入
        ReflectionTestUtils.setField(agentMessageService, "agentBaseUrl", "http://127.0.0.1:7050");
        ReflectionTestUtils.setField(agentMessageService, "internalToken", "your-token");
    }

    private void mockRestChain(ResponseVO<?> response) {
        RestClient client = mock(RestClient.class);
        lenient().when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenReturn(client);
        spec = mock(RestClient.RequestBodyUriSpec.class);
        lenient().when(client.post()).thenReturn(spec);
        lenient().when(spec.uri(anyString())).thenReturn(spec);
        lenient().when(spec.contentType(any(MediaType.class))).thenReturn(spec);
        lenient().when(spec.header(anyString(), anyString())).thenReturn(spec);
        lenient().when(spec.body(org.mockito.Mockito.<Object>any())).thenReturn(spec);
        responseSpec = mock(RestClient.ResponseSpec.class);
        lenient().when(spec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(response);
    }

    private ResponseVO<Map<String, Object>> okResponse(Object list, int total) {
        ResponseVO<Map<String, Object>> vo = new ResponseVO<>();
        vo.setStatus("success");
        vo.setCode(200);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("totalCount", total);
        data.put("list", list);
        vo.setData(data);
        return vo;
    }

    @Test
    void findListByPage_mapsAgentResponse() {
        mockRestChain(okResponse(List.of(Map.of(
                "messageId", 7,
                "userId", "U1",
                "userMessage", "你好",
                "assistant_message", "你好呀")), 1));
        AgentMessageQuery query = new AgentMessageQuery();
        query.setPageNo(2);

        PaginationResultVO<AgentMessage> result = agentMessageService.findListByPage(query);

        assertEquals(1, result.getTotalCount());
        AgentMessage msg = result.getList().get(0);
        assertEquals(7, msg.getMessageId());
        assertEquals("U1", msg.getUserId());
        assertEquals("你好", msg.getUserMessage());
        assertEquals("你好呀", msg.getAssistantMessage());

        // 内部令牌头被写入
        verify(spec).header(InternalApiHeaders.INTERNAL_TOKEN, "your-token");
    }

    @Test
    void findListByPage_nullData_emptyResult() {
        ResponseVO<Map<String, Object>> vo = new ResponseVO<>();
        vo.setStatus("success");
        vo.setCode(200);
        mockRestChain(vo);

        PaginationResultVO<AgentMessage> result = agentMessageService.findListByPage(new AgentMessageQuery());

        assertEquals(0, result.getTotalCount());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void findListByPage_errorStatus_throws() {
        ResponseVO<Map<String, Object>> vo = new ResponseVO<>();
        vo.setStatus("error");
        vo.setCode(600);
        vo.setInfo("Agent 服务异常");
        mockRestChain(vo);

        BusinessException e = assertThrows(BusinessException.class,
                () -> agentMessageService.findListByPage(new AgentMessageQuery()));
        assertTrue(e.getMessage().contains("Agent 服务异常"));
    }

    @Test
    void findListByPage_nullResponse_throws() {
        mockRestChain(null);

        assertThrows(BusinessException.class,
                () -> agentMessageService.findListByPage(new AgentMessageQuery()));
    }

    @Test
    void deleteAgentMessageByMessageId_deleted() {
        ResponseVO<Map<String, Object>> vo = new ResponseVO<>();
        vo.setStatus("success");
        vo.setCode(200);
        vo.setData(Map.of("deleted", true));
        mockRestChain(vo);

        assertEquals(1, agentMessageService.deleteAgentMessageByMessageId(3));
    }

    @Test
    void deleteAgentMessageByMessageId_notDeleted() {
        ResponseVO<Map<String, Object>> vo = new ResponseVO<>();
        vo.setStatus("success");
        vo.setCode(200);
        vo.setData(Map.of("deleted", false));
        mockRestChain(vo);

        assertEquals(0, agentMessageService.deleteAgentMessageByMessageId(3));
    }

    @Test
    void toInt_handlesInvalidValues() {
        // 间接验证：非法数值回退默认
        ResponseVO<Map<String, Object>> vo = new ResponseVO<>();
        vo.setStatus("success");
        vo.setCode(200);
        vo.setData(Map.of("totalCount", "not-a-number", "list", List.of()));
        mockRestChain(vo);

        PaginationResultVO<AgentMessage> result = agentMessageService.findListByPage(new AgentMessageQuery());

        assertEquals(0, result.getTotalCount());
    }
}
