package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.exception.PayOrderLifecycleBusyException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付/关单生命周期锁（Redisson 看门狗）集成测试。
 * <p>
 * 验证：看门狗在业务执行超过锁默认超时（30s）时自动续期；互斥；释放后可被后续线程获取。
 * 运行：mvn -pl common verify -Pit
 */
@Testcontainers
class PayOrderLifecycleLockIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private RedissonClient redissonClient;
    private StringRedisTemplate stringRedisTemplate;
    private RedisComponent redisComponent;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        String host = REDIS.getHost();
        int port = REDIS.getMappedPort(6379);
        Config config = new Config();
        // 与生产 RedissionConfig 一致：看门狗超时 30s（每 10s 续期）
        config.setLockWatchdogTimeout(30_000);
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        redissonClient = Redisson.create(config);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet();
        stringRedisTemplate = new StringRedisTemplate(factory);
        stringRedisTemplate.afterPropertiesSet();

        redisComponent = new RedisComponent();
        ReflectionTestUtils.setField(redisComponent, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(redisComponent, "stringRedisTemplate", stringRedisTemplate);

        executor = Executors.newFixedThreadPool(3);
        stringRedisTemplate.delete(lockKey("cleanup"));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
        }
    }

    private String lockKey(String payOrderId) {
        return Constants.REDIS_KEY_PAY_ORDER_LIFECYCLE_LOCK + payOrderId;
    }

    /**
     * 核心：业务执行 35s（超过默认 30s 锁超时），锁应被看门狗续期而持续有效；
     * 释放后 Redis 中 key 被删除。
     */
    @Test
    void watchdog_renewsLock_whileBusinessRunsLongerThanInitialTtl() throws Exception {
        String payOrderId = "WATCHDOG-1";
        CountDownLatch entered = new CountDownLatch(1);
        AtomicReference<Throwable> holderError = new AtomicReference<>();

        Future<?> holder = executor.submit(() -> {
            try {
                redisComponent.runWithPayOrderLifecycleLock(payOrderId, () -> {
                    entered.countDown();
                    Thread.sleep(35_000); // > 默认 30s 锁超时
                    return null;
                });
            } catch (Throwable t) {
                holderError.set(t);
            }
        });

        assertTrue(entered.await(10, TimeUnit.SECONDS), "持锁线程应进入业务");

        // t≈15s：锁应有效且 TTL 接近 30s（被续期过）
        Thread.sleep(15_000);
        Long ttl1 = stringRedisTemplate.getExpire(lockKey(payOrderId), TimeUnit.SECONDS);
        assertNotNull(ttl1, "15s 时刻锁 key 应存在");
        assertTrue(ttl1 > 15, "15s 时刻 TTL 应被续期刷新，实际 ttl=" + ttl1);

        // t≈33s：超过原始 30s TTL，无看门狗时锁早已过期；续期后仍存在且 TTL 接近 30s
        Thread.sleep(18_000);
        Long ttl2 = stringRedisTemplate.getExpire(lockKey(payOrderId), TimeUnit.SECONDS);
        assertNotNull(ttl2, "33s 时刻锁 key 应因看门狗续期仍存在");
        assertTrue(ttl2 > 10, "看门狗续期后 TTL 应保持接近 30s，实际 ttl=" + ttl2);

        holder.get(40, TimeUnit.SECONDS);
        assertNull(holderError.get(), "持锁业务不应抛异常");
        assertNull(stringRedisTemplate.opsForValue().get(lockKey(payOrderId)),
                "业务结束后锁应被释放，Redis key 应删除");
    }

    /** 持锁期间，第二请求等待满 PAY_ORDER_LIFECYCLE_LOCK_WAIT_MS 后抛忙碌异常 */
    @Test
    void secondAcquirer_waitsAndThrowsBusy_whileLocked() throws Exception {
        String payOrderId = "MUTEX-1";
        CountDownLatch entered = new CountDownLatch(1);

        Future<?> holder = executor.submit(() -> redisComponent.runWithPayOrderLifecycleLock(payOrderId, () -> {
            entered.countDown();
            Thread.sleep(25_000);
            return null;
        }));

        assertTrue(entered.await(10, TimeUnit.SECONDS));

        long start = System.currentTimeMillis();
        assertThrows(PayOrderLifecycleBusyException.class,
                () -> redisComponent.runWithPayOrderLifecycleLock(payOrderId, () -> null));
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 14_000, "应在等待 15s 后判定忙碌，实际等待 " + elapsed + "ms");
        holder.get(40, TimeUnit.SECONDS);
    }

    /** 释放后，后续线程可立即获取锁 */
    @Test
    void lockReleased_afterAction_nextAcquirerSucceedsImmediately() {
        String payOrderId = "RELEASE-1";
        redisComponent.runWithPayOrderLifecycleLock(payOrderId, () -> null);

        long start = System.currentTimeMillis();
        redisComponent.runWithPayOrderLifecycleLock(payOrderId, () -> null);

        assertTrue(System.currentTimeMillis() - start < 3_000, "释放后应能立即获取锁");
    }

    /** 持锁线程释放后，等待中的线程在等待期内自动获取并执行 */
    @Test
    void waitingThread_acquiresAfterHolderReleases() throws Exception {
        String payOrderId = "WAIT-1";
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Throwable> holderError = new AtomicReference<>();

        Future<?> holder = executor.submit(() -> {
            try {
                redisComponent.runWithPayOrderLifecycleLock(payOrderId, () -> {
                    entered.countDown();
                    release.await(20, TimeUnit.SECONDS);
                    return null;
                });
            } catch (Throwable t) {
                holderError.set(t);
            }
        });

        assertTrue(entered.await(10, TimeUnit.SECONDS), "A 应持锁进入业务");

        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean secondRan = new AtomicBoolean(false);
        Future<?> second = executor.submit(() -> {
            redisComponent.runWithPayOrderLifecycleLock(payOrderId, () -> {
                secondRan.set(true);
                return null;
            });
            done.countDown();
        });

        Thread.sleep(3_000); // B 处于等待锁状态
        release.countDown(); // A 释放锁

        assertTrue(done.await(10, TimeUnit.SECONDS), "B 应等锁释放后自动获取并执行");
        assertTrue(secondRan.get(), "B 的业务应被执行");
        holder.get(20, TimeUnit.SECONDS);
        assertNull(holderError.get());
    }
}

