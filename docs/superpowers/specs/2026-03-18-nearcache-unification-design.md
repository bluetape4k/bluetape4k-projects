# NearCache 통일 설계

**날짜:** 2026-03-18
**상태:** 승인됨 (리뷰 반영 v2)

## 목표

Redisson, Lettuce, Hazelcast, JCache 4가지 백엔드의 NearCache 구현을 공통 인터페이스(`NearCacheOperations`, `SuspendNearCacheOperations`)로 통일하여:

1. 동일한 API로 백엔드 교체 가능
2. Resilience를 Decorator 패턴으로 일원화
3. 테스트 시나리오를 abstract 클래스로 공유

## 설계 결정 요약

| 결정 항목 | 선택 |
|-----------|------|
| 인터페이스 위치 | `cache-core` |
| Resilience 전략 | 공통 `ResilientNearCacheDecorator` (cache-core) |
| 기존 JCache NearCache | `JCacheNearCache`로 리팩토링 (NearCacheOperations 구현) |
| 키 타입 | `String` 고정 (`NearCacheOperations<V>`) |
| Hazelcast resilience | Decorator 사용 가능 (사용자 선택) |
| Decorator 전략 | **retry + failure strategy only** (write-behind 없음, delegate의 write-through 원자성 유지) |
| `remove()` 반환 타입 | `Unit` (기존 구현체와 일치) |
| `putIfAbsent()` 반환 타입 | `V?` (기존 구현체 및 JCache/ConcurrentMap 관례와 일치) |

## 구현 순서

1. **cache-core**: 공통 인터페이스 + Statistics + ResilienceConfig + Decorator 생성
2. **cache-core**: 기존 `NearCache`/`SuspendNearCache` → `JCacheNearCache`/`JCacheSuspendNearCache` 리팩토링
3. **cache-core**: test fixture (abstract 테스트 클래스) 작성
4. **cache-lettuce**: `NearCacheOperations`/`SuspendNearCacheOperations` 구현, Resilient 변형 삭제
5. **cache-hazelcast**: 동일 + 누락 메서드 추가, Resilient 변형 삭제
6. **cache-redisson**: 새 직접 구현체 작성, 기존 JCache 팩토리 및 RESP3 하이브리드 삭제
7. **각 모듈 테스트**: abstract 테스트 상속으로 전환
8. **cache-core dead code 정리**: `ResilientNearCacheLocalCache`, `CaffeineResilientLocalCache` 등

## 섹션 1: 공통 인터페이스 (cache-core)

### NearCacheOperations<V: Any>

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
    fun remove(key: String)                       // Unit 반환 (기존 구현체 일치)
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

### SuspendNearCacheOperations<V: Any>

```kotlin
interface SuspendNearCacheOperations<V: Any> {

    val cacheName: String
    val isClosed: Boolean

    // Read (suspend)
    suspend fun get(key: String): V?
    suspend fun getAll(keys: Set<String>): Map<String, V>
    suspend fun containsKey(key: String): Boolean

    // Write (suspend)
    suspend fun put(key: String, value: V)
    suspend fun putAll(entries: Map<String, V>)
    suspend fun putIfAbsent(key: String, value: V): V?
    suspend fun replace(key: String, value: V): Boolean
    suspend fun replace(key: String, oldValue: V, newValue: V): Boolean

    // Delete (suspend)
    suspend fun remove(key: String)
    suspend fun removeAll(keys: Set<String>)
    suspend fun getAndRemove(key: String): V?
    suspend fun getAndReplace(key: String, value: V): V?

    // Cache management (non-suspend: 로컬 메모리 접근)
    fun clearLocal()
    suspend fun clearAll()
    fun localCacheSize(): Long
    suspend fun backCacheSize(): Long

    // Statistics (non-suspend: 로컬 카운터)
    fun stats(): NearCacheStatistics

    // Lifecycle
    suspend fun close()
}
```

**참고:** `SuspendNearCacheOperations`는 `AutoCloseable`을 구현하지 않음.
`AutoCloseable.close()`는 non-suspend이므로 suspend `close()`와 충돌.
대신 `suspend fun close()`를 직접 선언.

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

