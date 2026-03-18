# Cache 모듈 일관성 리팩토링 구현 계획

> **For agentic workers:
** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (
`- [ ]`) syntax for tracking.

**Goal:** Cache 모듈(cache-core, cache-lettuce, cache-hazelcast, cache-redisson) 간 팩토리 API 일관성 개선 및 LettuceBinaryCodec 통일

**Architecture:
** NearJCacheConfig Builder DSL을 cache-core에 추가하고, 각 모듈의 팩토리 함수를 통일된 네이밍/파라미터로 리팩토링. Lettuce 계열은 BinarySerializer → LettuceBinaryCodec으로 통일.

**Tech Stack:** Kotlin 2.3, JCache (JSR-107), Lettuce, Hazelcast, Redisson, Caffeine, JUnit 5, Kluent

**Spec:** `docs/superpowers/specs/2026-03-18-cache-consistency-refactoring-design.md`

---

## Task 1: NearJCacheConfig Builder DSL (cache-core)

**Files:**

- Create: `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilder.kt`
- Modify: `infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfig.kt`
- Create: `infra/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilderTest.kt`

- [ ] **Step 1: NearJCacheConfigBuilder 클래스 생성**

`infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilder.kt`:

```kotlin
package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.codec.Base58
import javax.cache.CacheManager
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration

/**
 * [NearJCacheConfig]를 DSL 방식으로 생성하기 위한 빌더 클래스입니다.
 *
 * ```kotlin
 * val config = nearJCacheConfig<String, MyValue> {
 *     cacheName = "my-near-cache"
 *     isSynchronous = true
 *     syncRemoteTimeout = 1000L
 * }
 * ```

*/ class NearJCacheConfigBuilder<K: Any, V: Any> {

    /** Front Cache용 [CacheManager] 팩토리. 기본값: Caffeine */
    var cacheManagerFactory: Factory<CacheManager> = NearJCacheConfig.CaffeineCacheManagerFactory

    /** 캐시 이름 */
    var cacheName: String = "near-jcache-" + Base58.randomString(8)

    /** Front Cache 설정. 기본값: 접근 기준 30분 만료 */
    var frontCacheConfiguration: MutableConfiguration<K, V> = NearJCacheConfig.getDefaultFrontCacheConfiguration()

    /** Back Cache 이벤트 동기화 여부. 기본값: false (비동기) */
    var isSynchronous: Boolean = false

    /** 원격 캐시 동기화 타임아웃 (밀리초). 기본값: 500ms */
    var syncRemoteTimeout: Long = NearJCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT

    /**
     * 설정값으로 [NearJCacheConfig] 인스턴스를 생성합니다.
     */
    fun build(): NearJCacheConfig<K, V> = NearJCacheConfig(
        cacheManagerFactory = cacheManagerFactory,
        cacheName = cacheName,
        frontCacheConfiguration = frontCacheConfiguration,
        isSynchronous = isSynchronous,
        syncRemoteTimeout = syncRemoteTimeout,
    )

}

/**

* DSL 방식으로 [NearJCacheConfig]를 생성합니다.
*
* ```kotlin
* val config = nearJCacheConfig<String, MyValue> {
*     cacheName = "my-near-cache"
*     isSynchronous = true
* }
* ```

*/ inline fun <K: Any, V: Any> nearJCacheConfig(
block: NearJCacheConfigBuilder<K, V>.() -> Unit,
): NearJCacheConfig<K, V> = NearJCacheConfigBuilder<K, V>().apply(block).build()

```

- [ ] **Step 2: 빌더 테스트 작성**

`infra/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilderTest.kt`:

