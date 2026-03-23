# common-base
> 通用基础组件 + Redis/锁/布隆过滤器封装组件，为高并发业务系统提供标准化、低耦合的底层能力支撑

## 📋 项目简介
`common-base` 是一套自研的通用组件库，包含**基础工具封装**和**Redis生态组件封装**两大核心模块，旨在解决业务系统中重复造轮子、底层逻辑耦合、代码不规范等问题；本组件已在「大学抢课秒杀系统」中落地验证，大幅提升业务代码整洁度和可维护性。

## 🎯 设计目标
- **解耦**：将Redis、分布式锁、布隆过滤器等底层操作与业务逻辑分离，业务层仅关注核心场景；
- **标准化**：统一缓存/锁/布隆过滤器的调用方式，降低团队协作成本；
- **健壮性**：内置异常处理、线程中断恢复、空值校验等兜底逻辑，避免低级BUG；
- **易用性**：极简API设计，一行代码完成缓存查询/锁获取/布隆过滤器校验。

## 📦 核心模块
### 1. Redis 缓存客户端（RedisCacheClientImpl）
封装StringRedisTemplate底层操作，提供标准化缓存能力，内置**空值缓存**、**随机过期**等防缓存问题特性。

#### 核心API
| 方法 | 功能 | 应用场景 |
|------|------|----------|
| `get(String key, Class<T> clazz)` | 缓存查询（自动反序列化） | 课程信息查询 |
| `getAll(String key, Class<T> clazz)` | 集合缓存查询 | 全量课程列表查询 |
| `setWithRandomExpire(key, value, baseMin, randomRange)` | 随机过期缓存写入 | 防缓存雪崩 |
| `setNullValue(key, minutes)` | 空值缓存写入 | 防缓存穿透 |
| `delete(key)` | 缓存删除 | 课程更新后清理缓存 |

#### 核心代码示例
```java
// 一行代码完成缓存查询（自动反序列化）
CourseVO courseVO = cacheClient.get("course:1001", CourseVO.class);

// 随机过期写入（基础30分钟，±5分钟浮动）
cacheClient.setWithRandomExpire("course:1001", courseVO, 30, 5);

// 空值缓存（防穿透）
cacheClient.setNullValue("course:9999", 10);
```

### 2. 布隆过滤器客户端（RedissonBloomFilterClientImpl）
封装Redisson布隆过滤器，提供初始化、校验、添加等标准化操作，是缓存穿透的第一道防线。

#### 核心API
| 方法 | 功能 | 应用场景 |
|------|------|----------|
| `initFilter(filterName, expectedInsertions, fpp)` | 初始化过滤器 | 系统启动时初始化课程ID过滤器 |
| `mightContain(filterName, value)` | 校验值是否存在 | 拦截无效课程ID查询请求 |
| `put(filterName, value)` | 添加值到过滤器 | 课程新增时同步添加到过滤器 |

#### 核心代码示例
```java
// 初始化课程ID过滤器（预计10万条数据，误判率0.01）
bloomFilterClient.initFilter("courseIdFilter", 100000, 0.01);

// 校验课程ID是否有效（拦截穿透请求）
if (!bloomFilterClient.mightContain("courseIdFilter", 9999)) {
    throw new BusinessException(404, "课程不存在");
}
```

### 3. 分布式锁客户端（RedissonLockClientImpl）
封装Redisson分布式锁，提供安全的锁获取/释放逻辑，内置线程中断恢复、锁持有校验等特性。

#### 核心API
| 方法 | 功能 | 应用场景 |
|------|------|----------|
| `tryLock(key, waitSeconds, leaseSeconds)` | 尝试获取锁（非阻塞） | 抢课/退课场景防超卖 |
| `lock(key, leaseSeconds)` | 阻塞获取锁 | 必须执行的核心操作 |
| `unlock(key)` | 释放锁（校验当前线程持有） | 锁使用完成后释放 |
| `isHeldByCurrentThread(key)` | 校验当前线程是否持有锁 | 兜底释放锁前校验 |

#### 核心代码示例
```java
// 尝试获取课程抢课锁（最多等3秒，自动释放10秒）
String lockKey = "course:select:1001";
if (lockClient.tryLock(lockKey, 3, 10)) {
    try {
        // 核心抢课逻辑
    } finally {
        lockClient.unlock(lockKey);
    }
}
```

## 🛠 技术依赖
- 核心框架：Spring Boot
- Redis客户端：StringRedisTemplate、Redisson
- 工具类：Hutool（JSON/字符串处理）
- 其他：Lombok（简化代码）、Slf4j（日志）

## 🚀 快速集成
### 1. 引入依赖
```xml
<!-- 如需发布到Maven私服，添加以下依赖 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>common-base</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置Redis/Redisson
```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0
```

### 3. 注入使用
```java
@Service
@RequiredArgsConstructor
public class CourseQueryServiceImpl {
    // 直接注入封装的客户端
    private final CacheClient cacheClient;
    private final BloomFilterClient bloomFilterClient;
    private final LockClient lockClient;
    
    // 业务方法中调用
    public CourseVO getCourseById(Long id) {
        // 1. 布隆过滤器防穿透
        if (!bloomFilterClient.mightContain("courseIdFilter", id)) {
            throw new BusinessException(404, "课程不存在");
        }
        // 2. 缓存查询
        CourseVO courseVO = cacheClient.get("course:" + id, CourseVO.class);
        if (courseVO != null) {
            return courseVO;
        }
        // 后续逻辑...
    }
}
```

## ✨ 核心优势
### 1. 代码解耦，业务聚焦
```java
// 封装前：耦合Redisson/StringRedisTemplate底层API
RBloomFilter<Long> filter = redisson.getBloomFilter("courseIdFilter");
String json = redis.opsForValue().get("course:1001");
RLock lock = redisson.getLock("lock:course:select:1001");

// 封装后：仅关注业务逻辑，无需关心底层实现
bloomFilterClient.mightContain("courseIdFilter", 1001);
cacheClient.get("course:1001", CourseVO.class);
lockClient.tryLock("course:select:1001", 3, 10);
```

### 2. 内置防坑逻辑
- 缓存空值自动识别，避免空指针；
- 随机过期时间自动计算（保底1分钟），防缓存雪崩；
- 锁释放前校验当前线程持有，避免误解锁；
- 线程中断后恢复中断状态，符合线程规范。

### 3. 可扩展性强
- 基于接口（CacheClient/LockClient/BloomFilterClient）封装，可无缝替换底层实现（如Redis缓存→Memcached）；
- 新增缓存/锁/布隆过滤器特性时，仅需修改组件层，业务层无感知。

## 📝 最佳实践
1. **缓存使用**：查询场景优先走缓存，更新/删除场景同步清理缓存，空结果写入空值缓存；
2. **锁使用**：抢课/退课等写操作必须加分布式锁，锁粒度控制到“课程级”，避免全表锁；
3. **布隆过滤器**：系统启动时初始化并预热数据，新增数据时同步添加到过滤器；
4. **异常处理**：组件层捕获底层异常并转化为业务异常，统一返回格式。

## 📄 许可证
MIT License

## 📞 关联项目
- 大学抢课秒杀系统：https://github.com/qixing6/university-course-selection-system
- 组件使用示例：详见抢课系统中`CourseQueryServiceImpl`/`CourseCommandServiceImpl`
