package com.simlect.component;

import com.simlect.constants.InternalApiHeaders;
import com.simlect.entity.dto.MqCompensationRecord;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.po.ProductItem;
import com.simlect.service.MqCompensationLogService;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RemoteCompensateRecorder {

    @Resource
    private MqCompensationLogService mqCompensationLogService;

    public void recordStockChangeBatch(String bizKey, String operationId,
                                       List<ProductItem> items, Throwable error) {
        recordStockChangeBatch(bizKey, operationId, null, items, error);
    }

    public void recordStockChangeBatch(String bizKey, String operationId, String requiredOperationId,
                                       List<ProductItem> items, Throwable error) {
        if (StringTools.isEmpty(bizKey) || StringTools.isEmpty(operationId)
                || items == null || items.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("operationId", operationId);
        if (!StringTools.isEmpty(requiredOperationId)) {
            payload.put("requiredOperationId", requiredOperationId);
        }
        payload.put("items", items);
        MqCompensationRecord record = baseRecord(
                "remote:stock:" + sha256(operationId),
                InternalApiHeaders.REMOTE_STOCK_CHANGE_BATCH,
                payload,
                error);
        persist(record);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public void recordCouponUnlock(String bizKey, String userCouponId, String userId,
                                   Integer fromStatus, Integer toStatus, Throwable error) {
        if (StringTools.isEmpty(bizKey) || StringTools.isEmpty(userCouponId)) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("userCouponId", userCouponId);
        payload.put("userId", userId);
        payload.put("fromStatus", fromStatus);
        payload.put("toStatus", toStatus);
        MqCompensationRecord record = baseRecord(
                "remote:coupon:unlock:" + bizKey,
                InternalApiHeaders.REMOTE_COUPON_UNLOCK,
                payload,
                error);
        persist(record);
    }

    private MqCompensationRecord baseRecord(String idempotencyKey, String routingKey, Object payload, Throwable error) {
        MqCompensationRecord record = new MqCompensationRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setExchange(InternalApiHeaders.REMOTE_COMPENSATE_EXCHANGE);
        record.setRoutingKey(routingKey);
        record.setPayload(payload);
        record.setReliabilityLevel(MessageReliabilityLevelEnum.HIGH);
        record.setFailedAt(System.currentTimeMillis());
        record.setRetryCount(0);
        record.setErrorMessage(error == null ? "remote compensate failed" : error.getMessage());
        return record;
    }

    private void persist(MqCompensationRecord record) {
        try {
            mqCompensationLogService.saveFromFailure(record);
            log.warn("已登记远程补偿任务 key={} routingKey={} err={}",
                    record.getIdempotencyKey(), record.getRoutingKey(), record.getErrorMessage());
        } catch (Exception e) {
            log.error("远程补偿落库失败 key={}", record.getIdempotencyKey(), e);
        }
    }
}
