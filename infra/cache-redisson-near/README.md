# bluetape4k-cache-redisson-near

`bluetape4k-cache-redisson-near`는 Redisson Back Cache와 Near Cache 조합을 위한 모듈입니다.

핵심 제공 요소:
- `RedisNearCachingProvider`
- `RedisNearCacheManager`, `RedisNearCacheConfig`
- `RedissonNearCache`, `RedissonNearSuspendCache` 팩토리

기본 Front는 Caffeine 기반이며, 사용자 지정 Front cache/suspendCache를 사용할 수 있습니다.

## 설치

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-redisson-near:${bluetape4kVersion}")
}
```

## CachingProvider 사용

`META-INF/services/javax.cache.spi.CachingProvider`에 아래 구현이 등록됩니다.
- `io.bluetape4k.cache.nearcache.redis.RedisNearCachingProvider`

Spring Boot JCache 설정 예:

```properties
spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.redis.RedisNearCachingProvider
```

## 사용 예시

### 1. NearCache 생성 (동기 API)

```kotlin
import io.bluetape4k.cache.nearcache.redis.RedissonNearCache

val near = RedissonNearCache<String, Any>(
    backCacheName = "users-near",
    redisson = redissonClient,
)
```

### 2. NearSuspendCache 생성 (기본 Front=Caffeine)

```kotlin
import io.bluetape4k.cache.nearcache.redis.RedissonNearSuspendCache

val nearSuspend = RedissonNearSuspendCache<String, Any>(
    backCacheName = "users-near-suspend",
    redisson = redissonClient,
)
```

### 3. NearSuspendCache 생성 (사용자 Front 지정)

```kotlin
val nearSuspend = RedissonNearSuspendCache(
    backCacheName = "users-near-suspend",
    redisson = redissonClient,
    frontSuspendCache = myFrontSuspendCache,
)
```
