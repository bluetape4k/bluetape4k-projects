package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.collections.toVarargArray
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.redisson.api.RMap

/**
 * Exposed JDBC와 Redisson `RMap`을 결합한 동기 캐시 리포지토리 계약입니다.
 *
 * ## 동작/계약
 * - `get/exists/getAll`은 Redisson read-through/write-through 설정에 따라 캐시 미스 시 DB loader를 통해 값을 채웁니다.
 * - `findByIdFromDb/findAllFromDb` 계열은 항상 DB를 직접 조회하며 캐시를 우회합니다.
 * - `put/putAll/invalidate/invalidateAll`은 캐시 조작이며, DB 반영 여부는 map writer 설정(`deleteFromDBOnInvalidate`)에 따릅니다.
 * - `batchSize`, `count`는 0보다 커야 하며 위반 시 [IllegalArgumentException]이 발생합니다.
 *
 * ```kotlin
 * val id = getExistingId()
 * val fromDb = repository.findByIdFromDb(id)
 * val fromCache = repository[id]
 * // fromCache == fromDb
 * ```
 *
 * @param T 엔티티 타입. Exposed 엔티티는 Redis 저장 시 Serializer 문제로 인해 반드시 Serializable Record를 사용해야 합니다.
 * @param ID 엔티티의 식별자 타입
 */
interface JdbcRedissonRepository<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>> {
    companion object {
        const val DEFAULT_BATCH_SIZE = 100
    }

    /**
     * 캐시 이름
     */
    val cacheName: String

    /**
     * Exposed의 엔티티 테이블
     */
    val entityTable: T

    /**
     * ResultRow를 엔티티로 변환하는 확장 함수
     */
    fun ResultRow.toEntity(): E

    /**
     * Redisson의 RMap 캐시 객체
     */
    val cache: RMap<ID, E?>

    /**
     * 캐시에 존재하지 않으면 Read Through로 DB에서 읽어옵니다. DB에도 없을 경우 false를 반환합니다.
     *
     * @param id 엔티티 식별자
     * @return 존재 여부
     */
    fun exists(id: ID): Boolean = cache.containsKey(id)


    /**
     * DB에서 최신 데이터를 조회합니다.
     *
     * @param id 엔티티 식별자
     * @return 엔티티 또는 null
     */
    fun findByIdFromDb(id: ID): E? = transaction {
        entityTable
            .selectAll()
            .where { entityTable.id eq id }
            .singleOrNull()
            ?.toEntity()
    }

    /**
     * DB에서 여러 엔티티를 조회합니다.
     *
     * @param ids 엔티티 식별자 목록
     * @return 엔티티 리스트
     */
    fun findAllFromDb(vararg ids: ID): List<E> = transaction {
        entityTable
            .selectAll()
            .where { entityTable.id inList ids.toList() }
            .map { it.toEntity() }
    }

    /**
     * DB에서 여러 엔티티를 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return 엔티티 리스트
     */
    fun findAllFromDb(ids: Collection<ID>): List<E> = transaction {
        entityTable
            .selectAll()
            .where { entityTable.id inList ids }
            .map { it.toEntity() }
    }

    /**
     * 캐시에서 엔티티를 조회합니다.
     *
     * @param id 엔티티 식별자
     * @return 엔티티 또는 null
     */
    operator fun get(id: ID): E? = cache[id]

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
    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = entityTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: () -> Op<Boolean> = { Op.TRUE },
    ): List<E>

    /**
     * 여러 엔티티를 캐시에서 일괄 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @param batchSize 배치 크기
     * @return 엔티티 리스트
     */
    fun getAll(ids: Collection<ID>, batchSize: Int = DEFAULT_BATCH_SIZE): List<E>

    /**
     * 엔티티를 캐시에 저장합니다.
     *
     * @param entity 저장할 엔티티
     */
    fun put(entity: E) = cache.fastPut(entity.id, entity)

    /**
     * 여러 엔티티를 캐시에 일괄 저장합니다.
     *
     * @param entities 저장할 엔티티 컬렉션
     * @param batchSize 배치 크기
     */
    fun putAll(entities: Collection<E>, batchSize: Int = DEFAULT_BATCH_SIZE) {
        require(batchSize > 0) { "batchSize must be greater than 0. batchSize=$batchSize" }
        cache.putAll(entities.associateBy { it.id }, batchSize)
    }

    /**
     * 지정한 식별자의 엔티티를 캐시에서 제거합니다.
     *
     * @param ids 엔티티 식별자 가변 인자
     * @return 제거된 엔티티 수
     */
    fun invalidate(vararg ids: ID): Long = cache.fastRemove(*ids)

    /**
     * 캐시를 모두 비웁니다.
     */
    fun invalidateAll() = cache.clear()

    /**
     * 패턴에 맞는 키를 가진 엔티티를 캐시에서 제거합니다.
     *
     * @param patterns 키 패턴
     * @param count 최대 제거 개수
     * @return 제거된 엔티티 수
     */
    fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long {
        count.requirePositiveNumber("count")
        
        val keys = cache.keySet(patterns, count)
        if (keys.isEmpty()) {
            return 0
        }
        return cache.fastRemove(*keys.toVarargArray())
    }
}
