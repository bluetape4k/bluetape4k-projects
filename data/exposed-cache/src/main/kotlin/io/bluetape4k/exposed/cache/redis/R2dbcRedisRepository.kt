package io.bluetape4k.exposed.cache.redis

import io.bluetape4k.exposed.cache.JdbcCacheRepository.Companion.DEFAULT_BATCH_SIZE
import io.bluetape4k.exposed.cache.R2dbcCacheRepository
import java.io.Serializable

/**
 * R2DBC + Redis 캐시 저장소 전용 인터페이스.
 *
 * [R2dbcCacheRepository]를 확장하며 Redis SCAN 명령을 이용한 패턴 기반 캐시 무효화를 추가로 제공합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입
 */
interface R2dbcRedisRepository<ID: Any, E: Serializable> : R2dbcCacheRepository<ID, E> {

    /**
     * Redis SCAN 명령으로 패턴에 맞는 캐시 키를 무효화합니다 (DB에는 영향 없음, suspend).
     *
     * ⚠️ R2dbcCacheRepository의 suspend 컨텍스트를 유지하기 위해 suspend fun 필수.
     *
     * @param patterns 캐시 키 패턴 (예: "*user*", "prefix:*")
     * @param count 한 번에 스캔할 키 수 (기본값: [DEFAULT_BATCH_SIZE])
     * @return 무효화된 캐시 항목 수
     */
    suspend fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long
}
