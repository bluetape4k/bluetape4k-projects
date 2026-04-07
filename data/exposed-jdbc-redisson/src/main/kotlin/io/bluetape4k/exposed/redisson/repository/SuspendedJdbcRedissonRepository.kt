package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.cache.redis.SuspendJdbcRedisRepository
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.future.await
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.redisson.api.RMap
import java.io.Serializable

/**
 * Exposed JDBC와 Redisson `RMap`을 코루틴으로 연결하는 비동기 캐시 리포지토리 계약입니다.
 *
 * ## 동작/계약
 * - `findByIdFromDb/findAllFromDb`는 `suspendedTransactionAsync(Dispatchers.IO)`로 DB를 직접 조회합니다.
 * - `get/containsKey/getAll/put/invalidate`는 Redisson async API를 `await()`로 감싸 suspend 형태로 제공합니다.
 * - `batchSize`, `count`는 0보다 커야 하며 위반 시 [IllegalArgumentException]이 발생합니다.
 * - 캐시 무효화가 DB 삭제로 이어지는지는 map writer 설정에 따라 달라집니다.
 *
 * ```kotlin
 * val id = getExistingId()
 * val fromDb = repository.findByIdFromDb(id)
 * val fromCache = repository.get(id)
 * // fromCache == fromDb
 * ```
 *
 * @param E 엔티티 타입. Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable type을 사용해 주세요.
 * @param ID 엔티티의 식별자 타입
 */
interface SuspendedJdbcRedissonRepository<ID: Any, E: Serializable>: SuspendJdbcRedisRepository<ID, E> {
    companion object {
        const val DEFAULT_BATCH_SIZE = 500
    }

    /**
     * 엔티티가 매핑되는 Exposed의 IdTable을 반환합니다.
     */
    override val table: IdTable<ID>

    /**
     * ResultRow를 엔티티로 변환합니다.
     */
    override fun ResultRow.toEntity(): E

    /**
     * 엔티티에서 식별자를 추출합니다.
     */
    override fun extractId(entity: E): ID

    /**
     * Redisson의 RMap 캐시 객체를 반환합니다.
     */
    val cache: RMap<ID, E?>

    /**
     * 주어진 ID의 엔티티가 캐시에 존재하는지 확인합니다.
     *
     * @param id 엔티티의 식별자
     * @return 존재 여부
     */
    override suspend fun containsKey(id: ID): Boolean = cache.containsKeyAsync(id).await()

    /**
     * 캐시에서 엔티티를 조회합니다.
     *
     * @param id 엔티티의 식별자
     * @return 조회된 엔티티 또는 null
     */
    override suspend fun get(id: ID): E? = cache.getAsync(id).await()

    /**
     * 여러 ID에 해당하는 엔티티를 캐시에서 일괄 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return ID를 키, 엔티티를 값으로 하는 맵
     */
    override suspend fun getAll(ids: Collection<ID>): Map<ID, E>

    /**
     * 조건에 맞는 모든 엔티티를 조회합니다.
     *
     * @param limit 조회할 최대 개수
     * @param offset 조회 시작 위치
     * @param sortBy 정렬 기준 컬럼
     * @param sortOrder 정렬 순서
     * @param where 조회 조건
     * @return 조회된 엔티티 목록
     */
    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<E>

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
     * 여러 엔티티를 캐시에 일괄 저장합니다.
     *
     * @param entities 식별자를 키, 엔티티를 값으로 하는 맵
     * @param batchSize 일괄 처리 크기
     */
    override suspend fun putAll(entities: Map<ID, E>, batchSize: Int) {
        batchSize.requirePositiveNumber("batchSize")
        cache.putAllAsync(entities, batchSize).await()
    }

    /**
     * 주어진 ID의 엔티티를 캐시에서 삭제합니다.
     *
     * @param id 삭제할 엔티티 식별자
     */
    override suspend fun invalidate(id: ID) {
        cache.fastRemoveAsync(id).await()
    }

    /**
     * 여러 ID의 엔티티를 캐시에서 삭제합니다.
     *
     * @param ids 삭제할 엔티티 식별자 목록
     */
    override suspend fun invalidateAll(ids: Collection<ID>) {
        if (ids.isEmpty()) return
        ids.forEach { cache.fastRemoveAsync(it).await() }
    }

    /**
     * 캐시의 모든 엔티티를 삭제합니다.
     */
    override suspend fun clear() {
        cache.clearAsync().await()
    }

    /**
     * 패턴에 해당하는 키를 가진 엔티티를 캐시에서 삭제합니다.
     *
     * @param patterns 삭제할 키 패턴
     * @param count 일괄 처리 크기
     * @return 삭제된 엔티티 수
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
        keys.forEach { removed += cache.fastRemoveAsync(it).await() }
        return removed
    }
}
