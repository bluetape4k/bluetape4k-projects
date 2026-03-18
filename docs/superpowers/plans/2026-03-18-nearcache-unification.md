# NearCache 통일 구현 계획

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 4개 백엔드(Lettuce, Hazelcast, Redisson, JCache)의 NearCache를 `NearCacheOperations`/`SuspendNearCacheOperations` 공통 인터페이스로 통일

**Architecture:** cache-core에 인터페이스 + Decorator 정의, 각 cache-* 모듈이 구현. Resilient 변형 삭제 → `.withResilience {}` Decorator로 대체. 공통 abstract 테스트로 일관된 검증.

**Tech Stack:** Kotlin 2.3, Caffeine, Lettuce, Hazelcast, Redisson, JCache, Resilience4j, JUnit 5, Kluent

**Spec:** `docs/superpowers/specs/2026-03-18-nearcache-unification-design.md`

---

## 주요 구현 주의사항

1. **JCache `putIfAbsent()` 반환 타입 변환**: 기존 JCache는 `Boolean` 반환, 새 인터페이스는 `V?` 반환. `JCacheNearCache`에서 `by backCache` 위임을 제거하고 수동 위임으로 변환 필요: `backCache.getAndPut()` 또는 `get() + put()` 조합.
2. **`SuspendNearCacheOperations.close()`는 suspend**: 기존 구현체(`LettuceSuspendNearCache`, `HazelcastSuspendNearCache`)가 `AutoCloseable`을 구현하는 경우 `: AutoCloseable` 제거하고 `override suspend fun close()`로 변경 필요.
3. **Redisson invalidation 전략**: `LocalCachedMapOptions` 사용 (Redisson 내장 client-side caching). Topic 기반 수동 pub/sub보다 안정적이고 Redisson이 자동 관리.
4. **`build.gradle.kts` 업데이트**: test fixture에 awaitility, coroutines-test 의존성 추가. cache-redisson에서 RESP3 삭제 후 불필요한 lettuce 의존성 정리.
5. **기존 유지 파일**: `GetFailureStrategy.kt`, `CacheEntryEventListener.kt`, `SuspendCacheEntryEventListener.kt`, `NearCacheConfig.kt` (JCache용), 각 모듈의 LocalCache 헬퍼 클래스들은 유지.
6. **`NearCacheStatistics.hitRate` KDoc 정정**: `(localHits + backHits) / (localHits + backHits + backMisses)`

---

## 파일 구조

### 신규 파일 (cache-core)

| 파일 | 역할 |
|------|------|
| `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt` | 공통 blocking 인터페이스 |
| `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/SuspendNearCacheOperations.kt` | 공통 suspend 인터페이스 |
| `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheStatistics.kt` | 통계 인터페이스 + DefaultNearCacheStatistics |
| `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheResilienceConfig.kt` | Resilience 설정 + DSL Builder |
| `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecorator.kt` | Retry Decorator (blocking) |
| `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecorator.kt` | Retry Decorator (suspend) |
| `infra/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractNearCacheOperationsTest.kt` | 공통 blocking 테스트 |
| `infra/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractSuspendNearCacheOperationsTest.kt` | 공통 suspend 테스트 |

### 리팩토링 파일

| 파일 | 변경 |
|------|------|
| `infra/cache-core/.../NearCache.kt` → `JCacheNearCache.kt` | `NearCacheOperations<V>` 구현, K→String |
| `infra/cache-core/.../SuspendNearCache.kt` → `JCacheSuspendNearCache.kt` | `SuspendNearCacheOperations<V>` 구현 |
| `infra/cache-lettuce/.../LettuceNearCache.kt` | `NearCacheOperations<V>` 구현 + stats 카운터 |
| `infra/cache-lettuce/.../LettuceSuspendNearCache.kt` | `SuspendNearCacheOperations<V>` 구현 + stats 카운터 |
| `infra/cache-lettuce/.../LettuceNearCacheFactory.kt` | `*Of` 팩토리 패턴 |
| `infra/cache-hazelcast/.../HazelcastNearCache.kt` | `NearCacheOperations<V>` 구현 + 누락 메서드 + stats |
| `infra/cache-hazelcast/.../HazelcastSuspendNearCache.kt` | `SuspendNearCacheOperations<V>` 구현 + stats |

### 신규 파일 (cache-redisson)

