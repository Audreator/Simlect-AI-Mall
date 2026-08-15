package com.simlect.api;

import com.simlect.api.dto.CouponIdDTO;
import com.simlect.api.dto.CouponValidateAndLockDTO;
import com.simlect.api.dto.UserCouponCreateDTO;
import com.simlect.api.dto.UserCouponIdDTO;
import com.simlect.api.dto.UserCouponStatusChangeDTO;
import com.simlect.api.vo.CouponBriefVO;
import com.simlect.api.vo.CouponLockResultVO;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.api.vo.StockChangeResultVO;
import com.simlect.api.vo.UserCouponVO;
import com.simlect.api.fallback.CouponFeignFallbackFactory;
import com.simlect.entity.vo.ResponseVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 这个接口干什么？
 * 声明「如何用 HTTP 调用 simlect-coupon（优惠券） 的内部 API」。本身没有实现代码；
 * 运行时由 OpenFeign 生成代理，按方法上的 @PostMapping 发请求。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单锁券/核销、秒杀扣券库存——券在 coupon 库
 * - 角色：跨服务契约（API 模块）。一域一库后禁止跨库 Mapper，同步读写别的域必须走 Feign。
 *   内部鉴权：请求头 X-Internal-Token 由 FeignInternalAuthInterceptor 自动加上。
 * <p>
 * 调用链：
 * <pre>
 * 1. order → CouponFeignSupport
 * 2. CouponFeignSupport 调用本接口方法
 * 3. Feign 代理 → Gateway → 目标服务 /internal/coupon/* → Controller → Service
 *    （失败时走 CouponFeignFallbackFactory，见该类注释）
 * </pre>
 * 和 MQ/Outbox 的区别：本接口是同步 RPC（当下就要结果）；关单通知等异步最终一致走消息队列。
 */
@FeignClient(name = "simlect-coupon", contextId = "couponFeignClient", path = "/internal/coupon",
        fallbackFactory = CouponFeignFallbackFactory.class)
public interface CouponFeignClient {

    @PostMapping("/validateAndLock")
    ResponseVO<CouponLockResultVO> validateAndLock(@RequestBody CouponValidateAndLockDTO dto);

    @PostMapping("/getCoupon")
    ResponseVO<DiscountCouponVO> getCoupon(@RequestBody CouponIdDTO dto);

    @PostMapping("/getCouponBrief")
    ResponseVO<CouponBriefVO> getCouponBrief(@RequestBody CouponIdDTO dto);

    @PostMapping("/getUserCoupon")
    ResponseVO<UserCouponVO> getUserCoupon(@RequestBody UserCouponIdDTO dto);

    @PostMapping("/changeUserCouponStatus")
    ResponseVO<Void> changeUserCouponStatus(@RequestBody UserCouponStatusChangeDTO dto);

    @PostMapping("/createUserCoupon")
    ResponseVO<Void> createUserCoupon(@RequestBody UserCouponCreateDTO dto);

    @PostMapping("/deductStock")
    ResponseVO<StockChangeResultVO> deductStock(@RequestBody CouponIdDTO dto);

    @PostMapping("/rush/assertNotBlocked")
    ResponseVO<Void> assertRushNotBlocked(@RequestBody com.simlect.api.dto.CouponRushOpsDTO dto);

    @PostMapping("/rush/hasAvailableStock")
    ResponseVO<Boolean> hasAvailableRushStock(@RequestBody com.simlect.api.dto.CouponRushOpsDTO dto);

    @PostMapping("/rush/syncFromDbIfRedisZero")
    ResponseVO<Void> syncRushStockFromDbIfRedisZero(@RequestBody com.simlect.api.dto.CouponRushOpsDTO dto);

    @PostMapping("/rush/releaseRedisReserve")
    ResponseVO<Void> releaseRushRedisReserve(@RequestBody com.simlect.api.dto.CouponRushOpsDTO dto);

    @PostMapping("/rush/releaseCouponReserve")
    ResponseVO<Void> releaseRushCouponReserve(@RequestBody com.simlect.api.dto.CouponRushOpsDTO dto);

    @PostMapping("/rush/invalidateCache")
    ResponseVO<Void> invalidateCouponCache(@RequestBody com.simlect.api.dto.CouponRushOpsDTO dto);
}
