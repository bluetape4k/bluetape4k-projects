package io.bluetape4k.exposed.r2dbc.caffeine.repository

import com.github.benmanes.caffeine.cache.AsyncCache
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.R2dbcCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import java.io.Serializable

/**
 * Exposed R2DBC와 Caffeine 로컬 캐시를 결합한 suspend 캐시 레포지토리 계약.
 *
 * ## 동작/계약
 * - `get`/`getAll(ids)` — 캐시 미스 시 DB에서 Read-through하여 Caffeine에 캐싱합니다.
 * - `findByIdFromDb`/`findAllFromDb` — 캐시를 우회하고 DB에서 직접 조회합니다.
 * - `put`/`putAll` — [LocalCacheConfig.writeMode]에 따라 Caffeine에 저장하고, WRITE_THROUGH이면 DB에도 즉시 반영합니다.
 * - `invalidate`/`invalidateAll` — Caffeine 캐시에서 엔트리를 제거합니다.
 * - `clear` — Caffeine 캐시의 모든 엔트리를 제거합니다.
 *
 * JDBC 의존 없이 `exposed-cache` 모듈의 [R2dbcCacheRepository]만 참조합니다.
 *
 * @param ID PK 타입 ([Comparable] 구현 필요)
 * @param E 엔티티(DTO) 타입. 캐시 저장을 위해 [Serializable]을 구현해야 합니다.
 */
interface R2dbcCaffeineRepository<ID: Any, E: Serializable>: R2dbcCacheRepository<ID, E> {

    companion object: KLoggingChannel() {
        const val DEFAULT_BATCH_SIZE = 500
    }

    /**
     * 이 레포지토리가 사용하는 Exposed [IdTable].
     */
    override val table: IdTable<ID>

    /**
     * [ResultRow]를 엔티티 [E]로 변환하는 suspend 함수.
     */
    override suspend fun ResultRow.toEntity(): E

    /**
     * 엔티티에서 식별자를 추출합니다.
     */
    override fun extractId(entity: E): ID

    /**
     * Caffeine 로컬 캐시 설정.
     */
    val config: LocalCacheConfig

    /**
     * Caffeine 비동기 캐시. 키는 문자열로 직렬화된 ID입니다.
     */
    val cache: AsyncCache<String, E>
}
