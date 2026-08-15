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
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RemoteCompensateRecorder {

    @Resource
    private MqCompensationLogService mqCompensationLogService;

    public void recordStockChangeBatch(String bizKey, List<ProductItem> items, Throwable error) {
        if (StringTools.isEmpty(bizKey) || items == null || items.isEmpty()) {
            return;
        }
        // 按 SKU 拆分为独立补偿记录：key 带 productId+hash，
        // 防同 payOrderId 多商品回补失败互相覆盖导致第二条永久不补
        for (ProductItem item : items) {
            if (item == null || StringTools.isEmpty(item.getProductId())) {
                continue;
            }
            String skuKey = "remote:stock:changeBatch:" + bizKey + ":" + item.getProductId()
                    + ":" + (item.getPropertyValueIdHash() == null ? "" : item.getPropertyValueIdHash());
            MqCompensationRecord record = baseRecord(
                    skuKey,
                    InternalApiHeaders.REMOTE_STOCK_CHANGE_BATCH,
                    List.of(item),
                    error);
            persist(record);
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
