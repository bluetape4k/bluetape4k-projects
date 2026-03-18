package io.bluetape4k.cache.nearcache

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * Redis GET 실패 시 동작 전략.
 */
enum class GetFailureStrategy {
    /** front cache에 값이 있으면 반환, 없으면 null 반환 (graceful degradation) */
    RETURN_FRONT_OR_NULL,

    /** 예외를 호출자에게 그대로 전파 */
    PROPAGATE_EXCEPTION,
}

/**
 * [ResilientLettuceSuspendNearCache] 설정.
 *
 * [LettuceNearCacheConfig]를 기반으로 하며, write-behind 채널과 resilience4j retry 설정을 추가한다.
 *
 * @param V 값 타입
 */
data class ResilientLettuceNearCacheConfig<K: Any, V: Any>(
    val base: LettuceNearCacheConfig<K, V>,
    val writeQueueCapacity: Int = 1024,
    val retryMaxAttempts: Int = 3,
    val retryWaitDuration: Duration = Duration.ofMillis(500),
    val retryExponentialBackoff: Boolean = true,
    val getFailureStrategy: GetFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL,
) {
    init {
        writeQueueCapacity.requirePositiveNumber("writeQueueCapacity")
        retryMaxAttempts.requirePositiveNumber("retryMaxAttempts")
    }

    /** base config로부터 위임 프로퍼티 */
    val cacheName: String get() = base.cacheName
    val maxLocalSize: Long get() = base.maxLocalSize
    val frontExpireAfterWrite: Duration get() = base.frontExpireAfterWrite
    val frontExpireAfterAccess: Duration? get() = base.frontExpireAfterAccess
    val redisTtl: Duration? get() = base.redisTtl
    val useRespProtocol3: Boolean get() = base.useRespProtocol3
    val recordStats: Boolean get() = base.recordStats

    /**
     * cacheName prefix를 포함한 Redis key를 생성한다.
     * 예: cacheName="orders", key="user:123" → "orders:user:123"
     *
     * key에는 ':'를 포함할 수 있다. invalidation 수신 시 startsWith + removePrefix 방식으로
     * cacheName prefix만 제거하므로 key의 ':' 문자는 그대로 보존된다.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun redisKey(key: String): String = base.redisKey(key)
}

/**
 * [ResilientLettuceNearCacheConfig] DSL 빌더.
 */
inline fun <K: Any, V: Any> resilientLettuceNearCacheConfig(
    block: ResilientLettuceNearCacheConfigBuilder<K, V>.() -> Unit,
): ResilientLettuceNearCacheConfig<K, V> =
    ResilientLettuceNearCacheConfigBuilder<K, V>().apply(block).build()

class ResilientLettuceNearCacheConfigBuilder<K: Any, V: Any> {
    var cacheName: String = "resilient-lettuce-near-cache"
    var maxLocalSize: Long = 10_000
    var frontExpireAfterWrite: Duration = Duration.ofMinutes(30)
    var frontExpireAfterAccess: Duration? = null
    var redisTtl: Duration? = null
    var useRespProtocol3: Boolean = true
    var recordStats: Boolean = false

    var writeQueueCapacity: Int = 1024
    var retryMaxAttempts: Int = 3
    var retryWaitDuration: Duration = Duration.ofMillis(500)
    var retryExponentialBackoff: Boolean = true
    var getFailureStrategy: GetFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL

    fun build(): ResilientLettuceNearCacheConfig<K, V> {
        val base = LettuceNearCacheConfig<K, V>(
            cacheName = cacheName.requireNotBlank("cacheName"),
            maxLocalSize = maxLocalSize.requirePositiveNumber("maxLocalSize"),
            frontExpireAfterWrite = frontExpireAfterWrite,
            frontExpireAfterAccess = frontExpireAfterAccess,
            redisTtl = redisTtl,
            useRespProtocol3 = useRespProtocol3,
            recordStats = recordStats,
        )
        return ResilientLettuceNearCacheConfig(
            base = base,
            writeQueueCapacity = writeQueueCapacity.requirePositiveNumber("writeQueueCapacity"),
            retryMaxAttempts = retryMaxAttempts.requirePositiveNumber("retryMaxAttempts"),
            retryWaitDuration = retryWaitDuration,
            retryExponentialBackoff = retryExponentialBackoff,
            getFailureStrategy = getFailureStrategy,
        )
    }
}