```kotlin
package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeBlank
import org.junit.jupiter.api.Test

class NearJCacheConfigBuilderTest {

    companion object: KLogging()

    @Test
    fun `기본값으로 NearJCacheConfig 생성`() {
        val config = nearJCacheConfig<String, String> { }

        config.cacheName.shouldNotBeBlank()
        config.isSynchronous shouldBeEqualTo false
        config.syncRemoteTimeout shouldBeEqualTo NearJCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT
    }

    @Test
    fun `DSL로 커스텀 NearJCacheConfig 생성`() {
        val config = nearJCacheConfig<String, String> {
            cacheName = "test-cache"
            isSynchronous = true
            syncRemoteTimeout = 1000L
        }

        config.cacheName shouldBeEqualTo "test-cache"
        config.isSynchronous shouldBeEqualTo true
        config.syncRemoteTimeout shouldBeEqualTo 1000L
    }
}
```

- [ ] **Step 3: 테스트 실행**

Run: `./gradlew :infra:cache-core:test --tests "*.NearJCacheConfigBuilderTest" --info`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilder.kt
git add infra/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilderTest.kt
git commit -m "feat(cache-core): NearJCacheConfig Builder DSL 추가"
```

---

## Task 2: HazelcastCaches nearJCache/suspendNearJCache 파라미터 축소

**Files:**

- Modify: `infra/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt`
- Modify: `infra/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastCachesTest.kt`
- Modify: 기타 nearJCache/suspendNearJCache 호출하는 테스트 파일

**의존성:** Task 1 완료 필요 (NearJCacheConfigBuilder)

- [ ] **Step 1: HazelcastCaches.nearJCache 변경**

`infra/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt` 수정:

기존 `nearJCache(frontCache, hazelcastInstance, configuration, nearCacheCfg)` 제거하고:

```kotlin
/**
 * Hazelcast 기반 [NearJCache]를 DSL로 생성합니다.
 *
 * ```kotlin
 * val cache = HazelcastCaches.nearJCache<String, MyValue>(hazelcastInstance) {
 *     cacheName = "my-near-cache"
 *     isSynchronous = true
 * }
 * ```

*/ inline fun <reified K: Any, reified V: Any> nearJCache(
hazelcastInstance: HazelcastInstance, block: NearJCacheConfigBuilder<K, V>.() -> Unit,
): NearJCache<K, V> { val config = nearJCacheConfig(block)
return nearJCache(hazelcastInstance, config)
}

/**

* Hazelcast 기반 [NearJCache]를 [NearJCacheConfig]로 생성합니다.
  */ inline fun <reified K: Any, reified V: Any> nearJCache(
  hazelcastInstance: HazelcastInstance, config: NearJCacheConfig<K, V>,
  ): NearJCache<K, V> { val backCache: JCache<K, V> = HazelcastJCaching.getOrCreate(
  hazelcastInstance, config.cacheName, getDefaultJCacheConfiguration(),
  )
  return NearJCache(config, backCache)
  }

```

- [ ] **Step 2: HazelcastCaches.suspendNearJCache 변경**

동일 파일에서 기존 `suspendNearJCache(frontCache, hazelcastInstance, configuration, nearCacheCfg)` 제거하고:

```kotlin
/**
 * Hazelcast 기반 [SuspendNearJCache]를 DSL로 생성합니다.
 */
inline fun <reified K: Any, reified V: Any> suspendNearJCache(
    hazelcastInstance: HazelcastInstance,
    block: NearJCacheConfigBuilder<K, V>.() -> Unit,
): SuspendNearJCache<K, V> {
    val config = nearJCacheConfig(block)
    return suspendNearJCache(hazelcastInstance, config)
}

/**
 * Hazelcast 기반 [SuspendNearJCache]를 [NearJCacheConfig]로 생성합니다.
 */
