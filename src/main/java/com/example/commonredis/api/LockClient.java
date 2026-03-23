package com.example.commonredis.api;

/**
 * 分布式锁客户端（纯锁能力，不感知缓存/业务）
 */
public interface LockClient {

    /**
     * 尝试获取锁，带等待时间 + 自动释放
     * @param key 锁key
     * @param waitSeconds 最大等待时间（秒）
     * @param leaseSeconds 锁自动释放时间（秒）
     * @return true=获取成功 false=失败
     */
    boolean tryLock(String key, long waitSeconds, long leaseSeconds);

    /**
     * 加锁（阻塞直到成功），带自动释放
     * @param key 锁key
     * @param leaseSeconds 锁自动释放时间（秒）
     */
    void lock(String key, long leaseSeconds);

    /**
     * 释放锁
     * 必须保证：加锁成功才调用，且只能由加锁线程解锁
     * @param key 锁key
     */
    void unlock(String key);

    /**
     * 判断当前线程是否持有该锁
     */
    boolean isHeldByCurrentThread(String key);
}