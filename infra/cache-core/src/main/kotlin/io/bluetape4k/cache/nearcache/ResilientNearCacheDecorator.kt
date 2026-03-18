package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig

/**
 * [NearCacheOperations] Resilience Decorator (Blocking).
 *
 * delegate의 모든 연산에 resilience4j [Retry]를 적용합니다.
 * delegate의 write-through 원자성을 유지하며, write-behind는 사용하지 않습니다.
 *
 * GET 실패 시 [GetFailureStrategy]에 따라:
 * - [GetFailureStrategy.RETURN_FRONT_OR_NULL]: null 반환 (graceful degradation)
 * - [GetFailureStrategy.PROPAGATE_EXCEPTION]: 예외 전파
 *
 * ```kotlin
 * val cache = lettuceNearCacheOf<String>(redisClient, codec, config)
 *     .withResilience {
 *         retryMaxAttempts = 5
 *         retryWaitDuration = Duration.ofSeconds(1)
 *     }
 * ```
 *
 * @param V 캐시 값 타입
 * @param delegate 감쌀 NearCacheOperations 구현체
 * @param config Resilience 설정
 */
class ResilientNearCacheDecorator<V: Any>(
    private val delegate: NearCacheOperations<V>,
    private val config: NearCacheResilienceConfig = NearCacheResilienceConfig(),
): NearCacheOperations<V> {
    companion object: KLogging()

    private val retry: Retry =
        Retry.of(
            "near-cache-${delegate.cacheName}",
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

    override fun get(key: String): V? =
        try {
            retry.executeCallable { delegate.get(key) }
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

    override fun getAll(keys: Set<String>): Map<String, V> =
        try {
            retry.executeCallable { delegate.getAll(keys) }
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

    override fun containsKey(key: String): Boolean =
        try {
            retry.executeCallable { delegate.containsKey(key) }
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

    override fun put(key: String, value: V) {
        log.debug { "put with retry. key=$key" }
        retry.executeRunnable { delegate.put(key, value) }
    }

    override fun putAll(entries: Map<String, V>) {
        retry.executeRunnable { delegate.putAll(entries) }
    }

    override fun putIfAbsent(key: String, value: V): V? =
        retry.executeCallable { delegate.putIfAbsent(key, value) }

    override fun replace(key: String, value: V): Boolean =
        retry.executeCallable { delegate.replace(key, value) }

    override fun replace(key: String, oldValue: V, newValue: V): Boolean =
        retry.executeCallable {
            delegate.replace(key, oldValue, newValue)
        }

    // -- Delete --

    override fun remove(key: String) {
        retry.executeRunnable { delegate.remove(key) }
    }

    override fun removeAll(keys: Set<String>) {
        retry.executeRunnable { delegate.removeAll(keys) }
    }

    override fun getAndRemove(key: String): V? = retry.executeCallable { delegate.getAndRemove(key) }

    override fun getAndReplace(key: String, value: V): V? =
        retry.executeCallable { delegate.getAndReplace(key, value) }

    // -- Cache Management --

    override fun clearLocal() = delegate.clearLocal()

    override fun clearAll() = retry.executeRunnable { delegate.clearAll() }

    override fun localCacheSize(): Long = delegate.localCacheSize()

    override fun backCacheSize(): Long = retry.executeCallable { delegate.backCacheSize() }

    // -- Statistics --

    override fun stats(): NearCacheStatistics = delegate.stats()

    // -- Lifecycle --

    override fun close() {
        delegate.close()
    }
}

/**
 * [NearCacheOperations]에 Resilience Decorator를 적용합니다.
 *
 * @param config Resilience 설정
 */
fun <V: Any> NearCacheOperations<V>.withResilience(config: NearCacheResilienceConfig): NearCacheOperations<V> =
    ResilientNearCacheDecorator(this, config)

/**
 * [NearCacheOperations]에 Resilience Decorator를 DSL로 적용합니다.
 *
 * ```kotlin
 * val cache = lettuceNearCacheOf<String>(redisClient, codec, config)
 *     .withResilience {
 *         retryMaxAttempts = 5
 *         getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
 *     }
 * ```
 */
inline fun <V: Any> NearCacheOperations<V>.withResilience(
    block: NearCacheResilienceConfigBuilder.() -> Unit,
): NearCacheOperations<V> = ResilientNearCacheDecorator(this, nearCacheResilienceConfig(block))
