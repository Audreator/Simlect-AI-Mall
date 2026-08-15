package com.simlect.api;

import com.simlect.api.dto.CouponRushPayRequestDTO;
import com.simlect.api.dto.CouponRushPrepareRequestDTO;
import com.simlect.api.dto.OrderIdDTO;
import com.simlect.api.dto.OrderStatsRangeDTO;
import com.simlect.api.dto.UserIdDTO;
import com.simlect.api.fallback.OrderFeignFallbackFactory;
import com.simlect.api.vo.OrderBriefVO;
import com.simlect.api.vo.OrderDailyStatsVO;
import com.simlect.api.vo.OrderRangeStatsVO;
import com.simlect.api.dto.CouponRushPrepareDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.entity.vo.ResponseVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 这个接口干什么？
 * 声明「如何用 HTTP 调用 simlect-order（订单） 的内部 API」。本身没有实现代码；
 * 运行时由 OpenFeign 生成代理，按方法上的 @PostMapping 发请求。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：支付回调入账、秒杀建单、超时关单、Agent/内部查单等——订单真相在 order 库
 * - 角色：跨服务契约（API 模块）。一域一库后禁止跨库 Mapper，同步读写别的域必须走 Feign。
 *   内部鉴权：请求头 X-Internal-Token 由 FeignInternalAuthInterceptor 自动加上。
 * <p>
 * 调用链：
 * <pre>
 * 1. pay（PayNotifyController）、coupon、admin、Agent 经 OrderFeignSupport
 * 2. OrderFeignSupport 调用本接口方法
 * 3. Feign 代理 → Gateway → 目标服务 /internal/order/* → Controller → Service
 *    （失败时走 OrderFeignFallbackFactory，见该类注释）
 * </pre>
 * 和 MQ/Outbox 的区别：本接口是同步 RPC（当下就要结果）；关单通知等异步最终一致走消息队列。
 */
@FeignClient(name = "simlect-order", contextId = "orderFeignClient", path = "/internal/order",
        fallbackFactory = OrderFeignFallbackFactory.class)
public interface OrderFeignClient {

    @PostMapping("/get")
    ResponseVO<OrderBriefVO> getOrder(@RequestBody OrderIdDTO dto);

    @PostMapping("/cancelUnpaidForPayTimeout")
    ResponseVO<Boolean> cancelUnpaidForPayTimeout(@RequestBody OrderIdDTO dto);

    @PostMapping("/confirmReceipt")
    ResponseVO<Boolean> confirmReceipt(@RequestBody OrderIdDTO dto);

    @PostMapping("/onConfirmed")
    ResponseVO<Void> onConfirmed(@RequestBody OrderIdDTO dto);

    @PostMapping("/paySuccess")
    ResponseVO<Void> paySuccess(@RequestBody PayOrderNotifyDTO dto);

    @PostMapping("/prepareCouponRush")
    ResponseVO<CouponRushPrepareDTO> prepareCouponRush(@RequestBody CouponRushPrepareRequestDTO dto);

    @PostMapping("/postCouponRushOrder")
    ResponseVO<PayInfoDTO> postCouponRushOrder(@RequestBody CouponRushPayRequestDTO dto);

    @PostMapping("/syncPaidCouponRushUserCoupons")
    ResponseVO<Void> syncPaidCouponRushUserCoupons(@RequestBody UserIdDTO dto);

    @PostMapping("/cancelOrder")
    ResponseVO<Void> cancelOrder(@RequestBody OrderIdDTO dto);

    @PostMapping("/stats/range")
    ResponseVO<OrderRangeStatsVO> aggregateRange(@RequestBody OrderStatsRangeDTO dto);

    @PostMapping("/stats/daily")
    ResponseVO<List<OrderDailyStatsVO>> aggregateDaily(@RequestBody OrderStatsRangeDTO dto);

    @PostMapping("/tool/addAllWaitPayToDelayQueue")
    ResponseVO<Void> addAllWaitPayToDelayQueue();
}
