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
    companion object : KLogging() {
        private const val serialVersionUID = 1L

        /** 읽기 전용 (캐시 읽기만, DB 쓰기 없음) */
        val READ_ONLY = LocalCacheConfig(writeMode = CacheWriteMode.READ_ONLY)

        /** Write-Through (캐시 + DB 동기 쓰기) */
        val WRITE_THROUGH = LocalCacheConfig(writeMode = CacheWriteMode.WRITE_THROUGH)

        /** Write-Behind (캐시 쓰기 후 DB 비동기 배치 쓰기) */
        val WRITE_BEHIND = LocalCacheConfig(writeMode = CacheWriteMode.WRITE_BEHIND)
    }
}
