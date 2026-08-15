package com.simlect.biz.impl;

import com.simlect.api.vo.SignRecordSyncResultVO;
import com.simlect.component.RedisComponent;
import com.simlect.entity.po.UserSignRecord;
import com.simlect.entity.query.UserSignRecordQuery;
import com.simlect.mappers.UserSignRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignRecordSyncServiceImplTest {

    @Mock
    private UserSignRecordMapper<UserSignRecord, UserSignRecordQuery> userSignRecordMapper;
    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private SignRecordSyncServiceImpl signRecordSyncService;

    private UserSignRecord buildRecord() {
        UserSignRecord record = new UserSignRecord();
        record.setUserId("U1");
        record.setContinuousDays(3);
        record.setTotalSignDays(10);
        record.setUsedCount(1);
        return record;
    }

    @Test
    void ensureHashHydrated_emptyUserId_ignored() {
        signRecordSyncService.ensureHashHydrated("");

        verifyNoInteractions(redisComponent, userSignRecordMapper);
    }

    @Test
    void ensureHashHydrated_snapshotExists_skipsSync() {
        when(redisComponent.hasSignHashSnapshot("U1")).thenReturn(true);

        signRecordSyncService.ensureHashHydrated("U1");

        verify(userSignRecordMapper, never()).selectByUserId(anyString());
    }

    @Test
    void ensureHashHydrated_syncsFromDbWhenMissing() {
        when(redisComponent.hasSignHashSnapshot("U1")).thenReturn(false);
        when(userSignRecordMapper.selectByUserId("U1")).thenReturn(buildRecord());

        signRecordSyncService.ensureHashHydrated("U1");

        verify(redisComponent).writeSignHash("U1", 3, 10, 1);
    }

    @Test
    void syncUserFromDb_noRecord_returnsFalse() {
        when(userSignRecordMapper.selectByUserId("U1")).thenReturn(null);

        assertFalse(signRecordSyncService.syncUserFromDb("U1", true));
        verify(redisComponent, never()).writeSignHash(anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void syncUserFromDb_notForcedWithSnapshot_returnsFalse() {
        when(redisComponent.hasSignHashSnapshot("U1")).thenReturn(true);

        assertFalse(signRecordSyncService.syncUserFromDb("U1", false));
        verify(userSignRecordMapper, never()).selectByUserId(anyString());
    }

    @Test
    void syncUserFromDb_forced_writesHash() {
        when(userSignRecordMapper.selectByUserId("U1")).thenReturn(buildRecord());

        assertTrue(signRecordSyncService.syncUserFromDb("U1", true));
        verify(redisComponent).writeSignHash("U1", 3, 10, 1);
    }

    @Test
    void syncAllFromDb_emptyRows_zeroCounts() {
        when(userSignRecordMapper.selectList(any())).thenReturn(List.of());

        SignRecordSyncResultVO result = signRecordSyncService.syncAllFromDb(false);

        assertEquals(0, result.getTotalInDb());
        assertEquals(0, result.getSynced());
    }

    @Test
    void syncAllFromDb_skipsExistingSnapshots() {
        UserSignRecord u1 = buildRecord();
        UserSignRecord u2 = buildRecord();
        u2.setUserId("U2");
        when(userSignRecordMapper.selectList(any())).thenReturn(List.of(u1, u2));
        when(redisComponent.hasSignHashSnapshot("U1")).thenReturn(true);
        when(redisComponent.hasSignHashSnapshot("U2")).thenReturn(false);

        SignRecordSyncResultVO result = signRecordSyncService.syncAllFromDb(false);

        assertEquals(2, result.getTotalInDb());
        assertEquals(1, result.getSynced());
        assertEquals(1, result.getSkipped());
        verify(redisComponent).writeSignHash("U2", 3, 10, 1);
    }

    @Test
    void warmUpMissingOnStartup_swallowsException() {
        when(userSignRecordMapper.selectList(any())).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> signRecordSyncService.warmUpMissingOnStartup());
    }
}
