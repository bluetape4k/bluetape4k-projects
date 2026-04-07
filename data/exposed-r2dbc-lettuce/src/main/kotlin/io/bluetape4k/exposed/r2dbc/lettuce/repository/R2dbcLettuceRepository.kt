package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.exposed.cache.R2dbcCacheRepository
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import java.io.Serializable

/**
 * Exposed R2DBC와 Lettuce Redis 캐시를 결합한 suspend 캐시 레포지토리 계약.
 *
 * ## 동작/계약
 * - `get`/`getAll(ids)` — 캐시 미스 시 DB에서 Read-through하여 Redis에 캐싱합니다.
 * - `findByIdFromDb`/`findAllFromDb` — 캐시를 우회하고 DB에서 직접 조회합니다.
 * - `put`/`putAll` — [LettuceCacheConfig.writeMode]에 따라 Redis에 저장하고, WRITE_THROUGH이면 DB에도 즉시 반영합니다.
 * - `invalidate`/`invalidateAll` — Redis와 DB를 함께 삭제합니다 (NONE 모드에서는 Redis만 삭제).
 * - `clear` — Redis에서 이 레포지토리의 키를 전부 삭제합니다.
 *
 * @param ID PK 타입 ([Comparable] 구현 필요)
 * @param E 엔티티(DTO) 타입. Redis 저장 시 직렬화 문제로 반드시 [java.io.Serializable]을 구현해야 합니다.
 */
interface R2dbcLettuceRepository<ID: Any, E: Serializable>: R2dbcCacheRepository<ID, E> {
    /** 이 레포지토리가 사용하는 Exposed [IdTable]. */
    override val table: IdTable<ID>

    /** Lettuce 캐시 동작 설정. */
    val config: LettuceCacheConfig
}
