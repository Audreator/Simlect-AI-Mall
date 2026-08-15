package com.simlect.biz.impl;

import com.simlect.entity.po.PayTradeRecord;
import com.simlect.entity.query.PayTradeRecordQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.mappers.PayTradeRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PayTradeRecordServiceImpl 交易记录状态流转单元测试。
 */
@ExtendWith(MockitoExtension.class)
class PayTradeRecordServiceImplTest {

    @Mock
    private PayTradeRecordMapper<PayTradeRecord, PayTradeRecordQuery> payTradeRecordMapper;

    @InjectMocks
    private PayTradeRecordServiceImpl payTradeRecordService;

    private static final String PAY_ORDER_ID = "PO20260815001";
    private static final String CHANNEL_ORDER_ID = "202608152200100001";

    // ==================== createPending ====================

    @Test
    void createPending_emptyPayOrderId_skips() {
        payTradeRecordService.createPending("U1", "", "O1", new BigDecimal("10.00"), "alipay");
        payTradeRecordService.createPending("U1", null, "O1", new BigDecimal("10.00"), "alipay");
        payTradeRecordService.createPending("U1", "PO1", "O1", null, "alipay");

        verify(payTradeRecordMapper, never()).selectCount(any());
        verify(payTradeRecordMapper, never()).insert(any());
    }

    @Test
    void createPending_existingPendingRecord_skipsInsert() {
        when(payTradeRecordMapper.selectCount(any(PayTradeRecordQuery.class))).thenReturn(1);

        payTradeRecordService.createPending("U1", PAY_ORDER_ID, "O1", new BigDecimal("10.00"), "alipay");

        verify(payTradeRecordMapper, never()).insert(any());
    }

    @Test
    void createPending_newRecord_insertsPendingTrade() {
        when(payTradeRecordMapper.selectCount(any(PayTradeRecordQuery.class))).thenReturn(0);
        when(payTradeRecordMapper.insert(any(PayTradeRecord.class))).thenReturn(1);

        payTradeRecordService.createPending("U1", PAY_ORDER_ID, "O1", new BigDecimal("10.00"), "alipay");

        ArgumentCaptor<PayTradeRecord> captor = ArgumentCaptor.forClass(PayTradeRecord.class);
        verify(payTradeRecordMapper).insert(captor.capture());
        PayTradeRecord record = captor.getValue();
        assertNotNull(record.getTradeId());
        assertEquals("U1", record.getUserId());
        assertEquals("O1", record.getOrderId());
        assertEquals(PAY_ORDER_ID, record.getPayOrderId());
        assertEquals("alipay", record.getPayChannel());
        assertEquals(new BigDecimal("10.00"), record.getPayAmount());
        assertEquals(0, record.getTradeStatus());
        assertNotNull(record.getCreateTime());
    }

    // ==================== markSuccess ====================

    @Test
    void markSuccess_noRecords_skipsUpdate() {
        when(payTradeRecordMapper.selectList(any(PayTradeRecordQuery.class))).thenReturn(Collections.emptyList());

        payTradeRecordService.markSuccess(PAY_ORDER_ID, CHANNEL_ORDER_ID);

        verify(payTradeRecordMapper, never()).updateByTradeId(any(), any());
    }

    @Test
    void markSuccess_alreadySuccess_skipsThatRecord() {
        PayTradeRecord success = new PayTradeRecord();
        success.setTradeId("T1");
        success.setTradeStatus(1);
        when(payTradeRecordMapper.selectList(any(PayTradeRecordQuery.class)))
                .thenReturn(List.of(success));

        payTradeRecordService.markSuccess(PAY_ORDER_ID, CHANNEL_ORDER_ID);

        verify(payTradeRecordMapper, never()).updateByTradeId(any(), any());
    }

