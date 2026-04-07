package io.bluetape4k.exposed.cache

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import java.io.Closeable
import java.io.Serializable

/**
 * Exposed JDBC + Redis 캐시 기반 저장소의 공통 인터페이스입니다.
 *
 * DB 직접 조회, 캐시 조회/저장/무효화 등의 기본 연산을 정의합니다.
 * javax.cache.Cache 관례를 따르는 메서드명을 사용합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입 (분산 캐시 저장을 위해 [Serializable] 구현 필수)
 */
interface JdbcCacheRepository<ID: Any, E: Serializable>: Closeable {

    companion object: KLogging() {
        /** 기본 배치 크기 */
        const val DEFAULT_BATCH_SIZE: Int = 500
    }

    /**
     * 대상 Exposed 테이블
     */
    val table: IdTable<ID>

    /**
     * Redis 캐시 이름 (캐시 키 접두사로 사용)
     */
    val cacheName: String

    /**
     * 캐시 저장 방식 (원격 전용 또는 NearCache)
     */
    val cacheMode: CacheMode

    /**
     * 캐시 쓰기 전략 (READ_ONLY, WRITE_THROUGH, WRITE_BEHIND)
     */
    val cacheWriteMode: CacheWriteMode

    /**
     * [ResultRow]를 엔티티 [E]로 변환합니다.
     *
     * @return 변환된 엔티티
     */
    fun ResultRow.toEntity(): E

    /**
     * 엔티티에서 식별자를 추출합니다.
     *
     * @param entity 엔티티
     * @return 엔티티의 식별자
     */
    fun extractId(entity: E): ID

    // ----------------------------------------------------------------
    // DB 직접 조회 (캐시 우회)
    // ----------------------------------------------------------------

    /**
     * 캐시를 우회하여 DB에서 직접 ID로 엔티티를 조회합니다.
     *
     * @param id 조회할 엔티티의 식별자
     * @return 조회된 엔티티, 없으면 null
     */
    fun findByIdFromDb(id: ID): E?

    /**
     * 캐시를 우회하여 DB에서 직접 여러 ID의 엔티티를 조회합니다.
     *
     * @param ids 조회할 엔티티의 식별자 목록
     * @return 조회된 엔티티 목록
     */
    fun findAllFromDb(ids: Collection<ID>): List<E>

    /**
     * 캐시를 우회하여 DB에서 직접 전체 레코드 수를 조회합니다.
     *
     * @return 전체 레코드 수
     */
    fun countFromDb(): Long

    // ----------------------------------------------------------------
    // DB 페이징 쿼리
    // ----------------------------------------------------------------

    /**
     * 조건에 맞는 엔티티를 페이징하여 조회합니다.
     *
     * @param limit 최대 조회 건수 (null이면 전체)
     * @param offset 건너뛸 레코드 수 (null이면 0)
     * @param sortBy 정렬 기준 컬럼 (기본값: [table].id)
     * @param sortOrder 정렬 순서 (기본값: ASC)
     * @param where 조회 조건 (기본값: 전체)
     * @return 조회된 엔티티 목록
     */
    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = table.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: () -> Op<Boolean> = { Op.TRUE },
    ): List<E>

    // ----------------------------------------------------------------
    // javax.cache.Cache 관례 - 캐시 조회
    // ----------------------------------------------------------------

    /**
     * 캐시에 해당 ID의 엔티티가 존재하는지 확인합니다.
     * 캐시에 없으면 DB에서 로드하여 캐시에 저장 후 존재 여부를 반환합니다.
     *
     * @param id 확인할 엔티티의 식별자
     * @return 존재하면 true, 아니면 false
     */
    fun containsKey(id: ID): Boolean

    /**
     * 캐시에서 ID로 엔티티를 조회합니다.
     * 캐시에 없으면 DB에서 읽어서 캐시에 저장 후 반환합니다 (Read-Through).
     *
     * @param id 조회할 엔티티의 식별자
     * @return 조회된 엔티티, 없으면 null
     */
    fun get(id: ID): E?

    /**
     * 캐시에서 여러 ID의 엔티티를 한 번에 조회합니다.
     * 캐시에 없는 항목은 DB에서 읽어서 캐시에 저장 후 반환합니다.
     *
     * @param ids 조회할 엔티티의 식별자 목록
     * @return ID를 키, 엔티티를 값으로 하는 맵
     */
    fun getAll(ids: Collection<ID>): Map<ID, E>

    // ----------------------------------------------------------------
    // javax.cache.Cache 관례 - 캐시 저장
    // ----------------------------------------------------------------

    /**
     * 캐시에 엔티티를 저장합니다.
     * [cacheWriteMode]에 따라 DB에도 동시 반영될 수 있습니다.
     *
     * @param id 엔티티의 식별자
     * @param entity 저장할 엔티티
     */
    fun put(id: ID, entity: E)

    /**
     * 캐시에 여러 엔티티를 일괄 저장합니다.
     * [cacheWriteMode]에 따라 DB에도 동시 반영될 수 있습니다.
     *
     * @param entities 식별자를 키, 엔티티를 값으로 하는 맵
     * @param batchSize 일괄 처리 크기 (기본값: [DEFAULT_BATCH_SIZE])
     */
    fun putAll(entities: Map<ID, E>, batchSize: Int = DEFAULT_BATCH_SIZE)

    // ----------------------------------------------------------------
    // javax.cache.Cache 관례 - 캐시 제거 (DB 미영향)
    // ----------------------------------------------------------------

    /**
     * 캐시에서 해당 ID의 엔티티를 제거합니다 (DB에는 영향 없음).
     *
     * @param id 제거할 엔티티의 식별자
     */
    fun invalidate(id: ID)

    /**
     * 캐시에서 여러 ID의 엔티티를 제거합니다 (DB에는 영향 없음).
     *
     * @param ids 제거할 엔티티의 식별자 목록
     */
    fun invalidateAll(ids: Collection<ID>)

    /**
     * 캐시의 모든 항목을 제거합니다 (DB에는 영향 없음).
     */
    fun clear()

    override fun close() {}
}