inline fun <reified K: Any, reified V: Any> suspendNearJCache(
    hazelcastInstance: HazelcastInstance,
    config: NearJCacheConfig<K, V>,
): SuspendNearJCache<K, V> {
    val backJCache: JCache<K, V> = HazelcastJCaching.getOrCreate(
        hazelcastInstance,
        config.cacheName,
        getDefaultJCacheConfiguration(),
    )
    val backCache = HazelcastSuspendJCache(backJCache)

    // front cache 생성 — CaffeineSuspendJCache는 AsyncCache를 받으므로 Caffeine에서 직접 생성
    val frontCache = CaffeineSuspendJCache<K, V> {
        maximumSize(10_000)
        expireAfterAccess(30, java.util.concurrent.TimeUnit.MINUTES)
    }

    return SuspendNearJCache(frontCache, backCache)
}
```

참고: `SuspendNearJCache.invoke(front, back)`는 front/back을 직접 받으며, `CaffeineSuspendJCache`는
`AsyncCache`를 받으므로 companion invoke의 Caffeine builder DSL을 사용합니다.

- [ ] **Step 3: 테스트 코드 추가**

기존 `HazelcastCachesTest.kt`에는 nearJCache/suspendNearJCache 테스트가 없으므로 새로 추가합니다:

```kotlin
@Test
fun `nearJCache DSL로 생성`() {
    val cache = HazelcastCaches.nearJCache<String, String>(hazelcastInstance) {
        cacheName = "test-near-jcache-${Base58.randomString(8)}"
    }
    cache.put("k1", "v1")
    cache.getDeeply("k1") shouldBeEqualTo "v1"
    cache.close()
}

@Test
fun `suspendNearJCache DSL로 생성`() = runTest {
    val cache = HazelcastCaches.suspendNearJCache<String, String>(hazelcastInstance) {
        cacheName = "test-suspend-near-jcache-${Base58.randomString(8)}"
    }
    cache.put("k1", "v1")
    cache.getDeeply("k1") shouldBeEqualTo "v1"
    cache.close()
}
```

기존 nearJCache 호출하는 테스트 파일(nearcache/jcache/ 하위)도 새 시그니처에 맞게 수정합니다.

- [ ] **Step 4: 테스트 실행**

Run: `./gradlew :infra:cache-hazelcast:test --info`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add infra/cache-hazelcast/
git commit -m "refactor(cache-hazelcast): nearJCache/suspendNearJCache 파라미터 2개로 축소 + DSL 지원"
```

---

## Task 3: LettuceBinaryCodec.serializer public 변경 + LettuceJCache 통일

**Files:**

- Modify: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt`
- Modify: `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt`
- Modify: `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheConfig.kt`
- Modify:
  `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheManager.kt` (createCache에서 codec 전달)
- Modify:
  `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCaching.kt` (getOrCreate에서 serializer → codec)
- Modify: 관련 테스트 파일

- [ ] **Step 1: LettuceBinaryCodec.serializer를 public으로 변경**

`infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt`:

```kotlin
// 변경 전
class LettuceBinaryCodec<V: Any>(
    private val serializer: BinarySerializer,
): RedisCodec<String, V>, ToByteBufEncoder<String, V>

// 변경 후
class LettuceBinaryCodec<V: Any>(
    val serializer: BinarySerializer,
): RedisCodec<String, V>, ToByteBufEncoder<String, V>
```

- [ ] **Step 2: LettuceCacheConfig에 codec 파라미터 추가**

`infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheConfig.kt`:

```kotlin
// 변경 전
class LettuceCacheConfig<K: Any, V: Any>(
    val ttlSeconds: Long? = null,
    val keyCodec: ((K) -> String)? = null,
    val keyDecoder: ((String) -> K)? = null,
    val serializer: BinarySerializer = BinarySerializers.Fory,
    keyType: Class<K>,
    valueType: Class<V>,
): MutableConfiguration<K, V>()

