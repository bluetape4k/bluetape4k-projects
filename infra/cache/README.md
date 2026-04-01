# Module bluetape4k-cache

`bluetape4k-cache`는 캐시 관련 모듈을 한 번에 묶어 쓰기 위한 Umbrella 모듈입니다.

> 캐시 모듈이 10개에서 5개+umbrella로 통합되었습니다. 각 분산 캐시 모듈에 Near Cache 기능이 포함되어 있습니다.

## 모듈 구성

| 모듈                           | 제공 기능                                                       |
|------------------------------|-------------------------------------------------------------|
| `bluetape4k-cache-core`      | JCache 추상화 + Caffeine/Cache2k/Ehcache 로컬 캐시 + Memorizer     |
| `bluetape4k-cache-hazelcast` | Hazelcast 분산 캐시 + Near Cache (구 `cache-hazelcast-near` 통합)  |
| `bluetape4k-cache-redisson`  | Redisson 분산 캐시 + Near Cache (구 `cache-redisson-near` 통합)    |
| `bluetape4k-cache-lettuce`   | Lettuce(Redis) 분산 캐시 + Near Cache                           |

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache:${bluetape4kVersion}")
}
```

모든 Provider가 포함되므로, 특정 Provider만 필요한 경우 해당 모듈을 직접 의존하는 것을 권장합니다.

## 선택 의존 권장 예시

### 1. 로컬 캐시만 필요

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-core:${bluetape4kVersion}")
}
```

### 2. Redisson 분산 캐시 + Near Cache

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-redisson:${bluetape4kVersion}")
}
```

### 3. Hazelcast 분산 캐시 + Near Cache

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-hazelcast:${bluetape4kVersion}")
}
```

## 빠른 시작

### 1. Caffeine 로컬 캐시

```kotlin
import io.bluetape4k.cache.jcache.JCaching

val cache = JCaching.Caffeine.getOrCreate<String, Any>("users")
cache.put("u:1", mapOf("name" to "debop"))
```

### 2. Hazelcast Near Cache (코루틴)

```kotlin
import io.bluetape4k.cache.nearcache.hazelcast.coroutines.HazelcastNearSuspendCache

val near = HazelcastNearSuspendCache<String, Any>("hz-users-near", hazelcastInstance)
near.put("key", "value")
val value = near.get("key")
```

### 3. Redisson Near Cache (코루틴)

```kotlin
import io.bluetape4k.cache.nearcache.redis.coroutines.RedissonNearSuspendCache

val near = RedissonNearSuspendCache<String, Any>("redis-users-near", redissonClient)
near.put("key", "value")
val value = near.get("key")
```

## 모듈 의존성 구조

```mermaid
flowchart TD
    A[bluetape4k-cache<br/>umbrella] --> B[bluetape4k-cache-core<br/>JCache 추상화 + 로컬 캐시]
    A --> C[bluetape4k-cache-hazelcast<br/>Hazelcast 분산 캐시 + NearCache]
    A --> D[bluetape4k-cache-redisson<br/>Redisson 분산 캐시 + NearCache]
    A --> E[bluetape4k-cache-lettuce<br/>Lettuce Redis 분산 캐시 + NearCache]

    B --> B1[Caffeine 로컬 캐시]
    B --> B2[Cache2k 로컬 캐시]
    B --> B3[Ehcache 로컬 캐시]
    B --> B4[NearCacheOperations 인터페이스]
    B --> B5[SuspendNearCacheOperations 인터페이스]

    C --> C1[HazelcastNearCache]
    C --> C2[HazelcastSuspendNearCache]

    D --> D1[RedissonNearCache<br/>RLocalCachedMap 기반]
    D --> D2[RedissonSuspendNearCache]
    D --> D3[RedissonResp3NearCache<br/>RESP3 하이브리드]

    E --> E1[LettuceNearCache<br/>RESP3 CLIENT TRACKING]
    E --> E2[LettuceSuspendNearCache]

    style A fill:#4a90d9
    style B fill:#5ba85a
    style C fill:#e07b39
    style D fill:#9b59b6
    style E fill:#c0392b
```

## Near Cache 2-Tier 아키텍처

```mermaid
flowchart LR
    App[애플리케이션] -->|get| LocalCache[로컬 캐시<br/>Caffeine/Cache2k]
    LocalCache -->|캐시 히트| App
    LocalCache -->|캐시 미스| RemoteCache[원격 캐시<br/>Redis / Hazelcast]
    RemoteCache -->|데이터 반환| LocalCache
    App -->|put| RemoteCache
    RemoteCache -->|Invalidation 전파| LocalCache

    style LocalCache fill:#5ba85a
    style RemoteCache fill:#c0392b
    style App fill:#4a90d9
```

## CachingProvider 자동 로딩 주의

여러 모듈이 `META-INF/services/javax.cache.spi.CachingProvider`를 등록합니다. Umbrella 모듈 사용 시 Provider를 명시적으로 지정하세요:

```kotlin
import javax.cache.Caching

val provider = Caching.getCachingProvider("io.bluetape4k.cache.nearcache.redis.RedissonNearCachingProvider")
val manager = provider.cacheManager
```

Spring Boot에서는 `application.properties`로 지정:

```properties
spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.redis.RedissonNearCachingProvider
```
