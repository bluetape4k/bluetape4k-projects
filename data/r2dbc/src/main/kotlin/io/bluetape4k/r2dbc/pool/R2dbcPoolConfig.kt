package io.bluetape4k.r2dbc.pool

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.utils.Runtimex
import java.time.Duration

/**
 * R2DBC 커넥션 풀(Connection Pool) 설정을 담는 데이터 클래스입니다.
 *
 * 모든 필드는 기본값이 제공되므로 변경이 필요한 항목만 DSL 람다로 지정할 수 있습니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * // 기본 설정으로 생성
 * val config = R2dbcPoolConfig()
 *
 * // DSL 람다로 설정 변경
 * val pool = connectionPoolOf(connectionFactoryOptions) {
 *     maxSize = 50
 *     initialSize = 5
 *     minIdle = 5
 *     maxIdleTime = Duration.ofMinutes(5)
 * }
 * ```
 *
 * @property maxIdleTime 커넥션이 풀에서 유휴 상태를 유지할 최대 시간 (기본값: 10분)
 * @property maxLifeTime 커넥션의 최대 생명 주기 (기본값: 30분)
 * @property maxCreateConnectionTime 커넥션 생성 최대 대기 시간 (기본값: 10초)
 * @property maxSize 풀의 최대 커넥션 수 (기본값: CPU 코어 수 × 8, 최소 100)
 * @property initialSize 초기 커넥션 수 (기본값: 8)
 * @property minIdle 최소 유휴 커넥션 수 (기본값: 8)
 * @property acquireRetry 커넥션 획득 재시도 횟수 (기본값: 3)
 * @property backgroundEvictionInterval 백그라운드 만료 검사 주기 (기본값: 1분)
 * @property maxAcquireTime 커넥션 획득 최대 대기 시간 (기본값: 3초)
 */
data class R2dbcPoolConfig(
    var maxIdleTime: Duration = DEFAULT_MAX_IDLE_TIME,
    var maxLifeTime: Duration = DEFAULT_MAX_LIFE_TIME,
    var maxCreateConnectionTime: Duration = DEFAULT_MAX_CREATE_CONNECTION_TIME,
    var maxSize: Int = DEFAULT_MAX_SIZE,
    var initialSize: Int = DEFAULT_INITIAL_SIZE,
    var minIdle: Int = DEFAULT_MIN_IDLE,
    var acquireRetry: Int = DEFAULT_ACQUIRE_RETRY,
    var backgroundEvictionInterval: Duration = DEFAULT_BACKGROUND_EVICTION_INTERVAL,
    var maxAcquireTime: Duration = DEFAULT_MAX_ACQUIRE_TIME,
) {
    init {
        maxSize.requirePositiveNumber("maxSize")
        initialSize.requirePositiveNumber("initialSize")
        minIdle.requireGe(0, "minIdle")
        acquireRetry.requireGe(0, "acquireRetry")
    }

    companion object : KLogging() {
        /** 커넥션이 풀에서 유휴 상태를 유지할 최대 시간 기본값 */
        val DEFAULT_MAX_IDLE_TIME: Duration = Duration.ofMinutes(10)

        /** 커넥션 최대 생명 주기 기본값 */
        val DEFAULT_MAX_LIFE_TIME: Duration = Duration.ofMinutes(30)

        /** 커넥션 생성 최대 대기 시간 기본값 */
        val DEFAULT_MAX_CREATE_CONNECTION_TIME: Duration = Duration.ofSeconds(10)

        /** 풀의 최대 커넥션 수 기본값 (CPU 코어 수 × 8, 최소 100) */
        val DEFAULT_MAX_SIZE: Int = maxOf(Runtimex.availableProcessors * 8, 100)

        /** 초기 커넥션 수 기본값 */
        const val DEFAULT_INITIAL_SIZE: Int = 8

        /** 최소 유휴 커넥션 수 기본값 */
        const val DEFAULT_MIN_IDLE: Int = 8

        /** 커넥션 획득 재시도 횟수 기본값 */
        const val DEFAULT_ACQUIRE_RETRY: Int = 3

        /** 백그라운드 만료 검사 주기 기본값 */
        val DEFAULT_BACKGROUND_EVICTION_INTERVAL: Duration = Duration.ofMinutes(1)

        /** 커넥션 획득 최대 대기 시간 기본값 */
        val DEFAULT_MAX_ACQUIRE_TIME: Duration = Duration.ofSeconds(3)
    }
}
