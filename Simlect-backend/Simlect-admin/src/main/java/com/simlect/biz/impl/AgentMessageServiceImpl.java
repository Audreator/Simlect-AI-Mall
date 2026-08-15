package com.simlect.biz.impl;

import com.simlect.biz.AgentMessageService;
import com.simlect.constants.InternalApiHeaders;
import com.simlect.entity.enums.PageSize;
import com.simlect.entity.po.AgentMessage;
import com.simlect.entity.query.AgentMessageQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.exception.BusinessException;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service("agentMessageService")
public class AgentMessageServiceImpl implements AgentMessageService {

	@Value("${simlect.agent.base-url:http://127.0.0.1:7050}")
	private String agentBaseUrl;

	@Value("${simlect.internal.token:your-token}")
	private String internalToken;

	@Resource
	private RestClient.Builder restClientBuilder;

	private RestClient client() {
		return restClientBuilder.baseUrl(agentBaseUrl.replaceAll("/$", "")).build();
	}

	private <T> T post(String path, Object body, ParameterizedTypeReference<ResponseVO<T>> type) {
		ResponseVO<T> vo = client().post()
				.uri(path)
				.contentType(MediaType.APPLICATION_JSON)
				.header(InternalApiHeaders.INTERNAL_TOKEN, internalToken)
				.body(body == null ? Map.of() : body)
				.retrieve()
				.body(type);
		if (vo == null) {
			throw new BusinessException("Agent 服务无响应");
		}
		if (vo.getStatus() != null && !"success".equalsIgnoreCase(String.valueOf(vo.getStatus()))) {
			throw new BusinessException(vo.getInfo() == null ? "Agent 调用失败" : vo.getInfo());
		}
		return vo.getData();
	}

	@Override
	public PaginationResultVO<AgentMessage> findListByPage(AgentMessageQuery param) {
		int pageNo = param.getPageNo() == null ? 1 : param.getPageNo();
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();
		Map<String, Object> body = new HashMap<>();
		body.put("pageNo", pageNo);
		body.put("pageSize", pageSize);
		if (param != null && !StringTools.isEmpty(param.getUserId())) {
			body.put("userId", param.getUserId());
		}
		Map<String, Object> data = post(
				"/api/agent/admin/loadMessages",
				body,
				new ParameterizedTypeReference<ResponseVO<Map<String, Object>>>() {});
		if (data == null) {
			return new PaginationResultVO<>(0, pageSize, pageNo, 0, Collections.emptyList());
		}
		int total = toInt(data.get("totalCount"), toInt(data.get("total"), 0));
		int pageTotal = toInt(data.get("pageTotal"), total == 0 ? 0 : (total + pageSize - 1) / pageSize);
		List<AgentMessage> list = mapMessages(data.get("list"));
		return new PaginationResultVO<>(total, pageSize, pageNo, pageTotal, list);
	}

	@Override
	public Integer deleteAgentMessageByMessageId(Integer messageId) {
		Map<String, Object> body = Map.of("messageId", messageId);
		Map<String, Object> data = post(
				"/api/agent/admin/deleteMessage",
				body,
				new ParameterizedTypeReference<ResponseVO<Map<String, Object>>>() {});
		if (data != null && Boolean.TRUE.equals(data.get("deleted"))) {
			return 1;
		}
		return 0;
	}

	private List<AgentMessage> mapMessages(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return new ArrayList<>();
		}
		List<AgentMessage> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) m;
				out.add(toMessage(map));
			}
		}
		return out;
	}

	private AgentMessage toMessage(Map<String, Object> m) {
		AgentMessage msg = new AgentMessage();
		if (m.get("messageId") != null) {
			msg.setMessageId(toInt(m.get("messageId"), null));
		}
		if (m.get("message_id") != null) {
			msg.setMessageId(toInt(m.get("message_id"), msg.getMessageId()));
		}
		Object userId = m.get("userId") != null ? m.get("userId") : m.get("user_id");
		if (userId != null) {
			msg.setUserId(String.valueOf(userId));
		}
		Object userMessage = m.get("userMessage") != null ? m.get("userMessage") : m.get("user_message");
		if (userMessage != null) {
			msg.setUserMessage(String.valueOf(userMessage));
		}
		Object assistant = m.get("assistantMessage") != null ? m.get("assistantMessage") : m.get("assistant_message");
		if (assistant != null) {
			msg.setAssistantMessage(String.valueOf(assistant));
		}
		Object status = m.get("status");
		if (status != null) {
			msg.setStatus(toInt(status, null));
		}
		Object bizType = m.get("bizType") != null ? m.get("bizType") : m.get("biz_type");
		if (bizType != null) {
			msg.setBizType(String.valueOf(bizType));
		}
		Object bizData = m.get("bizData") != null ? m.get("bizData") : m.get("biz_data");
		if (bizData != null) {
			msg.setBizData(String.valueOf(bizData));
		}
		return msg;
	}

	private static int toInt(Object v, Integer def) {
		if (v == null) {
			return def == null ? 0 : def;
		}
		try {
			return Integer.parseInt(String.valueOf(v));
		} catch (Exception e) {
			return def == null ? 0 : def;
		}
	}
}
