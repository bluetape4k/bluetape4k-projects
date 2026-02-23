package io.bluetape4k.exposed.ignite.repository

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.ignite.cache.IgniteNearCache
import io.bluetape4k.ignite.cache.IgniteNearCacheConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.client.IgniteClient
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed와 Apache Ignite 2.x를 사용하여 데이터를 캐싱하는 Repository의 추상 구현체입니다.
 *
 * Ignite 2.x 씬 클라이언트([org.apache.ignite.client.ClientCache])를 Back Cache로,
 * Caffeine을 Front Cache로 사용하는 2-Tier [IgniteNearCache]를 내장합니다.
 *
 * 임베디드 모드에서 진정한 Near Cache([org.apache.ignite.configuration.NearCacheConfiguration])를
 * 사용하려면 [io.bluetape4k.ignite.cache.IgniteEmbeddedNearCache]를 직접 활용하세요.
 *
 * **사용 예시:**
 * ```kotlin
 * class OrderRepository(igniteClient: IgniteClient) :
 *     AbstractIgniteCacheRepository<OrderRecord, Long>(
 *         igniteClient = igniteClient,
 *         config = IgniteNearCacheConfig(cacheName = "ORDERS"),
 *     ) {
 *     override val entityTable = OrdersTable
 *     override fun createNearCache(): IgniteNearCache<Long, OrderRecord> =
 *         IgniteNearCache(igniteClient, config)
 *     override fun ResultRow.toEntity() = OrderRecord(this[OrdersTable.id].value, ...)
 * }
 * ```
 *
 * @param T 엔티티 타입
 * @param ID 엔티티의 식별자 타입
 * @param igniteClient Apache Ignite 2.x 씬 클라이언트
 * @param config Ignite 2.x NearCache 설정
 */
abstract class AbstractIgniteCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    val igniteClient: IgniteClient,
    protected val config: IgniteNearCacheConfig,
): IgniteCacheRepository<T, ID> {

    companion object: KLogging()

    override val cacheName: String get() = config.cacheName

    /**
     * Caffeine(Front) + Ignite 2.x 씬 클라이언트(Back) 2-Tier NearCache.
     * 서브클래스에서 [createNearCache]를 구현하여 구체적인 타입을 지정합니다.
     */
    override val nearCache: IgniteNearCache<ID, T> by lazy {
        createNearCache()
    }

    /**
     * [IgniteNearCache] 인스턴스를 생성합니다.
     * 서브클래스에서 구체적인 키/값 타입을 지정하여 구현합니다.
     *
     * ```kotlin
     * override fun createNearCache(): IgniteNearCache<Long, OrderRecord> =
     *     IgniteNearCache(igniteClient, config)
     * ```
     *
     * @return [IgniteNearCache] 인스턴스
     */
    protected abstract fun createNearCache(): IgniteNearCache<ID, T>

    /**
     * DB에서 조건에 맞는 엔티티 목록을 조회하고 NearCache에 저장합니다.
     */
    override fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<T> {
        val entities = transaction {
            entityTable
                .selectAll()
                .where(where)
                .apply {
                    orderBy(sortBy, sortOrder)
                    limit?.run { limit(limit) }
                    offset?.run { offset(offset) }
                }.map { it.toEntity() }
        }

        if (entities.isNotEmpty()) {
            log.debug { "DB에서 ${entities.size}개 엔티티 조회 완료. Ignite 2.x NearCache에 저장합니다." }
            nearCache.putAll(entities.associateBy { it.id })
        }
        return entities
    }

    /**
     * 여러 ID의 엔티티를 배치 단위로 NearCache에서 조회합니다.
     * 캐시 미스된 항목은 DB에서 로드하여 NearCache에 추가합니다.
     */
    override fun getAll(ids: Collection<ID>, batchSize: Int): List<T> {
        require(batchSize > 0) { "batchSize must be greater than 0. batchSize=$batchSize" }
        if (ids.isEmpty()) return emptyList()

        return ids.chunked(batchSize).flatMap { chunk ->
            val chunkSet = chunk.toSet()
            val fromCache = nearCache.getAll(chunkSet)

            val missedIds = chunkSet - fromCache.keys
            val fromDb = if (missedIds.isNotEmpty()) {
                log.debug { "NearCache 미스 ${missedIds.size}개 - DB에서 조회합니다." }
                val dbEntities = findAllFromDb(missedIds)
                if (dbEntities.isNotEmpty()) {
                    nearCache.putAll(dbEntities.associateBy { it.id })
                }
                dbEntities
            } else emptyList()

            fromCache.values + fromDb
        }
    }
}
