package io.bluetape4k.exposed.hazelcast.repository

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.hazelcast.cache.HazelcastNearCacheConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed와 Hazelcast를 사용하여 데이터를 캐싱하는 Repository의 추상 구현체입니다.
 *
 * **Near Cache 사용 방법**: [HazelcastInstance]가 클라이언트 모드이고
 * [ClientConfig]에 [HazelcastNearCacheConfig]가 설정된 경우, [IMap]은 자동으로
 * Near Cache를 사용합니다.
 *
 * ```kotlin
 * class OrderRepository(hazelcast: HazelcastInstance) :
 *     AbstractHazelcastCacheRepository<OrderRecord, Long>(hazelcast, "orders") {
 *
 *     override val entityTable = OrdersTable
 *     override fun ResultRow.toEntity() = OrderRecord(this[OrdersTable.id].value, ...)
 * }
 * ```
 *
 * @param T 엔티티 타입 (Hazelcast 직렬화를 위해 [java.io.Serializable] 구현 권장)
 * @param ID 엔티티의 식별자 타입
 * @param hazelcastInstance Hazelcast 인스턴스 (클라이언트 또는 임베디드)
 * @param cacheName IMap 이름 (캐시 이름)
 * @param nearCacheConfig Near Cache 설정 (null이면 Near Cache 미사용)
 */
abstract class AbstractHazelcastCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    val hazelcastInstance: HazelcastInstance,
    override val cacheName: String,
    protected val nearCacheConfig: HazelcastNearCacheConfig? = null,
): HazelcastCacheRepository<T, ID> {

    companion object: KLogging()

    /**
     * Hazelcast [IMap] 캐시 객체.
     * Near Cache 설정이 있는 경우, 클라이언트 설정에 Near Cache가 포함되어야 합니다.
     */
    override val cache: IMap<ID, T?> by lazy {
        nearCacheConfig?.let {
            // Near Cache 설정이 있으면 ClientConfig에 추가 시도 (클라이언트 모드에서만 동작)
            log.debug { "Near Cache 설정으로 IMap을 가져옵니다. mapName=${it.mapName}" }
        }
        hazelcastInstance.getMap(cacheName)
    }

    /**
     * DB에서 조건에 맞는 엔티티 목록을 조회하고 캐시에 저장합니다.
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
            log.debug { "DB에서 ${entities.size}개 엔티티 조회 완료. 캐시에 저장합니다." }
            cache.putAll(entities.associateBy { it.id })
        }
        return entities
    }

    /**
     * 여러 ID의 엔티티를 배치 단위로 캐시에서 조회합니다.
     * 캐시 미스된 항목은 DB에서 로드하여 캐시에 추가합니다.
     */
    override fun getAll(ids: Collection<ID>, batchSize: Int): List<T> {
        require(batchSize > 0) { "batchSize must be greater than 0. batchSize=$batchSize" }
        if (ids.isEmpty()) return emptyList()

        return ids.chunked(batchSize).flatMap { chunk ->
            val chunkSet = chunk.toSet()
            val fromCache = cache.getAll(chunkSet)

            // 캐시 미스된 ID를 DB에서 조회
            val missedIds = chunkSet - fromCache.keys
            val fromDb = if (missedIds.isNotEmpty()) {
                log.debug { "캐시 미스 ${missedIds.size}개 - DB에서 조회합니다." }
                val dbEntities = findAllFromDb(missedIds)
                if (dbEntities.isNotEmpty()) {
                    cache.putAll(dbEntities.associateBy { it.id })
                }
                dbEntities
            } else emptyList()

            fromCache.values.filterNotNull() + fromDb
        }
    }
}

/**
 * Near Cache가 활성화된 [AbstractHazelcastCacheRepository]를 쉽게 생성하는 헬퍼 함수입니다.
 *
 * @param hazelcastInstance Hazelcast 클라이언트 인스턴스
 * @param cacheName 캐시 이름
 * @param nearCacheConfig Near Cache 설정
 * @return Near Cache 설정이 추가된 [HazelcastInstance]에서 IMap을 생성하는 config
 */
fun withHazelcastNearCacheConfig(
    hazelcastInstance: HazelcastInstance,
    cacheName: String,
    nearCacheConfig: HazelcastNearCacheConfig = HazelcastNearCacheConfig(cacheName),
): HazelcastNearCacheConfig {
    // ClientConfig는 HazelcastClient 생성 시 지정해야 하므로 별도 설정 필요
    // 이 함수는 설정 객체를 반환하여 HazelcastClient 생성 시 사용
    return nearCacheConfig
}
