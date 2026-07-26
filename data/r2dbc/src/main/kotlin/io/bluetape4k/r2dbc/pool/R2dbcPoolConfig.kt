package io.bluetape4k.r2dbc.pool

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireLe
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.utils.Runtimex
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ValidationDepth
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
 *     maxValidationTime = Duration.ofSeconds(2)
 * }
 * ```
 *
 * @property maxIdleTime 커넥션이 풀에서 유휴 상태를 유지할 최대 시간 (기본값: 10분)
 * @property maxLifeTime 커넥션의 최대 생명 주기 (기본값: 30분)
 * @property maxCreateConnectionTime 커넥션 생성 최대 대기 시간 (기본값: 10초)
 * @property maxSize 풀의 최대 커넥션 수 (기본값: CPU 코어 수 × 8, 최소 100)
 * @property initialSize 초기 커넥션 수 (기본값: 8, 0이면 지연 생성)
 * @property minIdle 최소 유휴 커넥션 수 (기본값: 8)
 * @property acquireRetry 커넥션 획득 재시도 횟수 (기본값: 3)
 * @property backgroundEvictionInterval 백그라운드 만료 검사 주기 (기본값: 1분)
 * @property maxAcquireTime 커넥션 획득 최대 대기 시간 (기본값: 3초)
 * @property maxPendingAcquire 풀 한도 도달 시 대기 큐에 둘 커넥션 획득 요청 수 (기본값: 1000). -1로 설정하면 무제한이 되어 OOM이 발생할 수 있으므로 운영 환경에서는 적절한 값을 지정하세요.
 * @property maxValidationTime 커넥션 검증 최대 대기 시간 (기본값: 2초)
 * @property validationDepth 커넥션 검증 깊이 (기본값: [ValidationDepth.LOCAL])
 * @property validationQuery 커넥션 검증 쿼리. 지정하면 검증마다 DB 왕복이 발생하므로 고처리량 경로에서는 기본값(null)을 권장합니다.
 * @property poolName 풀 이름. JMX 등록 또는 메트릭 식별자가 필요할 때 지정합니다.
 * @property registerJmx JMX 등록 여부. true이면 [poolName]이 필요합니다.
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
    var maxPendingAcquire: Int = DEFAULT_MAX_PENDING_ACQUIRE,
    var maxValidationTime: Duration = DEFAULT_MAX_VALIDATION_TIME,
    var validationDepth: ValidationDepth = DEFAULT_VALIDATION_DEPTH,
    var validationQuery: String? = null,
    var poolName: String? = null,
    var registerJmx: Boolean = false,
) {
    init {
        validate()
        if (maxPendingAcquire < 0) {
            log.warn {
                "maxPendingAcquire=$maxPendingAcquire (무제한). 고부하 환경에서 OOM이 발생할 수 있습니다. 운영 환경에서는 적절한 양수 값을 설정하세요."
            }
        }
    }

    /**
     * 현재 설정값의 제약을 검증합니다.
     *
     * DSL 람다로 생성 후 프로퍼티를 변경하는 경우에도 [ConnectionPoolConfiguration] 변환 직전에
     * 동일한 제약을 다시 확인할 수 있도록 공개합니다.
     */
    fun validate(): R2dbcPoolConfig = apply {
        maxSize.requirePositiveNumber("maxSize")
        initialSize.requireGe(0, "initialSize")
        initialSize.requireLe(maxSize, "initialSize")
        minIdle.requireGe(0, "minIdle")
        minIdle.requireLe(maxSize, "minIdle")
        acquireRetry.requireGe(0, "acquireRetry")
        maxPendingAcquire.requireGe(-1, "maxPendingAcquire")
        validationQuery?.requireNotBlank("validationQuery")
        poolName?.requireNotBlank("poolName")
        if (registerJmx) {
            poolName.requireNotBlank("poolName")
        }
    }

    companion object: KLogging() {
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

        /**
         * 풀 한도 도달 시 커넥션 획득 대기 큐 크기 기본값 (1000).
         *
         * -1로 설정하면 무제한 대기 큐가 생성되어 고부하 환경에서 OOM이 발생할 수 있습니다.
         */
        const val DEFAULT_MAX_PENDING_ACQUIRE: Int = 1000

        /** 커넥션 검증 최대 대기 시간 기본값 */
        val DEFAULT_MAX_VALIDATION_TIME: Duration = Duration.ofSeconds(2)

        /** 커넥션 검증 깊이 기본값 */
        val DEFAULT_VALIDATION_DEPTH: ValidationDepth = ValidationDepth.LOCAL

        /** 고처리량 프리셋의 초기 워밍업 커넥션 수 */
        val HIGH_THROUGHPUT_WARMUP_SIZE: Int = maxOf(Runtimex.availableProcessors * 2, 16)

        /** 고처리량 프리셋의 유휴 커넥션 유지 시간 */
        val HIGH_THROUGHPUT_MAX_IDLE_TIME: Duration = Duration.ofMinutes(5)

        /** 고처리량 프리셋의 백그라운드 만료 검사 주기 */
        val HIGH_THROUGHPUT_BACKGROUND_EVICTION_INTERVAL: Duration = Duration.ofSeconds(30)

        /** 고처리량 프리셋의 커넥션 생성 최대 대기 시간 */
        val HIGH_THROUGHPUT_MAX_CREATE_CONNECTION_TIME: Duration = Duration.ofSeconds(5)

        /** 고처리량 프리셋의 커넥션 획득 재시도 횟수 */
        const val HIGH_THROUGHPUT_ACQUIRE_RETRY: Int = 1

        /**
         * 고처리량 서비스에 맞춘 풀 설정을 생성합니다.
         *
         * DB 서버의 실제 `max_connections`, 서비스 인스턴스 수, 쿼리 지연 시간을 기준으로 [maxSize]를
         * 조정해야 합니다. 기본 프리셋은 워밍업 커넥션을 확보하고, SQL 검증 쿼리는 비활성화해
         * 커넥션 획득 경로의 추가 DB 왕복을 피합니다.
         *
         * @param maxSize 풀의 최대 커넥션 수
         * @param warmupSize 시작 시 확보할 커넥션 수와 최소 유휴 커넥션 수
         * @param poolName 풀 이름
         * @return 고처리량 기본값이 적용된 [R2dbcPoolConfig]
         */
        fun highThroughput(
            maxSize: Int = DEFAULT_MAX_SIZE,
            warmupSize: Int = minOf(maxSize, HIGH_THROUGHPUT_WARMUP_SIZE),
            poolName: String? = null,
        ): R2dbcPoolConfig {
            val normalizedWarmupSize = warmupSize.coerceAtMost(maxSize)
            return R2dbcPoolConfig(
                maxIdleTime = HIGH_THROUGHPUT_MAX_IDLE_TIME,
                maxLifeTime = DEFAULT_MAX_LIFE_TIME,
                maxCreateConnectionTime = HIGH_THROUGHPUT_MAX_CREATE_CONNECTION_TIME,
                maxSize = maxSize,
                initialSize = normalizedWarmupSize,
                minIdle = normalizedWarmupSize,
                acquireRetry = HIGH_THROUGHPUT_ACQUIRE_RETRY,
                backgroundEvictionInterval = HIGH_THROUGHPUT_BACKGROUND_EVICTION_INTERVAL,
                maxAcquireTime = DEFAULT_MAX_ACQUIRE_TIME,
                maxPendingAcquire = pendingAcquireLimit(maxSize),
                maxValidationTime = DEFAULT_MAX_VALIDATION_TIME,
                validationDepth = DEFAULT_VALIDATION_DEPTH,
                validationQuery = null,
                poolName = poolName,
                registerJmx = false,
            )
        }

        private fun pendingAcquireLimit(maxSize: Int): Int =
            minOf(Int.MAX_VALUE.toLong(), maxSize.toLong() * 4L).toInt()
    }
}
