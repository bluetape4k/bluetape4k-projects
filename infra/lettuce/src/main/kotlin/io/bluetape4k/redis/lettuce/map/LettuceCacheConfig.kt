package io.bluetape4k.redis.lettuce.map

import java.time.Duration

/**
 * [LettuceLoadedMap] 동작을 제어하는 설정.
 *
 * @param writeMode 쓰기 전략 ([WriteMode])
 * @param writeBehindBatchSize Write-Behind 배치 크기
 * @param writeBehindDelay Write-Behind 플러시 주기
 * @param writeBehindQueueCapacity Write-Behind 큐 최대 크기
 * @param writeBehindShutdownTimeout Write-Behind shutdown 대기 시간
 * @param writeRetryAttempts 쓰기 재시도 횟수
 * @param writeRetryInterval 쓰기 재시도 간격
 * @param ttl Redis 항목 TTL
 * @param keyPrefix Redis 키 prefix
 * @param nearCacheEnabled Caffeine 로컬 캐시(NearCache) 사용 여부
 * @param nearCacheName NearCache 이름 (`:` 포함 불가)
 * @param nearCacheMaxSize NearCache 최대 항목 수
 * @param nearCacheTtl NearCache 항목 TTL (null이면 무기한)
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
