package com.simlect.compensation;

import com.simlect.entity.po.ProductItem;

import java.util.List;

public interface StockBatchCompensatePort {

    int changeStockBatch(List<ProductItem> items);

    int changeStockBatchIdempotent(List<ProductItem> items, String operationId);

    int changeStockBatchIdempotent(List<ProductItem> items, String operationId, String requiredOperationId);
}
