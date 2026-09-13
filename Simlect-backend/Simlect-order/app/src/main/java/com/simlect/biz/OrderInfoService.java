package com.simlect.biz;

import java.util.List;

import com.simlect.api.dto.CouponRushPrepareDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.dto.PostOrderDTO;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.entity.po.OrderItem;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.entity.po.OrderInfo;
import com.simlect.api.vo.OrderCountVO;
import com.simlect.entity.vo.PaginationResultVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public interface OrderInfoService {

	List<OrderInfo> findListByParam(OrderInfoQuery param);

	Integer findCountByParam(OrderInfoQuery param);

	PaginationResultVO<OrderInfo> findListByPage(OrderInfoQuery param);

	Integer add(OrderInfo bean);

	Integer addBatch(List<OrderInfo> listBean);

	Integer addOrUpdateBatch(List<OrderInfo> listBean);

	Integer updateByParam(OrderInfo bean,OrderInfoQuery param);

	Integer deleteByParam(OrderInfoQuery param);

	OrderInfo getOrderInfoByOrderId(String orderId);

	Integer updateOrderInfoByOrderId(OrderInfo bean,String orderId);

	Integer deleteOrderInfoByOrderId(String orderId);

    PayInfoDTO postOrder(@NotEmpty String userId, @Valid PostOrderDTO postOrderDTO);

	CouponRushPrepareDTO prepareCouponRush(@NotEmpty String userId, @NotEmpty String couponId);

	PayInfoDTO postCouponRushOrder(@NotEmpty String userId, @NotEmpty String couponId, @NotEmpty String payMethod);

	PayInfoDTO getPayInfo(@NotEmpty String userId,@NotEmpty String orderId);

	void cancelOrder(String userId, @NotEmpty String orderId,@NotEmpty OrderStatusEnum orderStatusEnum);

	void paySuccess(@NotEmpty PayOrderNotifyDTO payOrderNotifyDTO);

	void syncPaidCouponRushUserCoupons(@NotEmpty String userId);

	void refund(@NotEmpty OrderItem orderItem, String userId);

	List<OrderCountVO> getOrderCountInfo(String userId);

	void onOrderConfirmed(String userId, String orderId);

	boolean confirmOrderReceipt(String userId, String orderId);

	boolean deleteOrder(String userId, String orderId);

	boolean cancelUnpaidOrderForPayTimeout(String orderId);

    PaginationResultVO<OrderInfo> findByProductNameFuzzy(PaginationResultVO<OrderInfo> resultVO, String productNameFuzzy);

	void addAllOrderToDelayQueue(List<OrderInfo> orderInfoList);

	void enrichCouponInfo(OrderInfo orderInfo);

	void enrichCouponInfo(List<OrderInfo> orderInfoList);
}
