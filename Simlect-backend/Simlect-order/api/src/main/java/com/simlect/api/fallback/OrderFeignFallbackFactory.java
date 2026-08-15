package com.simlect.api.fallback;

import com.simlect.api.OrderFeignClient;
import com.simlect.api.dto.CouponRushPayRequestDTO;
import com.simlect.api.dto.CouponRushPrepareDTO;
import com.simlect.api.dto.CouponRushPrepareRequestDTO;
import com.simlect.api.dto.OrderIdDTO;
import com.simlect.api.dto.OrderStatsRangeDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.dto.UserIdDTO;
import com.simlect.api.support.FeignFallbackResponses;
import com.simlect.api.vo.OrderBriefVO;
import com.simlect.api.vo.OrderDailyStatsVO;
import com.simlect.api.vo.OrderRangeStatsVO;
import com.simlect.entity.vo.ResponseVO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 这个类干什么？
 * 当 Feign 调不到「simlect-order（订单）」时（超时、宕机、熔断、网络错误），OpenFeign 会回调本工厂的 create()。
 * create() 返回一个「假的 OrderFeignClient」：方法里不再发 HTTP，直接返回「订单服务暂不可用」的 ResponseVO。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：支付回调入账、秒杀建单、超时关单、内部查单——订单在 order 库
 * - 角色：远程调用失败时的兜底。正常调用不会进这个类；它不实现真实业务，也不假装成功。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. pay.PayNotifyController / coupon / admin → OrderFeignSupport
 *    → 作用：业务只关心远程调用成功还是失败
 * 2. OrderFeignSupport → OrderFeignClient.xxx()
 *    → 作用：经 Gateway + X-Internal-Token 访问 /internal/order/*
 * 3a. 成功：进入目标微服务
 *    → 作用：order 改状态/建秒杀单等
 * 3b. 失败：本类 create(cause) → 假 Client
 *    → 作用：返回统一「不可用」，并记日志（cause）
 * 4. Support 拆包 → 抛业务异常 / 上层失败
 *    → 作用：回调等路径明确失败（可重试），不静默丢
 * </pre>
 * 为什么要降级工厂？
 * 避免超时异常打爆线程或返回 null 导致 NPE；把「下游挂了」变成明确失败（下单场景必须回滚）。
 */
@Component
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(OrderFeignFallbackFactory.class);

    /**
     * Feign 调用失败时由框架回调。
     * @param cause 下游失败根因，必须打日志
     * @return 假客户端，各方法返回「服务暂不可用」
     */
    @Override
    public OrderFeignClient create(Throwable cause) {
        return new OrderFeignClient() {
                @Override
                public ResponseVO<OrderBriefVO> getOrder(OrderIdDTO dto) {
                    log.error("OrderFeign getOrder fallback, orderId={}", dto == null ? null : dto.getOrderId(), cause);
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<Boolean> cancelUnpaidForPayTimeout(OrderIdDTO dto) {
                    log.error("OrderFeign cancelUnpaid fallback, orderId={}", dto == null ? null : dto.getOrderId(), cause);
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<Boolean> confirmReceipt(OrderIdDTO dto) {
                    log.error("OrderFeign confirmReceipt fallback, orderId={}", dto == null ? null : dto.getOrderId(), cause);
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<Void> onConfirmed(OrderIdDTO dto) {
                    log.error("OrderFeign onConfirmed fallback, orderId={}", dto == null ? null : dto.getOrderId(), cause);
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<Void> paySuccess(PayOrderNotifyDTO dto) {
                    log.error("OrderFeign paySuccess fallback, payOrderId={}", dto == null ? null : dto.getPayOrderId(), cause);
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<CouponRushPrepareDTO> prepareCouponRush(CouponRushPrepareRequestDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<PayInfoDTO> postCouponRushOrder(CouponRushPayRequestDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<Void> syncPaidCouponRushUserCoupons(UserIdDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<Void> cancelOrder(OrderIdDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<OrderRangeStatsVO> aggregateRange(OrderStatsRangeDTO dto) {
                    log.error("OrderFeign aggregateRange fallback", cause);
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<List<OrderDailyStatsVO>> aggregateDaily(OrderStatsRangeDTO dto) {
                    log.error("OrderFeign aggregateDaily fallback", cause);
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }

                @Override
                public ResponseVO<Void> addAllWaitPayToDelayQueue() {
                    log.error("OrderFeign addAllWaitPayToDelayQueue fallback", cause);
                    return FeignFallbackResponses.unavailable(log, "订单服务", cause);
                }
            };
    }
}
