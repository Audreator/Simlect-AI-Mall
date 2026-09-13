package com.simlect.biz.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.api.dto.PayOrderMessageDTO;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.api.enums.LogisticsStatusEnum;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderLogisticsInfoRecord;
import com.simlect.entity.query.OrderLogisticsInfoRecordQuery;
import com.simlect.exception.BusinessException;
import com.simlect.api.support.UserFeignSupport;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderLogisticsInfoRecordService;
import com.simlect.support.MqIdempotencyKeys;
import com.simlect.state.OrderStateEvent;
import com.simlect.state.OrderStateMachine;
import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

import com.simlect.entity.enums.PageSize;
import com.simlect.entity.query.OrderLogisticsInfoQuery;
import com.simlect.entity.po.OrderLogisticsInfo;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.query.SimplePage;
import com.simlect.mappers.OrderLogisticsInfoMapper;
import com.simlect.biz.OrderLogisticsInfoService;
import com.simlect.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;

@Service("orderLogisticsInfoService")
public class OrderLogisticsInfoServiceImpl implements OrderLogisticsInfoService {

	@Resource
	private OrderLogisticsInfoMapper<OrderLogisticsInfo, OrderLogisticsInfoQuery> orderLogisticsInfoMapper;

	@Resource
	private OrderLogisticsInfoRecordService orderLogisticsInfoRecordService;

	@Resource
	private OrderInfoService orderInfoService;

	@Resource
	private OrderStateMachine orderStateMachine;

	@Resource
	private UserFeignSupport userFeignSupport;

	@Resource
	private ReliableMessageSender reliableMessageSender;

	@Override
	public List<OrderLogisticsInfo> findListByParam(OrderLogisticsInfoQuery param) {
		return this.orderLogisticsInfoMapper.selectList(param);
	}

	@Override
	public Integer findCountByParam(OrderLogisticsInfoQuery param) {
		return this.orderLogisticsInfoMapper.selectCount(param);
	}

	@Override
	public PaginationResultVO<OrderLogisticsInfo> findListByPage(OrderLogisticsInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<OrderLogisticsInfo> list = this.findListByParam(param);
		PaginationResultVO<OrderLogisticsInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	@Override
	public Integer add(OrderLogisticsInfo bean) {
		return this.orderLogisticsInfoMapper.insert(bean);
	}

	@Override
	public Integer addBatch(List<OrderLogisticsInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.orderLogisticsInfoMapper.insertBatch(listBean);
	}

	@Override
	public Integer addOrUpdateBatch(List<OrderLogisticsInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.orderLogisticsInfoMapper.insertOrUpdateBatch(listBean);
	}

	@Override
	public Integer updateByParam(OrderLogisticsInfo bean, OrderLogisticsInfoQuery param) {
		StringTools.checkParam(param);
		return this.orderLogisticsInfoMapper.updateByParam(bean, param);
	}

	@Override
	public Integer deleteByParam(OrderLogisticsInfoQuery param) {
		StringTools.checkParam(param);
		return this.orderLogisticsInfoMapper.deleteByParam(param);
	}

	@Override
	public OrderLogisticsInfo getOrderLogisticsInfoByOrderId(String orderId) {
		return this.orderLogisticsInfoMapper.selectByOrderId(orderId);
	}

	@Override
	public Integer updateOrderLogisticsInfoByOrderId(OrderLogisticsInfo bean, String orderId) {
		return this.orderLogisticsInfoMapper.updateByOrderId(bean, orderId);
	}

	@Override
	public Integer deleteOrderLogisticsInfoByOrderId(String orderId) {
		return this.orderLogisticsInfoMapper.deleteByOrderId(orderId);
	}

	@Override
	public OrderLogisticsInfo getOrderLogisticsRecords(String userId, String orderId) {
		// 获得OrderLogisticsInfo
		OrderLogisticsInfo orderLogisticsInfo = this.getOrderLogisticsInfoByOrderId(orderId);
		if (orderLogisticsInfo == null) {
			throw new BusinessException("物流信息不存在");
		}
		if (!orderLogisticsInfo.getUserId().equals(userId) && userId != null){
			throw new BusinessException("物流信息不存在");
		}
		// 查询物流运输记录
		List<OrderLogisticsInfoRecord> recordList = new ArrayList<>();
		// 根据orderId查询，按recordId倒序排序
		OrderLogisticsInfoRecordQuery recordQuery = new OrderLogisticsInfoRecordQuery();
		recordQuery.setOrderId(orderId);
		recordQuery.setOrderBy("record_id desc");
		recordList = orderLogisticsInfoRecordService.findListByParam(recordQuery);
		orderLogisticsInfo.setRecordList(recordList);
		return orderLogisticsInfo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delivery(OrderLogisticsInfo orderLogisticsInfo) {
		OrderLogisticsInfoQuery query = new OrderLogisticsInfoQuery();
		orderLogisticsInfo.setLogisticsStatus(LogisticsStatusEnum.IN_TRANSIT.getStatus());
		query.setOrderId(orderLogisticsInfo.getOrderId());
		Integer count = this.updateByParam(orderLogisticsInfo, query);
		if (count != 1) {
			throw new BusinessException("该订单已经发货过了");
		}
		count = orderStateMachine.transition(
				orderLogisticsInfo.getOrderId(),
				OrderStatusEnum.PAID,
				OrderStateEvent.SHIP);
		if (count != 1) {
			throw new BusinessException("该订单已经发货过了");
		}
		// 将发货信息插入order_logistics_info_record表
		OrderLogisticsInfoRecord record = new OrderLogisticsInfoRecord();
		record.setOrderId(orderLogisticsInfo.getOrderId());
		record.setRecordTime(new Date());
		record.setRecordAddress(orderLogisticsInfo.getSenderAddress());
		orderLogisticsInfoRecordService.add(record);
		PayOrderMessageDTO confirmDto = new PayOrderMessageDTO(orderLogisticsInfo.getOrderId());
		reliableMessageSender.sendMessage(
				RabbitMQConfig.PAY_EXCHANGE,
				RabbitMQConfig.PAY_CONFIRM_DELAY_KEY,
				confirmDto,
				MqIdempotencyKeys.payConfirm(orderLogisticsInfo.getOrderId()),
				MessageReliabilityLevelEnum.STANDARD);
		OrderInfo shippedOrder = orderInfoService.getOrderInfoByOrderId(orderLogisticsInfo.getOrderId());
		if (shippedOrder != null && !StringTools.isEmpty(shippedOrder.getUserId())) {
			userFeignSupport.sendNotifyAsync(shippedOrder.getUserId(), "订单已发货",
					"您的订单 " + orderLogisticsInfo.getOrderId() + " 已发货，物流单号："
							+ (orderLogisticsInfo.getLogisticsNo() == null ? "待更新" : orderLogisticsInfo.getLogisticsNo()),
					"logistics", orderLogisticsInfo.getOrderId());
		}
	}
}
