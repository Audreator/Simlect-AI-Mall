package com.simlect.biz.impl;

import com.simlect.component.RedisComponent;
import com.simlect.entity.enums.PageSize;
import com.simlect.entity.po.MqCompensationLog;
import com.simlect.entity.query.MqCompensationLogQuery;
import com.simlect.entity.query.RagSyncFailureQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.RagSyncFailureVO;
import com.simlect.mappers.MqCompensationLogMapper;
import com.simlect.service.MqCompensationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagSyncFailureServiceImplTest {

    @Mock
    private MqCompensationLogMapper<MqCompensationLog, MqCompensationLogQuery> mqCompensationLogMapper;
    @Mock
    private MqCompensationLogService mqCompensationLogService;
    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private RagSyncFailureServiceImpl ragSyncFailureService;

    private MqCompensationLog buildLog() {
        MqCompensationLog log = new MqCompensationLog();
        log.setLogId(1);
        log.setIdempotencyKey("key1");
        log.setExchange("mq.consume");
        log.setRoutingKey("rag.queue");
        log.setPayloadJson("{\"dataId\":\"P1\",\"type\":\"PRODUCT\"}");
        log.setRetryCount(2);
        log.setStatus(1);
        log.setCreateTime(new Date());
        return log;
    }

    @Test
    void loadList_fromDb_parsesPayloadAndSource() {
        RagSyncFailureQuery param = new RagSyncFailureQuery();
        param.setPageNo(1);
        when(mqCompensationLogMapper.selectCount(any())).thenReturn(1);
        when(mqCompensationLogMapper.selectList(any())).thenReturn(List.of(buildLog()));

        PaginationResultVO<RagSyncFailureVO> result = ragSyncFailureService.loadList(param);

        assertEquals(1, result.getTotalCount());
        RagSyncFailureVO vo = result.getList().get(0);
        assertEquals("P1", vo.getDataId());
        assertEquals("PRODUCT", vo.getDataType());
        assertEquals("rag.queue", vo.getQueueName());
        assertEquals(2, vo.getRetryCount());
        assertEquals("CONSUME", vo.getSource());
    }

    @Test
    void loadList_redisSource_filtersByDataId() {
        RagSyncFailureQuery param = new RagSyncFailureQuery();
        param.setSource("REDIS_DLQ");
        param.setDataIdFuzzy("P1");
        param.setPageNo(1);
        when(redisComponent.countRagFailRedisSnapshots()).thenReturn(2L);
        RagSyncFailureVO v1 = new RagSyncFailureVO();
        v1.setDataId("P1");
        v1.setDataType("PRODUCT");
        RagSyncFailureVO v2 = new RagSyncFailureVO();
        v2.setDataId("P2");
        v2.setDataType("PRODUCT");
        when(redisComponent.listRagFailRedisSnapshots(0, PageSize.SIZE15.getSize()))
                .thenReturn(List.of(v1, v2));

        PaginationResultVO<RagSyncFailureVO> result = ragSyncFailureService.loadList(param);

        assertEquals(1, result.getList().size());
        assertEquals("P1", result.getList().get(0).getDataId());
        verify(mqCompensationLogMapper, never()).selectCount(any());
    }

    @Test
    void replay_delegates() {
        ragSyncFailureService.replay(1);

        verify(mqCompensationLogService).replay(1);
    }

    @Test
    void updateStatus_delegates() {
        ragSyncFailureService.updateStatus(1, 2, "已处理");

        verify(mqCompensationLogService).updateHandleStatus(1, 2, "已处理");
    }

    @Test
    void dismissRedisSnapshot_delegates() {
        ragSyncFailureService.dismissRedisSnapshot("P1", "PRODUCT");

        verify(redisComponent).removeRagFailRedisSnapshot("P1", "PRODUCT");
    }

    @Test
    void loadList_nullPageDefaults() {
        RagSyncFailureQuery param = new RagSyncFailureQuery();
        when(mqCompensationLogMapper.selectCount(any())).thenReturn(0);
        when(mqCompensationLogMapper.selectList(any())).thenReturn(List.of());

        PaginationResultVO<RagSyncFailureVO> result = ragSyncFailureService.loadList(param);

        assertEquals(1, result.getPageNo());
        assertEquals(PageSize.SIZE15.getSize(), result.getPageSize());
    }
}
