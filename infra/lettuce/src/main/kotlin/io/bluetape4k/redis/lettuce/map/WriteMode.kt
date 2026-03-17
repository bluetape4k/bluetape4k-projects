package io.bluetape4k.redis.lettuce.map

/**
 * Lettuce 기반 캐시 맵의 쓰기 전략.
 */
enum class WriteMode {
    /** Redis 전용 (write-back 없음). */
    NONE,

    /** Redis + DB 동시 쓰기 (Write-Through). */
    WRITE_THROUGH,

    /** Redis 즉시 + DB 비동기 (Write-Behind). */
    WRITE_BEHIND,
}
