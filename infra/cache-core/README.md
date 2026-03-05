# Module bluetape4k-cache-core

`bluetape4k-cache-core`는 캐시 기능의 공통 API, 핵심 추상화, 그리고 **로컬 캐시 구현체**를 제공하는 모듈입니다.

> 기존 `bluetape4k-cache-local` 모듈이 이 모듈에 통합되었습니다.

## 제공 기능

- **JCache 공통 유틸리티**: `JCaching`, `jcacheManager`, `jcacheConfiguration` 등
- **Coroutines 캐시 추상화**: `SuspendCache`, `SuspendCacheEntry`
- **Near Cache 공통 구현**: `NearCache`, `NearSuspendCache`, `NearCacheConfig`
- **Memorizer 추상화**: `Memorizer`, `AsyncMemorizer`, `SuspendMemorizer`
- **로컬 캐시 Provider** (구 `cache-local` 통합):
  - **Caffeine**: `CaffeineSupport`, `CaffeineSuspendCache`, `CaffeineMemorizer`
  - **Cache2k**: `Cache2kSupport`, `Cache2kMemorizer`
  - **Ehcache**: `EhcacheSupport`, `EhCacheMemorizer`

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-core:${bluetape4kVersion}")
}
```

분산 캐시가 필요하면 해당 Provider 모듈을 추가합니다.

## 기본 사용 예시

### 1. Caffeine 로컬 캐시

```kotlin
import io.bluetape4k.cache.caffeine.caffeine
import com.github.benmanes.caffeine.cache.Cache

val cache: Cache<String, Any> = caffeine {
    maximumSize(1_000)
    expireAfterWrite(10, TimeUnit.MINUTES)
}.build()
```

### 2. CaffeineSuspendCache

```kotlin
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache

val suspendCache = CaffeineSuspendCache<String, Any>("local-cache")
suspendCache.put("key", "value")
val value = suspendCache.get("key")
```

### 3. JCache 유틸리티

```kotlin
import io.bluetape4k.cache.jcache.jcacheConfiguration

val config = jcacheConfiguration<String, String> {
    isStatisticsEnabled = true
    isManagementEnabled = true
}
```

### 4. NearCache 공통 구성

```kotlin
import io.bluetape4k.cache.nearcache.NearCacheConfig

val nearConfig = NearCacheConfig<String, Any>(
    isSynchronous = false,
    checkExpiryPeriod = 30_000L,
)
```

### 5. Caffeine Memorizer

```kotlin
import io.bluetape4k.cache.memorizer.caffeine.CaffeineMemorizer

val factorial = CaffeineMemorizer<Int, Long> { n ->
    (1..n).fold(1L) { acc, i -> acc * i }
}

val result = factorial[10]  // 캐싱되어 반복 계산 방지
```

## 권장 사용 방식

| 사용 목적 | 권장 모듈 |
|-----------|-----------|
| 로컬 캐시(Caffeine/Cache2k/Ehcache) | `bluetape4k-cache-core` |
| Hazelcast 분산 캐시 + Near Cache | `bluetape4k-cache-hazelcast` |
| Ignite 분산 캐시 + Near Cache | `bluetape4k-cache-ignite` |
| Redisson 분산 캐시 + Near Cache | `bluetape4k-cache-redisson` |
| 전체 Provider 일괄 사용 | `bluetape4k-cache` (umbrella) |
