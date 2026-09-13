package com.simlect.biz;

import com.simlect.api.dto.CouponRushPayRequestDTO;
import com.simlect.api.dto.CouponRushPrepareRequestDTO;
import com.simlect.api.dto.OrderIdDTO;
import com.simlect.api.dto.OrderStatsRangeDTO;
import com.simlect.api.dto.UserIdDTO;
import com.simlect.api.vo.OrderBriefVO;
import com.simlect.api.vo.OrderDailyStatsVO;
import com.simlect.api.vo.OrderRangeStatsVO;
import com.simlect.api.dto.CouponRushPrepareDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.enums.OrderItemStatusEnum;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderItem;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class OrderInternalService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private static final Integer[] SALE_STATUSES = new Integer[]{
            OrderStatusEnum.PAID.getStatus(),
            OrderStatusEnum.SHIPPED.getStatus(),
            OrderStatusEnum.COMPLETED.getStatus(),
            OrderStatusEnum.PARTIALLY_REFUNDED.getStatus()
    };

    private static final Integer[] REFUND_STATUSES = new Integer[]{
            OrderStatusEnum.REFUNDED.getStatus(),
            OrderStatusEnum.PARTIALLY_REFUNDED.getStatus()
    };

    @Resource
    private OrderInfoService orderInfoService;

    public OrderBriefVO getOrder(OrderIdDTO dto) {
        if (dto == null || StringTools.isEmpty(dto.getOrderId())) {
            return null;
        }
        OrderInfo order = orderInfoService.getOrderInfoByOrderId(dto.getOrderId());
        if (order == null) {
            return null;
        }
        OrderBriefVO vo = new OrderBriefVO();
        vo.setOrderId(order.getOrderId());
        vo.setUserId(order.getUserId());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setAmount(order.getAmount());
        return vo;
    }

    public boolean cancelUnpaidForPayTimeout(OrderIdDTO dto) {
        return orderInfoService.cancelUnpaidOrderForPayTimeout(dto.getOrderId());
    }

    public boolean confirmReceipt(OrderIdDTO dto) {
        return orderInfoService.confirmOrderReceipt(dto.getUserId(), dto.getOrderId());
    }

    public void onConfirmed(OrderIdDTO dto) {
        orderInfoService.onOrderConfirmed(dto.getUserId(), dto.getOrderId());
    }

    public void paySuccess(PayOrderNotifyDTO dto) {
        orderInfoService.paySuccess(dto);
    }

    public CouponRushPrepareDTO prepareCouponRush(CouponRushPrepareRequestDTO dto) {
        return orderInfoService.prepareCouponRush(dto.getUserId(), dto.getCouponId());
    }

    public PayInfoDTO postCouponRushOrder(CouponRushPayRequestDTO dto) {
        return orderInfoService.postCouponRushOrder(dto.getUserId(), dto.getCouponId(), dto.getPayMethod());
    }

    public void syncPaidCouponRushUserCoupons(UserIdDTO dto) {
        orderInfoService.syncPaidCouponRushUserCoupons(dto.getUserId());
    }

    public void cancelOrder(OrderIdDTO dto) {
        String userId = StringTools.isEmpty(dto.getUserId()) ? null : dto.getUserId();
        OrderStatusEnum expectedStatus = userId == null
                ? OrderStatusEnum.CLOSED
                : OrderStatusEnum.WAIT_PAYMENT;
        orderInfoService.cancelOrder(userId, dto.getOrderId(), expectedStatus);
    }

    public OrderRangeStatsVO aggregateRange(OrderStatsRangeDTO dto) {
        OrderRangeStatsVO vo = new OrderRangeStatsVO();
        if (dto == null || StringTools.isEmpty(dto.getStartTime()) || StringTools.isEmpty(dto.getEndTime())) {
            return vo;
        }
        OrderInfoQuery saleQuery = baseQuery(dto.getStartTime(), dto.getEndTime());
        saleQuery.setOrderStatusList(SALE_STATUSES);
        for (OrderInfo order : safeList(orderInfoService.findListByParam(saleQuery))) {
            BigDecimal saleAmount = calcEffectiveSaleAmount(order);
            if (saleAmount.compareTo(BigDecimal.ZERO) > 0) {
                vo.setSaleAmount(vo.getSaleAmount().add(saleAmount));
                vo.setSaleOrderCount(vo.getSaleOrderCount().add(BigDecimal.ONE));
            }
        }

        OrderInfoQuery refundQuery = baseQuery(dto.getStartTime(), dto.getEndTime());
        refundQuery.setOrderStatusList(REFUND_STATUSES);
        BigDecimal refundAmount = BigDecimal.ZERO;
        for (OrderInfo order : safeList(orderInfoService.findListByParam(refundQuery))) {
            refundAmount = refundAmount.add(sumNonNormalItemAmount(order));
        }
        vo.setRefundAmount(refundAmount);
        return vo;
    }

    public List<OrderDailyStatsVO> aggregateDaily(OrderStatsRangeDTO dto) {
        if (dto == null || StringTools.isEmpty(dto.getStartTime()) || StringTools.isEmpty(dto.getEndTime())) {
            return Collections.emptyList();
        }
        OrderInfoQuery query = baseQuery(dto.getStartTime(), dto.getEndTime());
        query.setOrderBy("order_time asc");
        List<OrderInfo> orderInfoList = safeList(orderInfoService.findListByParam(query));
        if (orderInfoList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, OrderDailyStatsVO> buckets = new TreeMap<>();
        for (OrderInfo orderInfo : orderInfoList) {
            if (StringTools.isEmpty(orderInfo.getChannelOrderId()) || orderInfo.getOrderTime() == null) {
                continue;
            }
            String businessDate = orderInfo.getOrderTime().toInstant()
                    .atZone(BUSINESS_ZONE)
                    .minusHours(1)
                    .toLocalDate()
                    .toString();
            OrderDailyStatsVO bucket = buckets.computeIfAbsent(businessDate,
                    day -> toDailyVo(day, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO));
            bucket.setSaleCount(bucket.getSaleCount().add(BigDecimal.ONE));
            Integer status = orderInfo.getOrderStatus();
            if (OrderStatusEnum.REFUNDED.getStatus().equals(status)
                    || OrderStatusEnum.PARTIALLY_REFUNDED.getStatus().equals(status)) {
                bucket.setRefundCount(bucket.getRefundCount().add(BigDecimal.ONE));
            }
            BigDecimal saleAmount = calcEffectiveSaleAmount(orderInfo);
            if (saleAmount.compareTo(BigDecimal.ZERO) > 0) {
                bucket.setSaleAmount(bucket.getSaleAmount().add(saleAmount));
            }
            bucket.setRefundAmount(bucket.getRefundAmount().add(sumNonNormalItemAmount(orderInfo)));
        }
        return new ArrayList<>(buckets.values());
    }

    private OrderInfoQuery baseQuery(String start, String end) {
        OrderInfoQuery query = new OrderInfoQuery();
        query.setOrderTimeStart(start);
        query.setOrderTimeEnd(end);
        query.setQueryItems(true);
        query.setQueryUser(false);
        return query;
    }

    private OrderDailyStatsVO toDailyVo(String businessDate, BigDecimal saleAmount, BigDecimal saleCount,
                                        BigDecimal refundAmount, BigDecimal refundCount) {
        OrderDailyStatsVO vo = new OrderDailyStatsVO();
        vo.setStatisticsDate(businessDate);
        vo.setSaleAmount(saleAmount);
        vo.setSaleCount(saleCount);
        vo.setRefundAmount(refundAmount);
        vo.setRefundCount(refundCount);
        return vo;
    }

    private BigDecimal calcEffectiveSaleAmount(OrderInfo orderInfo) {
        List<OrderItem> items = orderInfo.getOrderItemList();
        if (items == null || items.isEmpty()) {
            return orderInfo.getAmount() == null ? BigDecimal.ZERO : orderInfo.getAmount();
        }
        BigDecimal originalNormal = BigDecimal.ZERO;
        BigDecimal originalAll = BigDecimal.ZERO;
        for (OrderItem orderItem : items) {
            BigDecimal itemAmount = orderItem.getItemAmount() == null ? BigDecimal.ZERO : orderItem.getItemAmount();
            originalAll = originalAll.add(itemAmount);
            if (OrderItemStatusEnum.NORMAL.getStatus().equals(orderItem.getOrderItemStatus())) {
                originalNormal = originalNormal.add(itemAmount);
            }
        }
        if (originalNormal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal orderAmount = orderInfo.getAmount() == null ? BigDecimal.ZERO : orderInfo.getAmount();
        if (originalAll.compareTo(BigDecimal.ZERO) <= 0 || originalNormal.compareTo(originalAll) == 0) {
            return orderAmount;
        }
        return orderAmount.multiply(originalNormal).divide(originalAll, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumNonNormalItemAmount(OrderInfo orderInfo) {
        List<OrderItem> items = orderInfo.getOrderItemList();
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal refundAmount = BigDecimal.ZERO;
        Set<String> seen = new HashSet<>();
        for (OrderItem orderItem : items) {
            if (orderItem == null) {
                continue;
            }
            String itemId = orderItem.getOrderItemId();
            if (!StringTools.isEmpty(itemId) && !seen.add(itemId)) {
                continue;
            }
            if (!OrderItemStatusEnum.NORMAL.getStatus().equals(orderItem.getOrderItemStatus())) {
                refundAmount = refundAmount.add(
                        orderItem.getItemAmount() == null ? BigDecimal.ZERO : orderItem.getItemAmount());
            }
        }
        return refundAmount;
    }

    private List<OrderInfo> safeList(List<OrderInfo> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
