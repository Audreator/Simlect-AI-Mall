package com.simlect.api.fallback;

import com.simlect.api.CouponFeignClient;
import com.simlect.api.dto.CouponIdDTO;
import com.simlect.api.dto.CouponRushOpsDTO;
import com.simlect.api.dto.CouponValidateAndLockDTO;
import com.simlect.api.dto.UserCouponCreateDTO;
import com.simlect.api.dto.UserCouponIdDTO;
import com.simlect.api.dto.UserCouponStatusChangeDTO;
import com.simlect.api.support.FeignFallbackResponses;
import com.simlect.api.vo.CouponBriefVO;
import com.simlect.api.vo.CouponLockResultVO;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.api.vo.StockChangeResultVO;
import com.simlect.api.vo.UserCouponVO;
import com.simlect.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 这个类干什么？
 * 当 Feign 调不到「simlect-coupon（优惠券）」时（超时、宕机、熔断、网络错误），OpenFeign 会回调本工厂的 create()。
 * create() 返回一个「假的 CouponFeignClient」：方法里不再发 HTTP，直接返回「优惠券服务暂不可用」的 ResponseVO。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单锁券/核销、秒杀扣券库存——券在 coupon 库
 * - 角色：远程调用失败时的兜底。正常调用不会进这个类；它不实现真实业务，也不假装成功。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. order → CouponFeignSupport
 *    → 作用：业务只关心远程调用成功还是失败
 * 2. CouponFeignSupport → CouponFeignClient.xxx()
 *    → 作用：经 Gateway + X-Internal-Token 访问 /internal/coupon/*
 * 3a. 成功：进入目标微服务
 *    → 作用：锁券/扣 remain_count
 * 3b. 失败：本类 create(cause) → 假 Client
 *    → 作用：返回统一「不可用」，并记日志（cause）
 * 4. Support 拆包 → 抛业务异常 / 上层失败
 *    → 作用：下单/秒杀失败回滚，避免超发
 * </pre>
 * 为什么要降级工厂？
 * 避免超时异常打爆线程或返回 null 导致 NPE；把「下游挂了」变成明确失败（下单场景必须回滚）。
 */
@Slf4j
@Component
public class CouponFeignFallbackFactory implements FallbackFactory<CouponFeignClient> {

    /**
     * Feign 调用失败时由框架回调。
     * @param cause 下游失败根因，必须打日志
     * @return 假客户端，各方法返回「服务暂不可用」
     */
    @Override
    public CouponFeignClient create(Throwable cause) {
        log.warn("Coupon Feign fallback: {}", cause == null ? "unknown" : cause.toString());
        return new CouponFeignClient() {
                @Override
                public ResponseVO<CouponLockResultVO> validateAndLock(CouponValidateAndLockDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<DiscountCouponVO> getCoupon(CouponIdDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<CouponBriefVO> getCouponBrief(CouponIdDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<UserCouponVO> getUserCoupon(UserCouponIdDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<Void> changeUserCouponStatus(UserCouponStatusChangeDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<Void> createUserCoupon(UserCouponCreateDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<StockChangeResultVO> deductStock(CouponIdDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<Void> assertRushNotBlocked(CouponRushOpsDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<Boolean> hasAvailableRushStock(CouponRushOpsDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<Void> syncRushStockFromDbIfRedisZero(CouponRushOpsDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<Void> releaseRushRedisReserve(CouponRushOpsDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<Void> releaseRushCouponReserve(CouponRushOpsDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }

                @Override
                public ResponseVO<Void> invalidateCouponCache(CouponRushOpsDTO dto) {
                    return FeignFallbackResponses.unavailable("优惠券服务");
                }
            };
    }
}
