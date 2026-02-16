package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
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

/**
 * R2dbcCacheRepository는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type   Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable Record를 사용해 주세요.
 * @param ID Entity ID Type
 */
interface R2dbcCacheRepository<T: HasIdentifier<ID>, ID: Any> {

    companion object: KLoggingChannel() {
        const val DEFAULT_BATCH_SIZE = 100

        @Deprecated(
            message = "Use DEFAULT_BATCH_SIZE",
            replaceWith = ReplaceWith("DEFAULT_BATCH_SIZE"),
            level = DeprecationLevel.WARNING,
        )
        const val DefaultBatchSize = DEFAULT_BATCH_SIZE
    }

    /**
     * 캐시 이름을 반환합니다.
     */
    val cacheName: String

    /**
     * 엔티티가 매핑되는 Exposed의 IdTable을 반환합니다.
     */
    val entityTable: IdTable<ID>

    /**
     * ResultRow를 엔티티로 변환합니다.
     */
    suspend fun ResultRow.toEntity(): T

    /**
     * Redisson의 RMap 캐시 객체를 반환합니다.
     */
    val cache: RMap<ID, T?>

    /**
     * 주어진 ID가 캐시에 존재하는지 확인합니다.
     *
     * @param id 엔티티의 식별자
     * @return 존재 여부
     */
    suspend fun exists(id: ID): Boolean = cache.containsKeyAsync(id).awaitSuspending()

    /**
     * 주어진 ID로 DB에서 최신 엔티티를 조회합니다.
     *
     * @param id 엔티티의 식별자
     * @return 조회된 엔티티 또는 null
     */
    @Deprecated("use findByIdFromDb", replaceWith = ReplaceWith("findByIdFromDb(id)"), level = DeprecationLevel.WARNING)
    suspend fun findFreshById(id: ID): T? = suspendTransaction {
        entityTable
            .selectAll()
            .where { entityTable.id eq id }
            .singleOrNull()
            ?.toEntity()
    }

    /**
     * 주어진 ID로 DB에서 최신 엔티티를 조회합니다.
     *
     * @param id 엔티티의 식별자
     * @return 조회된 엔티티 또는 null
     */
    suspend fun findByIdFromDb(id: ID): T? = suspendTransaction {
        entityTable
            .selectAll()
            .where { entityTable.id eq id }
            .singleOrNull()
            ?.toEntity()
    }

    /**
     * 여러 ID로 DB에서 최신 엔티티 목록을 조회합니다.
     *
     * @param ids 엔티티 식별자 목록
     * @return 조회된 엔티티 리스트
     */
    @Deprecated("use findAllFromDb", replaceWith = ReplaceWith("findAllFromDb(ids)"), level = DeprecationLevel.WARNING)
    suspend fun findFreshAll(vararg ids: ID): List<T> = suspendTransaction {
        entityTable
            .selectAll()
            .where { entityTable.id inList ids.toList() }
            .map { it.toEntity() }
            .toList()

    }

    /**
     * 여러 ID로 DB에서 최신 엔티티 목록을 조회합니다.
     *
     * @param ids 엔티티 식별자 목록
     * @return 조회된 엔티티 리스트
     */
    suspend fun findAllFromDb(vararg ids: ID): List<T> = suspendTransaction {
        entityTable
            .selectAll()
            .where { entityTable.id inList ids.toList() }
            .map { it.toEntity() }
            .toList()
    }

    /**
     * 여러 ID로 DB에서 최신 엔티티 목록을 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return 조회된 엔티티 리스트
     */
    @Deprecated("use findAllFromDb", replaceWith = ReplaceWith("findAllFromDb(ids)"), level = DeprecationLevel.WARNING)
    suspend fun findFreshAll(ids: Collection<ID>): List<T> = suspendTransaction {
        entityTable
            .selectAll()
            .where { entityTable.id inList ids }
            .map { it.toEntity() }
            .toList()
    }

    /**
     * 여러 ID로 DB에서 최신 엔티티 목록을 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return 조회된 엔티티 리스트
     */
    suspend fun findAllFromDb(ids: Collection<ID>): List<T> = suspendTransaction {
        entityTable
            .selectAll()
            .where { entityTable.id inList ids }
            .map { it.toEntity() }
            .toList()
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
    suspend fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = entityTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: () -> Op<Boolean> = { Op.TRUE },
    ): List<T>

    /**
     * 캐시에서 엔티티를 조회합니다.
     *
     * @param id 엔티티의 식별자
     * @return 조회된 엔티티 또는 null
     */
    suspend fun get(id: ID): T? = cache.getAsync(id).awaitSuspending()

    /**
     * 여러 ID로 캐시에서 엔티티 목록을 조회합니다.
     *
     * @param ids 엔티티 식별자 컬렉션
     * @param batchSize 배치 크기
     * @return 조회된 엔티티 리스트
     */
    suspend fun getAll(ids: Collection<ID>, batchSize: Int = DEFAULT_BATCH_SIZE): List<T>

    /**
     * 엔티티를 캐시에 저장합니다.
     *
     * @param entity 저장할 엔티티
     * @return 저장 성공 여부
     */
    suspend fun put(entity: T): Boolean? = cache.fastPutAsync(entity.id, entity).awaitSuspending()

    /**
     * 여러 엔티티를 캐시에 저장합니다.
     *
     * @param entities 저장할 엔티티 컬렉션
     * @param batchSize 배치 크기
     */
    suspend fun putAll(entities: Collection<T>, batchSize: Int = DEFAULT_BATCH_SIZE) {
        require(batchSize > 0) { "batchSize must be greater than 0. batchSize=$batchSize" }
        cache.putAllAsync(entities.associateBy { it.id }, batchSize).awaitSuspending()
    }

    /**
     * 주어진 ID의 엔티티를 캐시에서 제거합니다.
     *
     * @param ids 삭제할 엔티티 식별자 목록
     * @return 삭제된 엔티티 개수
     */
    suspend fun invalidate(vararg ids: ID): Long = cache.fastRemoveAsync(*ids).awaitSuspending()

    /**
     * 캐시의 모든 엔티티를 제거합니다.
     *
     * @return 성공 여부
     */
    suspend fun invalidateAll(): Boolean = cache.clearAsync().awaitSuspending()

    /**
     * 패턴에 맞는 키의 엔티티를 캐시에서 제거합니다.
     *
     * @param patterns 키 패턴
     * @param count 최대 삭제 개수
     * @return 삭제된 엔티티 개수
     */
    suspend fun invalidateByPattern(patterns: String, count: Int = DEFAULT_BATCH_SIZE): Long {
        require(count > 0) { "count must be greater than 0. count=$count" }
        val keys = cache.keySet(patterns, count)
        return cache.fastRemoveAsync(*keys.toTypedArray()).awaitSuspending()
    }
}
