package io.bluetape4k.exposed.ignite.repository

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.ignite.cache.IgniteNearCache
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed와 Apache Ignite 2.x [IgniteNearCache]를 활용하여 데이터를 캐싱하는 Repository 인터페이스입니다.
 *
 * Ignite 2.x 씬 클라이언트는 Near Cache를 기본 지원하지 않으므로,
 * Caffeine(Front) + [org.apache.ignite.client.ClientCache](Back) 2-Tier 구조의
 * [IgniteNearCache]를 사용합니다.
 *
 * 임베디드 모드의 진정한 Near Cache가 필요하다면,
 * [io.bluetape4k.ignite.cache.IgniteEmbeddedNearCache]를 직접 사용하세요.
 *
 * @param T 엔티티 타입
 * @param ID 엔티티의 식별자 타입
 */
interface IgniteCacheRepository<T: HasIdentifier<ID>, ID: Any> {

    companion object {
        const val DEFAULT_BATCH_SIZE = 100
    }

    /** 캐시 이름 (Ignite 2.x 캐시 이름) */
    val cacheName: String

    /** Exposed 엔티티 테이블 */
    val entityTable: IdTable<ID>

    /** Ignite 2.x 기반 NearCache (Caffeine Front + Ignite Back) */
    val nearCache: IgniteNearCache<ID, T>

    /** [ResultRow]를 엔티티로 변환하는 확장 함수 */
    fun ResultRow.toEntity(): T

    /**
     * 캐시에 엔티티가 존재하는지 확인합니다.
     *
     * @param id 엔티티 식별자
     * @return 존재 여부
     */
    fun exists(id: ID): Boolean = nearCache.containsKey(id)

    /**
     * DB에서 최신 데이터를 직접 조회합니다. (캐시 미사용)
     *
     * @param id 엔티티 식별자
     * @return 엔티티 또는 null
     */
    fun findByIdFromDb(id: ID): T? = transaction {
        entityTable
            .selectAll()
            .where { entityTable.id eq id }
            .singleOrNull()
            ?.toEntity()
    }

    /**
     * DB에서 여러 엔티티를 직접 조회합니다. (캐시 미사용)
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return 엔티티 리스트
     */
    fun findAllFromDb(ids: Collection<ID>): List<T> = transaction {
        entityTable
            .selectAll()
            .where { entityTable.id inList ids }
            .map { it.toEntity() }
    }

    /**
     * 캐시에서 엔티티를 조회합니다. 캐시 미스 시 DB에서 로드하여 캐시에 저장합니다.
     *
     * @param id 엔티티 식별자
     * @return 엔티티 또는 null
     */
    fun get(id: ID): T? = nearCache.get(id) ?: findByIdFromDb(id)?.also { put(it) }

    /**
     * 조건에 맞는 엔티티를 DB에서 조회하고 캐시에 저장합니다.
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
    ): List<T>

    /**
     * 여러 ID의 엔티티를 배치 단위로 캐시에서 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @param batchSize 배치 크기
     * @return 엔티티 리스트
     */
    fun getAll(ids: Collection<ID>, batchSize: Int = DEFAULT_BATCH_SIZE): List<T>

    /**
     * 엔티티를 NearCache에 저장합니다 (Front Cache + Ignite Back Cache).
     *
     * @param entity 저장할 엔티티
     */
    fun put(entity: T) {
        nearCache.put(entity.id, entity)
    }

    /**
     * 여러 엔티티를 NearCache에 일괄 저장합니다.
     *
     * @param entities 저장할 엔티티 컬렉션
     * @param batchSize 배치 크기
     */
    fun putAll(entities: Collection<T>, batchSize: Int = DEFAULT_BATCH_SIZE) {
        require(batchSize > 0) { "batchSize must be greater than 0. batchSize=$batchSize" }
        entities.chunked(batchSize).forEach { chunk ->
            nearCache.putAll(chunk.associateBy { it.id })
        }
    }

    /**
     * 지정한 식별자의 엔티티를 NearCache에서 제거합니다.
     *
     * @param ids 제거할 엔티티 식별자 가변 인자
     */
    fun invalidate(vararg ids: ID) {
        nearCache.removeAll(ids.toSet())
    }

    /**
     * Front Cache(Caffeine)를 초기화합니다.
     * Back Cache(Ignite)는 변경되지 않습니다.
     */
    fun invalidateFrontCache() = nearCache.clearFrontCache()
}
