package com.simlect.api.fallback;

import com.simlect.api.StockFeignClient;
import com.simlect.api.dto.LessStockPageDTO;
import com.simlect.api.dto.ProductIdDTO;
import com.simlect.api.dto.SkuStockBatchChangeDTO;
import com.simlect.api.dto.SkuStockChangeDTO;
import com.simlect.api.dto.SkuStockDTO;
import com.simlect.api.dto.SkuStockQueryDTO;
import com.simlect.api.dto.SkuStockSetDTO;
import com.simlect.api.support.FeignFallbackResponses;
import com.simlect.api.vo.ProductTotalStockVO;
import com.simlect.api.vo.StockChangeResultVO;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 这个类干什么？
 * 当 Feign 调不到「simlect-stock（库存）」时（超时、宕机、熔断、网络错误），OpenFeign 会回调本工厂的 create()。
 * create() 返回一个「假的 StockFeignClient」：方法里不再发 HTTP，直接返回「库存服务暂不可用」的 ResponseVO。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单锁库存/扣减、关单回补、查 SKU——库存只在 stock 库，order 必须远程调
 * - 角色：远程调用失败时的兜底。正常调用不会进这个类；它不实现真实业务，也不假装成功。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. order.OrderInfoServiceImpl → StockFeignSupport
 *    → 作用：业务只关心远程调用成功还是失败
 * 2. StockFeignSupport → StockFeignClient.xxx()
 *    → 作用：经 Gateway + X-Internal-Token 访问 /internal/stock/*
 * 3a. 成功：进入目标微服务
 *    → 作用：stock 改库存表，作为下单 Seata 分支
 * 3b. 失败：本类 create(cause) → 假 Client
 *    → 作用：返回统一「不可用」，并记日志（cause）
 * 4. Support 拆包 → 抛业务异常 / 上层失败
 *    → 作用：抛业务异常 → 下单全局事务回滚，绝不能假装扣库存成功
 * </pre>
 * 为什么要降级工厂？
 * 避免超时异常打爆线程或返回 null 导致 NPE；把「下游挂了」变成明确失败（下单场景必须回滚）。
 */
@Slf4j
@Component
public class StockFeignFallbackFactory implements FallbackFactory<StockFeignClient> {

    /**
     * Feign 调用失败时由框架回调。
     * @param cause 下游失败根因，必须打日志
     * @return 假客户端，各方法返回「服务暂不可用」
     */
    @Override
    public StockFeignClient create(Throwable cause) {
        log.warn("Stock Feign fallback: {}", cause == null ? "unknown" : cause.toString());
        return new StockFeignClient() {
                @Override
                public ResponseVO<SkuStockDTO> getStock(SkuStockQueryDTO dto) {
                    return FeignFallbackResponses.unavailable("库存服务");
                }

                @Override
                public ResponseVO<StockChangeResultVO> changeStock(SkuStockChangeDTO dto) {
                    return FeignFallbackResponses.unavailable("库存服务");
                }

                @Override
                public ResponseVO<StockChangeResultVO> changeStockBatch(SkuStockBatchChangeDTO dto) {
                    return FeignFallbackResponses.unavailable("库存服务");
                }

                @Override
                public ResponseVO<Void> lockAndVerify(SkuStockBatchChangeDTO dto) {
                    return FeignFallbackResponses.unavailable("库存服务");
                }

                @Override
                public ResponseVO<Void> setStock(SkuStockSetDTO dto) {
                    return FeignFallbackResponses.unavailable("库存服务");
                }

                @Override
                public ResponseVO<ProductTotalStockVO> totalByProduct(ProductIdDTO dto) {
                    return FeignFallbackResponses.unavailable("库存服务");
                }

                @Override
                public ResponseVO<PaginationResultVO<SkuStockDTO>> listLessThan(LessStockPageDTO dto) {
                    return FeignFallbackResponses.unavailable("库存服务");
                }
            };
    }
}
