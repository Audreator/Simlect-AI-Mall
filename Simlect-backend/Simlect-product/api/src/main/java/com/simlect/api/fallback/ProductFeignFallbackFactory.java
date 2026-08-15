package com.simlect.api.fallback;

import com.simlect.api.ProductFeignClient;
import com.simlect.api.dto.LessStockPageDTO;
import com.simlect.api.dto.ProductIdDTO;
import com.simlect.api.dto.ProductIdListDTO;
import com.simlect.api.dto.ProductSalesIncreaseDTO;
import com.simlect.api.dto.ProductSnapshotBatchVO;
import com.simlect.api.support.FeignFallbackResponses;
import com.simlect.api.vo.ProductRagIndexVO;
import com.simlect.api.vo.ProductSearchIndexVO;
import com.simlect.api.vo.ProductSkuListVO;
import com.simlect.api.vo.ProductSkuSnapshotVO;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 这个类干什么？
 * 当 Feign 调不到「simlect-product（商品）」时（超时、宕机、熔断、网络错误），OpenFeign 会回调本工厂的 create()。
 * create() 返回一个「假的 ProductFeignClient」：方法里不再发 HTTP，直接返回「商品服务暂不可用」的 ResponseVO。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单取商品/SKU 价格快照——商品在 product 库
 * - 角色：远程调用失败时的兜底。正常调用不会进这个类；它不实现真实业务，也不假装成功。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. order / Agent → ProductFeignSupport
 *    → 作用：业务只关心远程调用成功还是失败
 * 2. ProductFeignSupport → ProductFeignClient.xxx()
 *    → 作用：经 Gateway + X-Internal-Token 访问 /internal/product/*
 * 3a. 成功：进入目标微服务
 *    → 作用：服务端价格快照，防前端改价
 * 3b. 失败：本类 create(cause) → 假 Client
 *    → 作用：返回统一「不可用」，并记日志（cause）
 * 4. Support 拆包 → 抛业务异常 / 上层失败
 *    → 作用：下单失败，不能用空快照继续
 * </pre>
 * 为什么要降级工厂？
 * 避免超时异常打爆线程或返回 null 导致 NPE；把「下游挂了」变成明确失败（下单场景必须回滚）。
 */
@Slf4j
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    /**
     * Feign 调用失败时由框架回调。
     * @param cause 下游失败根因，必须打日志
     * @return 假客户端，各方法返回「服务暂不可用」
     */
    @Override
    public ProductFeignClient create(Throwable cause) {
        log.warn("Product Feign fallback: {}", cause == null ? "unknown" : cause.toString());
        return new ProductFeignClient() {
                @Override
                public ResponseVO<ProductSnapshotBatchVO> snapshotBatch(ProductIdListDTO dto) {
                    return FeignFallbackResponses.unavailable("商品服务");
                }

                @Override
                public ResponseVO<ProductSkuSnapshotVO> defaultSku(ProductIdDTO dto) {
                    return FeignFallbackResponses.unavailable("商品服务");
                }

                @Override
                public ResponseVO<Void> increaseSales(ProductSalesIncreaseDTO dto) {
                    return FeignFallbackResponses.unavailable("商品服务");
                }

                @Override
                public ResponseVO<ProductSearchIndexVO> getSearchIndex(ProductIdDTO dto) {
                    return FeignFallbackResponses.unavailable("商品服务");
                }

                @Override
                public ResponseVO<ProductRagIndexVO> getRagIndex(ProductIdDTO dto) {
                    return FeignFallbackResponses.unavailable("商品服务");
                }

                @Override
                public ResponseVO<PaginationResultVO<ProductSkuListVO>> lessStockSkuPage(LessStockPageDTO dto) {
                    return FeignFallbackResponses.unavailable("商品服务");
                }

                @Override
                public ResponseVO<java.util.List<String>> listOnSaleProductIds() {
                    return FeignFallbackResponses.unavailable("商品服务");
                }
            };
    }
}
