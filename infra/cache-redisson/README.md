# Module bluetape4k-cache-redisson

`bluetape4k-cache-redisson`은 Redisson 기반 JCache Provider, Coroutines 캐시 구현, 그리고 **Caffeine + Redisson 2-Tier Near Cache**를 제공합니다.

> 기존 `bluetape4k-cache-redisson-near` 모듈이 이 모듈에 통합되었습니다.

## 제공 기능

| 클래스 | 설명 |
|---|---|
| `org.redisson.jcache.JCachingProvider` | Redisson JCache Provider |
| `RedissonNearCachingProvider` | Redisson Near Cache JCache Provider |
| `RedissonSuspendCache` | JCache 기반 코루틴 캐시 |
| `RedissonNearCache` | Caffeine(front) + Redisson(back) 2-Tier Near Cache |
| `RedissonNearSuspendCache` | Near Cache 코루틴 래퍼 |
| `RedissonResp3NearCache<V>` | Redisson(데이터) + Lettuce RESP3(invalidation) 하이브리드 NearCache (write-through) |
| `RedissonResp3SuspendNearCache<V>` | RESP3 하이브리드 NearCache 코루틴 구현 (write-through) |
| `ResilientRedissonResp3NearCache<V>` | RESP3 하이브리드 + write-behind + retry (동기) |
| `ResilientRedissonResp3SuspendNearCache<V>` | RESP3 하이브리드 + write-behind + retry (코루틴) |
| `RedissonResp3NearCacheConfig` | RESP3 NearCache 설정 |
| `ResilientRedissonResp3NearCacheConfig` | Resilient RESP3 NearCache 추가 설정 |

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-redisson:${bluetape4kVersion}")
}
```

## 사용 예시

### 1. RedissonSuspendCache

```kotlin
import io.bluetape4k.cache.jcache.RedissonSuspendCache
import io.bluetape4k.cache.jcache.jcacheConfiguration

val config = jcacheConfiguration<String, Any> { }
val suspendCache = RedissonSuspendCache("redis-cache", redissonClient, config)

suspendCache.put("key", "value")
val value = suspendCache.get("key")
```

### 2. Redisson Near Cache (2-Tier)

```kotlin
import io.bluetape4k.cache.nearcache.RedissonNearCache
import io.bluetape4k.cache.nearcache.RedisNearCacheConfig

val nearConfig = RedisNearCacheConfig<String, Any>()
val nearCache = RedissonNearCache<String, Any>("redis-near", redissonClient, nearConfig)

nearCache.put("key", "value")
val value = nearCache.get("key")  // 로컬 Caffeine에서 우선 조회
```

### 3. RedissonSuspendNearCache (코루틴)

```kotlin
import io.bluetape4k.cache.nearcache.RedissonSuspendNearCache

val nearSuspend = RedissonSuspendNearCache<String, Any>("redis-near-suspend", redissonClient)
nearSuspend.put("key", "value")
val value = nearSuspend.get("key")
```

### 4. Spring Boot 설정 (Near Cache Provider 사용)

```kotlin
@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(redissonClient: RedissonClient): CacheManager {
        val config = RedisNearCacheConfig<String, Any>()
        return RedisNearCacheManager(redissonClient, config)
    }
}
```

또는 `application.properties`:

```properties
spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.RedissonNearCachingProvider
```

### 5. Spring `@Cacheable`과 함께 사용

```kotlin
@Service
class UserService {
    @Cacheable("users")
    fun findUser(id: Long): User = ...
}
```

### 6. RESP3 NearCache (Redisson + Lettuce RESP3 하이브리드)

기존 JCache 기반 Near Cache의 bulk 연산 이벤트 미발행 버그를 해결한 하이브리드 구현입니다.
데이터 연산은 Redisson `RBucket`을 사용하고, invalidation은 Lettuce RESP3 CLIENT TRACKING push를 사용합니다.

```kotlin
import io.bluetape4k.cache.nearcache.RedissonResp3NearCache
import io.bluetape4k.cache.nearcache.RedissonResp3NearCacheConfig
import io.lettuce.core.RedisClient

val config = RedissonResp3NearCacheConfig(
    cacheName = "my-cache",
    maxLocalSize = 10_000,
    frontExpireAfterWrite = Duration.ofMinutes(30),
    redisTtl = null,               // Redis TTL (null = 만료 없음)
    useRespProtocol3 = true,       // RESP3 CLIENT TRACKING 활성화
)
val nearCache = RedissonResp3NearCache(
    redisson = redissonClient,
    redisClient = resp3LettuceClient, // RESP3 활성화된 Lettuce RedisClient
    config = config,
)

nearCache.put("key", "value")
nearCache.get("key")      // 로컬 Caffeine에서 우선 조회
nearCache.clearLocal()    // 로컬 캐시만 초기화 (Redis 유지)
nearCache.clearAll()      // 로컬 + Redis 모두 초기화
nearCache.close()
```

#### Suspend (코루틴) 버전

```kotlin
import io.bluetape4k.cache.nearcache.RedissonResp3SuspendNearCache

val suspendCache = RedissonResp3SuspendNearCache(
    redisson = redissonClient,
    redisClient = resp3LettuceClient,
    config = config,
)

