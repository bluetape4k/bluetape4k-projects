package io.bluetape4k.exposed.cache

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * Exposed R2DBC + Redisson 캐시 기반 suspend 저장소의 공통 인터페이스입니다.
 *
 * [R2dbcCacheRepository]를 확장하여 Redisson 고유의 패턴 기반 캐시 무효화 기능을 제공합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입 (분산 캐시 저장을 위해 [Serializable] 구현 필수)
 */
interface R2dbcRedissonCacheRepository<ID: Any, E: Serializable>: R2dbcCacheRepository<ID, E> {

    companion object: KLogging()

    /**
     * 패턴에 맞는 캐시 키를 무효화합니다 (캐시만 제거, DB 영향 없음).
     *
     * Redisson `RMap.keySet(pattern, count)` 기반으로 동작합니다.
     *
     * @param patterns 캐시 키 패턴 (예: "*user*", "prefix:*")
     * @param count 한 번에 스캔할 키 수 (기본값: [DEFAULT_BATCH_SIZE])
     * @return 무효화된 캐시 항목 수
     */
    suspend fun invalidateByPattern(patterns: String, count: Int = R2dbcCacheRepository.DEFAULT_BATCH_SIZE): Long
}
