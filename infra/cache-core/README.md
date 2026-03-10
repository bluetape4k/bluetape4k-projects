# Module bluetape4k-cache-core

`bluetape4k-cache-core`는 캐시 기능의 공통 API, 핵심 추상화, 그리고 **로컬 캐시 구현체**를 제공하는 모듈입니다.

> 기존 `bluetape4k-cache-local` 모듈이 이 모듈에 통합되었습니다.

## 제공 기능

- **JCache 공통 유틸리티**: `JCaching`, `jcacheManager`, `jcacheConfiguration` 등
- **Coroutines 캐시 추상화**: `SuspendCache`, `SuspendCacheEntry`
- **Near Cache 공통 구현**: `NearCache`, `NearSuspendCache`, `NearCacheConfig`
- **Memorizer 추상화**: `Memorizer`, `AsyncMemorizer`, `SuspendMemorizer` (구 인터페이스)
- **Memoizer 추상화**: `Memoizer`, `AsyncMemoizer`, `SuspendMemoizer` (신 인터페이스)
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

## 제공 기능 (상세)

### Resilient NearCache (write-behind + retry + graceful degradation)

`ResilientNearCache` / `ResilientSuspendNearCache` 는 JCache(sync) 또는 SuspendCache(coroutine) 기반 back cache에 대해
**write-behind + Resilience4j retry + GetFailureStrategy** 패턴을 제공하는 2-Tier Near Cache입니다.

| 클래스 | 설명 |
|---|---|
| `BackCacheCommand<K,V>` | Put / PutAll / Remove / RemoveAll / ClearBack 커맨드 sealed interface |
| `GetFailureStrategy` | back cache GET 실패 시 동작 전략 enum (RETURN_FRONT_OR_NULL / PROPAGATE_EXCEPTION) |
| `ResilientNearCacheConfig<K,V>` | write-behind 큐 용량, retry 설정, GetFailureStrategy 포함 config |
| `ResilientNearCache<K,V>` | 동기(Blocking) Resilient NearCache. LinkedBlockingQueue + virtualThread consumer |
| `ResilientSuspendNearCache<K,V>` | 코루틴 Resilient NearCache. Channel + scope.launch consumer |

**아키텍처**:
```
Application
    |
[ResilientNearCache / ResilientSuspendNearCache]
    |
+---+----------+
|              |
Front          Write Queue (LinkedBlockingQueue / Channel)
Caffeine           |
(즉시 반영)    Consumer (virtualThread / coroutine)
               (retry { backCache.put/remove })
```

**주요 특성**:
- **write-behind**: put/remove는 front cache에 즉시 반영, back cache 쓰기는 비동기 큐/채널로 처리
- **tombstones**: remove 후 write-behind 완료 전 stale read 방지용 tombstone 집합
- **clearPending**: clearAll 호출 후 ClearBack이 처리될 때까지 back cache read 차단
- **retry**: Resilience4j Retry로 back cache 쓰기 실패 시 재시도 (지수 백오프 옵션)
- **GetFailureStrategy**: back cache GET 실패 시 null 반환(RETURN_FRONT_OR_NULL) 또는 예외 전파(PROPAGATE_EXCEPTION)

---

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
import io.bluetape4k.cache.jcache.CaffeineSuspendCache

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

### 5. ResilientNearCache (write-behind + retry)

```kotlin
import io.bluetape4k.cache.nearcache.ResilientNearCache
import io.bluetape4k.cache.nearcache.ResilientNearCacheConfig
import io.bluetape4k.cache.jcache.JCaching
import java.time.Duration

val backCache = JCaching.Caffeine.getOrCreate<String, String>("back-cache")

val cache = ResilientNearCache(
    backCache = backCache,
    config = ResilientNearCacheConfig(
        retryMaxAttempts = 3,
        retryWaitDuration = Duration.ofMillis(200),
        retryExponentialBackoff = true,
        writeQueueCapacity = 1024,
    ),
)

cache.put("key", "value")          // front 즉시 반영, back는 write-behind
cache.get("key")                   // front hit → 즉시 반환
cache.remove("key")                // front 즉시 삭제, back는 write-behind
cache.clearAll()                   // front 즉시 초기화, back는 write-behind
cache.close()
```

### 6. ResilientSuspendNearCache (코루틴)

```kotlin
import io.bluetape4k.cache.nearcache.ResilientSuspendNearCache
import io.bluetape4k.cache.jcache.CaffeineSuspendCache

val backCache = CaffeineSuspendCache<String, String> { maximumSize(10_000) }

val cache = ResilientSuspendNearCache(backCache = backCache)

// suspend 함수로 사용
cache.put("key", "value")
val value = cache.get("key")
cache.close()
```

### 7. Caffeine Memorizer

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

## testFixtures 활용 가이드

`bluetape4k-cache-core`는 분산 캐시 Provider 구현을 위한 **6개의 추상 테스트 클래스**를 `testFixtures`로 제공합니다.

### 추상 테스트 클래스 목록

| 클래스 | 패키지 | 설명 |
|--------|--------|------|
| `AbstractSuspendCacheTest` | `jcache` | `SuspendCache` 기본 CRUD + 동시성 검증 |
| `AbstractNearCacheTest` | `nearcache` | `NearCache` write-through/event 전파 검증 |
| `AbstractSuspendNearCacheTest` | `nearcache` | `SuspendNearCache` coroutines 검증 |
| `AbstractMemorizerTest` | `memorizer` | `Memorizer` 단일 계산 보장 |
| `AbstractAsyncMemorizerTest` | `memorizer` | `AsyncMemorizer` CompletableFuture 검증 |
| `AbstractSuspendMemorizerTest` | `memorizer` | `SuspendMemorizer` suspend 검증 |

### Provider별 호환성 매트릭스

| testFixtures | Hazelcast | Ignite2 | Redisson | Lettuce |
|---|:---:|:---:|:---:|:---:|
| `AbstractSuspendCacheTest` | ✅ | ✅ | ✅ | ✅ |
| `AbstractNearCacheTest` | ✅ | ✅ | ✅ | N/A(아키텍처 상이) |
| `AbstractSuspendNearCacheTest` | ✅ | ✅ | ✅ | N/A(아키텍처 상이) |
| `AbstractMemorizerTest` 3종 | N/A | N/A | N/A | N/A |

> Memorizer는 로컬 캐시 전용 패턴으로 분산 캐시 모듈에는 해당 없음

### 새 Provider에서 testFixtures 사용하기

```kotlin
// build.gradle.kts
dependencies {
    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
}
```

```kotlin
// 새 Provider NearCache 테스트 예시
class MyProviderNearCacheTest : AbstractNearCacheTest() {

    override val backCache: JCache<String, Any> by lazy {
        MyProviderJCaching.getOrCreate("my-back-cache-" + UUID.randomUUID().encodeBase62())
    }
}
```
