package com.simlect.api.support;

import com.simlect.api.StockFeignClient;
import com.simlect.api.dto.LessStockPageDTO;
import com.simlect.api.dto.ProductIdDTO;
import com.simlect.api.dto.SkuStockBatchChangeDTO;
import com.simlect.api.dto.SkuStockChangeDTO;
import com.simlect.api.dto.SkuStockDTO;
import com.simlect.api.dto.SkuStockQueryDTO;
import com.simlect.api.dto.SkuStockSetDTO;
import com.simlect.api.vo.ProductTotalStockVO;
import com.simlect.api.vo.StockChangeResultVO;
import com.simlect.compensation.StockBatchCompensatePort;
import com.simlect.entity.po.ProductItem;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
/**
 * 这个类干什么？
 * 业务 Service 调用 simlect-stock（库存） 时的门面：内部转调 StockFeignClient，
 * 把 ResponseVO 拆成领域数据；失败（含降级「不可用」）统一转成 BusinessException。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单锁库存/扣减、关单回补、查 SKU 库存——库存数据只在 stock 库，order 必须远程调
 * - 角色：防腐层。OrderInfoServiceImpl 等应注入本类，而不是自己处理 Feign 的 ResponseVO/异常细节。
 * <p>
 * 调用链：
 * <pre>
 * 1. 领域 Service（如下单）→ 本类方法（getAvailable / changeStockBatch / …）
 * 2. 本类 → StockFeignClient（Feign；失败可能进 StockFeignFallbackFactory）
 * 3. 成功：取出 data 返回给 Service
 * 4. 失败：抛业务异常 → Support 抛业务异常 → 下单全局事务回滚，绝不能假装扣库存成功
 * </pre>
 * 为什么多这一层？避免每个 Service 重复「拆包 + 判 status + 转异常」，并固定失败语义。
 */

@Component
public class StockFeignSupport implements StockBatchCompensatePort {

    @Resource
    private StockFeignClient stockFeignClient;
    @Resource
    private FeignResponseSupport feignResponseSupport;

    public int getAvailable(String productId, String propertyValueIdHash) {
        SkuStockDTO data = feignResponseSupport.call(
                () -> stockFeignClient.getStock(new SkuStockQueryDTO(productId, propertyValueIdHash)),
                "查询库存失败");
        return data == null || data.getStock() == null ? 0 : data.getStock();
    }

    public void lockAndVerify(List<ProductItem> items) {
        feignResponseSupport.run(() -> stockFeignClient.lockAndVerify(toBatch(items, true)), "库存校验失败");
    }

    @Override
    public int changeStockBatch(List<ProductItem> items) {
        StockChangeResultVO result = feignResponseSupport.call(
                () -> stockFeignClient.changeStockBatch(toBatch(items, false)), "库存变更失败");
        return result == null || result.getAffectedRows() == null ? 0 : result.getAffectedRows();
    }

    @Override
    public int changeStockBatchIdempotent(List<ProductItem> items, String operationId) {
        return changeStockBatchIdempotent(items, operationId, null);
    }

    @Override
    public int changeStockBatchIdempotent(List<ProductItem> items, String operationId,
                                           String requiredOperationId) {
        SkuStockBatchChangeDTO batch = toBatch(items, false);
        batch.setOperationId(operationId);
        batch.setRequiredOperationId(requiredOperationId);
        StockChangeResultVO result = feignResponseSupport.call(
                () -> stockFeignClient.changeStockBatch(batch), "库存变更失败");
        return result == null || result.getAffectedRows() == null ? 0 : result.getAffectedRows();
    }

    public void changeStock(String productId, String propertyValueIdHash, int changeAmount) {
        SkuStockChangeDTO dto = new SkuStockChangeDTO();
        dto.setProductId(productId);
        dto.setPropertyValueIdHash(propertyValueIdHash);
        dto.setChangeAmount(changeAmount);
        feignResponseSupport.run(() -> stockFeignClient.changeStock(dto), "库存变更失败");
    }

    public void setStock(String productId, String propertyValueIdHash, int stock) {
        feignResponseSupport.run(
                () -> stockFeignClient.setStock(new SkuStockSetDTO(productId, propertyValueIdHash, stock)),
                "设置库存失败");
    }

    public int totalByProduct(String productId) {
        ProductTotalStockVO vo = feignResponseSupport.call(
                () -> stockFeignClient.totalByProduct(new ProductIdDTO(productId)), "查询商品总库存失败");
        return vo == null || vo.getTotalStock() == null ? 0 : vo.getTotalStock();
    }

    public PaginationResultVO<SkuStockDTO> listLessThan(Integer pageNo, Integer pageSize, Integer threshold) {
        PaginationResultVO<SkuStockDTO> page = feignResponseSupport.call(
                () -> stockFeignClient.listLessThan(new LessStockPageDTO(pageNo, pageSize, threshold)),
                "查询低库存SKU失败");
        if (page == null) {
            return new PaginationResultVO<>(0, pageSize == null ? 15 : pageSize,
                    pageNo == null ? 1 : pageNo, 0, List.of());
        }
        return page;
    }

    private SkuStockBatchChangeDTO toBatch(List<ProductItem> items, boolean absForVerify) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("商品列表为空");
        }
        List<SkuStockChangeDTO> list = new ArrayList<>(items.size());
        for (ProductItem item : items) {
            if (StringTools.isEmpty(item.getProductId()) || StringTools.isEmpty(item.getPropertyValueIdHash())) {
                throw new BusinessException("商品sku不存在");
            }
            SkuStockChangeDTO dto = new SkuStockChangeDTO();
            dto.setProductId(item.getProductId());
            dto.setPropertyValueIdHash(item.getPropertyValueIdHash());
            int amount = item.getBuyCount() == null ? 0 : item.getBuyCount();
            dto.setChangeAmount(absForVerify ? Math.abs(amount) : amount);
            list.add(dto);
        }
        SkuStockBatchChangeDTO batch = new SkuStockBatchChangeDTO();
        batch.setItems(list);
        return batch;
    }
}
