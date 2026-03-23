package com.example.commonredis.impl;

import com.example.commonredis.api.BloomFilterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonBloomFilterClientImpl implements BloomFilterClient {

    private final RedissonClient redisson;

    @Override
    public void initFilter(String filterName, long expectedInsertions, double fpp) {
       RBloomFilter<Object> filter = redisson.getBloomFilter(filterName);
        if (!filter.isExists()) {
            filter.tryInit(expectedInsertions, fpp);
            log.info("布隆过滤器 [{}] 初始化完成，expected={}, fpp={}",
                    filterName, expectedInsertions, fpp);
        }
    }

    @Override
    public boolean mightContain(String filterName, Object value) {
        RBloomFilter<Object> filter = redisson.getBloomFilter(filterName);
        return filter.contains(value);
    }

    @Override
    public void put(String filterName, Object value) {
        RBloomFilter<Object> filter = redisson.getBloomFilter(filterName);
        filter.add(value);
    }
}