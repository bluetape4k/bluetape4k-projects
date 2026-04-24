package io.bluetape4k.exposed.cache

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.time.Duration

/**
 * 로컬 캐시(인프로세스) 저장소의 공통 설정.
 *
 * Caffeine, Cache2k 등 다양한 로컬 캐시 구현체에서 공통으로 사용합니다.
 * 캐시 특화 옵션이 필요한 경우 이 클래스를 상속하여 확장하세요.
 *
 * ## 입력 검증 (왜 필요한가?)
 * - [keyPrefix]: 빈 문자열이면 캐시 키 충돌이 발생하여 서로 다른 캐시 인스턴스가 데이터를 덮어쓸 수 있다.
 * - [maximumSize]: 0 이하 값은 캐시를 사실상 비활성화하거나 OOM을 유발한다.
 * - [expireAfterWrite]: null이 아닌 경우 반드시 양수여야 한다. 0 이하면 항목이 즉시 만료된다.
 * - [writeBehindBatchSize]: 1 미만이면 Write-Behind 배치가 영원히 flush되지 않는다.
 * - [writeBehindQueueCapacity]: UNLIMITED(Int.MAX_VALUE 등 극단적 값) 설정 방지.
 *   큐 용량이 무제한이면 메모리 누수로 이어진다. 최소 [writeBehindBatchSize] 이상이어야 한다.
 *
 * ```kotlin
 * val config = LocalCacheConfig(
 *     keyPrefix = "actor",
 *     maximumSize = 5_000L,
 *     expireAfterWrite = Duration.ofMinutes(30),
 *     writeMode = CacheWriteMode.WRITE_THROUGH,
 * )
 * ```
 *
 * @property keyPrefix 캐시 키 접두사 (기본값: "local")
 * @property maximumSize 캐시 최대 항목 수 (기본값: 10,000)
 * @property expireAfterWrite 마지막 쓰기 이후 만료 시간 (기본값: 10분)
 * @property expireAfterAccess 마지막 접근 이후 만료 시간 (null이면 비활성)
 * @property writeMode 캐시 쓰기 전략 (기본값: [CacheWriteMode.READ_ONLY])
 * @property writeBehindBatchSize Write-Behind 배치 처리 크기 (기본값: 100)
 * @property writeBehindQueueCapacity Write-Behind 큐 최대 용량 — UNLIMITED 금지 (기본값: 10,000)
 */
open class LocalCacheConfig(
    val keyPrefix: String = "local",
    val maximumSize: Long = 10_000L,
    val expireAfterWrite: Duration = Duration.ofMinutes(10),
    val expireAfterAccess: Duration? = null,
    val writeMode: CacheWriteMode = CacheWriteMode.READ_ONLY,
    val writeBehindBatchSize: Int = 100,
    val writeBehindQueueCapacity: Int = 10_000,
) : Serializable {

    init {
        // keyPrefix가 공백이면 캐시 키 네임스페이스 충돌로 서로 다른 저장소 데이터가 덮어써진다.
        require(keyPrefix.isNotBlank()) { "keyPrefix must not be blank." }
        // maximumSize가 0 이하면 캐시가 비활성화 상태와 동일하거나 구현체에 따라 OOM을 유발할 수 있다.
        require(maximumSize > 0) { "maximumSize[$maximumSize] must be positive." }
        // expireAfterWrite가 0 이하면 저장 직후 즉시 만료되어 캐시 효과가 없다.
        require(expireAfterWrite > Duration.ZERO) { "expireAfterWrite[$expireAfterWrite] must be positive." }
        // expireAfterAccess가 설정된 경우에도 0 이하 값은 즉시 만료를 의미한다.
        expireAfterAccess?.let {
            require(it > Duration.ZERO) { "expireAfterAccess[$it] must be positive when set." }
        }
        // writeBehindBatchSize가 1 미만이면 Write-Behind flush 배치가 영원히 실행되지 않는다.
        require(writeBehindBatchSize >= 1) { "writeBehindBatchSize[$writeBehindBatchSize] must be at least 1." }
        // writeBehindQueueCapacity가 writeBehindBatchSize보다 작으면 큐가 즉시 포화된다.
        // 또한 무제한 큐(Int.MAX_VALUE 등)는 메모리 누수로 이어진다.
        require(writeBehindQueueCapacity >= writeBehindBatchSize) {
            "writeBehindQueueCapacity[$writeBehindQueueCapacity] must be >= writeBehindBatchSize[$writeBehindBatchSize]."
        }
    }

    companion object : KLogging() {
        private const val serialVersionUID = 1L

        /**
         * 읽기 전용 기본 설정.
         * DB에 쓰기 연산이 전혀 발생하지 않으므로 읽기 부하가 많은 조회 전용 캐시에 적합하다.
         * 쓰기 연산이 필요한 경우 [WRITE_THROUGH] 또는 [WRITE_BEHIND]를 사용해야 한다.
         */
        val READ_ONLY = LocalCacheConfig(writeMode = CacheWriteMode.READ_ONLY)

        /**
         * Write-Through 기본 설정.
         * 캐시와 DB를 동기적으로 함께 쓰기 때문에 데이터 일관성이 보장된다.
         * 쓰기 지연이 허용되지 않는 트랜잭션 데이터에 적합하다.
         */
        val WRITE_THROUGH = LocalCacheConfig(writeMode = CacheWriteMode.WRITE_THROUGH)

        /**
         * Write-Behind 기본 설정.
         * 캐시에 먼저 쓰고 DB에는 배치로 비동기 반영하므로 쓰기 처리량이 높다.
         * 단, 서버 장애 시 아직 DB에 반영되지 않은 항목이 유실될 수 있다.
         * 유실 허용 가능한 집계·로그 성격 데이터에 적합하다.
         */
        val WRITE_BEHIND = LocalCacheConfig(writeMode = CacheWriteMode.WRITE_BEHIND)
    }
}
