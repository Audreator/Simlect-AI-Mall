package com.simlect.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.simlect.compensation.StockBatchCompensatePort;
import com.simlect.utils.JsonUtils;
import com.simlect.compensation.UserCouponStatusCompensatePort;
import com.simlect.component.MqCompensationStore;
import com.simlect.component.MqIdempotencyGuard;
import com.simlect.constants.InternalApiHeaders;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.dto.MqCompensationRecord;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.enums.MqCompensationLogStatusEnum;
import com.simlect.entity.enums.PageSize;
import com.simlect.entity.po.MqCompensationLog;
import com.simlect.entity.po.ProductItem;
import com.simlect.entity.query.MqCompensationLogQuery;
import com.simlect.entity.query.SimplePage;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.MqCompensationLogMapper;
import com.simlect.service.MqCompensationLogService;
import com.simlect.support.MqConsumeReplayRouter;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service("mqCompensationLogService")
@Slf4j
public class MqCompensationLogServiceImpl implements MqCompensationLogService {

    /** 手动重放的默认最大重试次数（达到后置 REPLAY_FAILED 终态） */
    private static final int DEFAULT_MANUAL_MAX_RETRY = 5;

    @Resource
    private MqCompensationLogMapper<MqCompensationLog, MqCompensationLogQuery> mqCompensationLogMapper;
    @Resource
    private MqIdempotencyGuard mqIdempotencyGuard;
    @Resource
    private MqCompensationStore mqCompensationStore;
    @Lazy
    @Resource
    private ReliableMessageSender reliableMessageSender;
    @Lazy
    @Resource
    private ObjectProvider<StockBatchCompensatePort> stockBatchCompensatePort;
    @Lazy
    @Resource
    private ObjectProvider<UserCouponStatusCompensatePort> userCouponStatusCompensatePort;

