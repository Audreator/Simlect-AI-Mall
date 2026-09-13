package com.simlect.biz;

import com.simlect.api.dto.SkuStockBatchChangeDTO;
import com.simlect.api.dto.SkuStockChangeDTO;
import com.simlect.api.dto.SkuStockDTO;
import com.simlect.domain.SkuStock;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.SkuStockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SkuStockService 库存扣减/回补（行锁与条件更新）单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SkuStockServiceTest {

    @Mock
    private SkuStockMapper skuStockMapper;

    @InjectMocks
    private SkuStockService skuStockService;

    private static final String PRODUCT_ID = "P001";
    private static final String SKU_HASH = "aabbccdd";

    private SkuStockChangeDTO changeDto(int amount) {
        SkuStockChangeDTO dto = new SkuStockChangeDTO();
        dto.setProductId(PRODUCT_ID);
        dto.setPropertyValueIdHash(SKU_HASH);
        dto.setChangeAmount(amount);
        return dto;
    }

    private SkuStockBatchChangeDTO batch(SkuStockChangeDTO... items) {
        SkuStockBatchChangeDTO batch = new SkuStockBatchChangeDTO();
        batch.setItems(new ArrayList<>(Arrays.asList(items)));
        return batch;
    }

    // ==================== getStock ====================

    @Test
    void getStock_rowNotFound_returnsZeroStock() {
        when(skuStockMapper.selectByKey(PRODUCT_ID, SKU_HASH)).thenReturn(null);

        SkuStockDTO dto = skuStockService.getStock(PRODUCT_ID, SKU_HASH);

        assertEquals(0, dto.getStock());
        assertEquals(PRODUCT_ID, dto.getProductId());
    }

    @Test
    void getStock_rowExists_returnsStock() {
        SkuStock row = new SkuStock();
        row.setProductId(PRODUCT_ID);
        row.setPropertyValueIdHash(SKU_HASH);
        row.setStock(50);
        when(skuStockMapper.selectByKey(PRODUCT_ID, SKU_HASH)).thenReturn(row);

        SkuStockDTO dto = skuStockService.getStock(PRODUCT_ID, SKU_HASH);

        assertEquals(50, dto.getStock());
    }

    // ==================== changeStock（条件扣减 0/1 分支） ====================

    @Test
    void changeStock_affectedZero_throwsStockInsufficient() {
        when(skuStockMapper.changeStock(PRODUCT_ID, SKU_HASH, -3)).thenReturn(0);

        BusinessException e = assertThrows(BusinessException.class,
                () -> skuStockService.changeStock(changeDto(-3)));

        assertEquals("库存不足", e.getMessage());
    }

    @Test
    void changeStock_affectedOne_succeeds() {
        when(skuStockMapper.changeStock(PRODUCT_ID, SKU_HASH, -3)).thenReturn(1);

        int affected = skuStockService.changeStock(changeDto(-3));

        assertEquals(1, affected);
    }

    // ==================== setStock ====================

    @Test
    void setStock_nullStock_throws() {
        assertThrows(BusinessException.class, () -> skuStockService.setStock(PRODUCT_ID, SKU_HASH, null));
        verifyNoInteractions(skuStockMapper);
    }

    @Test
    void setStock_negativeStock_throws() {
        assertThrows(BusinessException.class, () -> skuStockService.setStock(PRODUCT_ID, SKU_HASH, -1));
        verifyNoInteractions(skuStockMapper);
    }

    @Test
    void setStock_valid_upserts() {
        skuStockService.setStock(PRODUCT_ID, SKU_HASH, 100);
        verify(skuStockMapper).upsert(PRODUCT_ID, SKU_HASH, 100);
    }

    // ==================== totalByProductId ====================

    @Test
    void totalByProductId_nullResult_returnsZero() {
        when(skuStockMapper.selectTotalStockByProductId(PRODUCT_ID)).thenReturn(null);
        assertEquals(0, skuStockService.totalByProductId(PRODUCT_ID));
    }

    @Test
    void totalByProductId_returnsSum() {
        when(skuStockMapper.selectTotalStockByProductId(PRODUCT_ID)).thenReturn(88);
        assertEquals(88, skuStockService.totalByProductId(PRODUCT_ID));
    }

    // ==================== listLessThan ====================

    @Test
    void listLessThan_countZero_returnsEmptyWithoutQueryingRows() {
        when(skuStockMapper.countLessThan(10)).thenReturn(0);

        PaginationResultVO<SkuStockDTO> page = skuStockService.listLessThan(null, null, null);

        assertEquals(0, page.getTotalCount());
        assertTrue(page.getList().isEmpty());
        verify(skuStockMapper, never()).selectLessThan(anyInt(), anyInt(), anyInt());
    }

    @Test
    void listLessThan_countPositive_returnsPagedRows() {
        when(skuStockMapper.countLessThan(10)).thenReturn(5);
        SkuStock row = new SkuStock();
        row.setProductId(PRODUCT_ID);
        row.setPropertyValueIdHash(SKU_HASH);
        row.setStock(3);
        when(skuStockMapper.selectLessThan(10, 0, 15)).thenReturn(List.of(row));

        PaginationResultVO<SkuStockDTO> page = skuStockService.listLessThan(1, 15, 10);

        assertEquals(5, page.getTotalCount());
        assertEquals(1, page.getList().size());
        assertEquals(3, page.getList().get(0).getStock());
    }

    @Test
    void listLessThan_customThresholdAndBadPageNo_normalizes() {
        when(skuStockMapper.countLessThan(20)).thenReturn(30);
        when(skuStockMapper.selectLessThan(20, 0, 15)).thenReturn(List.of());

        PaginationResultVO<SkuStockDTO> page = skuStockService.listLessThan(0, null, 20);

        assertEquals(1, page.getPageNo());
        assertEquals(15, page.getPageSize());
    }

    // ==================== changeStockBatch（合并 + 0/1 分支） ====================

    @Test
    void changeStockBatch_emptyItems_throws() {
        SkuStockBatchChangeDTO batch = new SkuStockBatchChangeDTO();
        assertThrows(BusinessException.class, () -> skuStockService.changeStockBatch(batch));
        assertThrows(BusinessException.class, () -> skuStockService.changeStockBatch(null));
        verifyNoInteractions(skuStockMapper);
    }

    @Test
    void changeStockBatch_mergesDuplicatedSku_thenDeducts() {
        SkuStockChangeDTO a = changeDto(-2);
        SkuStockChangeDTO b = changeDto(-3);
        when(skuStockMapper.changeStock(PRODUCT_ID, SKU_HASH, -5)).thenReturn(1);

        int total = skuStockService.changeStockBatch(batch(a, b));

        assertEquals(1, total);
        verify(skuStockMapper, times(1)).changeStock(PRODUCT_ID, SKU_HASH, -5);
    }

    @Test
    void changeStockBatch_anySkuFails_throwsInsufficient() {
        SkuStockChangeDTO a = changeDto(-2);
        SkuStockChangeDTO b = new SkuStockChangeDTO();
        b.setProductId("P002");
        b.setPropertyValueIdHash("other");
        b.setChangeAmount(-1);
        when(skuStockMapper.changeStock(PRODUCT_ID, SKU_HASH, -2)).thenReturn(1);
        when(skuStockMapper.changeStock("P002", "other", -1)).thenReturn(0);

        BusinessException e = assertThrows(BusinessException.class,
                () -> skuStockService.changeStockBatch(batch(a, b)));

        assertEquals("库存不足", e.getMessage());
    }

    @Test
    void changeStockBatch_duplicateOperation_returnsStoredResultWithoutChangingStock() {
        SkuStockBatchChangeDTO request = batch(changeDto(3));
        request.setOperationId("order-close-restock:PO1");
        when(skuStockMapper.insertChangeOperation(request.getOperationId())).thenReturn(0);
        when(skuStockMapper.selectChangeOperationResult(request.getOperationId())).thenReturn(1);

        assertEquals(1, skuStockService.changeStockBatch(request));
        verify(skuStockMapper, never()).changeStock(anyString(), anyString(), anyInt());
    }

    @Test
    void changeStockBatch_newOperation_changesStockAndCompletesRecord() {
        SkuStockBatchChangeDTO request = batch(changeDto(3));
        request.setOperationId("order-close-restock:PO1");
        when(skuStockMapper.insertChangeOperation(request.getOperationId())).thenReturn(1);
        when(skuStockMapper.changeStock(PRODUCT_ID, SKU_HASH, 3)).thenReturn(1);
        when(skuStockMapper.completeChangeOperation(request.getOperationId(), 1)).thenReturn(1);

        assertEquals(1, skuStockService.changeStockBatch(request));
        verify(skuStockMapper).completeChangeOperation(request.getOperationId(), 1);
    }

    @Test
    void changeStockBatch_missingRequiredOperation_isSafeNoOp() {
        SkuStockBatchChangeDTO request = batch(changeDto(3));
        request.setOperationId("order-create-compensate:PO1");
        request.setRequiredOperationId("order-create-deduct:PO1");
        when(skuStockMapper.selectChangeOperationIdForUpdate(request.getRequiredOperationId())).thenReturn(null);
        when(skuStockMapper.insertCancelledChangeOperation(request.getRequiredOperationId())).thenReturn(1);

        assertEquals(0, skuStockService.changeStockBatch(request));
        verify(skuStockMapper, never()).insertChangeOperation(anyString());
        verify(skuStockMapper, never()).changeStock(anyString(), anyString(), anyInt());
    }

    @Test
    void changeStockBatch_cancelledOperation_cannotRunWhenDelayed() {
        SkuStockBatchChangeDTO request = batch(changeDto(-3));
        request.setOperationId("order-create-deduct:PO1");
        when(skuStockMapper.insertChangeOperation(request.getOperationId())).thenReturn(0);
        when(skuStockMapper.selectChangeOperationResult(request.getOperationId())).thenReturn(-1);

        assertEquals(0, skuStockService.changeStockBatch(request));
        verify(skuStockMapper, never()).changeStock(anyString(), anyString(), anyInt());
    }

    @Test
    void changeStockBatch_concurrentRequiredOperationWonRace_allowsCompensation() {
        SkuStockBatchChangeDTO request = batch(changeDto(3));
        request.setOperationId("order-create-compensate:PO1");
        request.setRequiredOperationId("order-create-deduct:PO1");
        when(skuStockMapper.selectChangeOperationIdForUpdate(request.getRequiredOperationId()))
                .thenReturn(null, request.getRequiredOperationId());
        when(skuStockMapper.insertCancelledChangeOperation(request.getRequiredOperationId())).thenReturn(0);
        when(skuStockMapper.selectChangeOperationResult(request.getRequiredOperationId())).thenReturn(1);
        when(skuStockMapper.insertChangeOperation(request.getOperationId())).thenReturn(1);
        when(skuStockMapper.changeStock(PRODUCT_ID, SKU_HASH, 3)).thenReturn(1);
        when(skuStockMapper.completeChangeOperation(request.getOperationId(), 1)).thenReturn(1);

        assertEquals(1, skuStockService.changeStockBatch(request));
    }

    @Test
    void changeStockBatch_committedRequiredOperation_allowsCompensation() {
        SkuStockBatchChangeDTO request = batch(changeDto(3));
        request.setOperationId("order-create-compensate:PO1");
        request.setRequiredOperationId("order-create-deduct:PO1");
        when(skuStockMapper.selectChangeOperationIdForUpdate(request.getRequiredOperationId()))
                .thenReturn(request.getRequiredOperationId());
        when(skuStockMapper.selectChangeOperationResult(request.getRequiredOperationId())).thenReturn(1);
        when(skuStockMapper.insertChangeOperation(request.getOperationId())).thenReturn(1);
        when(skuStockMapper.changeStock(PRODUCT_ID, SKU_HASH, 3)).thenReturn(1);
        when(skuStockMapper.completeChangeOperation(request.getOperationId(), 1)).thenReturn(1);

        assertEquals(1, skuStockService.changeStockBatch(request));
    }

    // ==================== lockAndVerify（行锁校验） ====================

    @Test
    void lockAndVerify_emptyItems_throws() {
        assertThrows(BusinessException.class, () -> skuStockService.lockAndVerify(batch()));
        assertThrows(BusinessException.class, () -> skuStockService.lockAndVerify(null));
    }

    @Test
    void lockAndVerify_skuNotExists_throws() {
        when(skuStockMapper.selectByKeyForUpdate(PRODUCT_ID, SKU_HASH)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> skuStockService.lockAndVerify(batch(changeDto(-5))));

        assertEquals("商品sku不存在", e.getMessage());
    }

    @Test
    void lockAndVerify_insufficientStock_throws() {
        SkuStock row = new SkuStock();
        row.setStock(4);
        when(skuStockMapper.selectByKeyForUpdate(PRODUCT_ID, SKU_HASH)).thenReturn(row);

        BusinessException e = assertThrows(BusinessException.class,
                () -> skuStockService.lockAndVerify(batch(changeDto(-5))));

        assertEquals("库存不足", e.getMessage());
    }

    @Test
    void lockAndVerify_enoughStock_passes() {
        SkuStock row = new SkuStock();
        row.setStock(10);
        when(skuStockMapper.selectByKeyForUpdate(PRODUCT_ID, SKU_HASH)).thenReturn(row);

        skuStockService.lockAndVerify(batch(changeDto(-5)));

        verify(skuStockMapper).selectByKeyForUpdate(PRODUCT_ID, SKU_HASH);
    }

    @Test
    void lockAndVerify_mergesDuplicatedNeed() {
        SkuStock row = new SkuStock();
        row.setStock(10);
        when(skuStockMapper.selectByKeyForUpdate(PRODUCT_ID, SKU_HASH)).thenReturn(row);

        skuStockService.lockAndVerify(batch(changeDto(-3), changeDto(-4)));

        verify(skuStockMapper, times(1)).selectByKeyForUpdate(PRODUCT_ID, SKU_HASH);
    }
}
