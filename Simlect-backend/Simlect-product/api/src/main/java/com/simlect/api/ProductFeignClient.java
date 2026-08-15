package com.simlect.api;

import com.simlect.api.dto.LessStockPageDTO;
import com.simlect.api.dto.ProductIdDTO;
import com.simlect.api.dto.ProductIdListDTO;
import com.simlect.api.dto.ProductSalesIncreaseDTO;
import com.simlect.api.dto.ProductSnapshotBatchVO;
import com.simlect.api.vo.ProductRagIndexVO;
import com.simlect.api.vo.ProductSearchIndexVO;
import com.simlect.api.vo.ProductSkuSnapshotVO;
import com.simlect.api.fallback.ProductFeignFallbackFactory;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.api.vo.ProductSkuListVO;
import com.simlect.entity.vo.ResponseVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 这个接口干什么？
 * 声明「如何用 HTTP 调用 simlect-product（商品） 的内部 API」。本身没有实现代码；
 * 运行时由 OpenFeign 生成代理，按方法上的 @PostMapping 发请求。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单取商品/SKU 价格快照、详情查询——商品在 product 库
 * - 角色：跨服务契约（API 模块）。一域一库后禁止跨库 Mapper，同步读写别的域必须走 Feign。
 *   内部鉴权：请求头 X-Internal-Token 由 FeignInternalAuthInterceptor 自动加上。
 * <p>
 * 调用链：
 * <pre>
 * 1. order / Agent 等 → ProductFeignSupport
 * 2. ProductFeignSupport 调用本接口方法
 * 3. Feign 代理 → Gateway → 目标服务 /internal/product/* → Controller → Service
 *    （失败时走 ProductFeignFallbackFactory，见该类注释）
 * </pre>
 * 和 MQ/Outbox 的区别：本接口是同步 RPC（当下就要结果）；关单通知等异步最终一致走消息队列。
 */
@FeignClient(name = "simlect-product", contextId = "productFeignClient", path = "/internal/product",
        fallbackFactory = ProductFeignFallbackFactory.class)
public interface ProductFeignClient {

    @PostMapping("/snapshotBatch")
    ResponseVO<ProductSnapshotBatchVO> snapshotBatch(@RequestBody ProductIdListDTO dto);

    @PostMapping("/defaultSku")
    ResponseVO<ProductSkuSnapshotVO> defaultSku(@RequestBody ProductIdDTO dto);

    @PostMapping("/increaseSales")
    ResponseVO<Void> increaseSales(@RequestBody ProductSalesIncreaseDTO dto);

    @PostMapping("/searchIndex")
    ResponseVO<ProductSearchIndexVO> getSearchIndex(@RequestBody ProductIdDTO dto);

    @PostMapping("/ragIndex")
    ResponseVO<ProductRagIndexVO> getRagIndex(@RequestBody ProductIdDTO dto);

    @PostMapping("/lessStockSkuPage")
    ResponseVO<PaginationResultVO<ProductSkuListVO>> lessStockSkuPage(@RequestBody LessStockPageDTO dto);

    @PostMapping("/listOnSaleProductIds")
    ResponseVO<List<String>> listOnSaleProductIds();
}
