package com.simlect.api.fallback;

import com.simlect.api.PayFeignClient;
import com.simlect.api.dto.PayCloseDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.dto.PayQueryDTO;
import com.simlect.api.dto.PayRefundDTO;
import com.simlect.api.dto.PayTradeCreateDTO;
import com.simlect.api.dto.PayTradeStatusDTO;
import com.simlect.api.dto.PayUrlRequestDTO;
import com.simlect.api.support.FeignFallbackResponses;
import com.simlect.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 这个类干什么？
 * 当 Feign 调不到「simlect-pay（支付）」时（超时、宕机、熔断、网络错误），OpenFeign 会回调本工厂的 create()。
 * create() 返回一个「假的 PayFeignClient」：方法里不再发 HTTP，直接返回「支付服务暂不可用」的 ResponseVO。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：创建待支付、拉支付参数、关单标记——支付单在 pay 库
 * - 角色：远程调用失败时的兜底。正常调用不会进这个类；它不实现真实业务，也不假装成功。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. order → PayFeignSupport
 *    → 作用：业务只关心远程调用成功还是失败
 * 2. PayFeignSupport → PayFeignClient.xxx()
 *    → 作用：经 Gateway + X-Internal-Token 访问 /internal/pay/*
 * 3a. 成功：进入目标微服务
 *    → 作用：生成支付参数/待支付记录
 * 3b. 失败：本类 create(cause) → 假 Client
 *    → 作用：返回统一「不可用」，并记日志（cause）
 * 4. Support 拆包 → 抛业务异常 / 上层失败
 *    → 作用：下单失败回滚，避免有订单无支付单
 * </pre>
 * 为什么要降级工厂？
 * 避免超时异常打爆线程或返回 null 导致 NPE；把「下游挂了」变成明确失败（下单场景必须回滚）。
 */
@Slf4j
@Component
public class PayFeignFallbackFactory implements FallbackFactory<PayFeignClient> {

    /**
     * Feign 调用失败时由框架回调。
     * @param cause 下游失败根因，必须打日志
     * @return 假客户端，各方法返回「服务暂不可用」
     */
    @Override
    public PayFeignClient create(Throwable cause) {
        return new PayFeignClient() {
                @Override
                public ResponseVO<Void> createPending(PayTradeCreateDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "支付服务", cause);
                }

                @Override
                public ResponseVO<Void> markSuccess(PayTradeStatusDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "支付服务", cause);
                }

                @Override
                public ResponseVO<Void> markClosed(PayTradeStatusDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "支付服务", cause);
                }

                @Override
                public ResponseVO<Void> markRefunded(PayTradeStatusDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "支付服务", cause);
                }

                @Override
                public ResponseVO<PayInfoDTO> getPayUrl(PayUrlRequestDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "支付服务", cause);
                }

                @Override
                public ResponseVO<Void> refund(PayRefundDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "支付服务", cause);
                }

                @Override
                public ResponseVO<Void> closeOrder(PayCloseDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "支付服务", cause);
                }

                @Override
                public ResponseVO<PayOrderNotifyDTO> queryOrder(PayQueryDTO dto) {
                    return FeignFallbackResponses.unavailable(log, "支付服务", cause);
                }
            };
    }
}
