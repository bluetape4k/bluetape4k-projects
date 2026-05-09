package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.nearcache.GetFailureStrategy

import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * [ResilientNearJCache] 및 [ResilientSuspendNearJCache] 설정.
 *
 * JCache 기반 back cache와 raw Caffeine front cache를 사용하는 Resilient NearCache 설정.
 *
 * ```kotlin
 * val config = ResilientNearJCacheConfig<String, Int>(
 *     cacheName = "my-resilient-cache",
 *     maxLocalSize = 5000,
 *     retryMaxAttempts = 5
 * )
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param maxLocalSize 로컬(Caffeine) 캐시 최대 크기
 * @param frontExpireAfterWrite 로컬 캐시 쓰기 후 만료 시간
 * @param frontExpireAfterAccess 로컬 캐시 접근 후 만료 시간 (null이면 비활성)
 * @param recordStats 로컬 캐시 통계 기록 여부
 * @param writeQueueCapacity write-behind 큐(또는 채널) 최대 용량
 * @param retryMaxAttempts back cache 쓰기 실패 시 최대 재시도 횟수
 * @param retryWaitDuration 재시도 대기 시간
 * @param retryExponentialBackoff 지수 백오프 사용 여부
 * @param getFailureStrategy back cache GET 실패 시 동작 전략
 */
data class ResilientNearJCacheConfig<K: Any, V: Any>(
    val cacheName: String = "resilient-near-cache",
    val maxLocalSize: Long = 10_000,
    val frontExpireAfterWrite: Duration = Duration.ofMinutes(30),
    val frontExpireAfterAccess: Duration? = null,
    val recordStats: Boolean = false,
    val writeQueueCapacity: Int = 1024,
    val retryMaxAttempts: Int = 3,
    val retryWaitDuration: Duration = Duration.ofMillis(500),
    val retryExponentialBackoff: Boolean = true,
    val getFailureStrategy: GetFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL,
) {
    init {
        maxLocalSize.requirePositiveNumber("maxLocalSize")
        writeQueueCapacity.requirePositiveNumber("writeQueueCapacity")
        retryMaxAttempts.requirePositiveNumber("retryMaxAttempts")
    }
}

/**
 * [ResilientNearJCacheConfig] DSL 빌더.
 *
 * ```kotlin
 * val config = resilientNearJCacheConfig<String, Int> {
 *     cacheName = "my-resilient-cache"
 *     maxLocalSize = 5000
 *     retryMaxAttempts = 5
 *     retryExponentialBackoff = true
 * }
 * ```
 */
inline fun <K: Any, V: Any> resilientNearJCacheConfig(
    block: ResilientNearJCacheConfigBuilder<K, V>.() -> Unit,
): ResilientNearJCacheConfig<K, V> =
    ResilientNearJCacheConfigBuilder<K, V>().apply(block).build()

/**
 * [ResilientNearJCacheConfig] 빌더 클래스.
 */
class ResilientNearJCacheConfigBuilder<K: Any, V: Any> {
    var cacheName: String = "resilient-near-cache"
    var maxLocalSize: Long = 10_000
    var frontExpireAfterWrite: Duration = Duration.ofMinutes(30)
    var frontExpireAfterAccess: Duration? = null
    var recordStats: Boolean = false
    var writeQueueCapacity: Int = 1024
    var retryMaxAttempts: Int = 3
    var retryWaitDuration: Duration = Duration.ofMillis(500)
    var retryExponentialBackoff: Boolean = true
    var getFailureStrategy: GetFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL

    fun build(): ResilientNearJCacheConfig<K, V> = ResilientNearJCacheConfig(
        cacheName = cacheName,
        maxLocalSize = maxLocalSize.requirePositiveNumber("maxLocalSize"),
        frontExpireAfterWrite = frontExpireAfterWrite,
        frontExpireAfterAccess = frontExpireAfterAccess,
        recordStats = recordStats,
        writeQueueCapacity = writeQueueCapacity.requirePositiveNumber("writeQueueCapacity"),
        retryMaxAttempts = retryMaxAttempts.requirePositiveNumber("retryMaxAttempts"),
        retryWaitDuration = retryWaitDuration,
        retryExponentialBackoff = retryExponentialBackoff,
        getFailureStrategy = getFailureStrategy,
    )
}
