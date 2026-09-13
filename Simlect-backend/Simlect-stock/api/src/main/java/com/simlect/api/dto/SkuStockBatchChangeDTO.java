package com.simlect.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;

public class SkuStockBatchChangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty
    @Valid
    private List<SkuStockChangeDTO> items;

    /** Optional business idempotency key for retryable stock changes. */
    private String operationId;

    /** Optional prerequisite operation that must have committed before this change is applied. */
    private String requiredOperationId;

    public List<SkuStockChangeDTO> getItems() {
        return items;
    }

    public void setItems(List<SkuStockChangeDTO> items) {
        this.items = items;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getRequiredOperationId() {
        return requiredOperationId;
    }

    public void setRequiredOperationId(String requiredOperationId) {
        this.requiredOperationId = requiredOperationId;
    }
}
