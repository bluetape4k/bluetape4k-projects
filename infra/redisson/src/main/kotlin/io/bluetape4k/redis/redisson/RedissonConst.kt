package io.bluetape4k.redis.redisson

import io.bluetape4k.redis.redisson.RedissonConst.DEFAULT_HOST
import io.bluetape4k.redis.redisson.RedissonConst.DEFAULT_PORT
import io.bluetape4k.redis.redisson.RedissonConst.DEFAULT_TIMEOUT_MILLIS

/**
 * Redisson 연결 및 키 네이밍에 사용하는 공통 상수 모음입니다.
 *
 * ## 동작/계약
 * - 모든 값은 불변 상수이며 런타임 계산 없이 즉시 접근 가능합니다.
 * - 기본 URL은 [DEFAULT_HOST], [DEFAULT_PORT] 조합으로 구성됩니다.
 * - 모듈 전반에서 동일 기본값을 재사용하기 위한 용도로 사용됩니다.
 *
 * ```kotlin
 * val url = RedissonConst.DEFAULT_URL
 * // url == "redis://127.0.0.1:6379"
 * ```
 */
object RedissonConst {
    const val OK: String = "OK"

    const val DEFAULT_HOST = "127.0.0.1"
    const val DEFAULT_PORT = 6379
    const val DEFAULT_URL = "redis://${DEFAULT_HOST}:${DEFAULT_PORT}"

    const val DEFAULT_SENTINEL_PORT = 26379
    const val DEFAULT_TIMEOUT_MILLIS: Long = 30_000L
    const val DEFAULT_DATABASE = 0

    const val DEFAULT_CHARSET = "UTF-8"
    const val DEFAULT_LOGBACK_CHANNEL = "channel:logback:logs"
    const val DEFAULT_DELIMITER = ":"

    @Deprecated(
        message = "오타 수정: DEFAULT_DELIMETER → DEFAULT_DELIMITER",
        replaceWith = ReplaceWith("DEFAULT_DELIMITER"),
        level = DeprecationLevel.WARNING
    )
    const val DEFAULT_DELIMETER = DEFAULT_DELIMITER

    /**
     * 고동시성 환경용 Connection Pool 크기: CPU × 8 (범위 64~256)
     */
    @JvmField
    val DEFAULT_CONNECTION_POOL_SIZE: Int = (Runtime.getRuntime().availableProcessors() * 8).coerceIn(64, 256)

    /**
     * 고동시성 환경용 Connection Minimum Idle 크기: CPU × 2 (범위 8~32)
     */
    @JvmField
    val DEFAULT_CONNECTION_MIN_IDLE_SIZE: Int = (Runtime.getRuntime().availableProcessors() * 2).coerceIn(8, 32)

    /**
     * 고동시성 환경용 Netty 스레드 수: CPU × 2 (최대 32)
     */
    @JvmField
    val DEFAULT_NETTY_THREADS: Int = (Runtime.getRuntime().availableProcessors() * 2).coerceAtMost(32)

    /** Ping connection interval (ms) for keep-alive */
    const val DEFAULT_PING_INTERVAL_MILLIS: Int = 30_000

    /** Idle connection timeout (ms) before closing */
    const val DEFAULT_IDLE_CONNECTION_TIMEOUT_MILLIS: Int = 10_000

    /** TCP connect timeout (ms) */
    const val DEFAULT_CONNECT_TIMEOUT_MILLIS: Int = 3_000

    /** Command operation timeout (ms) — separate from [DEFAULT_TIMEOUT_MILLIS] (Long, legacy) */
    const val DEFAULT_OPERATION_TIMEOUT_MILLIS: Int = 5_000

    /** Redisson retry attempts on failure */
    const val DEFAULT_RETRY_ATTEMPTS: Int = 3

    /** Redisson retry interval (ms) */
    const val DEFAULT_RETRY_INTERVAL_MILLIS: Int = 1_000
}
