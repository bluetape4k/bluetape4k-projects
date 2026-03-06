package io.bluetape4k.cache.nearcache

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * [ResilientRedissonResp3NearCache] 및 [ResilientRedissonResp3SuspendNearCache] 설정.
 *
 * [RedissonResp3NearCacheConfig]를 기반으로 하며, write-behind 큐와 resilience4j retry 설정을 추가한다.
 *
 * @param base 기본 Redisson RESP3 NearCache 설정
 * @param writeQueueCapacity write-behind 큐(또는 채널) 최대 용량
 * @param retryMaxAttempts Redisson 쓰기 실패 시 최대 재시도 횟수
 * @param retryWaitDuration 재시도 대기 시간
 * @param retryExponentialBackoff 지수 백오프 사용 여부
 * @param getFailureStrategy Redisson GET 실패 시 동작 전략
 */
data class ResilientRedissonResp3NearCacheConfig(
    val base: RedissonResp3NearCacheConfig,
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

    fun redisKey(key: String): String = base.redisKey(key)
}

/**
 * [ResilientRedissonResp3NearCacheConfig] DSL 빌더.
 */
inline fun resilientRedissonResp3NearCacheConfig(
    block: ResilientRedissonResp3NearCacheConfigBuilder.() -> Unit,
): ResilientRedissonResp3NearCacheConfig =
    ResilientRedissonResp3NearCacheConfigBuilder().apply(block).build()

/**
 * [ResilientRedissonResp3NearCacheConfig] 빌더 클래스.
 */
class ResilientRedissonResp3NearCacheConfigBuilder {
    var cacheName: String = "resilient-redisson-near-cache"
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

    fun build(): ResilientRedissonResp3NearCacheConfig {
        val base = RedissonResp3NearCacheConfig(
            cacheName = cacheName.requireNotBlank("cacheName"),
            maxLocalSize = maxLocalSize.requirePositiveNumber("maxLocalSize"),
            frontExpireAfterWrite = frontExpireAfterWrite,
            frontExpireAfterAccess = frontExpireAfterAccess,
            redisTtl = redisTtl,
            useRespProtocol3 = useRespProtocol3,
            recordStats = recordStats,
        )
        return ResilientRedissonResp3NearCacheConfig(
            base = base,
            writeQueueCapacity = writeQueueCapacity.requirePositiveNumber("writeQueueCapacity"),
            retryMaxAttempts = retryMaxAttempts.requirePositiveNumber("retryMaxAttempts"),
            retryWaitDuration = retryWaitDuration,
            retryExponentialBackoff = retryExponentialBackoff,
            getFailureStrategy = getFailureStrategy,
        )
    }
}