| 파일 | 역할 |
|------|------|
| `infra/cache-redisson/.../RedissonNearCache.kt` | 새 `NearCacheOperations<V>` 직접 구현 (기존 JCache 팩토리 대체) |
| `infra/cache-redisson/.../RedissonSuspendNearCache.kt` | 새 `SuspendNearCacheOperations<V>` 구현 |

### 삭제 파일

**cache-core:**
- `ResilientNearCache.kt`, `ResilientSuspendNearCache.kt`
- `ResilientNearCacheConfig.kt`, `ResilientNearCacheLocalCache.kt`, `BackCacheCommand.kt`

**cache-lettuce:**
- `ResilientLettuceNearCache.kt`, `ResilientLettuceSuspendNearCache.kt`
- `ResilientLettuceNearCacheConfig.kt`
- `LettuceNearCacheOperations.kt`, `LettuceSuspendNearCacheOperations.kt`

**cache-hazelcast:**
- `ResilientHazelcastNearCache.kt`, `ResilientHazelcastSuspendNearCache.kt`
- `ResilientHazelcastNearCacheConfig.kt`

**cache-redisson:**
- `RedissonResp3NearCache.kt`, `RedissonResp3SuspendNearCache.kt`
- `ResilientRedissonResp3NearCache.kt`, `ResilientRedissonResp3SuspendNearCache.kt`
- `ResilientRedissonResp3NearCacheConfig.kt`
- `RedissonNearCachingProvider.kt`, `RedissonNearCacheManager.kt`, `RedissonNearCacheConfig.kt`

---

## Task 1: 공통 인터페이스 생성 (cache-core)

**Files:**
- Create: `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt`
- Create: `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/SuspendNearCacheOperations.kt`
- Create: `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheStatistics.kt`

- [ ] **Step 1: `NearCacheStatistics.kt` 생성**

```kotlin
package io.bluetape4k.cache.nearcache

/**
 * NearCache 통계 인터페이스.
 * 로컬 캐시와 백엔드 캐시의 hit/miss 통계를 제공합니다.
 */
interface NearCacheStatistics {
    /** 로컬 캐시 히트 수 */
    val localHits: Long
    /** 로컬 캐시 미스 수 */
    val localMisses: Long
    /** 로컬 캐시 현재 크기 */
    val localSize: Long
    /** 로컬 캐시 퇴거 수 */
    val localEvictions: Long
    /** 백엔드 캐시 히트 수 (로컬 미스 후 백엔드에서 찾은 경우) */
    val backHits: Long
    /** 백엔드 캐시 미스 수 (로컬, 백엔드 모두 없는 경우) */
    val backMisses: Long
    /** 전체 히트율: (localHits + backHits) / (localHits + localMisses) */
    val hitRate: Double
}

/**
 * [NearCacheStatistics]의 기본 구현체.
 */
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

- [ ] **Step 2: `NearCacheOperations.kt` 생성**

```kotlin
package io.bluetape4k.cache.nearcache

/**
 * NearCache 공통 인터페이스 (Blocking).
 * Caffeine 로컬 캐시(front)와 분산 캐시(back)의 2-tier 캐시를 통일된 API로 제공합니다.
 *
 * @param V 캐시 값 타입 (키는 String 고정)
 */
interface NearCacheOperations<V: Any>: AutoCloseable {

    /** 캐시 이름 */
    val cacheName: String

    /** 캐시 종료 여부 */
    val isClosed: Boolean

    // -- Read --

    /** [key]에 해당하는 값을 조회합니다. 없으면 null 반환. */
    fun get(key: String): V?

    /** 여러 [keys]에 해당하는 값을 일괄 조회합니다. */
    fun getAll(keys: Set<String>): Map<String, V>

    /** [key]가 캐시에 존재하는지 확인합니다. */
    fun containsKey(key: String): Boolean

    // -- Write --

    /** [key]-[value] 쌍을 저장합니다. */
    fun put(key: String, value: V)

    /** 여러 [entries]를 일괄 저장합니다. */
    fun putAll(entries: Map<String, V>)

    /** [key]가 없을 때만 [value]를 저장합니다. 기존 값이 있으면 반환, 없으면 null. */
    fun putIfAbsent(key: String, value: V): V?

    /** [key]의 값을 [value]로 교체합니다. 키가 존재할 때만 성공. */
    fun replace(key: String, value: V): Boolean

    /** [key]의 값이 [oldValue]와 일치할 때만 [newValue]로 교체합니다. */
    fun replace(key: String, oldValue: V, newValue: V): Boolean

