package com.simlect.component;

import com.simlect.constants.Constants;
import com.simlect.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 用户端登录失败锁定（防撞库/暴力破解）：
 * 同一 IP 连续失败 5 次后锁定 15 分钟（与 admin 登录锁定隔离的独立 key 前缀）。
 */
@Component
public class UserLoginLockService {

    private static final int MAX_FAIL_COUNT = 5;
    private static final long LOCK_SECONDS = 15 * 60L;

    private static final String FAIL_PREFIX = "user:login:fail:";
    private static final String LOCK_PREFIX = "user:login:lock:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void ensureNotLocked(String ip) {
        if (ip == null) {
            return;
        }
        String lockKey = LOCK_PREFIX + ip;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            throw new BusinessException("登录失败次数过多，请 15 分钟后再试");
        }
    }

    public void recordFailure(String ip) {
        if (ip == null) {
            return;
        }
        String failKey = FAIL_PREFIX + ip;
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        stringRedisTemplate.expire(failKey, LOCK_SECONDS, TimeUnit.SECONDS);
        if (count != null && count >= MAX_FAIL_COUNT) {
            stringRedisTemplate.opsForValue().set(LOCK_PREFIX + ip, "1", LOCK_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.delete(failKey);
        }
    }

    public void clearFailures(String ip) {
        if (ip == null) {
            return;
        }
        stringRedisTemplate.delete(FAIL_PREFIX + ip);
        stringRedisTemplate.delete(LOCK_PREFIX + ip);
    }
}
