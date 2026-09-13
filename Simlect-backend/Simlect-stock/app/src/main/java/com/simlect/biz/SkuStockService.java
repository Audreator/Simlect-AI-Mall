package com.simlect.biz;

import com.simlect.api.dto.SkuStockBatchChangeDTO;
import com.simlect.api.dto.SkuStockChangeDTO;
import com.simlect.api.dto.SkuStockDTO;
import com.simlect.domain.SkuStock;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.SkuStockMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class SkuStockService {

    @Resource
    private SkuStockMapper skuStockMapper;

    public SkuStockDTO getStock(String productId, String propertyValueIdHash) {
        SkuStock row = skuStockMapper.selectByKey(productId, propertyValueIdHash);
        if (row == null) {
            return new SkuStockDTO(productId, propertyValueIdHash, 0);
        }
        return new SkuStockDTO(row.getProductId(), row.getPropertyValueIdHash(), row.getStock());
    }

    @Transactional(rollbackFor = Exception.class)
    public int changeStock(SkuStockChangeDTO dto) {
        int affected = skuStockMapper.changeStock(dto.getProductId(), dto.getPropertyValueIdHash(), dto.getChangeAmount());
        if (affected <= 0) {
            throw new BusinessException("库存不足");
        }
        return affected;
    }

    @Transactional(rollbackFor = Exception.class)
    public void setStock(String productId, String propertyValueIdHash, Integer stock) {
        if (stock == null || stock < 0) {
            throw new BusinessException("库存最低为0");
        }
        skuStockMapper.upsert(productId, propertyValueIdHash, stock);
    }

    public int totalByProductId(String productId) {
        Integer total = skuStockMapper.selectTotalStockByProductId(productId);
        return total == null ? 0 : total;
    }

    public com.simlect.entity.vo.PaginationResultVO<SkuStockDTO> listLessThan(Integer pageNo, Integer pageSize, Integer threshold) {
        int th = threshold == null ? 10 : threshold;
        int size = pageSize == null ? 15 : pageSize;
        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        Integer countObj = skuStockMapper.countLessThan(th);
        int count = countObj == null ? 0 : countObj;
        com.simlect.entity.query.SimplePage page = new com.simlect.entity.query.SimplePage(no, count, size);
        List<SkuStock> rows = count == 0
                ? java.util.Collections.emptyList()
                : skuStockMapper.selectLessThan(th, page.getStart(), page.getEnd());
        List<SkuStockDTO> list = new java.util.ArrayList<>();
        if (rows != null) {
            for (SkuStock row : rows) {
                list.add(new SkuStockDTO(row.getProductId(), row.getPropertyValueIdHash(), row.getStock()));
            }
        }
        return new com.simlect.entity.vo.PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    @Transactional(rollbackFor = Exception.class)
    public int changeStockBatch(SkuStockBatchChangeDTO batch) {
        if (batch == null || CollectionUtils.isEmpty(batch.getItems())) {
            throw new BusinessException("库存变更列表为空");
        }
        String operationId = batch.getOperationId();
        String requiredOperationId = batch.getRequiredOperationId();
        if (StringUtils.hasText(operationId)) {
            if (operationId.length() > 128) {
                throw new BusinessException("库存变更幂等键过长");
            }
            if (StringUtils.hasText(requiredOperationId)) {
                if (requiredOperationId.length() > 128) {
                    throw new BusinessException("库存前置操作键过长");
                }
                String committedOperation = skuStockMapper.selectChangeOperationIdForUpdate(requiredOperationId);
                if (committedOperation == null) {
                    // The compensation may beat a timed-out deduction to this service. Persist a
                    // tombstone for the prerequisite so that the delayed deduction cannot run later.
                    if (skuStockMapper.insertCancelledChangeOperation(requiredOperationId) == 1) {
                        return 0;
                    }
                    // A concurrent prerequisite won the unique-key race. Lock and inspect it before
                    // deciding whether the inverse operation is allowed to proceed.
                    committedOperation = skuStockMapper.selectChangeOperationIdForUpdate(requiredOperationId);
                    if (committedOperation == null) {
                        throw new BusinessException("库存前置操作状态异常，请稍后重试");
                    }
                }
                Integer requiredResult = skuStockMapper.selectChangeOperationResult(requiredOperationId);
                if (requiredResult == null) {
                    throw new BusinessException("库存前置操作正在处理中，请稍后重试");
                }
                if (requiredResult < 0) {
                    return 0;
                }
            }
            if (skuStockMapper.insertChangeOperation(operationId) == 0) {
                Integer completed = skuStockMapper.selectChangeOperationResult(operationId);
                if (completed == null) {
                    throw new BusinessException("库存变更正在处理中，请稍后重试");
                }
                return Math.max(completed, 0);
            }
        }
        Map<String, Integer> merged = mergeBySku(batch.getItems());
        int total = 0;
        for (Map.Entry<String, Integer> entry : new TreeMap<>(merged).entrySet()) {
            String[] parts = entry.getKey().split("\0", 2);
            int affected = skuStockMapper.changeStock(parts[0], parts[1], entry.getValue());
            if (affected <= 0) {
                throw new BusinessException("库存不足");
            }
            total += affected;
        }
        if (StringUtils.hasText(operationId)
                && skuStockMapper.completeChangeOperation(operationId, total) != 1) {
            throw new BusinessException("库存变更幂等记录更新失败");
        }
        return total;
    }

    @Transactional(rollbackFor = Exception.class)
    public void lockAndVerify(SkuStockBatchChangeDTO batch) {
        if (batch == null || CollectionUtils.isEmpty(batch.getItems())) {
            throw new BusinessException("库存校验列表为空");
        }
        Map<String, Integer> needBySku = new TreeMap<>();
        Map<String, SkuStockChangeDTO> sample = new HashMap<>();
        for (SkuStockChangeDTO item : batch.getItems()) {
            int need = item.getChangeAmount() == null ? 0 : Math.abs(item.getChangeAmount());
            String key = item.getProductId() + "\0" + item.getPropertyValueIdHash();
            needBySku.merge(key, need, Integer::sum);
            sample.putIfAbsent(key, item);
        }
        for (Map.Entry<String, Integer> entry : needBySku.entrySet()) {
            SkuStockChangeDTO s = sample.get(entry.getKey());
            SkuStock locked = skuStockMapper.selectByKeyForUpdate(s.getProductId(), s.getPropertyValueIdHash());
            if (locked == null) {
                throw new BusinessException("商品sku不存在");
            }
            if (locked.getStock() < entry.getValue()) {
                throw new BusinessException("库存不足");
            }
        }
    }

    private Map<String, Integer> mergeBySku(List<SkuStockChangeDTO> items) {
        Map<String, Integer> merged = new HashMap<>();
        items.stream()
                .sorted(Comparator.comparing(SkuStockChangeDTO::getProductId)
                        .thenComparing(SkuStockChangeDTO::getPropertyValueIdHash))
                .forEach(item -> {
                    String key = item.getProductId() + "\0" + item.getPropertyValueIdHash();
                    merged.merge(key, item.getChangeAmount(), Integer::sum);
                });
        return merged;
    }
}
