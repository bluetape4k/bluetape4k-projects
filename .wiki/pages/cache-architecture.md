# Cache 아키텍처

> 마지막 업데이트: 2026-04-07 | 관련 specs: 2개

## 개요

Bluetape4k의 NearCache 통일 설계와 Cache 모듈 일관성 리팩토링을 다룬다.
`NearCacheOperations` / `SuspendNearCacheOperations` 단일 인터페이스를 중심으로 lettuce, redisson,
hazelcast, JCache 4개 백엔드의 NearCache 구현을 통일하고, `ResilientNearCacheDecorator`로 Resilience를
일원화하는 설계를 정리한다.

---

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| `NearCacheOperations<V>` / `SuspendNearCacheOperations<V>` 단일 인터페이스 채택 | 4개 백엔드가 제각각 인터페이스를 가져 교체가 불가능했음. 공통 인터페이스로 백엔드를 투명하게 교체 가능하도록 함 | 2026-03-18 | nearcache-unification |
| 키 타입 `String` 고정 | 코드베이스 전체에서 non-String 키 사용 사례가 없음을 확인. 제네릭 `<K>` 제거로 API 단순화 | 2026-03-18 | nearcache-unification |
| `ResilientNearCacheDecorator` 분리 | Resilience를 각 구현체에 내장하면 중복 코드와 일관성 문제. Decorator 패턴으로 분리하여 어떤 백엔드에도 동일하게 적용 가능 | 2026-03-18 | nearcache-unification |
| write-behind를 Decorator에서 제외 | Decorator가 write-behind를 수행하면 delegate의 write-through 원자성이 깨짐. retry + failure strategy only | 2026-03-18 | nearcache-unification |
| `SuspendNearCacheOperations`가 `AutoCloseable` 미구현 | `AutoCloseable.close()`는 non-suspend인데 suspend `close()`와 충돌. 대신 `suspend fun close()` 직접 선언 | 2026-03-18 | nearcache-unification |
| lettuce 6.8.2 고정 (7.5.0 다운그레이드) | Lettuce 7.5.0은 `LettuceNearCache` CLIENT TRACKING 기반 invalidation과 호환되지 않음. 6.8.2로 고정 | 2026-03-28 | (infra/lettuce) |
| 팩토리 함수 `*Of` 패턴 통일 | `nearCacheOf()` → 명시적 반환 타입과 파라미터를 갖는 팩토리 함수. DSL 빌더와 조합 가능 | 2026-03-18 | nearcache-unification |
| 팩토리 네이밍 `nearJCache()` / `nearCache()` 구분 | JCache 기반(`NearJCache<K,V>`)과 NearCacheOperations 기반(`NearCacheOperations<V>`)을 명확히 구분 | 2026-03-18 | cache-consistency-refactoring |
| `LettuceBinaryCodec.serializer` public으로 변경 | 팩토리 API가 `LettuceBinaryCodec<V>`를 받고 내부 구현이 `codec.serializer`로 `BinarySerializer`를 추출하는 브릿지 전략 | 2026-03-18 | cache-consistency-refactoring |

---

## 패턴 & 사용법

### NearCacheOperations 인터페이스 (동기)

```kotlin
interface NearCacheOperations<V: Any>: AutoCloseable {
    val cacheName: String
    val isClosed: Boolean

    // Read
    fun get(key: String): V?
    fun getAll(keys: Set<String>): Map<String, V>
    fun containsKey(key: String): Boolean

    // Write
    fun put(key: String, value: V)
    fun putAll(entries: Map<String, V>)
    fun putIfAbsent(key: String, value: V): V?   // 기존 값 반환 (null이면 성공)
    fun replace(key: String, value: V): Boolean
    fun replace(key: String, oldValue: V, newValue: V): Boolean

    // Delete
    fun remove(key: String)          // Unit 반환 (기존 구현체 일치)
    fun removeAll(keys: Set<String>)
    fun getAndRemove(key: String): V?
    fun getAndReplace(key: String, value: V): V?

    // Cache management
    fun clearLocal()
    fun clearAll()
    fun localCacheSize(): Long
    fun backCacheSize(): Long

    // Statistics
    fun stats(): NearCacheStatistics
}
```

### SuspendNearCacheOperations 인터페이스 (코루틴)

동기 버전과 동일한 API. 차이점:
- 모든 Read/Write/Delete 메서드가 `suspend`
- `clearLocal()`, `localCacheSize()` 등 로컬 메모리 접근 메서드는 non-suspend 유지
- `AutoCloseable` 미구현 → `suspend fun close()` 직접 선언

### 백엔드별 구현체

