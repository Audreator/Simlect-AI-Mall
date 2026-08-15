package com.simlect.biz.impl;

import com.simlect.entity.po.OrderItem;
import com.simlect.entity.query.OrderItemQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.mappers.OrderItemMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemMapper<OrderItem, OrderItemQuery> orderItemMapper;

    @InjectMocks
    private OrderItemServiceImpl service;

    @Test
    void findListByParam_delegates() {
        OrderItem item = new OrderItem();
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(item));
        assertEquals(1, service.findListByParam(new OrderItemQuery()).size());
    }

    @Test
    void findCountByParam_delegates() {
        when(orderItemMapper.selectCount(any(OrderItemQuery.class))).thenReturn(7);
        assertEquals(7, service.findCountByParam(new OrderItemQuery()));
    }

    @Test
    void findListByPage_returnsPagination() {
        when(orderItemMapper.selectCount(any(OrderItemQuery.class))).thenReturn(21);
        when(orderItemMapper.selectList(any(OrderItemQuery.class))).thenReturn(List.of(new OrderItem()));
        OrderItemQuery q = new OrderItemQuery();
        q.setPageNo(2);
        q.setPageSize(10);

        PaginationResultVO<OrderItem> page = service.findListByPage(q);

        assertEquals(21, page.getTotalCount());
        assertEquals(3, page.getPageTotal());
        assertEquals(1, page.getList().size());
    }

    @Test
    void crud_delegates() {
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(orderItemMapper.insertBatch(anyList())).thenReturn(2);
        when(orderItemMapper.insertOrUpdateBatch(anyList())).thenReturn(3);
        when(orderItemMapper.updateByParam(any(), any())).thenReturn(4);
        when(orderItemMapper.deleteByParam(any())).thenReturn(5);
        OrderItem byId = new OrderItem();
        when(orderItemMapper.selectByOrderItemId("OI1")).thenReturn(byId);
        when(orderItemMapper.updateByOrderItemId(any(), anyString())).thenReturn(6);
        when(orderItemMapper.deleteByOrderItemId(anyString())).thenReturn(7);

        assertEquals(1, service.add(new OrderItem()));
        assertEquals(2, service.addBatch(List.of(new OrderItem(), new OrderItem())));
        assertEquals(0, service.addBatch(List.of()));
        assertEquals(3, service.addOrUpdateBatch(List.of(new OrderItem())));
        assertEquals(0, service.addOrUpdateBatch(null));
        OrderItemQuery updateQuery = new OrderItemQuery();
        updateQuery.setOrderItemId("OI1");
        assertEquals(4, service.updateByParam(new OrderItem(), updateQuery));
        OrderItemQuery deleteQuery = new OrderItemQuery();
        deleteQuery.setOrderItemId("OI1");
        assertEquals(5, service.deleteByParam(deleteQuery));
        assertSame(byId, service.getOrderItemByOrderItemId("OI1"));
        assertEquals(6, service.updateOrderItemByOrderItemId(new OrderItem(), "OI1"));
        assertEquals(7, service.deleteOrderItemByOrderItemId("OI1"));
    }
}
