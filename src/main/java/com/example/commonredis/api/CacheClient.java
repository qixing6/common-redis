package com.example.commonredis.api;

import java.util.List;

/**
 * 智能缓存客户端（内置：防穿透、防击穿、防雪崩、空值、随机过期）
 */
public interface CacheClient {

    // ====================== 基础缓存 ======================

    /**
     * 获取缓存并自动反序列化
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 获取缓存（字符串原样返回，业务层自己处理反序列化）
     */
    <T> List<T> getAll(String key, Class<T> clazz);

    /**
     * 普通存缓存（使用默认过期时间，配置里定义）
     */
    void set(String key, Object value);

    /**
     * 存缓存，指定固定过期时间
     * @param minutes 分钟
     */
    void setWithExpire(String key, Object value, int minutes);

    // ====================== 防雪崩：随机过期 ======================

    /**
     * 存缓存，带随机过期（避免雪崩）
     * @param baseMinutes 基础时间
     * @param randomRange 随机波动范围（±范围）
     */
    void setWithRandomExpire(String key, Object value, int baseMinutes, int randomRange);

    // ====================== 防穿透：空值缓存 ======================

    /**
     * 缓存空值（数据库不存在的数据）
     * @param minutes 过期时间（建议短一点，如 2~5 分钟）
     */
    void setNullValue(String key, int minutes);

    // ====================== 删除 ======================

    /**
     * 删除缓存
     */
    void delete(String key);

    // ====================== 内部工具方法（业务一般不用） ======================

    /**
     * 判断是否是空值缓存标记
     */
    boolean isNullValue(String value);
}