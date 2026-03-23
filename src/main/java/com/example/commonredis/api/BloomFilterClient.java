package com.example.commonredis.api;

/**
 * 分布式布隆过滤器（纯存在性判断，防穿透）
 */
public interface BloomFilterClient {

    /**
     * 初始化布隆过滤器（项目启动时调用）
     * 注意：已存在则不会重复创建
     * @param filterName 过滤器名称（如 course:id:bloom）
     * @param expectedInsertions 预计数据量
     * @param fpp 误判率（如 0.01）
     */
    void initFilter(String filterName, long expectedInsertions, double fpp);

    /**
     * 判断值是否可能存在
     * @param filterName 过滤器名称
     * @param value 判断的值（如 id）
     * @return true=可能存在 false=一定不存在
     */
    boolean mightContain(String filterName, Object value);

    /**
     * 向布隆过滤器添加值
     * @param filterName 过滤器名称
     * @param value 要添加的值
     */
    void put(String filterName, Object value);
}