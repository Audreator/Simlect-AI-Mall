package com.simlect.api.support;

import com.simlect.api.PayFeignClient;
import com.simlect.api.dto.PayCloseDTO;
import com.simlect.api.dto.PayQueryDTO;
import com.simlect.api.dto.PayRefundDTO;
import com.simlect.api.dto.PayTradeCreateDTO;
import com.simlect.api.dto.PayTradeStatusDTO;
import com.simlect.api.dto.PayUrlRequestDTO;
import com.simlect.api.dto.PayInfoDTO;
import com.simlect.api.dto.PayOrderNotifyDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
/**
 * 这个类干什么？
 * 业务 Service 调用 simlect-pay（支付） 时的门面：内部转调 PayFeignClient，
 * 把 ResponseVO 拆成领域数据；失败（含降级「不可用」）统一转成 BusinessException。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单拉起支付、创建待支付单、关单标记支付关闭——支付单在 pay 库
 * - 角色：防腐层。order 下单/关单应注入本类，而不是自己处理 Feign 的 ResponseVO/异常细节。
 * <p>
 * 调用链：
 * <pre>
 * 1. 领域 Service（如下单）→ 本类对外方法
 * 2. 本类 → PayFeignClient（Feign；失败可能进 PayFeignFallbackFactory）
 * 3. 成功：取出 data 返回给 Service
 * 4. 失败：抛业务异常 → 下单失败回滚，避免「有订单无支付单」
 * </pre>
 * 为什么多这一层？避免每个 Service 重复「拆包 + 判 status + 转异常」，并固定失败语义。
 */

@Component
public class PayFeignSupport {

    @Resource
    private PayFeignClient payFeignClient;
    @Resource
    private FeignResponseSupport feignResponseSupport;

    public void createPending(String userId, String payOrderId, String orderId, BigDecimal payAmount, String payChannel) {
        feignResponseSupport.run(
                () -> payFeignClient.createPending(new PayTradeCreateDTO(userId, payOrderId, orderId, payAmount, payChannel)),
                "创建支付流水失败");
    }

    public void markSuccess(String payOrderId, String channelOrderId) {
        feignResponseSupport.run(
                () -> payFeignClient.markSuccess(new PayTradeStatusDTO(payOrderId, channelOrderId)),
                "更新支付成功失败");
    }

    public void markClosed(String payOrderId) {
        feignResponseSupport.run(
                () -> payFeignClient.markClosed(new PayTradeStatusDTO(payOrderId)),
                "关闭支付流水失败");
    }

    public void markRefunded(String payOrderId) {
        feignResponseSupport.run(
                () -> payFeignClient.markRefunded(new PayTradeStatusDTO(payOrderId)),
                "更新退款状态失败");
    }

    public PayInfoDTO getPayUrl(String payChannel, String payOrderId, String subject, BigDecimal amount) {
        return feignResponseSupport.call(
                () -> payFeignClient.getPayUrl(new PayUrlRequestDTO(payChannel, payOrderId, subject, amount)),
                "获取支付链接失败");
    }

    public void refund(String sourcePayOrderId, String refundOrderId, BigDecimal refundAmount, String payChannel) {
        feignResponseSupport.run(
                () -> payFeignClient.refund(new PayRefundDTO(sourcePayOrderId, refundOrderId, refundAmount, payChannel)),
                "支付退款失败");
    }

    public void closeOrder(String payOrderId, String payChannel) {
        feignResponseSupport.run(
                () -> payFeignClient.closeOrder(new PayCloseDTO(payOrderId, payChannel)),
                "关闭支付渠道订单失败");
    }

    public PayOrderNotifyDTO queryOrder(String payOrderId, String payChannel) {
        return feignResponseSupport.call(
                () -> payFeignClient.queryOrder(new PayQueryDTO(payOrderId, payChannel)),
                "查询支付订单失败");
    }
}
