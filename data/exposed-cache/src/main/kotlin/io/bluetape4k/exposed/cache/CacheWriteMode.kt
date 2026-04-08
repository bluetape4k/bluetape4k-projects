package io.bluetape4k.exposed.cache

/**
 * 캐시 쓰기 전략을 정의합니다.
 *
 * DB와 캐시 간의 데이터 동기화 방식을 결정합니다.
 */
enum class CacheWriteMode {

    /**
     * 읽기 전용 (Read-Through만 사용).
     *
     * 캐시 미스 시 DB에서 읽어 캐시에 저장하지만,
     * 쓰기 시에는 캐시만 갱신하고 DB에는 반영하지 않습니다.
     */
    READ_ONLY,

    /**
     * 읽기/쓰기 동기 (Write-Through).
     *
     * 캐시와 DB를 동시에 반영합니다. 데이터 일관성이 보장되지만,
     * 쓰기 지연이 발생할 수 있습니다.
     */
    WRITE_THROUGH,

    /**
     * 비동기 쓰기 (Write-Behind).
     *
     * 캐시에 먼저 저장하고, DB에는 비동기로 반영합니다.
     * 쓰기 성능이 우수하지만, 장애 시 데이터 유실 가능성이 있습니다.
     */
    WRITE_BEHIND,
}
