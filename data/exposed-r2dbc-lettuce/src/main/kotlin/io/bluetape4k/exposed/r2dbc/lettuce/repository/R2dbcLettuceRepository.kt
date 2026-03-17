package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import java.io.Closeable

/**
 * Exposed R2DBC와 Lettuce Redis 캐시를 결합한 suspend 캐시 레포지토리 계약.
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
interface R2dbcLettuceRepository<ID: Any, E: Any>: Closeable {
    /** 이 레포지토리가 사용하는 Exposed [IdTable]. */
    val table: IdTable<ID>

    /** Lettuce 캐시 동작 설정. */
    val config: LettuceCacheConfig

    // -------------------------------------------------------------------------
    // DB 직접 조회 (캐시 우회)
    // -------------------------------------------------------------------------

    /** DB에서 직접 엔티티를 조회합니다 (캐시 우회). */
    suspend fun findByIdFromDb(id: ID): E?

    /** DB에서 여러 엔티티를 직접 조회합니다 (캐시 우회). */
    suspend fun findAllFromDb(ids: Collection<ID>): List<E>

    /** DB 전체 레코드 수를 반환합니다 (캐시 우회). */
    suspend fun countFromDb(): Long

    // -------------------------------------------------------------------------
    // 캐시 기반 조회 (Read-through)
    // -------------------------------------------------------------------------

    /** 캐시에서 엔티티를 조회합니다. 캐시 미스 시 DB에서 Read-through합니다. */
    suspend fun findById(id: ID): E?

    /** 여러 엔티티를 캐시에서 일괄 조회합니다. 캐시 미스 키는 DB에서 Read-through합니다. */
    suspend fun findAll(ids: Collection<ID>): Map<ID, E>

    /** DB에서 조건에 맞는 엔티티 목록을 조회한 뒤 캐시에 적재합니다. */
    suspend fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = table.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: () -> Op<Boolean> = { Op.TRUE },
    ): List<E>

    // -------------------------------------------------------------------------
    // 쓰기 (캐시 + DB)
    // -------------------------------------------------------------------------

    /** 엔티티를 캐시에 저장합니다. [LettuceCacheConfig.writeMode]에 따라 DB에도 반영됩니다. */
    suspend fun save(
        id: ID,
        entity: E,
    )

    /** 여러 엔티티를 일괄 저장합니다. */
    suspend fun saveAll(entities: Map<ID, E>)

    // -------------------------------------------------------------------------
    // 삭제
    // -------------------------------------------------------------------------

    /** 캐시에서 엔티티를 삭제합니다. WRITE_THROUGH/WRITE_BEHIND 모드에서는 DB에서도 삭제합니다. */
    suspend fun delete(id: ID)

    /** 여러 엔티티를 일괄 삭제합니다. */
    suspend fun deleteAll(ids: Collection<ID>)

    // -------------------------------------------------------------------------
    // 캐시 관리
    // -------------------------------------------------------------------------

    /** 이 레포지토리의 Redis 키를 모두 삭제합니다 (DB에는 영향 없음). */
    suspend fun clearCache()
}
