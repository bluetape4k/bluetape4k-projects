# bluetape4k-cache-core

`bluetape4k-cache-core`는 캐시 기능의 공통 API와 핵심 추상화를 제공하는 모듈입니다.

이 모듈은 다음을 담당합니다.
- JCache 공통 유틸리티 (`JCaching`, `jcacheManager`, `jcacheConfiguration` 등)
- Coroutines 캐시 추상화 (`SuspendCache`, `SuspendCacheEntry`)
- Near Cache 공통 구현 (`NearCache`, `NearSuspendCache`, `NearCacheConfig`)
- Memorizer 공통 추상화 (`Memorizer`, `AsyncMemorizer`, `SuspendMemorizer`)

## 무엇이 core에 있고, 무엇이 없는가

`cache-core`는 공통 코드 중심 모듈입니다.
- 포함: API/공통 구현
- 미포함: 각 Provider별 구현체(예: Redisson/Hazelcast/Ignite의 전용 구현)

Provider별 기능은 별도 모듈(`bluetape4k-cache-local`, `bluetape4k-cache-redisson` 등)을 함께 사용해야 합니다.

## 설치

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cache-core:${bluetape4kVersion}")
}
```

일반적으로는 provider 모듈을 함께 의존합니다.

## 기본 사용 예시

### 1. JCache 유틸리티

```kotlin
import io.bluetape4k.cache.jcache.jcacheConfiguration

val config = jcacheConfiguration<String, String> {
    isStatisticsEnabled = true
    isManagementEnabled = true
}
```

### 2. NearCache 공통 구성

```kotlin
import io.bluetape4k.cache.nearcache.NearCacheConfig

val nearConfig = NearCacheConfig<String, Any>(
    isSynchronous = false,
    checkExpiryPeriod = 30_000L,
)
```

### 3. SuspendCache 공통 인터페이스

```kotlin
import io.bluetape4k.cache.jcache.coroutines.SuspendCache

suspend fun <K: Any, V: Any> putAndGet(cache: SuspendCache<K, V>, key: K, value: V): V? {
    cache.put(key, value)
    return cache.get(key)
}
```

## 권장 사용 방식

- 단일 Provider만 쓸 경우: 해당 Provider 모듈 + `cache-core` 조합 사용
- 여러 Provider를 한 번에 쓸 경우: Umbrella 모듈(`bluetape4k-cache`) 사용
- Near 전용 조합이 필요할 경우: `*-near` 모듈 사용
