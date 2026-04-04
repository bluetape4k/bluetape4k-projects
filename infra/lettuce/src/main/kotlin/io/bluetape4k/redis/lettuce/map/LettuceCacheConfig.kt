package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.support.requireGt
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * [LettuceLoadedMap] 동작을 제어하는 설정.
 *
 * ```kotlin
 * val config = LettuceCacheConfig(
 *     writeMode = WriteMode.WRITE_THROUGH,
 *     ttl = Duration.ofMinutes(10),
 *     nearCacheEnabled = true,
 *     nearCacheMaxSize = 5_000,
 * )
 * val map = LettuceLoadedMap<String, MyData>(redisClient, loader, writer, config)
 * // 사전 정의 상수 사용
 * val readOnly = LettuceCacheConfig.READ_ONLY
 * val writeBehind = LettuceCacheConfig.WRITE_BEHIND
 * ```
 *
 * @param writeMode 쓰기 전략 ([WriteMode])
 * @param writeBehindBatchSize Write-Behind 배치 크기
 * @param writeBehindDelay Write-Behind 플러시 주기
 * @param writeBehindQueueCapacity Write-Behind 큐 최대 크기
 * @param writeBehindShutdownTimeout Write-Behind shutdown 대기 시간
 * @param writeRetryAttempts 쓰기 재시도 횟수
 * @param writeRetryInterval 쓰기 재시도 간격
 * @param ttl Redis 항목 TTL. 0보다 커야 한다.
 * @param keyPrefix Redis 키 prefix. 공백일 수 없다.
 * @param nearCacheEnabled Caffeine 로컬 캐시(NearCache) 사용 여부
 * @param nearCacheName NearCache 이름 (`:` 포함 불가). 공백일 수 없다.
 * @param nearCacheMaxSize NearCache 최대 항목 수. 0보다 커야 한다.
 * @param nearCacheTtl NearCache 항목 TTL (null이면 무기한). 지정하면 0보다 커야 한다.
 */
data class LettuceCacheConfig(
    val writeMode: WriteMode = WriteMode.WRITE_THROUGH,
    val writeBehindBatchSize: Int = 50,
    val writeBehindDelay: Duration = Duration.ofMillis(1000),
    val writeBehindQueueCapacity: Int = 10_000,
    val writeBehindShutdownTimeout: Duration = Duration.ofSeconds(10),
    val writeRetryAttempts: Int = 3,
    val writeRetryInterval: Duration = Duration.ofMillis(100),
    val ttl: Duration = Duration.ofMinutes(30),
    val keyPrefix: String = "cache",
    val nearCacheEnabled: Boolean = false,
    val nearCacheName: String = "lettuce-near",
    val nearCacheMaxSize: Long = 10_000,
    val nearCacheTtl: Duration? = null,
) {
    init {
        writeBehindBatchSize.requirePositiveNumber("writeBehindBatchSize")
        writeBehindQueueCapacity.requirePositiveNumber("writeBehindQueueCapacity")
        writeRetryAttempts.requirePositiveNumber("writeRetryAttempts")
        ttl.requireGt(Duration.ZERO, "ttl")
        keyPrefix.requireNotBlank("keyPrefix")
        nearCacheName.requireNotBlank("nearCacheName")
        nearCacheMaxSize.requirePositiveNumber("nearCacheMaxSize")
        nearCacheTtl?.requireGt(Duration.ZERO, "nearCacheTtl")
    }

    companion object {
        val READ_ONLY = LettuceCacheConfig(writeMode = WriteMode.NONE)
        val READ_WRITE_THROUGH = LettuceCacheConfig(writeMode = WriteMode.WRITE_THROUGH)
        val WRITE_BEHIND = LettuceCacheConfig(writeMode = WriteMode.WRITE_BEHIND)

        val READ_ONLY_WITH_NEAR_CACHE = LettuceCacheConfig(writeMode = WriteMode.NONE, nearCacheEnabled = true)
        val READ_WRITE_THROUGH_WITH_NEAR_CACHE =
            LettuceCacheConfig(writeMode = WriteMode.WRITE_THROUGH, nearCacheEnabled = true)
        val WRITE_BEHIND_WITH_NEAR_CACHE =
            LettuceCacheConfig(writeMode = WriteMode.WRITE_BEHIND, nearCacheEnabled = true)
    }
}
