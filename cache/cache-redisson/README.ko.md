# Module bluetape4k-cache-redisson

[English](./README.md) | 한국어

`bluetape4k-cache-redisson`은 Redisson 기반 cache adapter를 bluetape4k cache API에 맞춰 제공합니다. 핵심 범위는 Redisson JCache 연동, coroutine 친화 wrapper, Redisson `RLocalCachedMap` 기반 near cache, Redis-backed memoizer입니다.

## 패키지 / import 안정성

cache 폴더 재편으로 소스 위치는 `cache/cache-redisson/`이 되었지만 Gradle 프로젝트 이름, Maven artifact,
Kotlin package는 유지됩니다.

- Gradle project: `:bluetape4k-cache-redisson`
- Maven artifact: `io.github.bluetape4k:bluetape4k-cache-redisson`
- Kotlin package root: `io.bluetape4k.cache`

이번 재편으로 인한 사용자 import 변경은 필요하지 않습니다.

## 제공 API

| API | Package | 목적 |
| --- | --- | --- |
| `RedissonJCaching` | `jcache` | Redisson JCache provider helper |
| `RedissonSuspendJCache<K, V>` | `jcache` | Redisson JCache를 감싼 `SuspendJCache` |
| `RedissonNearCache<V>` | `nearcache` | `RLocalCachedMap` 기반 동기 `NearCacheOperations` |
| `RedissonSuspendNearCache<V>` | `nearcache` | `RLocalCachedMap` 기반 suspend `SuspendNearCacheOperations` |
| `RedissonCaches` | root package | JCache, suspend JCache, near cache factory |
| `RedissonMemoizer<T, R>` | `memoizer` | 동기 Redis-backed memoizer |
| `RedissonAsyncMemoizer<T, R>` | `memoizer` | `CompletableFuture`/`CompletionStage` memoizer |
| `RedissonSuspendMemoizer<T, R>` | `memoizer` | key별 in-flight 공유를 제공하는 coroutine memoizer |

이 모듈은 RESP3 hybrid near-cache class를 제공하지 않습니다. Redisson-managed local caching이 필요하면 실제 public API인 `RedissonNearCache` / `RedissonSuspendNearCache`를 사용하세요.

## Near-Cache Capability

Redisson native/JCache NearCache는 공통 conformance suite에서 supported로 검증됩니다.
Native `RedissonNearCache` / `RedissonSuspendNearCache`는 Redisson `RLocalCachedMap` invalidation을 사용합니다.
JCache 변형은 cache-entry listener를 등록하며, Redisson bulk event가 발생하지 않는 경로는 entry별 removal로 전파합니다.

전체 행렬은 [Near-Cache Backend Capability Matrix](../../docs/cache/near-cache-capability-matrix.md)를 참고하세요.

<!-- nearjcache-clear-authority-contract -->
### #1368 Redisson NearJCache clear authority

`RedissonCaches.nearJCache`의 기본값은 `NearJCacheClearAuthority.DENY`이며 Redis
namespace ownership을 추론하지 않습니다. `clear()`, `clearAllCache()`, 인자 없는
`removeAll()`은 `SecurityException`을 발생시키므로, 전체 back namespace를 caller가
소유한다고 확인한 경우에만 `NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE`를 전달합니다.
공유 tenant에는 key-scoped `removeAll(keys)`를 사용합니다. wrapper `close()`는 front만
닫고 전달받은 back cache나 Redisson provider는 닫지 않습니다. Native
`RedissonNearCache.clearAll()`은 별도 API입니다.

