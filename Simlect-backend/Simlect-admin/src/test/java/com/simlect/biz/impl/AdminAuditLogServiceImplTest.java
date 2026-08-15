package com.simlect.biz.impl;

import com.simlect.entity.po.AdminAuditLog;
import com.simlect.entity.query.AdminAuditLogQuery;
import com.simlect.mappers.AdminAuditLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceImplTest {

    @Mock
    private AdminAuditLogMapper<AdminAuditLog, AdminAuditLogQuery> adminAuditLogMapper;

    @InjectMocks
    private AdminAuditLogServiceImpl adminAuditLogService;

    @Test
    void log_emptyAction_ignored() {
        adminAuditLogService.log("admin", "", "U1", "detail");
        adminAuditLogService.log("admin", null, "U1", "detail");

        verifyNoInteractions(adminAuditLogMapper);
    }

    @Test
    void log_success_insertsWithUnknownOperator() {
        adminAuditLogService.log(null, "BAN_USER", "U1", "封禁");

        verify(adminAuditLogMapper).insert(argThat(row ->
                "unknown".equals(row.getOperator())
                        && "BAN_USER".equals(row.getAction())
                        && "U1".equals(row.getTargetUserId())
                        && row.getCreateTime() != null));
    }

    @Test
    void log_dbFailure_swallowed() {
        doThrow(new RuntimeException("db down")).when(adminAuditLogMapper).insert(any());

        adminAuditLogService.log("admin", "ACTION", "U1", "d");

        verify(adminAuditLogMapper).insert(any());
    }
}