    @Test
    void markSuccess_pendingRecord_markedSuccess() {
        PayTradeRecord pending = new PayTradeRecord();
        pending.setTradeId("T1");
        pending.setTradeStatus(0);
        when(payTradeRecordMapper.selectList(any(PayTradeRecordQuery.class)))
                .thenReturn(List.of(pending));

        payTradeRecordService.markSuccess(PAY_ORDER_ID, CHANNEL_ORDER_ID);

        ArgumentCaptor<PayTradeRecord> bean = ArgumentCaptor.forClass(PayTradeRecord.class);
        verify(payTradeRecordMapper).updateByTradeId(bean.capture(), org.mockito.ArgumentMatchers.eq("T1"));
        assertEquals(1, bean.getValue().getTradeStatus());
        assertEquals(CHANNEL_ORDER_ID, bean.getValue().getChannelOrderId());
        assertNotNull(bean.getValue().getPayTime());
    }

    @Test
    void markSuccess_mixedStatuses_updatesOnlyNonSuccess() {
        PayTradeRecord pending = new PayTradeRecord();
        pending.setTradeId("T1");
        pending.setTradeStatus(0);
        PayTradeRecord success = new PayTradeRecord();
        success.setTradeId("T2");
        success.setTradeStatus(1);
        when(payTradeRecordMapper.selectList(any(PayTradeRecordQuery.class)))
                .thenReturn(List.of(pending, success));

        payTradeRecordService.markSuccess(PAY_ORDER_ID, CHANNEL_ORDER_ID);

        verify(payTradeRecordMapper).updateByTradeId(any(), org.mockito.ArgumentMatchers.eq("T1"));
        verify(payTradeRecordMapper, never())
                .updateByTradeId(any(), org.mockito.ArgumentMatchers.eq("T2"));
    }

    // ==================== markClosed / markRefunded ====================

    @Test
    void markClosed_updatesPendingToClosed() {
        payTradeRecordService.markClosed(PAY_ORDER_ID);

        ArgumentCaptor<PayTradeRecord> bean = ArgumentCaptor.forClass(PayTradeRecord.class);
        ArgumentCaptor<PayTradeRecordQuery> query = ArgumentCaptor.forClass(PayTradeRecordQuery.class);
        verify(payTradeRecordMapper).updateByParam(bean.capture(), query.capture());
        assertEquals(2, bean.getValue().getTradeStatus());
        assertEquals(0, query.getValue().getTradeStatus());
        assertEquals(PAY_ORDER_ID, query.getValue().getPayOrderId());
    }

    @Test
    void markRefunded_updatesAllToRefunded() {
        payTradeRecordService.markRefunded(PAY_ORDER_ID);

        ArgumentCaptor<PayTradeRecord> bean = ArgumentCaptor.forClass(PayTradeRecord.class);
        ArgumentCaptor<PayTradeRecordQuery> query = ArgumentCaptor.forClass(PayTradeRecordQuery.class);
        verify(payTradeRecordMapper).updateByParam(bean.capture(), query.capture());
        assertEquals(3, bean.getValue().getTradeStatus());
        assertEquals(PAY_ORDER_ID, query.getValue().getPayOrderId());
    }

    // ==================== loadUserTrades ====================

    @Test
    void loadUserTrades_returnsPagedResult() {
        when(payTradeRecordMapper.selectCount(any(PayTradeRecordQuery.class))).thenReturn(40);
        PayTradeRecord record = new PayTradeRecord();
        record.setTradeId("T1");
        record.setUserId("U1");
        when(payTradeRecordMapper.selectList(any(PayTradeRecordQuery.class))).thenReturn(List.of(record));

        PaginationResultVO<PayTradeRecord> page = payTradeRecordService.loadUserTrades("U1", 2);

        assertEquals(40, page.getTotalCount());
        assertEquals(15, page.getPageSize());
        assertEquals(2, page.getPageNo());
        assertEquals(3, page.getPageTotal());
        assertEquals(1, page.getList().size());
        assertEquals("T1", page.getList().get(0).getTradeId());
    }
}
