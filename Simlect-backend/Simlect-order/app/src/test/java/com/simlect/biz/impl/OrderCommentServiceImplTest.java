package com.simlect.biz.impl;

import com.simlect.api.enums.CommentStatusEnum;
import com.simlect.api.enums.OrderCommentStatusEnum;
import com.simlect.api.support.UserFeignSupport;
import com.simlect.api.vo.ProductCommentStatsVO;
import com.simlect.biz.OrderInfoService;
import com.simlect.component.SensitiveWordFilter;
import com.simlect.entity.po.OrderComment;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.po.OrderItem;
import com.simlect.entity.query.OrderCommentQuery;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.OrderCommentMapper;
import com.simlect.mappers.OrderInfoMapper;
import com.simlect.mappers.OrderItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCommentServiceImplTest {

    private static final String ORDER_ID = "O1";
    private static final String USER_ID = "U1";

    @Mock
    private OrderCommentMapper<OrderComment, OrderCommentQuery> orderCommentMapper;
    @Mock
    private OrderInfoMapper<OrderInfo, OrderInfoQuery> orderInfoMapper;
    @Mock
    private OrderInfoService orderInfoService;
    @Mock
    private UserFeignSupport userFeignSupport;
    @Mock
    private OrderItemMapper<OrderItem, com.simlect.entity.query.OrderItemQuery> orderItemMapper;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private OrderCommentServiceImpl service;

    private OrderInfo evaluatedOrder() {
        OrderInfo o = new OrderInfo();
        o.setOrderId(ORDER_ID);
        o.setUserId(USER_ID);
        o.setCommentStatus(OrderCommentStatusEnum.EVALUATED.getStatus());
        OrderItem item = new OrderItem();
        item.setOrderItemId(ORDER_ID + "_1");
        item.setProductId("P1");
        item.setPropertyInfo("颜色:红色");
        o.setOrderItemList(List.of(item));
        return o;
    }

    private OrderInfo notEvaluatedOrder() {
        OrderInfo o = evaluatedOrder();
        o.setCommentStatus(OrderCommentStatusEnum.NOT_EVALUATED.getStatus());
        return o;
    }

    // ==================== postComment ====================

    @Test
    void postComment_normal_returnsFalseNotPending() {
        when(sensitiveWordFilter.replaceSensitiveWords(anyString())).thenReturn("好评");
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class)))
                .thenReturn(List.of(notEvaluatedOrder()));
        when(orderInfoMapper.markCommentEvaluatedIfNotEvaluated(ORDER_ID, USER_ID)).thenReturn(1);
        when(orderCommentMapper.insert(any(OrderComment.class))).thenReturn(1);

        boolean pending = service.postComment(USER_ID, ORDER_ID, "好评", "/upload/a.jpg", 5);

        assertFalse(pending);
        assertEquals(CommentStatusEnum.NORMAL.getStatus(), getInsertedComment().getStatus());
        verify(orderInfoMapper).markCommentEvaluatedIfNotEvaluated(ORDER_ID, USER_ID);
    }

    @Test
    void postComment_quarantineImage_pendingReview() {
        when(sensitiveWordFilter.replaceSensitiveWords(anyString())).thenReturn("好评");
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class)))
                .thenReturn(List.of(notEvaluatedOrder()));
        when(orderInfoMapper.markCommentEvaluatedIfNotEvaluated(ORDER_ID, USER_ID)).thenReturn(1);
        when(orderCommentMapper.insert(any(OrderComment.class))).thenReturn(1);

        boolean pending = service.postComment(USER_ID, ORDER_ID, "好评",
                "moderation/pending/abc.jpg", 4);

        assertTrue(pending);
        assertEquals(CommentStatusEnum.PENDING.getStatus(), getInsertedComment().getStatus());
    }

    @Test
    void postComment_orderNotFound_throws() {
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class)))
                .thenReturn(java.util.Collections.singletonList(null));
        assertThrows(BusinessException.class, () -> service.postComment(USER_ID, ORDER_ID, "好评", null, 5));
    }

    @Test
    void postComment_otherUserOrder_throws() {
        OrderInfo o = evaluatedOrder();
        o.setUserId("U999");
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(o));
        assertThrows(BusinessException.class, () -> service.postComment(USER_ID, ORDER_ID, "好评", null, 5));
    }

    @Test
    void postComment_alreadyEvaluated_throws() {
        when(sensitiveWordFilter.replaceSensitiveWords(anyString())).thenReturn("好评");
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class)))
                .thenReturn(List.of(notEvaluatedOrder()));
        when(orderInfoMapper.markCommentEvaluatedIfNotEvaluated(ORDER_ID, USER_ID)).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.postComment(USER_ID, ORDER_ID, "好评", null, 5));
    }

    @Test
    void postComment_insertFails_throws() {
        when(sensitiveWordFilter.replaceSensitiveWords(anyString())).thenReturn("好评");
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class)))
                .thenReturn(List.of(notEvaluatedOrder()));
        when(orderInfoMapper.markCommentEvaluatedIfNotEvaluated(ORDER_ID, USER_ID)).thenReturn(1);
        when(orderCommentMapper.insert(any(OrderComment.class))).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.postComment(USER_ID, ORDER_ID, "好评", null, 5));
    }

    @Test
    void postComment_badImagePath_throws() {
        when(sensitiveWordFilter.replaceSensitiveWords(anyString())).thenReturn("好评");
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class)))
                .thenReturn(List.of(evaluatedOrder()));
        assertThrows(BusinessException.class, () -> service.postComment(USER_ID, ORDER_ID, "好评", "/../../etc/passwd", 5));
    }

    // ==================== getComment / 追评 / 删除 ====================

    @Test
    void getComment_notExists_throws() {
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class)))
                .thenReturn(java.util.Collections.singletonList(null));
        assertThrows(BusinessException.class, () -> service.getComment(USER_ID, ORDER_ID));
    }

    @Test
    void getComment_returnsNormalComment() {
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(evaluatedOrder()));
        OrderComment comment = new OrderComment();
        comment.setOrderId(ORDER_ID);
        comment.setStatus(CommentStatusEnum.NORMAL.getStatus());
        when(orderCommentMapper.selectList(any(OrderCommentQuery.class))).thenReturn(List.of(comment));

        OrderComment result = service.getComment(USER_ID, ORDER_ID);

        assertEquals(CommentStatusEnum.NORMAL.getStatus(), result.getStatus());
    }

    @Test
    void getComment_deletedForUser_returnsNull() {
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(evaluatedOrder()));
        when(orderCommentMapper.selectList(any(OrderCommentQuery.class))).thenReturn(List.of());
        assertNull(service.getComment(USER_ID, ORDER_ID));
    }

    @Test
    void postReComment_success() {
        when(sensitiveWordFilter.replaceSensitiveWords(anyString())).thenReturn("补充评价");
        OrderInfo order = evaluatedOrder();
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(order));
        OrderComment comment = new OrderComment();
        comment.setOrderId(ORDER_ID);
        when(orderCommentMapper.selectByOrderId(ORDER_ID)).thenReturn(comment);
        when(orderCommentMapper.updateByOrderId(any(OrderComment.class), eq(ORDER_ID))).thenReturn(1);

        service.postReComment(USER_ID, ORDER_ID, "补充评价", null);

        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoService).updateByParam(orderCaptor.capture(), any(OrderInfoQuery.class));
        assertEquals(OrderCommentStatusEnum.ADDITIONAL_EVALUATED.getStatus(),
                orderCaptor.getValue().getCommentStatus());
        assertEquals("补充评价", comment.getRecommentContent());
        assertNotNull(comment.getRecommentTime());
    }

    @Test
    void postReComment_notEvaluated_throws() {
        OrderInfo o = evaluatedOrder();
        o.setCommentStatus(OrderCommentStatusEnum.NOT_EVALUATED.getStatus());
        when(sensitiveWordFilter.replaceSensitiveWords(anyString())).thenReturn("x");
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(o));
        assertThrows(BusinessException.class, () -> service.postReComment(USER_ID, ORDER_ID, "x", null));
    }

    @Test
    void postReComment_updateFails_throws() {
        when(sensitiveWordFilter.replaceSensitiveWords(anyString())).thenReturn("补充评价");
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(evaluatedOrder()));
        when(orderCommentMapper.selectByOrderId(ORDER_ID)).thenReturn(new OrderComment());
        when(orderCommentMapper.updateByOrderId(any(OrderComment.class), eq(ORDER_ID))).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.postReComment(USER_ID, ORDER_ID, "补充评价", null));
    }

    @Test
    void delMyComment_success_logicalDelete() {
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(evaluatedOrder()));
        OrderComment comment = new OrderComment();
        comment.setOrderId(ORDER_ID);
        comment.setStatus(CommentStatusEnum.NORMAL.getStatus());
        when(orderCommentMapper.selectByOrderId(ORDER_ID)).thenReturn(comment);
        when(orderCommentMapper.updateByOrderId(any(OrderComment.class), eq(ORDER_ID))).thenReturn(1);

        service.delMyComment(USER_ID, ORDER_ID);

        assertEquals(CommentStatusEnum.DEL.getStatus(), comment.getStatus());
    }

    @Test
    void delMyComment_notEvaluated_throws() {
        OrderInfo o = evaluatedOrder();
        o.setCommentStatus(OrderCommentStatusEnum.NOT_EVALUATED.getStatus());
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(o));
        assertThrows(BusinessException.class, () -> service.delMyComment(USER_ID, ORDER_ID));
    }

    @Test
    void delMyComment_alreadyDeleted_throws() {
        when(orderInfoService.findListByParam(any(OrderInfoQuery.class))).thenReturn(List.of(evaluatedOrder()));
        OrderComment comment = new OrderComment();
        comment.setStatus(CommentStatusEnum.DEL.getStatus());
        when(orderCommentMapper.selectByOrderId(ORDER_ID)).thenReturn(comment);
        assertThrows(BusinessException.class, () -> service.delMyComment(USER_ID, ORDER_ID));
    }

    // ==================== 商家回复 / 审核 ====================

    @Test
    void bizComment_success() {
        OrderInfo o = evaluatedOrder();
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(o);
        OrderComment comment = new OrderComment();
        comment.setOrderId(ORDER_ID);
        when(orderCommentMapper.selectByOrderId(ORDER_ID)).thenReturn(comment);

        service.bizComment(ORDER_ID, "感谢您的购买", null);

        assertEquals("感谢您的购买", comment.getCommentBizReply());
        verify(orderCommentMapper).insertOrUpdate(comment);
    }

    @Test
    void bizComment_notEvaluated_throws() {
        OrderInfo o = evaluatedOrder();
        o.setCommentStatus(OrderCommentStatusEnum.NOT_EVALUATED.getStatus());
        when(orderInfoService.getOrderInfoByOrderId(ORDER_ID)).thenReturn(o);
        assertThrows(BusinessException.class, () -> service.bizComment(ORDER_ID, "回复", null));
    }

    @Test
    void deletePendingCommentByOrderId_delegates() {
        when(orderCommentMapper.deleteByOrderIdIfStatus(ORDER_ID, CommentStatusEnum.PENDING.getStatus())).thenReturn(1);
        assertEquals(1, service.deletePendingCommentByOrderId(ORDER_ID));
    }

    @Test
    void publishPendingComment_delegates() {
        when(orderCommentMapper.publishIfStatus(ORDER_ID, "img", CommentStatusEnum.PENDING.getStatus(),
                CommentStatusEnum.NORMAL.getStatus())).thenReturn(1);
        assertEquals(1, service.publishPendingComment(ORDER_ID, "img"));
    }

    // ==================== 统计 / 筛选 / 分页 ====================

    @Test
    void getProductCommentStats_emptyProduct_defaults() {
        ProductCommentStatsVO vo = service.getProductCommentStats("");
        assertEquals(0, vo.getTotalCount());
        assertEquals(100, vo.getGoodRatePercent());
    }

    @Test
    void getProductCommentStats_computesRate() {
        when(orderCommentMapper.selectProductCommentStats("P1"))
                .thenReturn(Map.of("totalCount", 100L, "goodCount", 80L, "imageCount", 30L));
        ProductCommentStatsVO vo = service.getProductCommentStats("P1");
        assertEquals(100, vo.getTotalCount());
        assertEquals(80, vo.getGoodCount());
        assertEquals(30, vo.getImageCount());
        assertEquals(80, vo.getGoodRatePercent());
    }

    @Test
    void getProductCommentStats_noRows_defaults() {
        when(orderCommentMapper.selectProductCommentStats("P1")).thenReturn(null);
        ProductCommentStatsVO vo = service.getProductCommentStats("P1");
        assertEquals(0, vo.getTotalCount());
    }

    @Test
    void findByNickNameFuzzy_filters() {
        OrderComment c1 = new OrderComment();
        c1.setNickName("小明");
        OrderComment c2 = new OrderComment();
        c2.setNickName("小红");
        PaginationResultVO<OrderComment> page = new PaginationResultVO<>();
        page.setList(List.of(c1, c2));

        PaginationResultVO<OrderComment> result = service.findByNickNameFuzzy(page, "小明");

        assertEquals(1, result.getList().size());
    }

    @Test
    void findByProductNameFuzzy_filters() {
        OrderComment c1 = new OrderComment();
        c1.setProductName("苹果");
        OrderComment c2 = new OrderComment();
        c2.setProductName("香蕉");
        PaginationResultVO<OrderComment> page = new PaginationResultVO<>();
        page.setList(List.of(c1, c2));

        PaginationResultVO<OrderComment> result = service.findByProductNameFuzzy(page, "香蕉");

        assertEquals(1, result.getList().size());
    }

    @Test
    void findListByPage_withQueryProduct_enrichesItems() {
        OrderComment c1 = new OrderComment();
        c1.setOrderId(ORDER_ID);
        OrderCommentQuery q = new OrderCommentQuery();
        q.setQueryProduct(true);
        when(orderCommentMapper.selectCount(any(OrderCommentQuery.class))).thenReturn(1);
        when(orderCommentMapper.selectList(any(OrderCommentQuery.class))).thenReturn(List.of(c1));
        OrderItem item = new OrderItem();
        item.setOrderId(ORDER_ID);
        when(orderItemMapper.selectByOrderIds(anyList())).thenReturn(List.of(item));

        PaginationResultVO<OrderComment> page = service.findListByPage(q);

        assertEquals(1, page.getList().get(0).getOrderItems().size());
    }

    @Test
    void findListByParam_withQueryUserInfo_enrichesUser() {
        OrderComment c1 = new OrderComment();
        c1.setUserId(USER_ID);
        OrderCommentQuery q = new OrderCommentQuery();
        q.setQueryUserInfo(true);
        when(orderCommentMapper.selectList(any(OrderCommentQuery.class))).thenReturn(List.of(c1));
        when(userFeignSupport.mapBriefByUserIds(anyList())).thenReturn(Map.of(USER_ID,
                new com.simlect.api.vo.UserBriefVO(USER_ID, "小明", "a.png")));

        List<OrderComment> list = service.findListByParam(q);

        assertEquals("小明", list.get(0).getNickName());
    }

    @Test
    void crud_delegates() {
        when(orderCommentMapper.insert(any(OrderComment.class))).thenReturn(1);
        when(orderCommentMapper.insertBatch(anyList())).thenReturn(2);
        when(orderCommentMapper.insertOrUpdateBatch(anyList())).thenReturn(3);
        when(orderCommentMapper.updateByParam(any(), any())).thenReturn(4);
        when(orderCommentMapper.deleteByParam(any())).thenReturn(5);
        OrderComment byId = new OrderComment();
        when(orderCommentMapper.selectByOrderId(ORDER_ID)).thenReturn(byId);
        when(orderCommentMapper.updateByOrderId(any(), anyString())).thenReturn(6);
        when(orderCommentMapper.deleteByOrderId(anyString())).thenReturn(7);

        assertEquals(1, service.add(new OrderComment()));
        assertEquals(2, service.addBatch(List.of(new OrderComment())));
        assertEquals(0, service.addBatch(List.of()));
        assertEquals(3, service.addOrUpdateBatch(List.of(new OrderComment())));
        OrderCommentQuery updateQuery = new OrderCommentQuery();
        updateQuery.setOrderId(ORDER_ID);
        assertEquals(4, service.updateByParam(new OrderComment(), updateQuery));
        OrderCommentQuery deleteQuery = new OrderCommentQuery();
        deleteQuery.setOrderId(ORDER_ID);
        assertEquals(5, service.deleteByParam(deleteQuery));
        assertSame(byId, service.getOrderCommentByOrderId(ORDER_ID));
        assertEquals(6, service.updateOrderCommentByOrderId(new OrderComment(), ORDER_ID));
        assertEquals(7, service.deleteOrderCommentByOrderId(ORDER_ID));
    }

    private OrderComment getInsertedComment() {
        ArgumentCaptor<OrderComment> captor = ArgumentCaptor.forClass(OrderComment.class);
        verify(orderCommentMapper).insert(captor.capture());
        return captor.getValue();
    }
}
