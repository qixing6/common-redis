package com.example.commonredis.impl;

import com.example.commonredis.api.LockClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockClientImpl implements LockClient {

    private final RedissonClient redisson;
    @Override
    public boolean tryLock(String key, long waitSeconds, long leaseSeconds) {
        RLock lock = redisson.getLock("lock:" + key);
        try {
            return lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("tryLock 被中断, key:{}", key, e);
            return false;
        }
    }

    @Override
    public void lock(String key, long leaseSeconds) {
        RLock lock = redisson.getLock("lock:" + key);
        lock.lock(leaseSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void unlock(String key) {
        RLock lock = redisson.getLock("lock:" + key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isHeldByCurrentThread(String key) {
        RLock lock = redisson.getLock("lock:" + key);
        return lock.isHeldByCurrentThread();
    }
}