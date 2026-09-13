package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.entity.config.AppConfig;
import com.simlect.exception.BusinessException;
import com.simlect.exception.PayOrderLifecycleBusyException;
import com.simlect.redis.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RedisComponent 支付/关单生命周期锁（Redisson 看门狗）单元测试。
 */
@ExtendWith(MockitoExtension.class)
class RedisComponentTest {

    private static final String PAY_ORDER_ID = "PO20260815001";
    private static final String LOCK_KEY =
            Constants.REDIS_KEY_PAY_ORDER_LIFECYCLE_LOCK + PAY_ORDER_ID;

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private RedisUtils<Object> redisUtils;
    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private RedisComponent redisComponent;

    private Thread mainThread;

    @BeforeEach
    void setUp() {
        mainThread = Thread.currentThread();
    }

    @AfterEach
    void tearDown() {
        // 还原中断标志，避免影响其他用例
        Thread.interrupted();
    }

    // ==================== 获取成功 ====================

    @Test
    void lockAcquired_runsAction_andUnlocks() throws Exception {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(Constants.PAY_ORDER_LIFECYCLE_LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS))
                .thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        AtomicInteger ran = new AtomicInteger();

        redisComponent.runWithPayOrderLifecycleLock(PAY_ORDER_ID, ran::incrementAndGet);

        assertEquals(1, ran.get());
        verify(lock).tryLock(Constants.PAY_ORDER_LIFECYCLE_LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
    }

    @Test
    void lockAcquired_callable_returnsValue() throws Exception {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(Constants.PAY_ORDER_LIFECYCLE_LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS))
                .thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        String result = redisComponent.runWithPayOrderLifecycleLock(PAY_ORDER_ID, () -> "paid");

        assertEquals("paid", result);
        verify(lock).unlock();
    }

    @Test
    void lockAcquired_runtimeException_propagates_andUnlocks() throws Exception {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(Constants.PAY_ORDER_LIFECYCLE_LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS))
                .thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        BusinessException boom = new BusinessException("业务失败");
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> redisComponent.runWithPayOrderLifecycleLock(PAY_ORDER_ID, () -> {
                    throw boom;
                }));

        assertSame(boom, thrown);
        verify(lock).unlock();
    }

    // ==================== 获取失败 ====================

    @Test
    void lockBusy_throwsLifecycleBusy_andNeverUnlocks() throws Exception {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(Constants.PAY_ORDER_LIFECYCLE_LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS))
                .thenReturn(false);

        assertThrows(PayOrderLifecycleBusyException.class,
                () -> redisComponent.runWithPayOrderLifecycleLock(PAY_ORDER_ID, () -> {
                }));

        verify(lock, never()).unlock();
    }

    @Test
    void lockInterrupted_throwsBusinessException_andRestoresInterruptFlag() throws Exception {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(Constants.PAY_ORDER_LIFECYCLE_LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS))
                .thenThrow(new InterruptedException());

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> redisComponent.runWithPayOrderLifecycleLock(PAY_ORDER_ID, () -> {
                }));

        assertTrue(thrown.getMessage().contains("中断"));
        assertTrue(Thread.currentThread().isInterrupted());
        verify(lock, never()).unlock();
    }

    @Test
    void lockLost_beforeUnlock_skipUnlock() throws Exception {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(Constants.PAY_ORDER_LIFECYCLE_LOCK_WAIT_MS, -1, TimeUnit.MILLISECONDS))
                .thenReturn(true);
        // 锁已被看门狗过期并被他人获取：isHeldByCurrentThread = false
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        redisComponent.runWithPayOrderLifecycleLock(PAY_ORDER_ID, () -> {
        });

        verify(lock, never()).unlock();
    }

    // ==================== 空 payOrderId ====================

    @Test
    void emptyPayOrderId_runsActionDirectly_withoutRedisson() {
        redisComponent.runWithPayOrderLifecycleLock("", () -> {
        });
        redisComponent.runWithPayOrderLifecycleLock(null, () -> {
        });

        verifyNoInteractions(redissonClient);
    }

    // ==================== 关单幂等标记 ====================

    @Test
    void tryMarkPayOrderCloseOnce_firstCallTrue_secondFalse() {
        when(stringRedisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        when(stringRedisTemplate.opsForValue().setIfAbsent(
                Constants.REDIS_KEY_PAY_ORDER_CLOSE_DONE + PAY_ORDER_ID, "1",
                3L * 24 * 3600, TimeUnit.SECONDS)).thenReturn(true);

        assertTrue(redisComponent.tryMarkPayOrderCloseOnce(PAY_ORDER_ID));
        assertFalse(redisComponent.tryMarkPayOrderCloseOnce(""));
    }

    @Test
    void isPayOrderCloseMarked_queriesKey() {
        when(stringRedisTemplate.hasKey(Constants.REDIS_KEY_PAY_ORDER_CLOSE_DONE + PAY_ORDER_ID))
                .thenReturn(true);

        assertTrue(redisComponent.isPayOrderCloseMarked(PAY_ORDER_ID));
        assertFalse(redisComponent.isPayOrderCloseMarked(null));
    }

    @Test
    void clearPayOrderCloseMark_deletesOnlyNonEmptyKey() {
        redisComponent.clearPayOrderCloseMark(PAY_ORDER_ID);
        redisComponent.clearPayOrderCloseMark("");

        verify(stringRedisTemplate).delete(Constants.REDIS_KEY_PAY_ORDER_CLOSE_DONE + PAY_ORDER_ID);
    }

    @Test
    void tryMarkLatePaymentRefundOnce_usesSetIfAbsent() {
        when(stringRedisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        when(stringRedisTemplate.opsForValue().setIfAbsent(
                Constants.REDIS_KEY_PAY_LATE_REFUND_DONE + PAY_ORDER_ID, "1",
                3L * 24 * 3600, TimeUnit.SECONDS)).thenReturn(true);

        assertTrue(redisComponent.tryMarkLatePaymentRefundOnce(PAY_ORDER_ID));
        assertFalse(redisComponent.tryMarkLatePaymentRefundOnce(null));
    }
}