    // -- Delete --

    /** [key]를 삭제합니다. */
    fun remove(key: String)

    /** 여러 [keys]를 일괄 삭제합니다. */
    fun removeAll(keys: Set<String>)

    /** [key]의 값을 반환하고 삭제합니다. 없으면 null. */
    fun getAndRemove(key: String): V?

    /** [key]의 현재 값을 반환하고 [value]로 교체합니다. 없으면 null. */
    fun getAndReplace(key: String, value: V): V?

    // -- Cache Management --

    /** 로컬 캐시만 비웁니다. 백엔드 캐시는 유지됩니다. */
    fun clearLocal()

    /** 로컬 + 백엔드 캐시 모두 비웁니다. */
    fun clearAll()

    /** 로컬 캐시 엔트리 수 */
    fun localCacheSize(): Long

    /** 백엔드 캐시 엔트리 수 */
    fun backCacheSize(): Long

    // -- Statistics --

    /** 캐시 통계를 반환합니다. */
    fun stats(): NearCacheStatistics
}
```

- [ ] **Step 3: `SuspendNearCacheOperations.kt` 생성**

```kotlin
package io.bluetape4k.cache.nearcache

/**
 * NearCache 공통 인터페이스 (Coroutine Suspend).
 * [NearCacheOperations]의 suspend 버전입니다.
 *
 * 참고: [AutoCloseable]을 구현하지 않습니다. `AutoCloseable.close()`는 non-suspend이므로
 * suspend `close()`와 충돌합니다. 대신 [close]를 직접 선언합니다.
 *
 * @param V 캐시 값 타입 (키는 String 고정)
 */
interface SuspendNearCacheOperations<V: Any> {

    val cacheName: String
    val isClosed: Boolean

    // -- Read --
    suspend fun get(key: String): V?
    suspend fun getAll(keys: Set<String>): Map<String, V>
    suspend fun containsKey(key: String): Boolean

    // -- Write --
    suspend fun put(key: String, value: V)
    suspend fun putAll(entries: Map<String, V>)
    suspend fun putIfAbsent(key: String, value: V): V?
    suspend fun replace(key: String, value: V): Boolean
    suspend fun replace(key: String, oldValue: V, newValue: V): Boolean

    // -- Delete --
    suspend fun remove(key: String)
    suspend fun removeAll(keys: Set<String>)
    suspend fun getAndRemove(key: String): V?
    suspend fun getAndReplace(key: String, value: V): V?

    // -- Cache Management --
    fun clearLocal()
    suspend fun clearAll()
    fun localCacheSize(): Long
    suspend fun backCacheSize(): Long

    // -- Statistics --
    fun stats(): NearCacheStatistics

    // -- Lifecycle --
    suspend fun close()
}
```

- [ ] **Step 4: 컴파일 확인**

Run: `./gradlew :infra:cache-core:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt \
        infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/SuspendNearCacheOperations.kt \
        infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheStatistics.kt
git commit -m "feat: NearCacheOperations/SuspendNearCacheOperations 공통 인터페이스 추가"
```

---

## Task 2: NearCacheResilienceConfig + Decorator 생성 (cache-core)

**Files:**
- Create: `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheResilienceConfig.kt`
- Create: `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecorator.kt`
- Create: `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecorator.kt`

- [ ] **Step 1: `NearCacheResilienceConfig.kt` 생성**

`NearCacheResilienceConfig` data class + `NearCacheResilienceConfigBuilder` + `nearCacheResilienceConfig {}` DSL 팩토리.
필드: `retryMaxAttempts` (3), `retryWaitDuration` (500ms), `retryExponentialBackoff` (true), `getFailureStrategy` (RETURN_FRONT_OR_NULL).

- [ ] **Step 2: `ResilientNearCacheDecorator.kt` 생성**

`NearCacheOperations<V>`를 감싸는 Decorator.
- Read 메서드: `retry.executeCallable { delegate.get(key) }` + catch에서 `GetFailureStrategy` 적용
- Write 메서드: `retry.executeRunnable { delegate.put(key, value) }`
- Management 메서드: delegate에 직접 위임
- `close()`: delegate.close()

- [ ] **Step 3: `ResilientSuspendNearCacheDecorator.kt` 생성**

`SuspendNearCacheOperations<V>`를 감싸는 Decorator.
- Read/Write: `retry.executeSuspendFunction { delegate.get(key) }` (resilience4j kotlin extension 활용)
- `close()`: delegate.close()

- [ ] **Step 4: `.withResilience {}` 확장 함수 추가**

`NearCacheResilienceConfig.kt` 하단 또는 별도 파일에:
```kotlin
fun <V: Any> NearCacheOperations<V>.withResilience(config: NearCacheResilienceConfig): NearCacheOperations<V>
fun <V: Any> NearCacheOperations<V>.withResilience(init: NearCacheResilienceConfigBuilder.() -> Unit): NearCacheOperations<V>
fun <V: Any> SuspendNearCacheOperations<V>.withResilience(config: NearCacheResilienceConfig): SuspendNearCacheOperations<V>
fun <V: Any> SuspendNearCacheOperations<V>.withResilience(init: NearCacheResilienceConfigBuilder.() -> Unit): SuspendNearCacheOperations<V>
```

- [ ] **Step 5: 컴파일 확인**

Run: `./gradlew :infra:cache-core:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheResilienceConfig.kt \
        infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecorator.kt \
        infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecorator.kt
