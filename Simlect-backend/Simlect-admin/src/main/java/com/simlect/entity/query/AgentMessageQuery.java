package com.simlect.entity.query;

public class AgentMessageQuery extends BaseParam {

	// 消息ID
	private Integer messageId;

	// AI消息
	private String assistantMessage;

	private String assistantMessageFuzzy;

	// 用户消息
	private String userMessage;

	private String userMessageFuzzy;

	// 发送时间
	private String sendTime;

	private String sendTimeStart;

	private String sendTimeEnd;

	// 用户ID
	private String userId;

	private String userIdFuzzy;

	// 0:用户取消 1:回答中 2:完成
	private Integer status;

	// 业务类型
	private String bizType;

	private String bizTypeFuzzy;

	// 业务数据
	private String bizData;

	private String bizDataFuzzy;

	private Integer maxMessageId;

	public Integer getMaxMessageId() {
		return maxMessageId;
	}

	public void setMaxMessageId(Integer maxMessageId) {
		this.maxMessageId = maxMessageId;
	}

	public void setMessageId(Integer messageId){
		this.messageId = messageId;
	}

	public Integer getMessageId(){
		return this.messageId;
	}

	public void setAssistantMessage(String assistantMessage){
		this.assistantMessage = assistantMessage;
	}

	public String getAssistantMessage(){
		return this.assistantMessage;
	}

	public void setAssistantMessageFuzzy(String assistantMessageFuzzy){
		this.assistantMessageFuzzy = assistantMessageFuzzy;
	}

	public String getAssistantMessageFuzzy(){
		return this.assistantMessageFuzzy;
	}

	public void setUserMessage(String userMessage){
		this.userMessage = userMessage;
	}

	public String getUserMessage(){
		return this.userMessage;
	}

	public void setUserMessageFuzzy(String userMessageFuzzy){
		this.userMessageFuzzy = userMessageFuzzy;
	}

	public String getUserMessageFuzzy(){
		return this.userMessageFuzzy;
	}

	public void setSendTime(String sendTime){
		this.sendTime = sendTime;
	}

	public String getSendTime(){
		return this.sendTime;
	}

	public void setSendTimeStart(String sendTimeStart){
		this.sendTimeStart = sendTimeStart;
	}

	public String getSendTimeStart(){
		return this.sendTimeStart;
	}
	public void setSendTimeEnd(String sendTimeEnd){
		this.sendTimeEnd = sendTimeEnd;
	}

	public String getSendTimeEnd(){
		return this.sendTimeEnd;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setUserIdFuzzy(String userIdFuzzy){
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getUserIdFuzzy(){
		return this.userIdFuzzy;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setBizType(String bizType){
		this.bizType = bizType;
	}

	public String getBizType(){
		return this.bizType;
	}

	public void setBizTypeFuzzy(String bizTypeFuzzy){
		this.bizTypeFuzzy = bizTypeFuzzy;
	}

	public String getBizTypeFuzzy(){
		return this.bizTypeFuzzy;
	}

	public void setBizData(String bizData){
		this.bizData = bizData;
	}

	public String getBizData(){
		return this.bizData;
	}

	public void setBizDataFuzzy(String bizDataFuzzy){
		this.bizDataFuzzy = bizDataFuzzy;
	}

	public String getBizDataFuzzy(){
		return this.bizDataFuzzy;
	}

}
