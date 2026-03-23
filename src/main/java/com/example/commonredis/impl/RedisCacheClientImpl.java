package com.example.commonredis.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.commonredis.api.BloomFilterClient;
import com.example.commonredis.api.CacheClient;
import com.example.commonredis.api.LockClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("CacheClient")
@RequiredArgsConstructor
public class RedisCacheClientImpl implements CacheClient {

    private final StringRedisTemplate redis;
    private final BloomFilterClient bloomFilterClient;
    private final LockClient lockClient;

    public static final String NULL_VALUE = "NULL_DATA";

    // ====================== 基础 get/set ======================

    @Override
    public <T> T get(String key, Class<T> clazz) {
        String val = redis.opsForValue().get(key);
        if (StrUtil.isBlank(val)) return null;
        if (isNullValue(val)) return null;
        return JSONUtil.toBean(val, clazz);
    }

    @Override
    public <T> List<T> getAll(String key, Class<T> clazz){
        String val = redis.opsForValue().get(key);
        if(StrUtil.isBlank(val)||isNullValue(val)) return Collections.emptyList();
        return JSONUtil.toList(val, clazz);
    }

    @Override
    public void set(String key, Object value) {
        redis.opsForValue().set(key, JSONUtil.toJsonStr(value));
    }

    @Override
    public void setWithExpire(String key, Object value, int minutes) {
        redis.opsForValue().set(key, JSONUtil.toJsonStr(value), minutes, TimeUnit.MINUTES);
    }

    // ====================== 随机过期（防雪崩） ======================

    @Override
    public void setWithRandomExpire(String key, Object value, int baseMin, int randomRange) {
        int offset = ThreadLocalRandom.current().nextInt(-randomRange, randomRange + 1);
        int realMin = Math.max(baseMin + offset, 1);
        redis.opsForValue().set(key, JSONUtil.toJsonStr(value), realMin, TimeUnit.MINUTES);
    }

    // ====================== 空值缓存（防穿透） ======================

    @Override
    public void setNullValue(String key, int minutes) {
        redis.opsForValue().set(key, NULL_VALUE, minutes, TimeUnit.MINUTES);
    }

    @Override
    public boolean isNullValue(String value) {
        return NULL_VALUE.equals(value);
    }

    // ====================== 删除 ======================

    @Override
    public void delete(String key) {
        redis.delete(key);
    }
}