git commit -m "feat: ResilientNearCacheDecorator + NearCacheResilienceConfig 추가"
```

---

## Task 3: cache-core test fixtures 작성

**Files:**
- Create: `infra/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractNearCacheOperationsTest.kt`
- Create: `infra/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractSuspendNearCacheOperationsTest.kt`

- [ ] **Step 1: `AbstractNearCacheOperationsTest.kt` 작성**

기존 `AbstractNearCacheTest.kt` 패턴 참조. 핵심 차이점:
- `abstract fun createCache(): NearCacheOperations<V>` (단일 캐시, 2-tier 내부)
- `abstract fun sampleValue(): V` / `abstract fun anotherValue(): V`
- 테스트 시나리오: get miss→null, put+get round trip, getAll batch, putIfAbsent→V? 반환,
  replace existing, replace with oldValue, remove, getAndRemove, getAndReplace,
  clearLocal (front only), clearAll (both), containsKey, stats tracking
- Kluent assertions, `@Execution(ExecutionMode.SAME_THREAD)`

- [ ] **Step 2: `AbstractSuspendNearCacheOperationsTest.kt` 작성**

동일 시나리오를 `runTest {}` / `runSuspendIO {}` 로 작성.
- `abstract fun createCache(): SuspendNearCacheOperations<V>`
- `awaitility` + `untilSuspending` 패턴 사용

- [ ] **Step 3: `build.gradle.kts` testFixtures 의존성 확인/추가**

`infra/cache-core/build.gradle.kts`의 testFixtures 블록에 다음 의존성이 있는지 확인하고 없으면 추가:
```kotlin
testFixturesApi(Libs.awaitility_kotlin)
testFixturesApi(Libs.kotlinx_coroutines_test)
testFixturesApi(Libs.kluent)
testFixturesApi(Libs.junit_jupiter)
```

- [ ] **Step 4: 컴파일 확인**

Run: `./gradlew :infra:cache-core:compileKotlin :infra:cache-core:compileTestFixturesKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add infra/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractNearCacheOperationsTest.kt \
        infra/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractSuspendNearCacheOperationsTest.kt \
        infra/cache-core/build.gradle.kts
git commit -m "test: NearCacheOperations 공통 abstract 테스트 클래스 추가"
```

---

## Task 4: cache-lettuce 리팩토링

**Files:**
- Modify: `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt`
- Modify: `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceSuspendNearCache.kt`
- Modify: `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheFactory.kt`
- Delete: `LettuceNearCacheOperations.kt`, `LettuceSuspendNearCacheOperations.kt`
- Delete: `ResilientLettuceNearCache.kt`, `ResilientLettuceSuspendNearCache.kt`, `ResilientLettuceNearCacheConfig.kt`
- Modify/Delete tests: `ResilientLettuceNearCacheTest.kt`, `ResilientLettuceSuspendNearCacheTest.kt`

- [ ] **Step 1: `LettuceNearCache.kt` — `NearCacheOperations<V>` 구현**

- 기존 클래스 선언에 `: NearCacheOperations<V>` 추가 (기존 `LettuceNearCacheOperations<V>` 대체)
- `remove()` 반환 타입 → Unit 확인
- `putIfAbsent()` 반환 타입 → V? 확인
- `stats()` 메서드 추가: Caffeine `CacheStats`에서 localHits/Misses/Evictions 매핑 + AtomicLong backHits/backMisses 카운터 추가
- `get()` 내부: front miss → back 조회 시 `backHits.incrementAndGet()` 또는 `backMisses.incrementAndGet()`
- `localStats(): CacheStats?`는 인터페이스 외 추가 public 메서드로 유지 (Caffeine 상세 통계 접근용)

- [ ] **Step 2: `LettuceSuspendNearCache.kt` — `SuspendNearCacheOperations<V>` 구현**

동일 패턴. suspend 메서드 시그니처 일치 확인.

- [ ] **Step 3: `LettuceNearCacheFactory.kt` — `*Of` 팩토리 패턴으로 변경**

```kotlin
fun <V: Any> lettuceNearCacheOf(
    redisClient: RedisClient,
    codec: RedisCodec<String, V>,
    config: LettuceNearCacheConfig = lettuceNearCacheConfig {},
): NearCacheOperations<V> = LettuceNearCache(redisClient, codec, config)