suspendCache.put("key", "value")
val value = suspendCache.get("key")
```

#### RESP3 RedisClient 설정 예시

```kotlin
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.protocol.ProtocolVersion

val resp3Client = RedisClient.create("redis://localhost:6379").also { client ->
    client.options = ClientOptions.builder()
        .protocolVersion(ProtocolVersion.RESP3)
        .build()
}
```

#### 아키텍처

```
Application
    |
[RedissonResp3NearCache]
    |
+--------+--------+------------+
|        |        |            |
Front   Back    Tracking
Caffeine Redisson  Lettuce RESP3
(local) (RBucket)  (CLIENT TRACKING push)
```

- **Read**: front hit → return / front miss → Redisson GET + Lettuce tracking GET → populate → return
- **Write**: front put + Redisson SET (write-through)
- **Invalidation**: RESP3 CLIENT TRACKING push → 로컬 캐시 무효화

#### NOLOOP 동작 주의사항

Redisson 데이터 연결과 Lettuce tracking 연결은 서로 다른 연결이므로,
자신이 Redisson으로 쓴 키도 Lettuce tracking 연결에 invalidation이 전파될 수 있습니다.
이는 cache-lettuce의 단일 연결 방식과 다른 동작입니다.

### 7. ResilientRedissonResp3NearCache (write-behind + retry)

RESP3 하이브리드 NearCache에 write-behind + Resilience4j retry + graceful degradation 패턴을 추가한 구현입니다.

```kotlin
import io.bluetape4k.cache.nearcache.ResilientRedissonResp3NearCache
import io.bluetape4k.cache.nearcache.ResilientRedissonResp3NearCacheConfig
import io.bluetape4k.cache.nearcache.RedissonResp3NearCacheConfig
import java.time.Duration

val cache = ResilientRedissonResp3NearCache<String>(
    redisson = redissonClient,
    redisClient = resp3LettuceClient,
    config = ResilientRedissonResp3NearCacheConfig(
        base = RedissonResp3NearCacheConfig(cacheName = "orders"),
        retryMaxAttempts = 3,
        retryWaitDuration = Duration.ofMillis(200),
        writeQueueCapacity = 1024,
    ),
)

cache.put("key", "value")       // front 즉시 반영, Redis는 write-behind
cache.get("key")                // front hit → 즉시 반환
cache.localCacheSize()          // 로컬 Caffeine 크기
cache.backCacheSize()           // Redis 키 개수
cache.clearAll()                // front 즉시 초기화, Redis는 write-behind
cache.close()
```

### 8. ResilientRedissonResp3SuspendNearCache (코루틴)

```kotlin
import io.bluetape4k.cache.nearcache.ResilientRedissonResp3SuspendNearCache

val cache = ResilientRedissonResp3SuspendNearCache<String>(
    redisson = redissonClient,
    redisClient = resp3LettuceClient,
    config = ResilientRedissonResp3NearCacheConfig(
        base = RedissonResp3NearCacheConfig(cacheName = "sessions"),
    ),
)

// suspend 함수로 사용
cache.put("session-1", "token-abc")
val token = cache.get("session-1")
cache.close()
```

#### Resilient RESP3 아키텍처

```
Application
    |
[ResilientRedissonResp3NearCache]
    |
+---+----------+------------------+
|              |                  |
Front          Write Queue        Tracking
Caffeine       (LinkedBlocking    Lettuce RESP3
(즉시 반영)    Queue / Channel)   (CLIENT TRACKING push)
               |
               Consumer (virtualThread / coroutine)
               (retry { redisson.bucket.set/delete })
```

- **write-behind**: put/remove → front 즉시, Redis는 비동기 큐로 처리
- **tombstones + clearPending**: stale read 방지
- **retry**: Resilience4j Retry로 Redis 쓰기 실패 시 재시도
- **invalidation**: Lettuce RESP3 CLIENT TRACKING push → 로컬 캐시 무효화

#### ResilientRedissonResp3NearCacheConfig 옵션

| 옵션 | 기본값 | 설명 |
|---|---|---|
| `base` | `RedissonResp3NearCacheConfig()` | 기본 RESP3 NearCache 설정 |
| `writeQueueCapacity` | `1024` | write-behind 큐 최대 용량 |
| `retryMaxAttempts` | `3` | Redis 쓰기 최대 재시도 횟수 |
| `retryWaitDuration` | `500ms` | 재시도 대기 시간 |
| `retryExponentialBackoff` | `true` | 지수 백오프 사용 여부 |
| `getFailureStrategy` | `RETURN_FRONT_OR_NULL` | Redis GET 실패 시 동작 전략 |

## CachingProvider 등록 목록

`META-INF/services/javax.cache.spi.CachingProvider`에 등록된 Provider:

```
org.redisson.jcache.JCachingProvider
io.bluetape4k.cache.nearcache.RedissonNearCachingProvider
```

클래스패스에 여러 Provider가 공존할 때는 명시적으로 지정하세요:

```kotlin
val provider = Caching.getCachingProvider("io.bluetape4k.cache.nearcache.RedissonNearCachingProvider")
```
