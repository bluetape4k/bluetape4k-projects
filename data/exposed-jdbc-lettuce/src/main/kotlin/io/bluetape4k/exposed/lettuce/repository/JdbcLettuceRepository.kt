package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.exposed.cache.JdbcCacheRepository
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import java.io.Serializable

/**
 * Exposed JDBC와 Lettuce Redis 캐시를 결합한 동기 캐시 레포지토리 계약입니다.
 *
 * ## 동작/계약
 * - `get(id)` — 캐시 미스 시 DB에서 Read-through하여 Redis에 캐싱합니다.
 * - `getAll(ids)` — 캐시 미스 키는 DB에서 Read-through합니다.
 * - `findByIdFromDb`/`findAllFromDb` — 캐시를 우회하고 DB에서 직접 조회합니다.
 * - `put`/`putAll` — [LettuceCacheConfig.writeMode]에 따라 Redis에 저장하고, WRITE_THROUGH이면 DB에도 즉시 반영합니다.
 * - `invalidate`/`invalidateAll` — Redis와 DB를 함께 삭제합니다 (NONE 모드에서는 Redis만 삭제).
 * - `clear` — Redis에서 이 레포지토리의 키를 전부 삭제합니다.
 *
 * @param ID PK 타입 ([Comparable] 구현 필요)
 * @param E 엔티티(DTO) 타입. Redis 저장 시 직렬화 문제로 반드시 [java.io.Serializable]을 구현해야 합니다.
 */
interface JdbcLettuceRepository<ID: Any, E: Serializable>: JdbcCacheRepository<ID, E> {

    /**
     * Lettuce 캐시 동작 설정.
     */
    val config: LettuceCacheConfig

    // -------------------------------------------------------------------------
    // DB 페이징 쿼리 (JdbcCacheRepository 재선언)
    // -------------------------------------------------------------------------

    /**
     * DB에서 조건에 맞는 엔티티 목록을 조회한 뒤 캐시에 적재합니다.
     *
     * @param limit 최대 조회 개수 (null이면 무제한)
     * @param offset 조회 시작 위치 (null이면 0)
     * @param sortBy 정렬 기준 컬럼
     * @param sortOrder 정렬 순서
     * @param where 조회 조건
     * @return 엔티티 리스트
     */
    override fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<E>
}
