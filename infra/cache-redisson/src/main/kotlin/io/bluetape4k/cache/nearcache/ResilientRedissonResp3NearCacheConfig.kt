package io.bluetape4k.cache.nearcache

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * [ResilientRedissonResp3NearCache] 및 [ResilientRedissonResp3SuspendNearCache] 설정.
 *
 * [RedissonResp3NearCacheConfig]를 기반으로 하며, Redisson 백엔드 장애 상황에서의
 * write-behind 큐 크기 및 resilience4j Retry 정책을 추가로 설정한다.
 *
 * ## 사용 예시
 * ```kotlin
 * val config = resilientRedissonResp3NearCacheConfig {
 *     cacheName = "my-cache"
 *     retryMaxAttempts = 5
 *     retryWaitDuration = Duration.ofSeconds(1)
 *     retryExponentialBackoff = true
 *     getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL
 * }
 * ```
 *
 * @param base 기본 [RedissonResp3NearCacheConfig] 설정 (cacheName, TTL, maxLocalSize 등)
 * @param writeQueueCapacity write-behind 큐(또는 채널) 최대 용량. 큐가 가득 찬 경우 쓰기가 차단될 수 있다
 * @param retryMaxAttempts Redisson 쓰기 실패 시 최대 재시도 횟수
 * @param retryWaitDuration 재시도 간 대기 시간. [retryExponentialBackoff]가 true이면 지수 증가한다
 * @param retryExponentialBackoff 지수 백오프 사용 여부. true이면 재시도마다 대기 시간이 2배씩 증가한다
 * @param getFailureStrategy Redisson GET 실패 시 동작 전략 ([GetFailureStrategy] 참고)
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

    /** [base] 설정의 캐시 이름. Redis key prefix로 사용된다 */
    val cacheName: String get() = base.cacheName

    /** [base] 설정의 로컬 캐시 최대 항목 수 */
    val maxLocalSize: Long get() = base.maxLocalSize

    /** [base] 설정의 로컬 캐시 쓰기 후 만료 시간 */
    val frontExpireAfterWrite: Duration get() = base.frontExpireAfterWrite

    /** [base] 설정의 로컬 캐시 접근 후 만료 시간. null이면 접근 기반 만료 없음 */
    val frontExpireAfterAccess: Duration? get() = base.frontExpireAfterAccess

    /** [base] 설정의 Redis TTL. null이면 만료 없음 */
    val redisTtl: Duration? get() = base.redisTtl

    /** [base] 설정의 RESP3 프로토콜 사용 여부 */
    val useRespProtocol3: Boolean get() = base.useRespProtocol3

    /** [base] 설정의 Caffeine 통계 수집 여부 */
    val recordStats: Boolean get() = base.recordStats

    /**
     * 애플리케이션 키를 Redis 저장 키로 변환한다.
     * 결과: `{cacheName}:{key}`
     *
     * @param key 애플리케이션 수준의 원본 키
     * @return Redis에 저장되는 전체 키
     */
    fun redisKey(key: String): String = base.redisKey(key)
}

/**
 * [ResilientRedissonResp3NearCacheConfig] DSL 빌더 함수.
 *
 * 사용 예시:
 * ```kotlin
 * val config = resilientRedissonResp3NearCacheConfig {
 *     cacheName = "my-resilient-cache"
 *     maxLocalSize = 5_000
 *     redisTtl = Duration.ofMinutes(10)
 *     retryMaxAttempts = 5
 *     retryWaitDuration = Duration.ofMillis(200)
 *     retryExponentialBackoff = true
 *     getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL
 * }
 * ```
 *
 * @param block [ResilientRedissonResp3NearCacheConfigBuilder]에 대한 설정 블록
 * @return 빌드된 [ResilientRedissonResp3NearCacheConfig] 인스턴스
 */
inline fun resilientRedissonResp3NearCacheConfig(
    block: ResilientRedissonResp3NearCacheConfigBuilder.() -> Unit,
): ResilientRedissonResp3NearCacheConfig =
    ResilientRedissonResp3NearCacheConfigBuilder().apply(block).build()

/**
 * [ResilientRedissonResp3NearCacheConfig] 빌더 클래스.
 *
 * [resilientRedissonResp3NearCacheConfig] DSL 함수에서 내부적으로 사용되며,
 * 직접 인스턴스화하여 사용할 수도 있다.
 * [build]를 호출하면 유효성 검사 후 [ResilientRedissonResp3NearCacheConfig]를 반환한다.
 */
class ResilientRedissonResp3NearCacheConfigBuilder {
    /** 캐시 이름. Redis key prefix로 사용된다. 기본값: `"resilient-redisson-near-cache"` */
    var cacheName: String = "resilient-redisson-near-cache"

    /** 로컬 캐시(Caffeine) 최대 항목 수. 기본값: `10_000` */
    var maxLocalSize: Long = 10_000

    /** 로컬 캐시 쓰기 후 만료 시간. 기본값: 30분 */
    var frontExpireAfterWrite: Duration = Duration.ofMinutes(30)

    /** 로컬 캐시 접근 후 만료 시간. `null`이면 접근 기반 만료 없음. 기본값: `null` */
    var frontExpireAfterAccess: Duration? = null

    /** Redis TTL. `null`이면 만료 없음. 기본값: `null` */
    var redisTtl: Duration? = null

    /** RESP3 프로토콜 기반 CLIENT TRACKING 사용 여부. 기본값: `true` */
    var useRespProtocol3: Boolean = true

    /** Caffeine 통계 수집 여부. 기본값: `false` */
    var recordStats: Boolean = false

    /** write-behind 큐(또는 채널) 최대 용량. 기본값: `1024` */
    var writeQueueCapacity: Int = 1024

    /** Redisson 쓰기 실패 시 최대 재시도 횟수. 기본값: `3` */
    var retryMaxAttempts: Int = 3

    /** 재시도 간 대기 시간. [retryExponentialBackoff]가 true이면 지수 증가한다. 기본값: 500ms */
    var retryWaitDuration: Duration = Duration.ofMillis(500)

    /** 지수 백오프 사용 여부. true이면 재시도마다 대기 시간이 2배씩 증가한다. 기본값: `true` */
    var retryExponentialBackoff: Boolean = true

    /** Redisson GET 실패 시 동작 전략. 기본값: [GetFailureStrategy.RETURN_FRONT_OR_NULL] */
    var getFailureStrategy: GetFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL

    /**
     * 설정값을 검증하고 [ResilientRedissonResp3NearCacheConfig]를 생성한다.
     *
     * @return 빌드된 [ResilientRedissonResp3NearCacheConfig] 인스턴스
     * @throws IllegalArgumentException cacheName이 blank이거나, 수치 설정이 양수가 아닌 경우
     */
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