    @Override
    public PaginationResultVO<MqCompensationLog> findListByPage(MqCompensationLogQuery param) {
        int count = mqCompensationLogMapper.selectCount(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();
        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<MqCompensationLog> list = mqCompensationLogMapper.selectList(param);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    @Override
    public MqCompensationLog getByLogId(Integer logId) {
        return mqCompensationLogMapper.selectByLogId(logId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveFromFailure(MqCompensationRecord record) {
        if (record == null || StringTools.isEmpty(record.getIdempotencyKey())) {
            return;
        }
        Date now = new Date();
        MqCompensationLog existing = mqCompensationLogMapper.selectByIdempotencyKey(record.getIdempotencyKey());
        if (existing != null) {
            // 终态（REPLAYED/IGNORED/REPLAY_FAILED）不复活：避免「复活后立即再终态化、不执行任何重试」的空转
            if (MqCompensationLogStatusEnum.REPLAYED.getStatus().equals(existing.getStatus())
                    || MqCompensationLogStatusEnum.IGNORED.getStatus().equals(existing.getStatus())
                    || MqCompensationLogStatusEnum.REPLAY_FAILED.getStatus().equals(existing.getStatus())) {
                return;
            }
            MqCompensationLog patch = new MqCompensationLog();
            patch.setErrorMessage(record.getErrorMessage());
            patch.setRetryCount((existing.getRetryCount() == null ? 0 : existing.getRetryCount()));
            patch.setStatus(MqCompensationLogStatusEnum.PENDING.getStatus());
            patch.setUpdateTime(now);
            mqCompensationLogMapper.updateByLogId(patch, existing.getLogId());
            return;
        }
        MqCompensationLog logRow = new MqCompensationLog();
        logRow.setIdempotencyKey(record.getIdempotencyKey());
        logRow.setExchange(record.getExchange());
        logRow.setRoutingKey(record.getRoutingKey());
        logRow.setBizScene(resolveBizScene(record));
        logRow.setPayloadJson(JsonUtils.toJson(record.getPayload()));
        logRow.setReliabilityLevel(record.getReliabilityLevel() == null
                ? MessageReliabilityLevelEnum.HIGH.getCode()
                : record.getReliabilityLevel().getCode());
        logRow.setErrorMessage(record.getErrorMessage());
        logRow.setRetryCount(record.getRetryCount());
        logRow.setStatus(MqCompensationLogStatusEnum.PENDING.getStatus());
        logRow.setCreateTime(now);
        logRow.setUpdateTime(now);
        mqCompensationLogMapper.insert(logRow);
    }

    @Override
    public void updateHandleStatus(Integer logId, Integer status, String handleRemark) {
        MqCompensationLog existing = mqCompensationLogMapper.selectByLogId(logId);
        if (existing == null) {
            throw new BusinessException("补偿日志不存在");
        }
        MqCompensationLogStatusEnum statusEnum = MqCompensationLogStatusEnum.getByStatus(status);
        if (statusEnum == null) {
            throw new BusinessException("无效的处理状态");
        }
        Date now = new Date();
        MqCompensationLog patch = new MqCompensationLog();
        patch.setStatus(status);
        patch.setHandleRemark(handleRemark);
        patch.setHandleTime(now);
        patch.setUpdateTime(now);
        mqCompensationLogMapper.updateByLogId(patch, logId);
    }

    @Override
    public void replay(Integer logId) {
        MqCompensationLog existing = mqCompensationLogMapper.selectByLogId(logId);
        if (existing == null) {
            throw new BusinessException("补偿日志不存在");
        }
        if (MqCompensationLogStatusEnum.REPLAYED.getStatus().equals(existing.getStatus())
                || MqCompensationLogStatusEnum.IGNORED.getStatus().equals(existing.getStatus())) {
            throw new BusinessException("当前状态不可重放");
        }
        if (InternalApiHeaders.REMOTE_COMPENSATE_EXCHANGE.equals(existing.getExchange())) {
            replayRemoteCompensate(existing, DEFAULT_MANUAL_MAX_RETRY);
            return;
        }
        if (MqConsumeReplayRouter.isConsumeFailure(existing.getExchange())) {
            replayConsumeFailure(existing, DEFAULT_MANUAL_MAX_RETRY);
            return;
        }
        replaySendFailure(existing, DEFAULT_MANUAL_MAX_RETRY);
    }

    @Override
    public int autoReplayPendingSendFailures(int batchSize, int maxRetryCount) {
        if (batchSize <= 0) {
            batchSize = 10;
        }
        MqCompensationLogQuery query = new MqCompensationLogQuery();
        query.setStatus(MqCompensationLogStatusEnum.PENDING.getStatus());
        query.setOrderBy("log_id asc");
        query.setPageNo(1);
        query.setPageSize(batchSize * 2);
        SimplePage page = new SimplePage(1, batchSize * 2, batchSize * 2);
        query.setSimplePage(page);
        List<MqCompensationLog> list = mqCompensationLogMapper.selectList(query);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int replayed = 0;
        for (MqCompensationLog row : list) {
            if (replayed >= batchSize) {
                break;
            }
            if (MqConsumeReplayRouter.isConsumeFailure(row.getExchange())) {
                continue;
            }
            int retry = row.getRetryCount() == null ? 0 : row.getRetryCount();
            if (retry >= maxRetryCount) {
                // 已达重试上限仍未成功：置为终态，避免每轮空转扫描同一批超限行
                MqCompensationLog terminal = new MqCompensationLog();
                terminal.setStatus(MqCompensationLogStatusEnum.REPLAY_FAILED.getStatus());
                terminal.setErrorMessage("超过最大重试次数(" + maxRetryCount + ")");
                terminal.setUpdateTime(new Date());
                mqCompensationLogMapper.updateByLogId(terminal, row.getLogId());
                continue;
            }
            try {
                if (InternalApiHeaders.REMOTE_COMPENSATE_EXCHANGE.equals(row.getExchange())) {
                    replayRemoteCompensate(row, maxRetryCount);
                } else {
                    replaySendFailure(row, maxRetryCount);
                }
                replayed++;
            } catch (Exception e) {
                log.warn("MQ 自动补偿重放失败 logId={}", row.getLogId(), e);
            }
        }
        return replayed;
    }

    private void replayRemoteCompensate(MqCompensationLog existing, int maxRetryCount) {
        Date now = new Date();
        markProcessing(existing.getLogId(), now);
        try {
            String routingKey = existing.getRoutingKey();
            if (InternalApiHeaders.REMOTE_STOCK_CHANGE_BATCH.equals(routingKey)) {
                JsonNode payload = JsonUtils.parseTree(existing.getPayloadJson());
                String operationId;
                String requiredOperationId = null;
                List<ProductItem> items;
                if (payload.isArray()) {
                    operationId = "legacy-compensate:" + sha256(existing.getIdempotencyKey());
                    items = JsonUtils.parseArray(existing.getPayloadJson(), ProductItem.class);
                } else {
                    operationId = textOrNull(payload, "operationId");
                    requiredOperationId = textOrNull(payload, "requiredOperationId");
                    JsonNode itemNode = payload.get("items");
                    if (StringTools.isEmpty(operationId) || itemNode == null || !itemNode.isArray()) {
                        throw new BusinessException("库存补偿参数不完整");
                    }
                    items = JsonUtils.parseArray(itemNode.toString(), ProductItem.class);
                }
                StockBatchCompensatePort stockPort = stockBatchCompensatePort.getIfAvailable();
                if (stockPort == null) {
                    throw new BusinessException("库存补偿能力不可用（缺少 stock-api）");
                }
                stockPort.changeStockBatchIdempotent(items, operationId, requiredOperationId);
            } else if (InternalApiHeaders.REMOTE_COUPON_UNLOCK.equals(routingKey)) {
                JsonNode payload = JsonUtils.parseTree(existing.getPayloadJson());
                UserCouponStatusCompensatePort couponPort = userCouponStatusCompensatePort.getIfAvailable();
                if (couponPort == null) {
                    throw new BusinessException("优惠券补偿能力不可用（缺少 coupon-api）");
                }
                couponPort.changeUserCouponStatus(
                        textOrNull(payload, "userCouponId"),
                        textOrNull(payload, "userId"),
                        intOrNull(payload, "fromStatus"),
                        intOrNull(payload, "toStatus"),
                        null);
            } else {
                throw new BusinessException("未知远程补偿类型：" + routingKey);
            }
            markReplayed(existing, now);
            mqCompensationStore.remove(existing.getIdempotencyKey());
        } catch (Exception e) {
            log.error("远程补偿重放失败 logId={}", existing.getLogId(), e);
            markReplayFailedOrPending(existing, e.getMessage(), maxRetryCount);
            throw new BusinessException("远程补偿重放失败：" + e.getMessage());
        }
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private void replaySendFailure(MqCompensationLog existing, int maxRetryCount) {
        Date now = new Date();
        markProcessing(existing.getLogId(), now);
        mqIdempotencyGuard.releaseSend(existing.getIdempotencyKey());
        try {
            Object payload = JsonUtils.parse(existing.getPayloadJson());
            reliableMessageSender.replaySend(
                    existing.getExchange(),
                    existing.getRoutingKey(),
                    payload,
                    existing.getIdempotencyKey());
            markReplayed(existing, now);
            mqCompensationStore.remove(existing.getIdempotencyKey());
        } catch (Exception e) {
            log.error("MQ 补偿重放失败 logId={}", existing.getLogId(), e);
            markReplayFailedOrPending(existing, e.getMessage(), maxRetryCount);
            throw new BusinessException("重放失败：" + e.getMessage());
        }
    }

    private void replayConsumeFailure(MqCompensationLog existing, int maxRetryCount) {
        MqConsumeReplayRouter.Target target = MqConsumeReplayRouter.resolve(existing.getRoutingKey());
        if (target == null) {
            throw new BusinessException("未知消费队列，无法重放：" + existing.getRoutingKey());
        }
        Date now = new Date();
        markProcessing(existing.getLogId(), now);
        mqIdempotencyGuard.releaseSend(existing.getIdempotencyKey());
        try {
            Object payload = JsonUtils.parse(existing.getPayloadJson());
            reliableMessageSender.replaySend(
                    target.exchange(),
                    target.routingKey(),
                    payload,
                    existing.getIdempotencyKey());
            markReplayed(existing, now);
            mqCompensationStore.remove(existing.getIdempotencyKey());
        } catch (Exception e) {
            log.error("MQ 消费补偿重放失败 logId={}", existing.getLogId(), e);
            markReplayFailedOrPending(existing, e.getMessage(), maxRetryCount);
            throw new BusinessException("重放失败：" + e.getMessage());
        }
    }

    private void markProcessing(Integer logId, Date now) {
        MqCompensationLog processing = new MqCompensationLog();
        processing.setStatus(MqCompensationLogStatusEnum.PROCESSING.getStatus());
        processing.setUpdateTime(now);
        mqCompensationLogMapper.updateByLogId(processing, logId);
    }

    private void markReplayed(MqCompensationLog existing, Date now) {
        MqCompensationLog success = new MqCompensationLog();
        success.setStatus(MqCompensationLogStatusEnum.REPLAYED.getStatus());
        success.setRetryCount((existing.getRetryCount() == null ? 0 : existing.getRetryCount()) + 1);
        success.setHandleTime(now);
        success.setUpdateTime(now);
        mqCompensationLogMapper.updateByLogId(success, existing.getLogId());
    }

    /**
     * 重放失败处理：未达最大重试次数时置回 PENDING（由自动补偿调度继续重试），
     * 达到上限才置为 REPLAY_FAILED 终态——避免 retryCount < max 时仍永久停留在终态。
     */
    private void markReplayFailedOrPending(MqCompensationLog existing, String errorMessage, int maxRetryCount) {
        int nextRetry = (existing.getRetryCount() == null ? 0 : existing.getRetryCount()) + 1;
        MqCompensationLog patch = new MqCompensationLog();
        patch.setRetryCount(nextRetry);
        patch.setErrorMessage(errorMessage);
        patch.setUpdateTime(new Date());
        if (nextRetry >= maxRetryCount) {
            patch.setStatus(MqCompensationLogStatusEnum.REPLAY_FAILED.getStatus());
        } else {
            patch.setStatus(MqCompensationLogStatusEnum.PENDING.getStatus());
        }
        mqCompensationLogMapper.updateByLogId(patch, existing.getLogId());
    }

    static String resolveBizScene(MqCompensationRecord record) {
        if (InternalApiHeaders.REMOTE_COMPENSATE_EXCHANGE.equals(record.getExchange())) {
            return "REMOTE_" + resolveBizScene(record.getRoutingKey());
        }
        if (MqConsumeReplayRouter.isConsumeFailure(record.getExchange())) {
            return "CONSUME_" + resolveBizScene(record.getRoutingKey());
        }
        return resolveBizScene(record.getRoutingKey());
    }

    static String resolveBizScene(String routingKey) {
        if (StringTools.isEmpty(routingKey)) {
            return "OTHER";
        }
        String key = routingKey.toLowerCase();
        if (key.contains("stock")) {
            return "STOCK";
        }
        if (key.contains("coupon")) {
            return "COUPON";
        }
        if (key.contains("rag")) {
            return "RAG";
        }
        if (key.contains("notify")) {
            return "NOTIFY";
        }
        if (key.contains("browse")) {
            return "BROWSE";
        }
        if (key.contains("sign")) {
            return "SIGN";
        }
        if (key.contains("pay") || key.contains("timeout") || key.contains("logistics") || key.contains("confirm")) {
            return "PAY";
        }
        if (key.contains("ban")) {
            return "BAN";
        }
        return "OTHER";
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asInt();
    }
}
