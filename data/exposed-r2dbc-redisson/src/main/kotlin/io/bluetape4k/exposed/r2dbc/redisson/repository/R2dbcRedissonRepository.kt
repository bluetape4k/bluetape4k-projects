package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.exposed.cache.R2dbcCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.redisson.api.RMap
import java.io.Serializable

/**
 * Exposed R2DBC와 Redisson `RMap`을 결합한 비동기 캐시 리포지토리 계약입니다.
 *
 * [R2dbcCacheRepository]를 확장하며 Redisson RMap 기반의 캐시 조작 기능을 제공합니다.
 *
 * ## 동작/계약
 * - `get/containsKey/getAll`은 캐시 중심 API이며 read-through 설정 시 캐시 미스에서 DB loader가 동작합니다.
 * - `findByIdFromDb/findAllFromDb`는 항상 DB를 직접 조회하며 캐시를 우회합니다.
 * - `put/putAll/invalidate/clear`는 캐시 반영 API이고 DB 반영 여부는 writer 설정에 따라 달라집니다.
 *
 * ```kotlin
 * val id = getExistingId()
 * val fromDb = repository.findByIdFromDb(id)
 * val fromCache = repository.get(id)
 * // fromCache == fromDb
 * ```
 *
 * @param E Entity Type   분산 캐시(Redisson) 저장을 위해 [Serializable] 구현이 필수입니다.
 * @param ID Entity ID Type
 */
interface R2dbcRedissonRepository<ID: Any, E: Serializable>: R2dbcCacheRepository<ID, E> {
    companion object: KLoggingChannel() {
        const val DEFAULT_BATCH_SIZE = 500
    }

    /**
     * 엔티티가 매핑되는 Exposed의 IdTable을 반환합니다.
     */
    override val table: IdTable<ID>

    /**
     * 엔티티에서 ID를 추출합니다.
     *
     * @param entity 엔티티
     * @return 엔티티의 ID
     */
    override fun extractId(entity: E): ID

    /**
     * ResultRow를 엔티티로 변환합니다.
     */
    override suspend fun ResultRow.toEntity(): E

    /**
     * Redisson의 RMap 캐시 객체를 반환합니다.
     */
    val cache: RMap<ID, E?>

    /**
     * 주어진 ID가 캐시에 존재하는지 확인합니다.
     *
     * @param id 엔티티의 식별자
     * @return 존재 여부
     */
    override suspend fun containsKey(id: ID): Boolean = cache.containsKeyAsync(id).await()

    /**
     * 주어진 ID로 DB에서 최신 엔티티를 조회합니다.
     *
     * @param id 엔티티의 식별자
     * @return 조회된 엔티티 또는 null
     */
    override suspend fun findByIdFromDb(id: ID): E? =
        suspendTransaction {
            table
                .selectAll()
                .where { table.id eq id }
                .singleOrNull()
                ?.toEntity()
        }

    /**
     * 여러 ID로 DB에서 최신 엔티티 목록을 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return 조회된 엔티티 리스트
     */
    override suspend fun findAllFromDb(ids: Collection<ID>): List<E> =
        suspendTransaction {
            table
                .selectAll()
                .where { table.id inList ids }
                .map { it.toEntity() }
                .toList()
        }

    /**
     * DB에서 전체 레코드 수를 조회합니다.
     *
     * @return 전체 레코드 수
     */
    override suspend fun countFromDb(): Long =
        suspendTransaction {
            table.selectAll().count()
        }

    /**
     * 조건에 맞는 엔티티 전체를 조회합니다.
     *
     * @param limit 조회할 최대 개수
     * @param offset 조회 시작 위치
     * @param sortBy 정렬 기준 컬럼
     * @param sortOrder 정렬 순서
     * @param where 조회 조건
     * @return 조회된 엔티티 리스트
     */
    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<E>

    /**
     * 캐시에서 엔티티를 조회합니다.
     *
     * @param id 엔티티의 식별자
     * @return 조회된 엔티티 또는 null
     */
    override suspend fun get(id: ID): E? = cache.getAsync(id).await()

    /**
     * 여러 ID로 캐시에서 엔티티 맵을 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return ID를 키, 엔티티를 값으로 하는 맵
     */
    override suspend fun getAll(ids: Collection<ID>): Map<ID, E>

    /**
     * 엔티티를 캐시에 저장합니다.
     *
     * @param id 엔티티의 식별자
     * @param entity 저장할 엔티티
     */
    override suspend fun put(id: ID, entity: E) {
        cache.fastPutAsync(id, entity).await()
    }

    /**
     * 여러 엔티티를 캐시에 저장합니다.
     *
     * @param entities 식별자를 키, 엔티티를 값으로 하는 맵
     * @param batchSize 배치 크기
     */
    override suspend fun putAll(entities: Map<ID, E>, batchSize: Int) {
        batchSize.requirePositiveNumber("batchSize")
        @Suppress("UNCHECKED_CAST")
        cache.putAllAsync(entities as Map<ID, E?>, batchSize).await()
    }

    /**
     * 주어진 ID의 엔티티를 캐시에서 제거합니다.
     *
     * @param id 삭제할 엔티티 식별자
     */
    override suspend fun invalidate(id: ID) {
        cache.fastRemoveAsync(id).await()
    }

    /**
     * 여러 ID의 엔티티를 캐시에서 제거합니다.
     *
     * @param ids 삭제할 엔티티 식별자 목록
     */
    override suspend fun invalidateAll(ids: Collection<ID>) {
        if (ids.isEmpty()) return
        ids.forEach { id -> cache.fastRemoveAsync(id).await() }
    }

    /**
     * 캐시의 모든 엔티티를 제거합니다.
     */
    override suspend fun clear() {
        cache.clearAsync().await()
    }

    /**
     * 패턴에 맞는 키의 엔티티를 캐시에서 제거합니다.
     *
     * @param patterns 키 패턴
     * @param count 최대 삭제 개수
     * @return 삭제된 엔티티 개수
     */
    override suspend fun invalidateByPattern(
        patterns: String,
        count: Int,
    ): Long {
        count.requirePositiveNumber("count")
        val keys = cache.keySet(patterns, count)
        if (keys.isEmpty()) {
            return 0
        }
        var removed = 0L
        keys.forEach { key -> removed += cache.fastRemoveAsync(key).await() }
        return removed
    }
}
