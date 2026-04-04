package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig

/**
 * [SuspendNearCacheOperations] Resilience Decorator (Coroutine Suspend).
 *
 * delegate의 모든 연산에 resilience4j [Retry]를 적용합니다.
 * delegate의 write-through 원자성을 유지하며, write-behind는 사용하지 않습니다.
 *
 * ```kotlin
 * val cache = lettuceSuspendNearCacheOf<String>(redisClient, codec, config)
 *     .withResilience { retryMaxAttempts = 3 }
 * cache.put("hello", "world")
 * val value = cache.get("hello")
 * // value == "world"
 * ```
 *
 * @param V 캐시 값 타입
 * @param delegate 감쌀 SuspendNearCacheOperations 구현체
 * @param config Resilience 설정
 * @see ResilientNearCacheDecorator blocking 버전
 */
class ResilientSuspendNearCacheDecorator<V: Any>(
    private val delegate: SuspendNearCacheOperations<V>,
    private val config: NearCacheResilienceConfig = NearCacheResilienceConfig(),
): SuspendNearCacheOperations<V> {
    companion object: KLogging()

    private val retry: Retry =
        Retry.of(
            "near-cache-suspend-${delegate.cacheName}",
            RetryConfig
                .custom<Any>()
                .maxAttempts(config.retryMaxAttempts)
                .intervalFunction(
                    if (config.retryExponentialBackoff) {
                        IntervalFunction.ofExponentialBackoff(config.retryWaitDuration.toMillis())
                    } else {
                        IntervalFunction.of(config.retryWaitDuration.toMillis())
                    }
                ).build()
        )

    override val cacheName: String get() = delegate.cacheName
    override val isClosed: Boolean get() = delegate.isClosed

    // -- Read --

    override suspend fun get(key: String): V? =
        try {
            retry.executeSuspendFunction { delegate.get(key) }
        } catch (e: Exception) {
            when (config.getFailureStrategy) {
                GetFailureStrategy.RETURN_FRONT_OR_NULL -> {
                    log.warn(e) { "get() 실패, null 반환. key=$key" }
                    null
                }
                GetFailureStrategy.PROPAGATE_EXCEPTION -> {
                    throw e
                }
            }
        }

    override suspend fun getAll(keys: Set<String>): Map<String, V> =
        try {
            retry.executeSuspendFunction { delegate.getAll(keys) }
        } catch (e: Exception) {
            when (config.getFailureStrategy) {
                GetFailureStrategy.RETURN_FRONT_OR_NULL -> {
                    log.warn(e) { "getAll() 실패, 빈 Map 반환. keys=$keys" }
                    emptyMap()
                }
                GetFailureStrategy.PROPAGATE_EXCEPTION -> {
                    throw e
                }
            }
        }

    override suspend fun containsKey(key: String): Boolean =
        try {
            retry.executeSuspendFunction { delegate.containsKey(key) }
        } catch (e: Exception) {
            when (config.getFailureStrategy) {
                GetFailureStrategy.RETURN_FRONT_OR_NULL -> {
                    log.warn(e) { "containsKey() 실패, false 반환. key=$key" }
                    false
                }
                GetFailureStrategy.PROPAGATE_EXCEPTION -> {
                    throw e
                }
            }
        }

    // -- Write --

    override suspend fun put(key: String, value: V) {
        retry.executeSuspendFunction {
            delegate.put(key, value)
        }
    }

    override suspend fun putAll(entries: Map<String, V>) {
        retry.executeSuspendFunction {
            delegate.putAll(entries)
        }
    }

    override suspend fun putIfAbsent(key: String, value: V): V? =
        retry.executeSuspendFunction {
            delegate.putIfAbsent(key, value)
        }

    override suspend fun replace(key: String, value: V): Boolean =
        retry.executeSuspendFunction {
            delegate.replace(key, value)
        }

    override suspend fun replace(key: String, oldValue: V, newValue: V): Boolean =
        retry.executeSuspendFunction {
            delegate.replace(key, oldValue, newValue)
        }

    // -- Delete --

    override suspend fun remove(key: String) {
        retry.executeSuspendFunction {
            delegate.remove(key)
        }
    }

    override suspend fun removeAll(keys: Set<String>) {
        retry.executeSuspendFunction {
            delegate.removeAll(keys)
        }
    }

    override suspend fun getAndRemove(key: String): V? =
        retry.executeSuspendFunction {
            delegate.getAndRemove(key)
        }

    override suspend fun getAndReplace(key: String, value: V): V? =
        retry.executeSuspendFunction {
            delegate.getAndReplace(key, value)
        }

    // -- Cache Management --

    override fun clearLocal() = delegate.clearLocal()

    override suspend fun clearAll() {
        retry.executeSuspendFunction {
            delegate.clearAll()
        }
    }

    override fun localCacheSize(): Long = delegate.localCacheSize()

    override suspend fun backCacheSize(): Long = retry.executeSuspendFunction { delegate.backCacheSize() }

    // -- Statistics --

    override fun stats(): NearCacheStatistics = delegate.stats()

    // -- Lifecycle --

    override suspend fun close() {
        runCatching { delegate.close() }
    }
}

/**
 * [SuspendNearCacheOperations]에 Resilience Decorator를 적용합니다.
 *
 * ```kotlin
 * val config = NearCacheResilienceConfig(retryMaxAttempts = 3)
 * val cache = lettuceSuspendNearCacheOf<String>(redisClient, codec, nearCacheConfig)
 *     .withResilience(config)
 * cache.put("hello", "world")
 * val value = cache.get("hello")
 * // value == "world"
 * ```
 */
fun <V: Any> SuspendNearCacheOperations<V>.withResilience(
    config: NearCacheResilienceConfig,
): SuspendNearCacheOperations<V> =
    ResilientSuspendNearCacheDecorator(this, config)

/**
 * [SuspendNearCacheOperations]에 Resilience Decorator를 DSL로 적용합니다.
 *
 * ```kotlin
 * val cache = lettuceSuspendNearCacheOf<String>(redisClient, codec, nearCacheConfig)
 *     .withResilience {
 *         retryMaxAttempts = 5
 *         getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
 *     }
 * cache.put("key", "value")
 * val result = cache.get("key")
 * // result == "value"
 * ```
 */
inline fun <V: Any> SuspendNearCacheOperations<V>.withResilience(
    block: NearCacheResilienceConfigBuilder.() -> Unit,
): SuspendNearCacheOperations<V> =
    ResilientSuspendNearCacheDecorator(
        this,
        nearCacheResilienceConfig(block)
    )
