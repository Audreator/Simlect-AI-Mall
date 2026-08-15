package com.simlect.api.support;

import com.simlect.api.OrderFeignClient;
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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
/**
 * 这个类干什么？
 * 业务 Service 调用 simlect-order（订单） 时的门面：内部转调 OrderFeignClient，
 * 把 ResponseVO 拆成领域数据；失败（含降级「不可用」）统一转成 BusinessException。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：支付回调入账、秒杀建单、超时关单、Agent/内部查单等——订单真相在 order 库
 * - 角色：防腐层。pay / coupon / admin / Agent 应注入本类，而不是自己处理 Feign 的 ResponseVO/异常细节。
 * <p>
 * 调用链：
 * <pre>
 * 1. 领域 Service（如下单）→ 本类对外方法
 * 2. 本类 → OrderFeignClient（Feign；失败可能进 OrderFeignFallbackFactory）
 * 3. 成功：取出 data 返回给 Service
 * 4. 失败：抛业务异常 → 支付回调等路径明确失败（可让渠道重试），不会静默丢回调
 * </pre>
 * 为什么多这一层？避免每个 Service 重复「拆包 + 判 status + 转异常」，并固定失败语义。
 */

@Component
public class OrderFeignSupport {

    @Resource
    private OrderFeignClient orderFeignClient;
    @Resource
    private FeignResponseSupport feignResponseSupport;

    public OrderBriefVO getOrder(String orderId) {
        return feignResponseSupport.call(() -> orderFeignClient.getOrder(new OrderIdDTO(orderId)), "查询订单失败");
    }

    public void paySuccess(PayOrderNotifyDTO dto) {
        feignResponseSupport.run(() -> orderFeignClient.paySuccess(dto), "支付成功处理失败");
    }

    public CouponRushPrepareDTO prepareCouponRush(String userId, String couponId) {
        return feignResponseSupport.call(
                () -> orderFeignClient.prepareCouponRush(new CouponRushPrepareRequestDTO(userId, couponId)),
                "秒杀预占失败");
    }

    public PayInfoDTO postCouponRushOrder(String userId, String couponId, String payMethod) {
        return feignResponseSupport.call(
                () -> orderFeignClient.postCouponRushOrder(new CouponRushPayRequestDTO(userId, couponId, payMethod)),
                "秒杀下单支付失败");
    }

    public void syncPaidCouponRushUserCoupons(String userId) {
        feignResponseSupport.run(
                () -> orderFeignClient.syncPaidCouponRushUserCoupons(new UserIdDTO(userId)),
                "同步秒杀用户券失败");
    }

    public void cancelOrder(String orderId, String userId) {
        feignResponseSupport.run(
                () -> orderFeignClient.cancelOrder(new OrderIdDTO(orderId, userId)),
                "取消订单失败");
    }

    public OrderRangeStatsVO aggregateRange(String startTime, String endTime) {
        return feignResponseSupport.call(
                () -> orderFeignClient.aggregateRange(new OrderStatsRangeDTO(startTime, endTime)),
                "订单区间统计失败");
    }

    public List<OrderDailyStatsVO> aggregateDaily(String startTime, String endTime) {
        List<OrderDailyStatsVO> list = feignResponseSupport.call(
                () -> orderFeignClient.aggregateDaily(new OrderStatsRangeDTO(startTime, endTime)),
                "订单按日统计失败");
        return list == null ? Collections.emptyList() : list;
    }

    public void addAllWaitPayToDelayQueue() {
        feignResponseSupport.run(orderFeignClient::addAllWaitPayToDelayQueue, "待付款订单加入延时队列失败");
    }
}