data class DefaultNearCacheStatistics(
    override val localHits: Long = 0,
    override val localMisses: Long = 0,
    override val localSize: Long = 0,
    override val localEvictions: Long = 0,
    override val backHits: Long = 0,
    override val backMisses: Long = 0,
): NearCacheStatistics {
    override val hitRate: Double
        get() {
            val total = localHits + backHits + backMisses
            return if (total == 0L) 0.0 else (localHits + backHits).toDouble() / total
        }
}
```

**구현 참고:**
- `localHits`/`localMisses`/`localEvictions`: Caffeine `CacheStats`에서 직접 매핑 (`stats.hitCount()` 등)
- `backHits`/`backMisses`: 각 구현체에서 `AtomicLong` 카운터로 추적 (get 시 front miss → back hit/miss 카운트)
- 구현체가 Caffeine `CacheStats`의 더 상세한 정보가 필요하면 구현 클래스에서 직접 `localStats(): CacheStats?` 제공 가능 (인터페이스 외)

## 섹션 2: Resilient Decorator (cache-core)

### 설계 원칙: Retry + Failure Strategy Only

Decorator는 **write-behind를 하지 않음**. delegate의 write-through 원자성을 유지하기 위해:
- Read: delegate에 resilience4j `Retry` 적용 + `GetFailureStrategy`
- Write: delegate에 resilience4j `Retry` 적용 (delegate가 front+back을 원자적으로 처리)
- write-behind가 필요한 경우 별도 설계로 추후 대응

### NearCacheResilienceConfig

```kotlin
data class NearCacheResilienceConfig(
    val retryMaxAttempts: Int = 3,
    val retryWaitDuration: Duration = 500.milliseconds,
    val retryExponentialBackoff: Boolean = true,
    val getFailureStrategy: GetFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL,
)

// DSL Builder
fun nearCacheResilienceConfig(
    initializer: NearCacheResilienceConfigBuilder.() -> Unit,
): NearCacheResilienceConfig =
    NearCacheResilienceConfigBuilder().apply(initializer).build()
```

**참고:** write-behind 제거에 따라 `writeQueueCapacity` 필드 삭제됨.

### ResilientNearCacheDecorator

- `NearCacheOperations<V>`를 감싸는 Decorator
- Read: delegate에 resilience4j `Retry` 적용 + `GetFailureStrategy`
- Write: delegate에 resilience4j `Retry` 적용 (원자적 write-through 유지)
- `stats()`, `localCacheSize()` 등 관리 메서드: delegate에 직접 위임
- `close()`: delegate.close() 위임

### ResilientSuspendNearCacheDecorator

- `SuspendNearCacheOperations<V>`를 감싸는 Decorator
- Read/Write: delegate에 resilience4j `Retry` 적용 (suspend 버전)
- 동일하게 원자적 write-through 유지

### 확장 함수

```kotlin
// config 직접 전달
fun <V: Any> NearCacheOperations<V>.withResilience(
    config: NearCacheResilienceConfig,
): NearCacheOperations<V> = ResilientNearCacheDecorator(this, config)

// DSL builder
fun <V: Any> NearCacheOperations<V>.withResilience(
    initializer: NearCacheResilienceConfigBuilder.() -> Unit,
): NearCacheOperations<V> = ResilientNearCacheDecorator(this, nearCacheResilienceConfig(initializer))

// Suspend 버전도 동일
fun <V: Any> SuspendNearCacheOperations<V>.withResilience(
    config: NearCacheResilienceConfig,
): SuspendNearCacheOperations<V> = ResilientSuspendNearCacheDecorator(this, config)

