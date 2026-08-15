package com.simlect.api;

import com.simlect.api.dto.PayCloseDTO;
import com.simlect.api.dto.PayQueryDTO;
import com.simlect.api.dto.PayRefundDTO;
import com.simlect.api.dto.PayTradeCreateDTO;
import com.simlect.api.dto.PayTradeStatusDTO;
import com.simlect.api.dto.PayUrlRequestDTO;
import com.simlect.api.fallback.PayFeignFallbackFactory;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.entity.vo.ResponseVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 这个接口干什么？
 * 声明「如何用 HTTP 调用 simlect-pay（支付） 的内部 API」。本身没有实现代码；
 * 运行时由 OpenFeign 生成代理，按方法上的 @PostMapping 发请求。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单拉起支付、创建待支付单、关单标记支付关闭——支付单在 pay 库
 * - 角色：跨服务契约（API 模块）。一域一库后禁止跨库 Mapper，同步读写别的域必须走 Feign。
 *   内部鉴权：请求头 X-Internal-Token 由 FeignInternalAuthInterceptor 自动加上。
 * <p>
 * 调用链：
 * <pre>
 * 1. order → PayFeignSupport
 * 2. PayFeignSupport 调用本接口方法
 * 3. Feign 代理 → Gateway → 目标服务 /internal/pay/* → Controller → Service
 *    （失败时走 PayFeignFallbackFactory，见该类注释）
 * </pre>
 * 和 MQ/Outbox 的区别：本接口是同步 RPC（当下就要结果）；关单通知等异步最终一致走消息队列。
 */
@FeignClient(name = "simlect-pay", contextId = "payFeignClient", path = "/internal/pay",
        fallbackFactory = PayFeignFallbackFactory.class)
public interface PayFeignClient {

    @PostMapping("/trade/createPending")
    ResponseVO<Void> createPending(@RequestBody PayTradeCreateDTO dto);

    @PostMapping("/trade/markSuccess")
    ResponseVO<Void> markSuccess(@RequestBody PayTradeStatusDTO dto);

    @PostMapping("/trade/markClosed")
    ResponseVO<Void> markClosed(@RequestBody PayTradeStatusDTO dto);

    @PostMapping("/trade/markRefunded")
    ResponseVO<Void> markRefunded(@RequestBody PayTradeStatusDTO dto);

    @PostMapping("/channel/getPayUrl")
    ResponseVO<PayInfoDTO> getPayUrl(@RequestBody PayUrlRequestDTO dto);

    @PostMapping("/channel/refund")
    ResponseVO<Void> refund(@RequestBody PayRefundDTO dto);

    @PostMapping("/channel/closeOrder")
    ResponseVO<Void> closeOrder(@RequestBody PayCloseDTO dto);

    @PostMapping("/channel/queryOrder")
    ResponseVO<PayOrderNotifyDTO> queryOrder(@RequestBody PayQueryDTO dto);
}