fun <V: Any> lettuceSuspendNearCacheOf(
    redisClient: RedisClient,
    codec: RedisCodec<String, V>,
    config: LettuceNearCacheConfig = lettuceNearCacheConfig {},
): SuspendNearCacheOperations<V> = LettuceSuspendNearCache(redisClient, codec, config)
```

- [ ] **Step 4: 삭제 대상 파일 삭제**

```bash
git rm infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheOperations.kt
git rm infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceSuspendNearCacheOperations.kt
git rm infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientLettuceNearCache.kt
git rm infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientLettuceSuspendNearCache.kt
git rm infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientLettuceNearCacheConfig.kt
```

- [ ] **Step 5: 테스트 마이그레이션**

- `LettuceNearCacheTest.kt`: `AbstractNearCacheOperationsTest<String>` 상속으로 전환
- `LettuceSuspendNearCacheTest.kt`: `AbstractSuspendNearCacheOperationsTest<String>` 상속으로 전환
- `ResilientLettuceNearCacheTest.kt`, `ResilientLettuceSuspendNearCacheTest.kt`: 삭제 또는 `.withResilience {}` 패턴으로 전환
- `AbstractLettuceNearCacheTest.kt`: 새 abstract 테스트와 통합 여부 확인

- [ ] **Step 6: 컴파일 + 테스트 확인**

Run: `./gradlew :infra:cache-lettuce:compileKotlin`
Expected: BUILD SUCCESSFUL

Run: `./gradlew :infra:cache-lettuce:test`
Expected: 테스트 통과 (Redis 서버 필요 — Testcontainers)

- [ ] **Step 7: 커밋**

```bash
git add -A infra/cache-lettuce/
git commit -m "refactor: cache-lettuce NearCacheOperations 통일 + Resilient 변형 삭제"
```

---

## Task 5: cache-hazelcast 리팩토링

**Files:**
- Modify: `infra/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCache.kt`
- Modify: `infra/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastSuspendNearCache.kt`
- Delete: `ResilientHazelcastNearCache.kt`, `ResilientHazelcastSuspendNearCache.kt`, `ResilientHazelcastNearCacheConfig.kt`
- Modify/Delete tests

- [ ] **Step 1: `HazelcastNearCache.kt` — `NearCacheOperations<V>` 구현**

- `: NearCacheOperations<V>` 추가
- **메서드 이름/타입 변경:**
  - `localSize()` → `localCacheSize()` (인터페이스 이름 일치)
  - `backCacheSize()` 반환 타입: `Int` → `Long` (`imap.size.toLong()`)
- **누락 메서드 구현:**
  - `putIfAbsent()`: `imap.putIfAbsent(key, value)` → 기존 값 V? 반환
  - `replace(key, value)`: `imap.replace(key, value) != null`
  - `replace(key, old, new)`: `imap.replace(key, old, new)`
  - `getAndRemove()`: `imap.remove(key)` (Hazelcast remove는 이전 값 반환)
  - `getAndReplace()`: `imap.replace(key, value)` (이전 값 반환)
- `stats()` 추가: AtomicLong 카운터

- [ ] **Step 2: `HazelcastSuspendNearCache.kt` — `SuspendNearCacheOperations<V>` 구현**

동일 패턴. `imap.putIfAbsentAsync()`, `imap.replaceAsync()` 등 사용.
`putIfAbsent`가 async 미지원 시 `withContext(Dispatchers.IO)` 래핑.
- **`: AutoCloseable` 제거** → `override suspend fun close()` 로 변경
- `localSize()` → `localCacheSize()`, `backCacheSize()` 반환 `Long`

- [ ] **Step 3: 팩토리 함수 추가**

`HazelcastNearCacheFactory.kt` 또는 기존 파일에:
```kotlin
fun <V: Any> hazelcastNearCacheOf(imap: IMap<String, V>, config: HazelcastNearCacheConfig = ...): NearCacheOperations<V>
fun <V: Any> hazelcastSuspendNearCacheOf(imap: IMap<String, V>, config: HazelcastNearCacheConfig = ...): SuspendNearCacheOperations<V>
```

- [ ] **Step 4: Resilient 변형 삭제**

```bash
git rm infra/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientHazelcastNearCache.kt
git rm infra/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientHazelcastSuspendNearCache.kt
git rm infra/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientHazelcastNearCacheConfig.kt
```

- [ ] **Step 5: 테스트 마이그레이션**

- `HazelcastNearCacheTest.kt`, `HazelcastSuspendNearCacheTest.kt`: abstract 테스트 상속으로 전환
- `ResilientHazelcastNearCacheTest.kt`, `ResilientHazelcastSuspendNearCacheTest.kt`: 삭제

- [ ] **Step 6: 컴파일 + 테스트 확인**

Run: `./gradlew :infra:cache-hazelcast:compileKotlin`
Expected: BUILD SUCCESSFUL

Run: `./gradlew :infra:cache-hazelcast:test`
Expected: 테스트 통과

- [ ] **Step 7: 커밋**

```bash
git add -A infra/cache-hazelcast/
git commit -m "refactor: cache-hazelcast NearCacheOperations 통일 + Resilient 변형 삭제"
```

---

## Task 6: cache-redisson 리팩토링

**Files:**
- Create: `infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCache.kt` (새 구현체)
- Create: `infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonSuspendNearCache.kt` (새 구현체)
- Delete: 기존 JCache 팩토리, RESP3 하이브리드, Resilient 변형

**참고:** Redisson은 `RedissonClient`에 내장 retry가 있으므로 `.withResilience {}` 불필요.

- [ ] **Step 0: 기존 파일 삭제 먼저 (이름 충돌 방지)**

기존 `RedissonNearCache.kt`는 `object` (팩토리)이므로, 새 `class RedissonNearCache<V>`와 이름 충돌.
**반드시 삭제 후 새 파일 생성.**

```bash
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCache.kt
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonSuspendNearCache.kt
```

- [ ] **Step 1: 새 `RedissonNearCache.kt` 작성**

`NearCacheOperations<V>` 직접 구현.
- front: Caffeine 캐시
- back: Redisson `RLocalCachedMap` 기반 (`LocalCachedMapOptions` 사용)
- **invalidation: `LocalCachedMapOptions`** — Redisson 내장 client-side caching 활용
  - `LocalCachedMapOptions.create().cacheSize(maxLocalSize).evictionPolicy(LRU).syncStrategy(INVALIDATE)`
  - Redisson이 자동으로 invalidation 관리 (topic 기반 수동 구현 불필요)
- 키 패턴: `cacheName:key` (Lettuce와 동일)
- `put()`: front + `map.put(redisKey, value)`
- `get()`: front miss → `map.get(redisKey)` → front populate
- `remove()`: front evict + `map.remove(redisKey)`

- [ ] **Step 2: 새 `RedissonSuspendNearCache.kt` 작성**

- back: `getBucket(redisKey).getAsync().await()`, `setAsync().await()`, `deleteAsync().await()`

- [ ] **Step 3: 팩토리 함수 추가**

```kotlin
fun <V: Any> redissonNearCacheOf(redisson: RedissonClient, config: RedissonNearCacheConfig = ...): NearCacheOperations<V>
fun <V: Any> redissonSuspendNearCacheOf(redisson: RedissonClient, config: RedissonNearCacheConfig = ...): SuspendNearCacheOperations<V>
```

`RedissonResp3NearCacheConfig.kt`를 `RedissonNearCacheConfig.kt`로 리팩토링 또는 신규 작성.

- [ ] **Step 4: 나머지 기존 파일 삭제**

```bash
# 기존 JCache 팩토리
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCachingProvider.kt
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheManager.kt
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfig.kt

