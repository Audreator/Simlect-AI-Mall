package com.simlect.controller.admin;

import java.util.List;

import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.po.OrderInfo;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.biz.OrderInfoService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController("orderInfoController")
@RequestMapping("/admin/orderInfo")
public class OrderInfoController extends com.simlect.controller.admin.ABaseController{

	@Resource
	private OrderInfoService orderInfoService;

	@PostMapping("/loadDataList")
	public ResponseVO loadDataList(OrderInfoQuery query){
		return getSuccessResponseVO(orderInfoService.findListByPage(query));
	}

	@PostMapping("/add")
	public ResponseVO add(OrderInfo bean) {
		orderInfoService.add(bean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addBatch")
	public ResponseVO addBatch(@RequestBody List<OrderInfo> listBean) {
		orderInfoService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addOrUpdateBatch")
	public ResponseVO addOrUpdateBatch(@RequestBody List<OrderInfo> listBean) {
		orderInfoService.addOrUpdateBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/getOrderInfoByOrderId")
	public ResponseVO getOrderInfoByOrderId(String orderId) {
		return getSuccessResponseVO(orderInfoService.getOrderInfoByOrderId(orderId));
	}

	@AdminSensitiveConfirm
    @PostMapping("/updateOrderInfoByOrderId")
	public ResponseVO updateOrderInfoByOrderId(OrderInfo bean,String orderId) {
		orderInfoService.updateOrderInfoByOrderId(bean,orderId);
		return getSuccessResponseVO(null);
	}

@AdminSensitiveConfirm
    	@PostMapping("/deleteOrderInfoByOrderId")
	public ResponseVO deleteOrderInfoByOrderId(String orderId) {
		orderInfoService.deleteOrderInfoByOrderId(orderId);
		return getSuccessResponseVO(null);
	}
}
