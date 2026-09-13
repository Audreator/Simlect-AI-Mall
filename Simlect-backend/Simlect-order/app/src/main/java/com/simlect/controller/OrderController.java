package com.simlect.controller;

import com.simlect.annotation.GlobalInterceptor;
import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PostOrderDTO;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.api.enums.OrderCommentStatusEnum;
import com.simlect.api.enums.OrderFromTypeEnum;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderItem;
import com.simlect.entity.po.OrderLogisticsInfo;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.entity.query.OrderItemQuery;
import com.simlect.api.vo.OrderCountVO;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.exception.BusinessException;
import com.simlect.biz.OrderInfoService;
import com.simlect.biz.OrderItemService;
import com.simlect.biz.OrderLogisticsInfoService;
import com.simlect.utils.OrderPayAmountUtil;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/order")
@RestController
public class OrderController extends ABaseController{

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private OrderItemService orderItemService;

    @Resource
    private OrderLogisticsInfoService orderLogisticsInfoService;

    // 提交订单
    @PostMapping("/postOrder")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO postOrder(@Valid @RequestBody PostOrderDTO postOrderDTO){
        // 根据userId和postOrderDTO生成订单
        PayInfoDTO payInfoDTO = orderInfoService.postOrder(getTokenUserInfo().getUserId(), postOrderDTO);
        return getSuccessResponseVO(payInfoDTO);
    }

    // 获取支付信息
    @PostMapping("/getPayInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getPayInfo(@NotEmpty String orderId){
        PayInfoDTO payInfoDTO = orderInfoService.getPayInfo(getTokenUserInfo().getUserId(),orderId);
        return getSuccessResponseVO(payInfoDTO);
    }

    // 查询订单信息
    @PostMapping("/getOrderInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getOrderInfo(@NotEmpty String payOrderId){
        TokenUserInfoDTO tokenUserInfo = getTokenUserInfo();
        if (tokenUserInfo == null || StringTools.isEmpty(tokenUserInfo.getUserId())) {
            throw new BusinessException("登录超时");
        }
        String userId = tokenUserInfo.getUserId();
        // 查询query
        OrderInfoQuery query = new OrderInfoQuery();
        query.setPayOrderId(payOrderId);
        // 根据payOrderId查询订单信息
        List<OrderInfo> orderInfoList = orderInfoService.findListByParam(query);
        if (orderInfoList.isEmpty()) {
            throw new BusinessException("订单不存在");
        }
        OrderInfo orderInfo = orderInfoList.get(0);
        if (!orderInfo.getUserId().equals(userId)){
            throw new BusinessException("订单不存在");
        }
        BigDecimal payTotal = orderInfoList.stream()
                .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orderInfo.setPayTotalAmount(OrderPayAmountUtil.normalizeChannelPayAmount(payTotal));
        return getSuccessResponseVO(orderInfo);
    }

    // 查询我的订单
    @PostMapping("/loadMyOrder")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadMyOrder(@NotNull Integer pageNo, Integer status){
        // 返回PaginationResultVO<OrderInfo> 分页对象，包含订单列表和分页信息
        // 不查询OrderStatus为-1，即已删除的订单
        OrderInfoQuery query = new OrderInfoQuery();
        query.setPageNo(pageNo);
        query.setUserId(getTokenUserInfo().getUserId());
        query.setQueryItems(true);
        query.setOrderBy("order_time desc");
        if (status == null){
            query.setOrderStatusList(new Integer[]{
                    OrderStatusEnum.WAIT_PAYMENT.getStatus(),
                    OrderStatusEnum.PAID.getStatus(),
                    OrderStatusEnum.SHIPPED.getStatus(),
                    OrderStatusEnum.COMPLETED.getStatus(),
                    OrderStatusEnum.CANCELLED.getStatus(),
                    OrderStatusEnum.CLOSED.getStatus(),
                    OrderStatusEnum.REFUNDED.getStatus(),
                    OrderStatusEnum.PARTIALLY_REFUNDED.getStatus()
            });
        }else {
            query.setOrderStatus(status);
        }
        // 待评价页：已完成且（未评价）
        if (status != null && status == OrderStatusEnum.WAIT_COMMENT.getStatus()) {
            query.setOrderStatus(OrderStatusEnum.COMPLETED.getStatus());
            query.setCommentStatusList(new Integer[]{
                    OrderCommentStatusEnum.NOT_EVALUATED.getStatus(),
            });
        }
        PaginationResultVO<OrderInfo> resultVO = orderInfoService.findListByPage(query);
        return getSuccessResponseVO(resultVO);
    }

    // 取消订单
    @PostMapping("/cancelOrder")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cancelOrder(@NotEmpty String orderId){
        // 只能取消待支付的本人的订单
//        获得当前用户的userId
        String userId = getTokenUserInfo().getUserId();
        // 根据orderId查询订单
        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderId(orderId);
        if (orderInfo == null || !orderInfo.getUserId().equals(userId)){
            throw new BusinessException("订单不存在");
        }
        orderInfoService.cancelOrder(userId,orderId,OrderStatusEnum.WAIT_PAYMENT);
        return getSuccessResponseVO(null);
    }

    // 删除订单（更新数据库的orderStatus，逻辑删除）
    @PostMapping("/deleteOrder")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO deleteOrder(@NotEmpty String orderId){
        String userId = getTokenUserInfo().getUserId();
        orderInfoService.deleteOrder(userId, orderId);
        return getSuccessResponseVO(null);
    }

    // 确认订单
    @PostMapping("/confirmOrder")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO confirmOrder(@NotEmpty String orderId){
        String userId = getTokenUserInfo().getUserId();
        if (!orderInfoService.confirmOrderReceipt(userId, orderId)) {
            return getSuccessResponseVO(null);
        }
        orderInfoService.onOrderConfirmed(userId, orderId);
        return getSuccessResponseVO(null);
    }

    // 退款
    @PostMapping("/refundOrder")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO refundOrder(@NotEmpty String orderItemId){
        String userId = getTokenUserInfo().getUserId();
        // 根据orderItemId查询订单
        OrderItem orderItem = orderItemService.getOrderItemByOrderItemId(orderItemId);
        if (orderItem == null) {
            throw new BusinessException("订单明细不存在");
        }
        orderInfoService.refund(orderItem, userId);
        return getSuccessResponseVO(null);
    }

    // 订单详情（含明细）
    @PostMapping("/getMyOrderDetail")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getMyOrderDetail(@NotEmpty String orderId){
        String userId = getTokenUserInfo().getUserId();
        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderId(orderId);
        if (orderInfo == null || !orderInfo.getUserId().equals(userId)){
            throw new BusinessException("订单不存在");
        }
        OrderItemQuery orderItemQuery = new OrderItemQuery();
        orderItemQuery.setOrderId(orderId);
        orderInfo.setOrderItemList(orderItemService.findListByParam(orderItemQuery));
        orderInfoService.enrichCouponInfo(orderInfo);
        return getSuccessResponseVO(orderInfo);
    }

    // 查看物流
    @PostMapping("/getLogistics")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getLogistics(@NotEmpty String orderId){
        return getSuccessResponseVO(orderLogisticsInfoService.getOrderLogisticsRecords(getTokenUserInfo().getUserId(), orderId));
    }

    // 获取订单数量 待付款、待发货、待收货、待评价
    @GetMapping("/getOrderCountInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getOrderCountInfo(){
        String userId = getTokenUserInfo().getUserId();
        List<OrderCountVO> orderCountVOList = orderInfoService.getOrderCountInfo(userId);
        return getSuccessResponseVO(orderCountVOList);
    }

}
