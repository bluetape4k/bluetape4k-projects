# Module bluetape4k-cache-redisson

`bluetape4k-cache-redisson`은 Redisson 기반 JCache Provider, Coroutines 캐시 구현, 그리고 **Caffeine + Redisson 2-Tier Near Cache**를 제공합니다.

> 기존 `bluetape4k-cache-redisson-near` 모듈이 이 모듈에 통합되었습니다.

## 제공 기능

- **Redisson JCache Provider** (`org.redisson.jcache.JCachingProvider`)
- **Redisson Near Cache Provider** (`RedissonNearCachingProvider`)
- **`RedissonSuspendCache`**: JCache 기반 코루틴 캐시
- **`RedissonNearCache`**: Caffeine(로컬) + Redisson(분산) 2-Tier Near Cache
- **`RedissonNearSuspendCache`**: Near Cache 코루틴 래퍼

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-redisson:${bluetape4kVersion}")
}
```

## 사용 예시

### 1. RedissonSuspendCache

```kotlin
import io.bluetape4k.cache.jcache.coroutines.RedissonSuspendCache
import io.bluetape4k.cache.jcache.jcacheConfiguration

val config = jcacheConfiguration<String, Any> { }
val suspendCache = RedissonSuspendCache("redis-cache", redissonClient, config)

suspendCache.put("key", "value")
val value = suspendCache.get("key")
```

### 2. Redisson Near Cache (2-Tier)

```kotlin
import io.bluetape4k.cache.nearcache.redis.RedissonNearCache
import io.bluetape4k.cache.nearcache.redis.RedisNearCacheConfig

val nearConfig = RedisNearCacheConfig<String, Any>()
val nearCache = RedissonNearCache<String, Any>("redis-near", redissonClient, nearConfig)

nearCache.put("key", "value")
val value = nearCache.get("key")  // 로컬 Caffeine에서 우선 조회
```

### 3. RedissonNearSuspendCache (코루틴)

```kotlin
import io.bluetape4k.cache.nearcache.redis.coroutines.RedissonNearSuspendCache

val nearSuspend = RedissonNearSuspendCache<String, Any>("redis-near-suspend", redissonClient)
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
spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.redis.RedissonNearCachingProvider
```

### 5. Spring `@Cacheable`과 함께 사용

```kotlin
@Service
class UserService {
    @Cacheable("users")
    fun findUser(id: Long): User = ...
}
```

## CachingProvider 등록 목록

`META-INF/services/javax.cache.spi.CachingProvider`에 등록된 Provider:

```
org.redisson.jcache.JCachingProvider
io.bluetape4k.cache.nearcache.redis.RedissonNearCachingProvider
```

클래스패스에 여러 Provider가 공존할 때는 명시적으로 지정하세요:

```kotlin
val provider = Caching.getCachingProvider("io.bluetape4k.cache.nearcache.redis.RedissonNearCachingProvider")
```