fun <V: Any> SuspendNearCacheOperations<V>.withResilience(
    initializer: NearCacheResilienceConfigBuilder.() -> Unit,
): SuspendNearCacheOperations<V> = ResilientSuspendNearCacheDecorator(this, nearCacheResilienceConfig(initializer))
```

## 섹션 3: 모듈별 구현체

### cache-lettuce

| 파일 | 역할 |
|------|------|
| `LettuceNearCache<V>` | `NearCacheOperations<V>` 구현, RedisCommands (sync) |
| `LettuceSuspendNearCache<V>` | `SuspendNearCacheOperations<V>` 구현, RedisCoroutinesCommands |
| `LettuceNearCacheConfig` | 기존 유지 |
| `LettuceNearCacheFactory` | `lettuceNearCacheOf()`, `lettuceSuspendNearCacheOf()` 팩토리 |

**유지되는 헬퍼 클래스:**
- `LettuceLocalCache.kt` — front cache 인터페이스
- `LettuceCaffeineLocalCache.kt` — Caffeine 기반 front cache + RESP3 invalidation
- `TrackingInvalidationListener.kt` — CLIENT TRACKING push 처리

### cache-hazelcast

| 파일 | 역할 |
|------|------|
| `HazelcastNearCache<V>` | `NearCacheOperations<V>` 구현, IMap (sync) |
| `HazelcastSuspendNearCache<V>` | `SuspendNearCacheOperations<V>` 구현, IMap async + await |
| 누락 메서드 추가 | `putIfAbsent`, `replace`, `getAndRemove`, `getAndReplace` |

**유지되는 헬퍼 클래스:**
- `HazelcastLocalCache.kt` — front cache 인터페이스
- `CaffeineHazelcastLocalCache.kt` — Caffeine + IMap EntryListener
- `HazelcastEntryEventListener.kt` — invalidation 이벤트 처리

### cache-redisson

| 파일 | 역할 |
|------|------|
| `RedissonNearCache<V>` (신규) | `NearCacheOperations<V>` 직접 구현, RBucket (내장 retry) |
| `RedissonSuspendNearCache<V>` (신규) | `SuspendNearCacheOperations<V>`, RBucket.*Async().await() |

**Redisson invalidation 전략:**
기존 RESP3 하이브리드(Redisson 데이터 + Lettuce tracking)를 Redisson 네이티브로 단순화.
Redisson의 `LocalCachedMapOptions`나 topic 기반 invalidation 활용.

### cache-core (JCache)

| 파일 | 역할 |
|------|------|
| `JCacheNearCache<V>` | `NearCacheOperations<V>`, javax.cache.Cache<String, V> back |
| `JCacheSuspendNearCache<V>` | `SuspendNearCacheOperations<V>`, withContext(IO) 래핑 |

**참고:** 기존 `NearCache<K, V>`의 제네릭 K를 String으로 고정.
현재 코드베이스에서 `NearCache<K, V>`를 non-String 키로 사용하는 곳이 없음을 확인 완료.
기존 `RedissonNearCache` 팩토리가 JCache 기반 `NearCache<K, V>`를 생성하지만,
이 팩토리 자체가 새 직접 구현체로 대체되므로 호환성 문제 없음.

### 팩토리 함수 패턴 (`*Of`)

```kotlin
fun <V: Any> lettuceNearCacheOf(redisClient, codec, config): NearCacheOperations<V>
fun <V: Any> lettuceSuspendNearCacheOf(redisClient, codec, config): SuspendNearCacheOperations<V>
fun <V: Any> hazelcastNearCacheOf(imap, config): NearCacheOperations<V>
fun <V: Any> hazelcastSuspendNearCacheOf(imap, config): SuspendNearCacheOperations<V>
fun <V: Any> redissonNearCacheOf(redisson, config): NearCacheOperations<V>
fun <V: Any> redissonSuspendNearCacheOf(redisson, config): SuspendNearCacheOperations<V>
fun <V: Any> jcacheNearCacheOf(cache, config): NearCacheOperations<V>
fun <V: Any> jcacheSuspendNearCacheOf(cache, config): SuspendNearCacheOperations<V>
```

**참고:** `LettuceNearCacheFactory.kt`는 기존 파일을 리팩토링하여 `*Of` 패턴으로 변경.

## 섹션 4: 테스트 전략

### cache-core test fixtures

```kotlin
// AbstractNearCacheOperationsTest<V: Any>
abstract class AbstractNearCacheOperationsTest<V: Any> {
    abstract fun createCache(): NearCacheOperations<V>
    abstract fun sampleValue(): V
    abstract fun anotherValue(): V