// 변경 후 - serializer를 codec에서 추출
class LettuceCacheConfig<K: Any, V: Any>(
    val ttlSeconds: Long? = null,
    val keyCodec: ((K) -> String)? = null,
    val keyDecoder: ((String) -> K)? = null,
    val codec: LettuceBinaryCodec<*> = LettuceBinaryCodecs.lz4Fory(),
    keyType: Class<K>,
    valueType: Class<V>,
): MutableConfiguration<K, V>()
```

`lettuceCacheConfigOf` DSL 함수도 동일하게 변경:

```kotlin
inline fun <reified K: Any, reified V: Any> lettuceCacheConfigOf(
    ttlSeconds: Long? = null,
    noinline keyCodec: ((K) -> String)? = null,
    noinline keyDecoder: ((String) -> K)? = null,
    codec: LettuceBinaryCodec<*> = LettuceBinaryCodecs.lz4Fory(),
): LettuceCacheConfig<K, V>
```

- [ ] **Step 3: LettuceJCache 생성자 변경**

`infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt`:

```kotlin
// 변경 전
class LettuceJCache<K: Any, V: Any>(
    private val map: LettuceMap<ByteArray>,
    private val keyCodec: (K) -> String = { it.toString() },
    private val keyDecoder: ((String) -> K)? = null,
    private val serializer: BinarySerializer = BinarySerializers.Fory,
    ...

// 변경 후
class LettuceJCache<K: Any, V: Any>(
    private val map: LettuceMap<ByteArray>,
    private val keyCodec: (K) -> String = { it.toString() },
    private val keyDecoder: ((String) -> K)? = null,
    private val codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.lz4Fory(),
    ...
```

내부에서 `serializer.serialize(value)` → `codec.serializer.serialize(value)`, `serializer.deserialize(bytes)` →
`codec.serializer.deserialize(bytes)`로 일괄 변경합니다.

- [ ] **Step 4: LettuceCacheManager.createCache 수정**

`LettuceCacheManager.createCache()`에서 `LettuceCacheConfig.serializer` → `LettuceCacheConfig.codec` 사용:

```kotlin
// LettuceJCache 생성 시 codec 전달
val config = configuration as? LettuceCacheConfig<K, V>
LettuceJCache(
    map = ...,
    keyCodec = config?.keyCodec ?: { it.toString() },
    keyDecoder = config?.keyDecoder,
    codec = config?.codec as? LettuceBinaryCodec<V> ?: LettuceBinaryCodecs.lz4Fory(),
    ttlSeconds = config?.ttlSeconds,
    cacheManager = this,
    configuration = configuration,
    closeResource = ...,
)
```

- [ ] **Step 5: 테스트 코드 수정**

`LettuceJCache` 관련 테스트에서 `serializer = BinarySerializers.Fory` → `codec = LettuceBinaryCodecs.fory()` 등으로 변경.

- [ ] **Step 6: 테스트 실행**

Run: `./gradlew :infra:lettuce:test :infra:cache-lettuce:test --info`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt
git add infra/cache-lettuce/
git commit -m "refactor(cache-lettuce): BinarySerializer → LettuceBinaryCodec 통일"
```

---

## Task 4: LettuceSuspendCacheManager/LettuceCacheManager 파라미터 활용

**Files:**

- Modify: `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendCacheManager.kt`
- Modify: `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheManager.kt`
- Modify: `infra/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCacheManagerTest.kt`
- Modify: `infra/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceJCacheManagerTest.kt`

**의존성:** Task 3 완료 필요

- [ ] **Step 1: LettuceSuspendCacheManager 생성자 변경**

```kotlin
// 변경 전
class LettuceSuspendCacheManager(
    val redisClient: RedisClient,
    val ttlSeconds: Long? = null,
    val codec: LettuceBinaryCodec<Any>? = null,
)

// 변경 후
class LettuceSuspendCacheManager(
    val redisClient: RedisClient,
    val defaultTtlSeconds: Long? = null,
    val defaultCodec: LettuceBinaryCodec<Any> = LettuceBinaryCodecs.lz4Fory(),
)
```

- [ ] **Step 2: getOrCreate에서 파라미터 실제 활용**

```kotlin
fun <V: Any> getOrCreate(
    cacheName: String,
    ttlSeconds: Long? = this.defaultTtlSeconds,
    codec: LettuceBinaryCodec<V>? = null,
): LettuceSuspendJCache<V> {
    checkNotClosed()
    cacheName.requireNotBlank("cacheName")

    return caches.computeIfAbsent(cacheName) { name ->
        log.info { "Create LettuceSuspendCache. name=$name, ttlSeconds=$ttlSeconds" }
        @Suppress("UNCHECKED_CAST")
        val effectiveCodec = codec ?: (defaultCodec as LettuceBinaryCodec<V>)
        val config = lettuceCacheConfigOf<String, V>(
            ttlSeconds = ttlSeconds,
            codec = effectiveCodec,
        )
        val jcache = jcacheManager.getOrCreate(name, config)
        LettuceSuspendJCache(jcache as LettuceJCache<String, V>)
    } as LettuceSuspendJCache<V>
}
```

- [ ] **Step 3: 테스트 코드 수정**

`LettuceSuspendJCacheManagerTest`에서 생성자 파라미터명 변경 반영 및 codec/ttl 활용 테스트 추가:

```kotlin
@Test
fun `매니저 기본 TTL이 캐시에 적용되는지 확인`() = runTest {
    val manager = LettuceSuspendCacheManager(
        redisClient = redisClient,
        defaultTtlSeconds = 60L,
        defaultCodec = LettuceBinaryCodecs.lz4Fory(),
    )
    val cache = manager.getOrCreate<String>("ttl-test-cache")
    cache.put("key", "value")
    cache.get("key") shouldBeEqualTo "value"
    manager.close()
}
```

- [ ] **Step 4: 테스트 실행**

Run:
`./gradlew :infra:cache-lettuce:test --tests "*.LettuceSuspendJCacheManagerTest" --tests "*.LettuceJCacheManagerTest" --info`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add infra/cache-lettuce/
git commit -m "refactor(cache-lettuce): LettuceSuspendCacheManager 미사용 파라미터 실제 활용"
```

---

## Task 5: LettuceCaches 팩토리 함수 추가

**Files:**

- Modify: `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt`
- Modify/Create: `infra/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/LettuceCachesTest.kt`

**의존성:** Task 3, 4 완료 필요

- [ ] **Step 1: 기존 jcache() 시그니처 변경**

```kotlin
// 변경 전
inline fun <reified K : Any, reified V : Any> jcache(
    redisClient: RedisClient,
    cacheName: String,
    ttlSeconds: Long? = null,
    serializer: BinarySerializer = BinarySerializers.Fory,
): JCache<K, V>

// 변경 후
inline fun <reified K : Any, reified V : Any> jcache(
    redisClient: RedisClient,
    cacheName: String,
    ttlSeconds: Long? = null,
    codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.lz4Fory(),
): JCache<K, V>
```

내부 구현에서 `lettuceCacheConfigOf(serializer = ...)` → `lettuceCacheConfigOf(codec = ...)` 변경.

- [ ] **Step 2: suspendJCache() 추가**

```kotlin
/**
 * Lettuce 기반 [LettuceSuspendJCache]를 생성합니다.
 */
inline fun <reified V : Any> suspendJCache(
    redisClient: RedisClient,
    cacheName: String,
    ttlSeconds: Long? = null,
    codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.lz4Fory(),
): LettuceSuspendJCache<V> {
    val jcache = jcache<String, V>(redisClient, cacheName, ttlSeconds, codec)
    return LettuceSuspendJCache(jcache as LettuceJCache<String, V>)
}
```

- [ ] **Step 3: nearJCache() 추가**

```kotlin
/**
 * Lettuce JCache 기반 [NearJCache]를 DSL로 생성합니다.
 */
inline fun <reified K : Any, reified V : Any> nearJCache(
    redisClient: RedisClient,
    codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.lz4Fory(),
    block: NearJCacheConfigBuilder<K, V>.() -> Unit,
): NearJCache<K, V> {
    val config = nearJCacheConfig(block)
    return nearJCache(redisClient, config, codec)
}

/**
 * Lettuce JCache 기반 [NearJCache]를 [NearJCacheConfig]로 생성합니다.
 */
inline fun <reified K : Any, reified V : Any> nearJCache(
    redisClient: RedisClient,
    config: NearJCacheConfig<K, V>,
    codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.lz4Fory(),
): NearJCache<K, V> {
    val backCache = jcache<K, V>(redisClient, config.cacheName, codec = codec)
    return NearJCache(config, backCache)
}
```

- [ ] **Step 4: suspendNearJCache() 추가**

```kotlin
/**
 * Lettuce JCache 기반 [SuspendNearJCache]를 DSL로 생성합니다.
 */
inline fun <reified V : Any> suspendNearJCache(
    redisClient: RedisClient,
    codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.lz4Fory(),
    block: NearJCacheConfigBuilder<String, V>.() -> Unit,
): SuspendNearJCache<String, V> {
    val config = nearJCacheConfig(block)
    return suspendNearJCache(redisClient, config, codec)
}

/**
 * Lettuce JCache 기반 [SuspendNearJCache]를 [NearJCacheConfig]로 생성합니다.
 */
inline fun <reified V : Any> suspendNearJCache(
    redisClient: RedisClient,
    config: NearJCacheConfig<String, V>,
    codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.lz4Fory(),
): SuspendNearJCache<String, V> {
    val backJCache = jcache<String, V>(redisClient, config.cacheName, codec = codec)
    val backCache = LettuceSuspendJCache(backJCache as LettuceJCache<String, V>)

    // front cache 생성 — CaffeineSuspendJCache는 AsyncCache를 받으므로 Caffeine builder 사용
    val frontCache = CaffeineSuspendJCache<String, V> {
        maximumSize(10_000)
        expireAfterAccess(30, java.util.concurrent.TimeUnit.MINUTES)
    }

    return SuspendNearJCache(frontCache, backCache)
}
```

- [ ] **Step 5: 테스트 추가**

기존 `LettuceCachesTest`에 새 팩토리 함수 테스트 추가:

```kotlin
@Test
fun `suspendJCache 생성 및 기본 동작`() = runTest {
    val cache = LettuceCaches.suspendJCache<String>(redisClient, "suspend-test")
    cache.put("k1", "v1")
    cache.get("k1") shouldBeEqualTo "v1"
    cache.close()
}

@Test
fun `nearJCache DSL로 생성`() {
    val cache = LettuceCaches.nearJCache<String, String>(redisClient) {
        cacheName = "near-jcache-test"
    }
    cache.put("k1", "v1")
    cache.get("k1") shouldBeEqualTo "v1"
    cache.close()
}

@Test
fun `suspendNearJCache DSL로 생성`() = runTest {
    val cache = LettuceCaches.suspendNearJCache<String, String>(redisClient) {
        cacheName = "suspend-near-jcache-test"
    }
    cache.put("k1", "v1")
    cache.get("k1") shouldBeEqualTo "v1"
    cache.close()
}
```

- [ ] **Step 6: 테스트 실행**

Run: `./gradlew :infra:cache-lettuce:test --info`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add infra/cache-lettuce/
git commit -m "feat(cache-lettuce): LettuceCaches에 suspendJCache/nearJCache/suspendNearJCache 팩토리 추가"
```

---

## Task 6: RedissonCaches 네이밍 통일

**Files:**

- Modify: `infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt`
- Modify: `infra/cache-redisson/src/test/kotlin/io/bluetape4k/cache/RedissonCachesTest.kt`
- Modify: 기타 `nearCacheOps`/`suspendNearCacheOps` 호출하는 파일

- [ ] **Step 1: JCache 기반 nearCache() → nearJCache() 이름 변경**

`infra/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt`:

```kotlin
// 변경 전
fun <K: Any, V: Any> nearCache(
    backCache: JCache<K, V>,
    nearJCacheConfig: NearJCacheConfig<K, V> = NearJCacheConfig(),
): NearJCache<K, V>

inline fun <reified K: Any, reified V: Any> nearCache(
    backCacheName: String,
    redisson: RedissonClient,
    ...
): NearJCache<K, V>

// 변경 후
fun <K: Any, V: Any> nearJCache(
    backCache: JCache<K, V>,
    nearJCacheConfig: NearJCacheConfig<K, V> = NearJCacheConfig(),
): NearJCache<K, V>

inline fun <reified K: Any, reified V: Any> nearJCache(
    backCacheName: String,
    redisson: RedissonClient,
    ...
): NearJCache<K, V>
```

- [ ] **Step 2: JCache 기반 suspendNearCache() → suspendNearJCache() 이름 변경**

```kotlin
// 변경 전
fun <K: Any, V: Any> suspendNearCache(...)
inline fun <reified K: Any, reified V: Any> suspendNearCache(...)

// 변경 후
fun <K: Any, V: Any> suspendNearJCache(...)
inline fun <reified K: Any, reified V: Any> suspendNearJCache(...)
```

- [ ] **Step 3: nearCacheOps() → nearCache() 이름 변경**

```kotlin
// 변경 전
fun <V: Any> nearCacheOps(
    redisson: RedissonClient,
    config: RedissonNearCacheConfig = RedissonNearCacheConfig(),
    codec: Codec = RedissonCodecs.LZ4Fory,
): NearCacheOperations<V>

// 변경 후
fun <V: Any> nearCache(
    redisson: RedissonClient,
    config: RedissonNearCacheConfig = RedissonNearCacheConfig(),
    codec: Codec = RedissonCodecs.LZ4Fory,
): NearCacheOperations<V>
```

- [ ] **Step 4: suspendNearCacheOps() → suspendNearCache() 이름 변경**

```kotlin
// 변경 전
fun <V: Any> suspendNearCacheOps(...): SuspendNearCacheOperations<V>

// 변경 후
fun <V: Any> suspendNearCache(...): SuspendNearCacheOperations<V>
```

- [ ] **Step 5: 모든 호출처 수정**

`nearCacheOps` → `nearCache`, `suspendNearCacheOps` → `suspendNearCache` 전체 검색 후 변경. JCache 기반 `nearCache(` →
`nearJCache(`, `suspendNearCache(` (SuspendNearJCache 반환) → `suspendNearJCache(` 변경.

테스트 파일 포함:

- `RedissonCachesTest.kt`
- 기타 호출처

- [ ] **Step 6: 테스트 실행**

Run: `./gradlew :infra:cache-redisson:test --info`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add infra/cache-redisson/
git commit -m "refactor(cache-redisson): RedissonCaches 팩토리 네이밍 통일 (nearJCache/suspendNearJCache/nearCache/suspendNearCache)"
```

---

## Task 7: 전체 빌드 검증

- [ ] **Step 1: cache 전체 모듈 빌드**

Run:
`./gradlew :infra:cache-core:build :infra:cache-hazelcast:build :infra:cache-lettuce:build :infra:cache-redisson:build --info`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: cache umbrella 모듈 테스트**

Run: `./gradlew :infra:cache:test --info`
Expected: PASS

- [ ] **Step 3: nearCacheOps/suspendNearCacheOps 잔여 참조 검색**

Run: `rg "nearCacheOps\|suspendNearCacheOps" --type kotlin`
Expected: 0 matches (모든 참조 변경 완료)

- [ ] **Step 4: BinarySerializers.Fory 잔여 참조 검색 (cache-lettuce 내)**

Run: `rg "BinarySerializers\." infra/cache-lettuce/ --type kotlin`
Expected: 0 matches (모두 LettuceBinaryCodecs로 변경됨)

- [ ] **Step 5: 최종 커밋 (필요시)**

변경 사항이 있으면 커밋합니다.