| 백엔드 | 동기 구현체 | Suspend 구현체 | 특이사항 |
|--------|------------|--------------|--------|
| **Lettuce** | `LettuceNearCache<V>` | `LettuceSuspendNearCache<V>` | RESP3 CLIENT TRACKING 기반 invalidation, lettuce 6.8.2 고정 |
| **Redisson** | `RedissonNearCache<V>` | `RedissonSuspendNearCache<V>` | `RBucket` 기반, Redisson 내장 retry 활용 |
| **Hazelcast** | `HazelcastNearCache<V>` | `HazelcastSuspendNearCache<V>` | `IMap` 기반, `putIfAbsent` 등 누락 메서드 추가 (Hazelcast 5.x는 `putIfAbsentAsync` 없음 → `putAsync()` 사용) |
| **JCache** | `JCacheNearCache<V>` | `JCacheSuspendNearCache<V>` | `javax.cache.Cache<String, V>` 기반, `withContext(IO)` 래핑 |

### `*Of` 팩토리 함수 패턴

```kotlin
// Lettuce
fun <V: Any> lettuceNearCacheOf(
    redisClient: RedisClient,
    codec: LettuceBinaryCodec<V>,
    config: LettuceNearCacheConfig,
): NearCacheOperations<V>

fun <V: Any> lettuceSuspendNearCacheOf(
    redisClient: RedisClient,
    codec: LettuceBinaryCodec<V>,
    config: LettuceNearCacheConfig,
): SuspendNearCacheOperations<V>

// Redisson
fun <V: Any> redissonNearCacheOf(redisson: RedissonClient, config: ...): NearCacheOperations<V>
fun <V: Any> redissonSuspendNearCacheOf(redisson: RedissonClient, config: ...): SuspendNearCacheOperations<V>

// Hazelcast
fun <V: Any> hazelcastNearCacheOf(imap: IMap<String, V>, config: ...): NearCacheOperations<V>

// JCache
fun <V: Any> jcacheNearCacheOf(cache: Cache<String, V>, config: ...): NearCacheOperations<V>
```

### withResilience {} 사용법

```kotlin
// DSL builder
val resilientCache = lettuceNearCacheOf<MyValue>(redisClient, codec, config)
    .withResilience {
        retryMaxAttempts = 5
        retryWaitDuration = 1.seconds
        retryExponentialBackoff = true
        getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL
    }

// config 객체 직접 전달
val config = NearCacheResilienceConfig(retryMaxAttempts = 3)
val resilientCache = cache.withResilience(config)

// Suspend 버전
val suspendResilient: SuspendNearCacheOperations<V> = suspendCache.withResilience { ... }
```

### 팩토리 네이밍 규칙 (3개 모듈 통일)

| 유형 | 함수명 | 반환 타입 |
|------|--------|---------|
| JCache | `jcache()` | `JCache<K, V>` |
| Suspend JCache | `suspendJCache()` | `SuspendJCache<K, V>` |
| JCache 기반 NearCache (동기) | `nearJCache()` | `NearJCache<K, V>` |
| JCache 기반 NearCache (suspend) | `suspendNearJCache()` | `SuspendNearJCache<K, V>` |
| NearCacheOperations (동기) | `nearCache()` | `NearCacheOperations<V>` |
| NearCacheOperations (suspend) | `suspendNearCache()` | `SuspendNearCacheOperations<V>` |

### NearCacheStatistics

```kotlin
interface NearCacheStatistics {
    val localHits: Long
    val localMisses: Long
    val localSize: Long
    val localEvictions: Long
    val backHits: Long
    val backMisses: Long
    val hitRate: Double  // (localHits + backHits) / total
}
```

### 기본값 규칙

| 항목 | 기본값 |
|------|-------|
| Lettuce Codec | `LettuceBinaryCodecs.lz4Fory()` |
| Redisson Codec | `RedissonCodecs.LZ4Fory` |
| NearJCacheConfig | Caffeine front cache, 30분 접근 만료 |

---

## 선택하지 않은 방식 / 트레이드오프

| 방식 | 선택하지 않은 이유 |
|------|-----------------|
| Resilience를 각 구현체에 내장 | 중복 코드, 일관성 문제. Decorator로 분리하면 어떤 백엔드에도 동일하게 적용 가능 |
| write-behind를 Decorator에 포함 | delegate의 write-through 원자성이 깨짐. 추후 별도 설계로 대응 |
| Lettuce 7.5.0 사용 | LettuceNearCache CLIENT TRACKING 기반 invalidation 비호환으로 6.8.2로 고정 |
| non-String 키 지원 (`<K>` 제네릭 유지) | 코드베이스 전체에서 non-String 키 사용 사례 없음. API 복잡성만 증가 |
| RESP3 하이브리드(Redisson 데이터 + Lettuce tracking) | Redisson 네이티브 직접 구현으로 단순화. Redisson `LocalCachedMapOptions` 활용 |

---

## 관련 페이지

- [exposed-patterns.md](exposed-patterns.md) — Exposed Cache Repository 인터페이스 통일
- [dependency-decisions.md](dependency-decisions.md) — lettuce 6.8.2 고정 등 라이브러리 버전 결정