```kotlin
val shared = RedissonCaches.nearJCache(backCache, NearJCacheConfig())
shared.removeAll(setOf("tenant-a:key-1"))
val owner = RedissonCaches.nearJCache(
    backCache,
    NearJCacheConfig(cacheName = "users-owner"),
    NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
)
owner.clearAllCache()
```
<!-- /nearjcache-clear-authority-contract -->

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-redisson:$bluetape4kVersion")
}
```

## 권장 사용 시나리오

- 이미 Redisson을 사용하고 있고 bluetape4k의 `JCache`, `SuspendJCache`, `NearCacheOperations`, `SuspendNearCacheOperations`와 맞춘 cache API가 필요할 때.
- 같은 JVM 안에서 같은 key의 중복 계산을 줄이고, 완료된 값은 Redis `RMap`에 저장하는 memoizer가 필요할 때.
- 별도 local cache와 Redis invalidation 계층을 직접 유지하지 않고 Redisson `RLocalCachedMap`의 invalidation 동작을 사용하고 싶을 때.
- coroutine call site에서 Redisson async operation을 blocking 없이 `await()`하고 싶을 때.

## Anti-Patterns

- `close()`를 데이터 삭제로 해석하지 마세요. `close()`는 wrapper/provider resource lifecycle 계약이고, entry 삭제는 `clear()` 또는 `clearAll()`을 명시적으로 호출해야 합니다.
- 이 모듈에 없는 RESP3 helper API를 문서화하거나 호출하지 마세요. 현재 public surface는 위 표의 Redisson/JCache/RLocalCachedMap 기반 API입니다.
- 외부 side effect가 있는 비멱등 연산을 memoizer evaluator에 넣지 마세요. evaluator 실패나 취소는 성공 값으로 캐시되지 않으므로 재시도 시 evaluator가 다시 실행될 수 있습니다.
- `RedissonSuspendMemoizer`의 in-flight 공유를 JVM 간 분산 락처럼 기대하지 마세요. 완료 값은 Redis에 저장되지만 in-flight 조율 map은 프로세스 로컬입니다.

## 예제

### Suspend JCache

```kotlin
import io.bluetape4k.cache.RedissonCaches
import javax.cache.configuration.MutableConfiguration

val cache = RedissonCaches.suspendJCache<String, String>(
    redisson = redissonClient,
    cacheName = "users",
    configuration = MutableConfiguration<String, String>().apply {
        setTypes(String::class.java, String::class.java)
    },
)

cache.put("u:1", "debop")
val user = cache.get("u:1")
// user == "debop"

cache.close()
```

### Native Redisson Near Cache

```kotlin
import io.bluetape4k.cache.RedissonCaches
import io.bluetape4k.cache.nearcache.RedissonNearCacheConfig

val nearCache = RedissonCaches.nearCache<String>(
    redisson = redissonClient,
    config = RedissonNearCacheConfig(cacheName = "products"),
)

nearCache.put("p:1", "keyboard")
val product = nearCache.get("p:1")
nearCache.clearLocal()   // local tier만 정리
nearCache.clearAll()     // local + Redis tier 모두 정리
nearCache.close()
```

### Suspend Memoizer

```kotlin
import io.bluetape4k.cache.memoizer.suspendMemoizer
import org.redisson.client.codec.IntegerCodec

val map = redissonClient.getMap<Int, Int>("memoizer:squares", IntegerCodec())
val memoizer = map.suspendMemoizer { key ->
    expensiveSquare(key)
}

val first = memoizer(7)   // 계산 후 49 저장
val second = memoizer(7)  // 캐시된 49 반환
```

evaluator가 실패하거나 취소되면 in-flight 항목은 예외 완료 후 제거됩니다. 따라서 같은 key의 다음 호출은 실패한 `Deferred`에 고착되지 않고 새 계산을 시작합니다.

## Lifecycle Notes

- `RedissonSuspendJCache.close()`는 감싼 Redisson JCache에 close를 위임하며 저장된 entry를 삭제하지 않습니다.
- `RedissonSuspendNearCache.close()`는 local cached map wrapper를 destroy하고 wrapper를 닫힘 상태로 표시합니다. entry 삭제는 `clearAll()`의 책임입니다.
- suspend close 경로는 `runCatching`으로 `CancellationException`을 삼키지 않고 호출자에게 전파합니다.

## 검증 초점

관련 테스트는 다음 축을 포함합니다.

- JCache CRUD와 close/data-preservation 동작.
- native near-cache read/write/clear/stat 동작.
- `MultithreadingTester`, `StructuredTaskScopeTester`, `SuspendedJobTester` 기반 memoizer thread, virtual-thread, coroutine 경합.
- suspend memoizer evaluator 실패, 명시적 cancellation, 실제 `Job.cancel()` 복구.
