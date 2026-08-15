package com.simlect.api;

import com.simlect.api.dto.LessStockPageDTO;
import com.simlect.api.dto.SkuStockBatchChangeDTO;
import com.simlect.api.dto.SkuStockChangeDTO;
import com.simlect.api.dto.SkuStockDTO;
import com.simlect.api.dto.SkuStockQueryDTO;
import com.simlect.api.dto.SkuStockSetDTO;
import com.simlect.api.dto.ProductIdDTO;
import com.simlect.api.vo.ProductTotalStockVO;
import com.simlect.api.vo.StockChangeResultVO;
import com.simlect.api.fallback.StockFeignFallbackFactory;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.ResponseVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 这个接口干什么？
 * 声明「如何用 HTTP 调用 simlect-stock（库存） 的内部 API」。本身没有实现代码；
 * 运行时由 OpenFeign 生成代理，按方法上的 @PostMapping 发请求。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单锁库存/扣减、关单回补、查 SKU 库存——库存数据只在 stock 库，order 必须远程调
 * - 角色：跨服务契约（API 模块）。一域一库后禁止跨库 Mapper，同步读写别的域必须走 Feign。
 *   内部鉴权：请求头 X-Internal-Token 由 FeignInternalAuthInterceptor 自动加上。
 * <p>
 * 调用链：
 * <pre>
 * 1. order 服务 OrderInfoServiceImpl → StockFeignSupport（普通下单/关单）
 * 2. StockFeignSupport 调用本接口方法
 * 3. Feign 代理 → Gateway → 目标服务 /internal/stock/* → Controller → Service
 *    （失败时走 StockFeignFallbackFactory，见该类注释）
 * </pre>
 * 和 MQ/Outbox 的区别：本接口是同步 RPC（当下就要结果）；关单通知等异步最终一致走消息队列。
 */
@FeignClient(name = "simlect-stock", contextId = "stockFeignClient", path = "/internal/stock",
        fallbackFactory = StockFeignFallbackFactory.class)
public interface StockFeignClient {

    @PostMapping("/get")
    ResponseVO<SkuStockDTO> getStock(@RequestBody SkuStockQueryDTO dto);

    @PostMapping("/change")
    ResponseVO<StockChangeResultVO> changeStock(@RequestBody SkuStockChangeDTO dto);

    @PostMapping("/changeBatch")
    ResponseVO<StockChangeResultVO> changeStockBatch(@RequestBody SkuStockBatchChangeDTO dto);

    @PostMapping("/lockAndVerify")
    ResponseVO<Void> lockAndVerify(@RequestBody SkuStockBatchChangeDTO dto);

    @PostMapping("/set")
    ResponseVO<Void> setStock(@RequestBody SkuStockSetDTO dto);

    @PostMapping("/totalByProduct")
    ResponseVO<ProductTotalStockVO> totalByProduct(@RequestBody ProductIdDTO dto);

    @PostMapping("/listLessThan")
    ResponseVO<PaginationResultVO<SkuStockDTO>> listLessThan(@RequestBody LessStockPageDTO dto);
}