    // 공통 테스트 시나리오
    @Test fun `get - cache miss returns null`()
    @Test fun `put and get - round trip`()
    @Test fun `getAll - batch read`()
    @Test fun `putIfAbsent - returns null on success, existing value on failure`()
    @Test fun `replace - existing key only`()
    @Test fun `replace - with oldValue check`()
    @Test fun `remove - removes existing key`()
    @Test fun `getAndRemove - returns value and removes`()
    @Test fun `getAndReplace - returns old value`()
    @Test fun `clearAll - empties both tiers`()
    @Test fun `clearLocal - empties front only`()
    @Test fun `local cache populated on get`()
    @Test fun `stats - hits and misses tracked`()
    @Test fun `containsKey - checks both tiers`()
}

// AbstractSuspendNearCacheOperationsTest<V: Any> — 동일 시나리오, runTest 사용
```

### 각 모듈 테스트 패턴

```kotlin
// cache-lettuce
class LettuceNearCacheTest : AbstractNearCacheOperationsTest<String>() {
    override fun createCache() = lettuceNearCacheOf<String>(redisClient, codec, config)
    override fun sampleValue() = "hello"
    override fun anotherValue() = "world"
}

// Resilient Decorator 테스트
class ResilientLettuceNearCacheTest : AbstractNearCacheOperationsTest<String>() {
    override fun createCache() = lettuceNearCacheOf<String>(redisClient, codec, config)
        .withResilience { retryMaxAttempts = 3 }
}
```

### Resilient Decorator 전용 테스트 (cache-core)

- transient failure retry 동작 검증
- `GetFailureStrategy.RETURN_FRONT_OR_NULL` / `PROPAGATE_EXCEPTION` 검증
- retry 횟수 초과 시 예외 전파 확인
- `close()` 위임 확인

## 섹션 5: 삭제 대상

### 삭제 파일

| 모듈 | 파일 | 대체 |
|------|------|------|
| **cache-lettuce** | `ResilientLettuceNearCache.kt` | `.withResilience {}` |
| cache-lettuce | `ResilientLettuceSuspendNearCache.kt` | `.withResilience {}` |
| cache-lettuce | `ResilientLettuceNearCacheConfig.kt` | `NearCacheResilienceConfig` |
| cache-lettuce | `LettuceNearCacheOperations.kt` | `NearCacheOperations` (cache-core) |
| cache-lettuce | `LettuceSuspendNearCacheOperations.kt` | `SuspendNearCacheOperations` (cache-core) |
| cache-lettuce | `ResilientLettuceNearCacheTest.kt` | abstract 테스트 상속 |
| cache-lettuce | `ResilientLettuceSuspendNearCacheTest.kt` | abstract 테스트 상속 |
| **cache-hazelcast** | `ResilientHazelcastNearCache.kt` | `.withResilience {}` |
| cache-hazelcast | `ResilientHazelcastSuspendNearCache.kt` | `.withResilience {}` |
| cache-hazelcast | `ResilientHazelcastNearCacheConfig.kt` | `NearCacheResilienceConfig` |
| cache-hazelcast | `ResilientHazelcastNearCacheTest.kt` | abstract 테스트 상속 |
| cache-hazelcast | `ResilientHazelcastSuspendNearCacheTest.kt` | abstract 테스트 상속 |
| **cache-redisson** | `RedissonNearCache.kt` (JCache 팩토리) | 새 직접 구현체 |
| cache-redisson | `RedissonResp3NearCache.kt` | `RedissonNearCache`로 통합 |
| cache-redisson | `RedissonResp3SuspendNearCache.kt` | `RedissonSuspendNearCache`로 통합 |
| cache-redisson | `ResilientRedissonResp3NearCache.kt` | `.withResilience {}` |
| cache-redisson | `ResilientRedissonResp3SuspendNearCache.kt` | `.withResilience {}` |
| cache-redisson | `ResilientRedissonResp3NearCacheConfig.kt` | `NearCacheResilienceConfig` |
| cache-redisson | `ResilientRedissonResp3NearCacheTest.kt` | abstract 테스트 상속 |
| cache-redisson | `ResilientRedissonResp3SuspendNearCacheTest.kt` | abstract 테스트 상속 |
| **cache-core** | `ResilientNearCacheLocalCache.kt` | Decorator가 front 직접 관리 안 함 |
| cache-core | `CaffeineResilientLocalCache.kt` | 동일 |
| cache-core | `ResilientLocalCache.kt` (인터페이스) | 동일 |

### 리팩토링 파일

| 모듈 | 변경 |
|------|------|
| cache-core `NearCache.kt` → `JCacheNearCache.kt` | `NearCacheOperations<V>` 구현, K→String 고정 |
| cache-core `SuspendNearCache.kt` → `JCacheSuspendNearCache.kt` | `SuspendNearCacheOperations<V>` 구현 |
| cache-core `ResilientNearCache.kt` → `ResilientNearCacheDecorator.kt` | retry+failure strategy Decorator |
| cache-core `ResilientSuspendNearCache.kt` → `ResilientSuspendNearCacheDecorator.kt` | 동일 |
| cache-core `ResilientNearCacheConfig.kt` → `NearCacheResilienceConfig.kt` | 네이밍 + Builder, writeQueueCapacity 제거 |
| cache-core `BackCacheCommand.kt` | 삭제 (write-behind 제거로 불필요) |
| cache-lettuce `LettuceNearCache.kt` | `NearCacheOperations<V>` 구현 + stats 카운터 추가 |
| cache-lettuce `LettuceSuspendNearCache.kt` | `SuspendNearCacheOperations<V>` 구현 + stats 카운터 추가 |
| cache-lettuce `LettuceNearCacheFactory.kt` | `*Of` 패턴으로 변경 |
| cache-hazelcast `HazelcastNearCache.kt` | `NearCacheOperations<V>` + 누락 메서드 + stats 카운터 |
| cache-hazelcast `HazelcastSuspendNearCache.kt` | `SuspendNearCacheOperations<V>` + stats 카운터 |

### 신규 파일

| 모듈 | 파일 |
|------|------|
| cache-core | `NearCacheOperations.kt` |
| cache-core | `SuspendNearCacheOperations.kt` |
| cache-core | `NearCacheStatistics.kt` (interface + DefaultNearCacheStatistics) |
| cache-core | `NearCacheResilienceConfig.kt` + `NearCacheResilienceConfigBuilder` |
| cache-core test fixture | `AbstractNearCacheOperationsTest.kt` |
| cache-core test fixture | `AbstractSuspendNearCacheOperationsTest.kt` |
| cache-redisson | `RedissonNearCache.kt` (직접 구현체) |
| cache-redisson | `RedissonSuspendNearCache.kt` |

### JCache SPI 파일 처리

| 모듈 | 파일 | 처리 |
|------|------|------|
| cache-redisson | `RedissonNearCacheManager.kt` | JCache 팩토리 삭제 시 함께 삭제 |
| cache-redisson | `RedissonNearCachingProvider.kt` | 동일 |
| cache-redisson | `RedissonNearCacheConfig.kt` | 동일 |
| cache-redisson | `META-INF/services/javax.cache.spi.CachingProvider` | NearCachingProvider 항목 제거 |

## 사용 예시

```kotlin
// Lettuce 기본
val cache = lettuceNearCacheOf<MyValue>(redisClient, codec, config)
cache.put("user:1", myValue)
val value = cache.get("user:1")

// Lettuce + Resilience
val resilientCache = lettuceNearCacheOf<MyValue>(redisClient, codec, config)
    .withResilience {
        retryMaxAttempts = 5
        retryWaitDuration = 1.seconds
        getFailureStrategy = PROPAGATE_EXCEPTION
    }

// Hazelcast suspend
val suspendCache = hazelcastSuspendNearCacheOf<MyValue>(imap, config)
    .withResilience { retryMaxAttempts = 3 }

// Redisson (내장 retry, Decorator 불필요)
val redissonCache = redissonNearCacheOf<MyValue>(redisson, config)

// JCache (범용)
val jcache = jcacheNearCacheOf<MyValue>(cache, config)
```
