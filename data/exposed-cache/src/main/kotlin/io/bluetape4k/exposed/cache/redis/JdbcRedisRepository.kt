package io.bluetape4k.exposed.cache.redis

import io.bluetape4k.exposed.cache.JdbcCacheRepository
import io.bluetape4k.exposed.cache.JdbcCacheRepository.Companion.DEFAULT_BATCH_SIZE
import java.io.Serializable

/**
 * JDBC + Redis 캐시 저장소 전용 인터페이스.
 *
 * [JdbcCacheRepository]를 확장하며 Redis SCAN 명령을 이용한 패턴 기반 캐시 무효화를 추가로 제공합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입
 */
interface JdbcRedisRepository<ID: Any, E: Serializable> : JdbcCacheRepository<ID, E> {

    /**
     * Redis SCAN 명령으로 패턴에 맞는 캐시 키를 무효화합니다 (DB에는 영향 없음).
     *
     * @param patterns 캐시 키 패턴 (예: "*user*", "prefix:*")
     * @param count 한 번에 스캔할 키 수 (기본값: [DEFAULT_BATCH_SIZE])
     * @return 무효화된 캐시 항목 수
     */
    fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long
}
