package com.simlect.api.fallback;

import com.simlect.api.CartFeignClient;
import com.simlect.api.dto.CartDeleteBatchDTO;
import com.simlect.api.support.FeignFallbackResponses;
import com.simlect.entity.vo.ResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 这个类干什么？
 * 当 Feign 调不到「simlect-cart（购物车）」时（超时、宕机、熔断、网络错误），OpenFeign 会回调本工厂的 create()。
 * create() 返回一个「假的 CartFeignClient」：方法里不再发 HTTP，直接返回「购物车服务暂不可用」的 ResponseVO。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单成功后按项删除购物车——购物车数据在 cart 库，order 必须远程调
 * - 角色：远程调用失败时的兜底。正常调用不会进这个类；不实现真实删车逻辑，也不假装删成功。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. order.postOrder → CartFeignSupport.deleteBatch
 *    → 作用：下单后清理已买商品行
 * 2. CartFeignSupport → CartFeignClient.deleteBatch
 *    → 作用：经 Gateway + X-Internal-Token 访问 /internal/cart/*
 * 3a. 成功：cart 服务删除对应购物车项
 * 3b. 失败：本类 create(cause) → 假 Client
 *    → 作用：返回「购物车服务暂不可用」并记日志
 * 4. Support 拆包失败 → 抛业务异常
 *    → 作用：让上层按失败处理（避免长期「扣了库存、车还在」却毫无感知）
 * </pre>
 * 为什么要降级工厂？
 * 避免超时异常打爆线程或返回 null；把「下游挂了」变成明确失败。
 */
@Component
public class CartFeignFallbackFactory implements FallbackFactory<CartFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(CartFeignFallbackFactory.class);

    /**
     * Feign 调用失败时由框架回调。
     * @param cause 下游失败根因，必须打日志
     * @return 假客户端，deleteBatch 返回「服务暂不可用」
     */
    @Override
    public CartFeignClient create(Throwable cause) {
        return new CartFeignClient() {
            @Override
            public ResponseVO<Void> deleteBatch(CartDeleteBatchDTO dto) {
                log.error("CartFeign deleteBatch fallback", cause);
                return FeignFallbackResponses.unavailable("购物车服务");
            }
        };
    }
}
