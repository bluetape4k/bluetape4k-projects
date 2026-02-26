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

## 동작 시퀀스

### 1. Read-through (Front miss -> Back hit)

```mermaid
sequenceDiagram
    participant A as App
    participant F as Front Cache
    participant B as Back Cache (Redisson)
    A->>F: get(key)
    F-->>A: miss
    A->>B: get(key)
    B-->>A: value
    A->>F: put(key, value)
    F-->>A: return value
```

### 2. Write-through + 전파

```mermaid
sequenceDiagram
    participant A as NearCache-A
    participant B as Back Cache (Redisson)
    participant C as NearCache-B Front
    A->>A: put(key, value)
    A->>B: write-through put(key, value)
    B-->>C: cache entry event (created/updated)
    C->>C: front cache sync
```

### 3. Back 만료 -> Front 무효화

```mermaid
sequenceDiagram
    participant T as Expiry Checker / Listener
    participant B as Back Cache (Redisson)
    participant F as Front Cache
    T->>B: check key state
    B-->>T: expired or missing
    T->>F: remove(key)
```
