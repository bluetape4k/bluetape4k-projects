package io.bluetape4k.redis.lettuce.map

/**
 * Lettuce 기반 캐시 맵의 쓰기 전략.
 *
 * ```kotlin
 * val config = LettuceCacheConfig(writeMode = WriteMode.WRITE_THROUGH)
 * val loadedMap = LettuceLoadedMap(connection, config, mapLoader, mapWriter)
 * loadedMap["key"] = "value"  // Redis와 DB에 동시 저장
 *
 * val behindConfig = LettuceCacheConfig(writeMode = WriteMode.WRITE_BEHIND)
 * val behindMap = LettuceLoadedMap(connection, behindConfig, mapLoader, mapWriter)
 * behindMap["key"] = "value"  // Redis 즉시 + DB 비동기 저장
 * ```
 */
enum class WriteMode {
    /** Redis 전용 (write-back 없음). */
    NONE,

    /** Redis + DB 동시 쓰기 (Write-Through). */
    WRITE_THROUGH,

    /** Redis 즉시 + DB 비동기 (Write-Behind). */
    WRITE_BEHIND,
}
