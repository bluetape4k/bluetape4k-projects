package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.exposed.cache.redis.SuspendJdbcRedisRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.LettuceSuspendedLoadedMap
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import java.io.Serializable

/**
 * Exposed JDBC와 Lettuce Redis 캐시를 결합한 suspend(코루틴) 기반 캐시 레포지토리 계약입니다.
 *
 * ## 동작/계약
 * - `get(id)` — 캐시 미스 시 DB에서 Read-through하여 Redis에 캐싱합니다.
 * - `getAll(ids)` — 캐시 미스 키는 DB에서 Read-through합니다.
 * - `findByIdFromDb`/`findAllFromDb` — 캐시를 우회하고 DB에서 직접 조회합니다 (`suspendedTransactionAsync(IO)`).
 * - `put`/`putAll` — [LettuceCacheConfig.writeMode]에 따라 Redis에 저장하고, WRITE_THROUGH이면 DB에도 즉시 반영합니다.
 * - `invalidate`/`invalidateAll` — Redis와 DB를 함께 삭제합니다 (NONE 모드에서는 Redis만 삭제).
 * - `clear` — Redis에서 이 레포지토리의 키를 전부 삭제합니다.
 *
 * 모든 메서드는 `suspend` 함수로, `withContext(Dispatchers.IO)` 또는 `suspendedTransactionAsync`를 통해
 * 코루틴 친화적으로 동작합니다.
 *
 * @param ID PK 타입 ([Comparable] 구현 필요)
 * @param E 엔티티(DTO) 타입. Redis 저장 시 직렬화 문제로 반드시 [java.io.Serializable]을 구현해야 합니다.
 */
interface SuspendedJdbcLettuceRepository<ID: Any, E: Serializable>: SuspendJdbcRedisRepository<ID, E> {

    companion object: KLoggingChannel() {
        const val DEFAULT_BATCH_SIZE = 500
    }

    /**
     * 엔티티가 매핑되는 Exposed의 IdTable.
     */
    override val table: IdTable<ID>

    /**
     * [ResultRow]를 엔티티 [E]로 변환합니다.
     */
    override fun ResultRow.toEntity(): E

    /**
     * 엔티티에서 식별자를 추출합니다.
     */
    override fun extractId(entity: E): ID

    /**
     * Lettuce 캐시 동작 설정.
     */
    val config: LettuceCacheConfig

    /**
     * Lettuce 기반 코루틴 네이티브 Read-through / Write-through 캐시 맵.
     */
    val cache: LettuceSuspendedLoadedMap<ID, E>

    /**
     * 캐시에 해당 ID가 존재하는지 확인합니다.
     *
     * @param id 엔티티 식별자
     * @return 존재 여부
     */
    override suspend fun containsKey(id: ID): Boolean = get(id) != null

    /**
     * 캐시에서 ID로 엔티티를 조회합니다 (캐시 미스 시 DB Read-through).
     *
     * @param id 엔티티 식별자
     * @return 조회된 엔티티 또는 null
     */
    override suspend fun get(id: ID): E? = cache.get(id)

    /**
     * 여러 ID에 해당하는 엔티티를 캐시에서 일괄 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return ID를 키, 엔티티를 값으로 하는 맵
     */
    override suspend fun getAll(ids: Collection<ID>): Map<ID, E> = cache.getAll(ids.toSet())

    /**
     * 조건에 맞는 엔티티를 페이징하여 조회합니다.
     *
     * @param limit 최대 조회 건수 (null이면 전체)
     * @param offset 건너뛸 레코드 수 (null이면 0)
     * @param sortBy 정렬 기준 컬럼
     * @param sortOrder 정렬 순서
     * @param where 조회 조건
     * @return 엔티티 리스트
     */
    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<E>

    /**
     * 엔티티를 캐시에 저장합니다 (Write-through 시 DB에도 반영).
     *
     * @param id 엔티티 식별자
     * @param entity 저장할 엔티티
     */
    override suspend fun put(id: ID, entity: E) {
        cache.set(id, entity)
    }

    /**
     * 여러 엔티티를 캐시에 일괄 저장합니다.
     *
     * @param entities 식별자를 키, 엔티티를 값으로 하는 맵
     * @param batchSize 배치 크기
     */
    override suspend fun putAll(entities: Map<ID, E>, batchSize: Int) {
        batchSize.requirePositiveNumber("batchSize")
        entities.forEach { (id, entity) -> cache.set(id, entity) }
    }

    /**
     * 지정한 ID의 엔티티를 캐시에서 제거합니다.
     *
     * @param id 엔티티 식별자
     */
    override suspend fun invalidate(id: ID) {
        cache.evict(id)
    }

    /**
     * 여러 ID의 엔티티를 캐시에서 제거합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     */
    override suspend fun invalidateAll(ids: Collection<ID>) {
        cache.evictAll(ids)
    }

    /**
     * 패턴에 맞는 키를 가진 엔티티를 캐시에서 제거합니다.
     *
     * @param patterns 키 패턴
     * @param count 최대 제거 개수
     * @return 제거된 엔티티 수
     */
    override suspend fun invalidateByPattern(patterns: String, count: Int): Long {
        count.requirePositiveNumber("count")
        return cache.invalidateByPattern(patterns, count.toLong())
    }

    /**
     * 캐시를 모두 비웁니다.
     */
    override suspend fun clear() {
        cache.clear()
    }
}
