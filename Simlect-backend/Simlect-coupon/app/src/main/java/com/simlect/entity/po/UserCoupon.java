package com.simlect.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.simlect.entity.enums.DateTimePatternEnum;
import com.simlect.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

public class UserCoupon implements Serializable {

	private String userCouponId;

	private String userId;

	private String couponId;

	private Date receiveTime;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date useTime;

	private String orderId;

	private Integer status;

	public void setUserCouponId(String userCouponId){
		this.userCouponId = userCouponId;
	}

	public String getUserCouponId(){
		return this.userCouponId;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setCouponId(String couponId){
		this.couponId = couponId;
	}

	public String getCouponId(){
		return this.couponId;
	}

	public Date getReceiveTime() {
		return receiveTime;
	}

	public void setReceiveTime(Date receiveTime) {
		this.receiveTime = receiveTime;
	}

	public void setUseTime(Date useTime){
		this.useTime = useTime;
	}

	public Date getUseTime(){
		return this.useTime;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	@Override
	public String toString (){
		return "用户优惠券记录ID:"+(userCouponId == null ? "空" : userCouponId)+"，用户ID:"+(userId == null ? "空" : userId)+"，优惠券ID:"+(couponId == null ? "空" : couponId)+"，使用时间:"+(useTime == null ? "空" : DateUtil.format(useTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，状态 0:未使用 1:已使用 2:已过期 3:已作废:"+(status == null ? "空" : status);
	}
}
