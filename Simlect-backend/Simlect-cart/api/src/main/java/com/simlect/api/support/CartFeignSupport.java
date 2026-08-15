package com.simlect.api.support;

import com.simlect.api.CartFeignClient;
import com.simlect.api.dto.CartDeleteBatchDTO;
import com.simlect.api.dto.CartDeleteItemDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * 这个类干什么？
 * 业务 Service 调用 simlect-cart（购物车） 时的门面：内部转调 CartFeignClient，
 * 把 ResponseVO 拆成领域数据；失败（含降级「不可用」）统一转成 BusinessException。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单成功后按项删除购物车——购物车在 cart 库
 * - 角色：防腐层。order 下单流程应注入本类，而不是自己处理 Feign 的 ResponseVO/异常细节。
 * <p>
 * 调用链：
 * <pre>
 * 1. 领域 Service（如下单）→ 本类对外方法
 * 2. 本类 → CartFeignClient（Feign；失败可能进 CartFeignFallbackFactory）
 * 3. 成功：取出 data 返回给 Service
 * 4. 失败：抛业务异常 → 下单主流程应失败/补偿，避免「扣了库存车还在」不一致长期存在（视事务边界）
 * </pre>
 * 为什么多这一层？避免每个 Service 重复「拆包 + 判 status + 转异常」，并固定失败语义。
 */

@Component
public class CartFeignSupport {

    @Resource
    private CartFeignClient cartFeignClient;
    @Resource
    private FeignResponseSupport feignResponseSupport;

    public void deleteBatch(List<CartDeleteItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        feignResponseSupport.run(
                () -> cartFeignClient.deleteBatch(new CartDeleteBatchDTO(items)),
                "清理购物车失败");
    }
}