# RESP3 하이브리드
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonResp3NearCache.kt
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonResp3SuspendNearCache.kt
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonResp3NearCacheConfig.kt
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonTrackingInvalidationListener.kt
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonLocalCache.kt

# Resilient 변형
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientRedissonResp3NearCache.kt
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientRedissonResp3SuspendNearCache.kt
git rm infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientRedissonResp3NearCacheConfig.kt
```

- [ ] **Step 4a: `build.gradle.kts` 의존성 정리**

RESP3 하이브리드 삭제 후 불필요한 의존성 제거 검토:
- `implementation(Libs.lettuce_core)` — Lettuce tracking 삭제로 불필요하면 제거
- `implementation(Libs.kotlinx_coroutines_reactive)` — Lettuce coroutines API 삭제로 불필요하면 제거

- [ ] **Step 5: SPI 파일 정리**

`infra/cache-redisson/src/main/resources/META-INF/services/javax.cache.spi.CachingProvider`에서
`RedissonNearCachingProvider` 항목 제거.

- [ ] **Step 6: 테스트 마이그레이션**

- 새 `RedissonNearCacheTest.kt`: `AbstractNearCacheOperationsTest<String>` 상속
- 새 `RedissonSuspendNearCacheTest.kt`: `AbstractSuspendNearCacheOperationsTest<String>` 상속
- 기존 테스트 삭제: `RedissonResp3NearCacheTest.kt`, `RedissonResp3SuspendNearCacheTest.kt`, `ResilientRedissonResp3*Test.kt`, `RedissonNearCachingProviderTest.kt`, `RedissonNearCacheManagerTest.kt`
- `AbstractRedissonResp3NearCacheTest.kt`: 삭제
- `RedissonResp3NearCacheTrackingTest.kt`: 삭제 (LocalCachedMapOptions가 tracking 대체)
- **`spring/SpringCacheUsingNearCacheTest.kt`**: `RedissonNearCacheConfig`, `RedissonNearCachingProvider` 임포트 → 새 구현체 기반으로 리팩토링 또는 삭제

- [ ] **Step 7: 컴파일 + 테스트 확인**

Run: `./gradlew :infra:cache-redisson:compileKotlin`
Expected: BUILD SUCCESSFUL

Run: `./gradlew :infra:cache-redisson:test`
Expected: 테스트 통과

- [ ] **Step 8: 커밋**

```bash
git add -A infra/cache-redisson/
git commit -m "refactor: cache-redisson NearCacheOperations 직접 구현 + RESP3/JCache 팩토리 삭제"
```

---

## Task 7: cache-core JCache NearCache 리팩토링

**Files:**
- Rename+Modify: `NearCache.kt` → `JCacheNearCache.kt`
- Rename+Modify: `SuspendNearCache.kt` → `JCacheSuspendNearCache.kt`
- Delete: `ResilientNearCache.kt`, `ResilientSuspendNearCache.kt`, `ResilientNearCacheConfig.kt`, `ResilientNearCacheLocalCache.kt`, `BackCacheCommand.kt`
- Update: `NearCacheConfig.kt`

- [ ] **Step 1: `NearCache.kt` → `JCacheNearCache.kt` 리팩토링**

- 클래스명 변경: `NearCache<K, V>` → `JCacheNearCache<V>`
- K 파라미터 제거, String으로 고정
- `: NearCacheOperations<V>` 구현
- **`by backCache` 위임 제거** — JCache `putIfAbsent()` → `Boolean`, 새 인터페이스 → `V?`. 수동 위임으로 변환:
  - `putIfAbsent()`: `val existing = backCache.get(key); if (existing != null) return existing; backCache.put(key, value); return null`
  - `remove()`: `backCache.remove(key)` (Boolean 반환값 무시)
- `stats()` 추가: Caffeine stats + AtomicLong 카운터
- 팩토리 함수:
  ```kotlin
  fun <V: Any> jcacheNearCacheOf(backCache: Cache<String, V>, config: NearCacheConfig = ...): NearCacheOperations<V>
  ```

- [ ] **Step 2: `SuspendNearCache.kt` → `JCacheSuspendNearCache.kt` 리팩토링**

동일 패턴. `SuspendNearCacheOperations<V>` 구현.

- [ ] **Step 3: Resilient + dead code 삭제**

```bash
git rm infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCache.kt
git rm infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCache.kt
git rm infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheConfig.kt
git rm infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheLocalCache.kt
git rm infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/BackCacheCommand.kt
```

- [ ] **Step 4: 기존 test fixture 및 테스트 처리**

- `AbstractNearCacheTest.kt`, `AbstractSuspendNearCacheTest.kt`: **삭제하지 않고** `JCacheNearCache` 사용으로 업데이트.
  이들은 JCache 2-NearCache-shared-back-cache 패턴을 테스트하는 것으로, 새 `AbstractNearCacheOperationsTest`와는 별개.
  `putIfAbsent()` 관련 assertion (`shouldBeTrue/shouldBeFalse`) → V? 반환 기반으로 변경 (`shouldBeNull`/`shouldNotBeNull`).
- 기존 JCache 테스트(`Cache2kNearCacheTest.kt`, `EhcacheNearCacheTest.kt`): `NearCache` → `JCacheNearCache` 클래스명 변경 반영.
- `ResilientNearCacheTest.kt`, `ResilientSuspendNearCacheTest.kt`: 삭제 (Decorator 전용 테스트가 Task 8에서 대체).

- [ ] **Step 5: 컴파일 + 테스트 확인**

Run: `./gradlew :infra:cache-core:compileKotlin :infra:cache-core:test`
Expected: BUILD SUCCESSFUL + 테스트 통과

- [ ] **Step 6: 커밋**

```bash
git add -A infra/cache-core/
git commit -m "refactor: cache-core NearCache → JCacheNearCache + Resilient dead code 삭제"
```

---

## Task 8: Resilient Decorator 테스트 (cache-core)

**Files:**
- Create: `infra/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecoratorTest.kt`
- Create: `infra/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecoratorTest.kt`

- [ ] **Step 1: `ResilientNearCacheDecoratorTest.kt` 작성**

MockK로 `NearCacheOperations<String>` mock 생성.
- `get()` 실패 시 retry 동작 검증 (N회 실패 후 성공)
- `get()` 실패 시 `RETURN_FRONT_OR_NULL` 전략 검증
- `get()` 실패 시 `PROPAGATE_EXCEPTION` 전략 검증
- `put()` 실패 시 retry 동작 검증
- retry 횟수 초과 시 예외 전파 확인
- `close()` 위임 확인

- [ ] **Step 2: `ResilientSuspendNearCacheDecoratorTest.kt` 작성**

동일 시나리오, `coEvery`/`coVerify` 사용.

- [ ] **Step 3: 테스트 실행**

Run: `./gradlew :infra:cache-core:test --tests "*.ResilientNearCacheDecoratorTest" --tests "*.ResilientSuspendNearCacheDecoratorTest"`
Expected: 모든 테스트 통과

- [ ] **Step 4: 커밋**

```bash
git add infra/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecoratorTest.kt \
        infra/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecoratorTest.kt
