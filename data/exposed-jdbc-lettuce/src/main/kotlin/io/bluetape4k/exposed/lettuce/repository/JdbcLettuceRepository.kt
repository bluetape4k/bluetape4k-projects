package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import java.io.Closeable

/**
 * Exposed JDBC와 Lettuce Redis 캐시를 결합한 동기 캐시 레포지토리 계약입니다.
 *
 * ## 동작/계약
 * - `findById`/`findAll(ids)` — 캐시 미스 시 DB에서 Read-through하여 Redis에 캐싱합니다.
 * - `findByIdFromDb`/`findAllFromDb` — 캐시를 우회하고 DB에서 직접 조회합니다.
 * - `save`/`saveAll` — [LettuceCacheConfig.writeMode]에 따라 Redis에 저장하고, WRITE_THROUGH이면 DB에도 즉시 반영합니다.
 * - `delete`/`deleteAll` — Redis와 DB를 함께 삭제합니다 (NONE 모드에서는 Redis만 삭제).
 * - `clearCache` — Redis에서 이 레포지토리의 키를 전부 삭제합니다.
 *
 * @param ID PK 타입 ([Comparable] 구현 필요)
 * @param E 엔티티(DTO) 타입. Redis 저장 시 직렬화 문제로 반드시 [java.io.Serializable]을 구현해야 합니다.
 */
interface JdbcLettuceRepository<ID : Comparable<ID>, E : Any> : Closeable {
    companion object {
        const val DEFAULT_BATCH_SIZE = 500
    }

    /**
     * 이 레포지토리가 사용하는 Exposed [IdTable].
     */
    val table: IdTable<ID>

    /**
     * Lettuce 캐시 동작 설정.
     */
    val config: LettuceCacheConfig

    // -------------------------------------------------------------------------
    // DB 직접 조회 (캐시 우회)
    // -------------------------------------------------------------------------

    /**
     * DB에서 직접 엔티티를 조회합니다 (캐시 우회).
     *
     * @param id 엔티티 식별자
     * @return 엔티티 또는 null
     */
    fun findByIdFromDb(id: ID): E?

    /**
     * DB에서 여러 엔티티를 직접 조회합니다 (캐시 우회).
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return 엔티티 리스트
     */
    fun findAllFromDb(ids: Collection<ID>): List<E>

    /**
     * DB 전체 레코드 수를 반환합니다 (캐시 우회).
     */
    fun countFromDb(): Long

    // -------------------------------------------------------------------------
    // 캐시 기반 조회 (Read-through)
    // -------------------------------------------------------------------------

    /**
     * 캐시에서 엔티티를 조회합니다. 캐시 미스 시 DB에서 Read-through합니다.
     *
     * @param id 엔티티 식별자
     * @return 엔티티 또는 null
     */
    fun findById(id: ID): E?

    /**
     * 여러 엔티티를 캐시에서 일괄 조회합니다. 캐시 미스 키는 DB에서 Read-through합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return ID → 엔티티 맵
     */
    fun findAll(ids: Collection<ID>): Map<ID, E>

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
    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = table.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: () -> Op<Boolean> = { Op.TRUE },
    ): List<E>

    // -------------------------------------------------------------------------
    // 쓰기 (캐시 + DB)
    // -------------------------------------------------------------------------

    /**
     * 엔티티를 캐시에 저장합니다. [LettuceCacheConfig.writeMode]에 따라 DB에도 반영됩니다.
     *
     * @param id 엔티티 식별자
     * @param entity 저장할 엔티티
     */
    fun save(
        id: ID,
        entity: E,
    )

    /**
     * 여러 엔티티를 일괄 저장합니다.
     *
     * @param entities ID → 엔티티 맵
     */
    fun saveAll(entities: Map<ID, E>)

    // -------------------------------------------------------------------------
    // 삭제
    // -------------------------------------------------------------------------

    /**
     * 캐시에서 엔티티를 삭제합니다. WRITE_THROUGH/WRITE_BEHIND 모드에서는 DB에서도 삭제합니다.
     *
     * @param id 엔티티 식별자
     */
    fun delete(id: ID)

    /**
     * 여러 엔티티를 일괄 삭제합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     */
    fun deleteAll(ids: Collection<ID>)

    // -------------------------------------------------------------------------
    // 캐시 관리
    // -------------------------------------------------------------------------

    /**
     * 이 레포지토리의 Redis 키를 모두 삭제합니다 (DB에는 영향 없음).
     */
    fun clearCache()
}
