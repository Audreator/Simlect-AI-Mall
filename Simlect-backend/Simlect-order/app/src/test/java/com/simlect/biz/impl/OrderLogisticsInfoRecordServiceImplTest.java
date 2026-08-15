package com.simlect.biz.impl;

import com.simlect.entity.po.OrderLogisticsInfoRecord;
import com.simlect.entity.query.OrderLogisticsInfoRecordQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.mappers.OrderLogisticsInfoRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLogisticsInfoRecordServiceImplTest {

    @Mock
    private OrderLogisticsInfoRecordMapper<OrderLogisticsInfoRecord, OrderLogisticsInfoRecordQuery> mapper;

    @InjectMocks
    private OrderLogisticsInfoRecordServiceImpl service;

    @Test
    void findListByParam_delegates() {
        when(mapper.selectList(any(OrderLogisticsInfoRecordQuery.class))).thenReturn(List.of(new OrderLogisticsInfoRecord()));
        assertEquals(1, service.findListByParam(new OrderLogisticsInfoRecordQuery()).size());
    }

    @Test
    void findCountByParam_delegates() {
        when(mapper.selectCount(any(OrderLogisticsInfoRecordQuery.class))).thenReturn(5);
        assertEquals(5, service.findCountByParam(new OrderLogisticsInfoRecordQuery()));
    }

    @Test
    void findListByPage_returnsPagination() {
        when(mapper.selectCount(any(OrderLogisticsInfoRecordQuery.class))).thenReturn(5);
        when(mapper.selectList(any(OrderLogisticsInfoRecordQuery.class))).thenReturn(List.of(new OrderLogisticsInfoRecord()));
        PaginationResultVO<OrderLogisticsInfoRecord> page = service.findListByPage(new OrderLogisticsInfoRecordQuery());
        assertEquals(5, page.getTotalCount());
        assertEquals(1, page.getList().size());
    }

    @Test
    void crud_delegates() {
        when(mapper.insert(any(OrderLogisticsInfoRecord.class))).thenReturn(1);
        when(mapper.insertBatch(anyList())).thenReturn(2);
        when(mapper.insertOrUpdateBatch(anyList())).thenReturn(3);
        when(mapper.updateByParam(any(), any())).thenReturn(4);
        when(mapper.deleteByParam(any())).thenReturn(5);
        when(mapper.updateByRecordId(any(), any())).thenReturn(6);
        when(mapper.deleteByRecordId(any())).thenReturn(7);
        OrderLogisticsInfoRecord byId = new OrderLogisticsInfoRecord();
        when(mapper.selectByRecordId(1)).thenReturn(byId);

        assertEquals(1, service.add(new OrderLogisticsInfoRecord()));
        assertEquals(2, service.addBatch(List.of(new OrderLogisticsInfoRecord(), new OrderLogisticsInfoRecord())));
        assertEquals(0, service.addBatch(List.of()));
        assertEquals(3, service.addOrUpdateBatch(List.of(new OrderLogisticsInfoRecord())));
        assertEquals(0, service.addOrUpdateBatch(null));
        OrderLogisticsInfoRecordQuery updateQuery = new OrderLogisticsInfoRecordQuery();
        updateQuery.setRecordId(1);
        assertEquals(4, service.updateByParam(new OrderLogisticsInfoRecord(), updateQuery));
        OrderLogisticsInfoRecordQuery deleteQuery = new OrderLogisticsInfoRecordQuery();
        deleteQuery.setRecordId(1);
        assertEquals(5, service.deleteByParam(deleteQuery));
        assertSame(byId, service.getOrderLogisticsInfoRecordByRecordId(1));
        assertEquals(6, service.updateOrderLogisticsInfoRecordByRecordId(new OrderLogisticsInfoRecord(), 1));
        assertEquals(7, service.deleteOrderLogisticsInfoRecordByRecordId(1));
    }
}