git commit -m "test: ResilientNearCacheDecorator 단위 테스트 추가"
```

---

## Task 9: 전체 통합 확인 + CLAUDE.md 업데이트

- [ ] **Step 1: 전체 cache 모듈 빌드**

Run: `./gradlew :infra:cache-core:build :infra:cache-lettuce:build :infra:cache-hazelcast:build :infra:cache-redisson:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 전체 cache 모듈 테스트**

Run: `./gradlew :infra:cache-core:test :infra:cache-lettuce:test :infra:cache-hazelcast:test :infra:cache-redisson:test`
Expected: 모든 테스트 통과

- [ ] **Step 3: CLAUDE.md 업데이트**

`cache-*` 모듈 설명에 `NearCacheOperations`/`SuspendNearCacheOperations` 통일 인터페이스 반영.
Resilient Decorator 패턴 설명 추가. `*Of` 팩토리 패턴 설명.

- [ ] **Step 4: 최종 커밋**

```bash
git add CLAUDE.md
git commit -m "docs: CLAUDE.md NearCache 통일 인터페이스 반영"
```

---

## 의존성 순서 요약

```
Task 1 (인터페이스) ──┐
Task 2 (Decorator)  ──┤
Task 3 (test fixtures)┤
                      ├── Task 4 (Lettuce)  ─┐
                      ├── Task 5 (Hazelcast) ─┤
                      ├── Task 6 (Redisson)  ─┤
                      └── Task 7 (JCache)    ─┤
                                              ├── Task 8 (Decorator 테스트)
                                              └── Task 9 (통합 확인)
```

- Task 1~3: 순차 (인터페이스 → Decorator → test fixtures)
- Task 4~7: **병렬 가능** (각 모듈 독립)
- Task 8~9: Task 4~7 완료 후
