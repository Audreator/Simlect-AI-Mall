package com.simlect.biz.impl;

import com.simlect.biz.OrderCommentService;
import com.simlect.entity.po.CommentReport;
import com.simlect.entity.po.OrderComment;
import com.simlect.entity.query.CommentReportQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.CommentReportMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentReportServiceImplTest {

    @Mock
    private CommentReportMapper<CommentReport, CommentReportQuery> commentReportMapper;
    @Mock
    private OrderCommentService orderCommentService;

    @InjectMocks
    private CommentReportServiceImpl service;

    @Test
    void submitReport_success_insertsBean() {
        when(orderCommentService.getOrderCommentByOrderId("O1")).thenReturn(null);

        CommentReport report = service.submitReport("U1", "O1", "P1", "广告", "详情", "快照");

        assertNotNull(report);
        assertEquals("O1", report.getOrderId());
        assertEquals("U1", report.getReporterUserId());
        assertEquals(0, report.getStatus());
        assertNotNull(report.getReportTime());
        verify(commentReportMapper).insert(report);
    }

    @Test
    void submitReport_missingOrderId_throws() {
        assertThrows(BusinessException.class, () -> service.submitReport("U1", "", "P1", "广告", null, null));
    }

    @Test
    void submitReport_missingReason_throws() {
        assertThrows(BusinessException.class, () -> service.submitReport("U1", "O1", "P1", "", null, null));
    }

    @Test
    void submitReport_selfReport_throws() {
        OrderComment comment = new OrderComment();
        comment.setUserId("U1");
        when(orderCommentService.getOrderCommentByOrderId("O1")).thenReturn(comment);
        assertThrows(BusinessException.class, () -> service.submitReport("U1", "O1", "P1", "广告", null, null));
    }

    @Test
    void handleReport_defaultsStatusTo1() {
        when(commentReportMapper.updateByReportId(any(CommentReport.class), eq(1))).thenReturn(1);
        assertEquals(1, service.handleReport(1, null, "已处理"));
        verify(commentReportMapper).updateByReportId(any(CommentReport.class), eq(1));
    }

    @Test
    void handleReport_nullReportId_throws() {
        assertThrows(BusinessException.class, () -> service.handleReport(null, 1, "x"));
    }

    @Test
    void handleReport_withStatus_passesThrough() {
        when(commentReportMapper.updateByReportId(any(CommentReport.class), eq(1))).thenReturn(1);
        assertEquals(1, service.handleReport(1, 2, "删除"));
    }

    @Test
    void getByReportId_delegates() {
        CommentReport report = new CommentReport();
        when(commentReportMapper.selectByReportId(1)).thenReturn(report);
        assertSame(report, service.getByReportId(1));
    }

    @Test
    void deleteByReportId_delegates() {
        when(commentReportMapper.deleteByReportId(1)).thenReturn(1);
        assertEquals(1, service.deleteByReportId(1));
    }

    @Test
    void findListByPage_returnsPagination() {
        when(commentReportMapper.selectCount(any(CommentReportQuery.class))).thenReturn(11);
        when(commentReportMapper.selectList(any(CommentReportQuery.class))).thenReturn(List.of(new CommentReport()));
        PaginationResultVO<CommentReport> page = service.findListByPage(new CommentReportQuery());
        assertEquals(11, page.getTotalCount());
        assertEquals(1, page.getList().size());
    }
}
