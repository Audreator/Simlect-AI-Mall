package com.simlect.entity.po;

import java.util.Date;
import com.simlect.entity.enums.DateTimePatternEnum;
import com.simlect.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

public class AgentMessage implements Serializable {

	// 消息ID
	private Integer messageId;

	// AI消息
	private String assistantMessage;

	// 用户消息
	private String userMessage;

	// 发送时间
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date sendTime;

	// 用户ID
	private String userId;

	// 0:用户取消 1:回答中 2:完成
	private Integer status;

	// 业务类型
	private String bizType;

	// 业务数据
	private String bizData;

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

	public void setUserMessage(String userMessage){
		this.userMessage = userMessage;
	}

	public String getUserMessage(){
		return this.userMessage;
	}

	public void setSendTime(Date sendTime){
		this.sendTime = sendTime;
	}

	public Date getSendTime(){
		return this.sendTime;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
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

	public void setBizData(String bizData){
		this.bizData = bizData;
	}

	public String getBizData(){
		return this.bizData;
	}

	@Override
	public String toString (){
		return "消息ID:"+(messageId == null ? "空" : messageId)+"，AI消息:"+(assistantMessage == null ? "空" : assistantMessage)+"，用户消息:"+(userMessage == null ? "空" : userMessage)+"，发送时间:"+(sendTime == null ? "空" : DateUtil.format(sendTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，用户ID:"+(userId == null ? "空" : userId)+"，0:用户取消 1:回答中 2:完成:"+(status == null ? "空" : status)+"，业务类型:"+(bizType == null ? "空" : bizType)+"，业务数据:"+(bizData == null ? "空" : bizData);
	}
}
