package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.cache.redis.JdbcRedisRepository
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.redisson.api.RMap
import java.io.Serializable

/**
 * Exposed JDBC와 Redisson `RMap`을 결합한 동기 캐시 리포지토리 계약입니다.
 *
 * ## 동작/계약
 * - `get/containsKey/getAll`은 Redisson read-through/write-through 설정에 따라 캐시 미스 시 DB loader를 통해 값을 채웁니다.
 * - `findByIdFromDb/findAllFromDb` 계열은 항상 DB를 직접 조회하며 캐시를 우회합니다.
 * - `put/putAll/invalidate/clear`는 캐시 조작이며, DB 반영 여부는 map writer 설정(`deleteFromDBOnInvalidate`)에 따릅니다.
 * - `batchSize`, `count`는 0보다 커야 하며 위반 시 [IllegalArgumentException]이 발생합니다.
 *
 * ```kotlin
 * val id = getExistingId()
 * val fromDb = repository.findByIdFromDb(id)
 * val fromCache = repository[id]
 * // fromCache == fromDb
 * ```
 *
 * @param E 엔티티 타입. Exposed 엔티티는 Redis 저장 시 Serializer 문제로 인해 반드시 Serializable Record를 사용해야 합니다.
 * @param ID 엔티티의 식별자 타입
 */
interface JdbcRedissonRepository<ID: Any, E: Serializable>: JdbcRedisRepository<ID, E> {
    companion object {
        const val DEFAULT_BATCH_SIZE = 500
    }

    /**
     * Exposed의 엔티티 테이블
     */
    override val table: IdTable<ID>

    /**
     * ResultRow를 엔티티로 변환하는 확장 함수
     */
    override fun ResultRow.toEntity(): E

    /**
     * 엔티티에서 식별자를 추출합니다.
     */
    override fun extractId(entity: E): ID

    /**
     * Redisson의 RMap 캐시 객체
     */
    val cache: RMap<ID, E?>

    /**
     * 캐시에 해당 ID가 존재하는지 확인합니다. 캐시에 없으면 DB에서 로드합니다.
     *
     * @param id 엔티티 식별자
     * @return 존재 여부
     */
    override fun containsKey(id: ID): Boolean = cache.containsKey(id)

    /**
     * 캐시에서 엔티티를 조회합니다.
     *
     * @param id 엔티티 식별자
     * @return 엔티티 또는 null
     */
    override fun get(id: ID): E? = cache[id]

    /**
     * 여러 엔티티를 캐시에서 일괄 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return ID를 키, 엔티티를 값으로 하는 맵
     */
    override fun getAll(ids: Collection<ID>): Map<ID, E>

    /**
     * 조건에 맞는 엔티티를 조회합니다.
     *
     * @param limit 최대 조회 개수
     * @param offset 조회 시작 위치
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

    /**
     * 엔티티를 캐시에 저장합니다.
     *
     * @param id 엔티티 식별자
     * @param entity 저장할 엔티티
     */
    override fun put(id: ID, entity: E) {
        cache.fastPut(id, entity)
    }

    /**
     * 여러 엔티티를 캐시에 일괄 저장합니다.
     *
     * @param entities 식별자를 키, 엔티티를 값으로 하는 맵
     * @param batchSize 배치 크기
     */
    override fun putAll(entities: Map<ID, E>, batchSize: Int) {
        batchSize.requirePositiveNumber("batchSize")
        cache.putAll(entities, batchSize)
    }

    /**
     * 지정한 식별자의 엔티티를 캐시에서 제거합니다.
     *
     * @param id 엔티티 식별자
     */
    override fun invalidate(id: ID) {
        cache.fastRemove(id)
    }

    /**
     * 여러 식별자의 엔티티를 캐시에서 제거합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     */
    override fun invalidateAll(ids: Collection<ID>) {
        if (ids.isEmpty()) return
        ids.forEach { cache.fastRemove(it) }
    }

    /**
     * 캐시를 모두 비웁니다.
     */
    override fun clear() = cache.clear()

    /**
     * 패턴에 맞는 키를 가진 엔티티를 캐시에서 제거합니다.
     *
     * @param patterns 키 패턴
     * @param count 최대 제거 개수
     * @return 제거된 엔티티 수
     */
    override fun invalidateByPattern(
        patterns: String,
        count: Int,
    ): Long {
        count.requirePositiveNumber("count")

        val keys = cache.keySet(patterns, count)
        if (keys.isEmpty()) {
            return 0
        }
        return keys.sumOf { cache.fastRemove(it) }
    }
}
