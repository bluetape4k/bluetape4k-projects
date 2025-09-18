package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.redisson.map.EntityMapLoader
import io.bluetape4k.exposed.redisson.map.EntityMapWriter
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapLoader
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapWriter
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import io.bluetape4k.redis.redisson.cache.localCachedMap
import io.bluetape4k.redis.redisson.cache.mapCache
import io.bluetape4k.support.requireNotNull
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.redisson.api.EvictionMode
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import java.time.Duration

/**
 * AbstractExposedCacheRepository 는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type   Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 */
abstract class AbstractExposedCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    protected val config: RedisCacheConfig,
): ExposedCacheRepository<T, ID> {

    companion object: KLogging()

    /**
     * DB의 정보를 Read Through로 캐시에 로딩하는 [EntityMapLoader] 입니다.
     */
    protected open val mapLoader: EntityMapLoader<ID, T> by lazy {
        ExposedEntityMapLoader(entityTable) { toEntity() }
    }

    /**
     * [EntityMapWriter] 에서 캐시에서 변경된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doUpdateEntity(statement: UpdateStatement, entity: T) {
        if (config.isReadWrite) {
            error("MapWriter 에서 변경된 cache item을 DB에 반영할 수 있도록 재정의해주세요. ")
        }
    }

    /**
     * [EntityMapWriter] 에서 캐시에서 추가된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doInsertEntity(statement: BatchInsertStatement, entity: T) {
        if (config.isReadWrite) {
            error("MapWriter 에서 추가된 cache item을 DB에 추가할 수 있도록 재정의해주세요. ")
        }
    }

    /**
     * Write Through 모드라면 [ExposedEntityMapWriter]를 생성하여 제공합니다.
     * Read Through Only 라면 null을 반환합니다.
     */
    protected val mapWriter: EntityMapWriter<ID, T>? by lazy {
        when (config.cacheMode) {
            RedisCacheConfig.CacheMode.READ_ONLY -> null
            RedisCacheConfig.CacheMode.READ_WRITE ->
                ExposedEntityMapWriter(
                    entityTable = entityTable,
                    updateBody = { stmt, entity -> doUpdateEntity(stmt, entity) },
                    batchInsertBody = { entity -> doInsertEntity(this, entity) },
                    deleteFromDBOnInvalidate = config.deleteFromDBOnInvalidate,  // 캐시 invalidated 시 DB에서도 삭제할 것인지 여부
                    writeMode = config.writeMode,  // Write Through 모드
                )
        }
    }

    override val cache: RMap<ID, T?> by lazy {
        if (config.isNearCacheEnabled) {
            createLocalCacheMap()
        } else {
            createMapCache()
        }
    }

    protected fun createLocalCacheMap() =
        localCachedMap(cacheName, redissonClient) {
            log.info { "RLocalCacheMap 를 생성합니다. config=$config" }
            if (config.isReadOnly) {
                loader(mapLoader)
            } else {
                loader(mapLoader)
                mapWriter.requireNotNull("mapWriter")
                writer(mapWriter)
                writeMode(config.writeMode)
            }

            codec(config.codec)
            syncStrategy(config.nearCacheSyncStrategy)
            writeRetryAttempts(config.writeRetryAttempts)
            writeRetryInterval(config.writeRetryInterval)
            timeToLive(config.ttl)
            if (config.nearCacheMaxIdleTime > Duration.ZERO) {
                maxIdle(config.nearCacheMaxIdleTime)
            }
        }

    protected fun createMapCache() =
        mapCache(cacheName, redissonClient) {
            log.info { "RMapCache 를 생성합니다. config=$config" }
            if (config.isReadOnly) {
                loader(mapLoader)
            } else {
                loader(mapLoader)
                mapWriter.requireNotNull("mapWriter")
                writer(mapWriter)
                writeMode(config.writeMode)
            }
            codec(config.codec)
            writeRetryAttempts(config.writeRetryAttempts)
            writeRetryInterval(config.writeRetryInterval)
        }.apply {
            if (config.nearCacheMaxSize > 0) {
                setMaxSize(config.nearCacheMaxSize, EvictionMode.LRU)
            }
        }

    /**
     * DB에서 조건에 맞는 엔티티 목록을 조회하고, 조회된 엔티티들을 캐시에 저장합니다.
     *
     * @param limit 조회할 최대 개수 (nullable)
     * @param offset 조회 시작 위치 (nullable)
     * @param sortBy 정렬 기준 컬럼
     * @param sortOrder 정렬 순서
     * @param where 조회 조건을 반환하는 함수
     * @return 조회된 엔티티 목록
     */
    override fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<T> {
        return transaction {
            entityTable
                .selectAll()
                .where(where)
                .apply {
                    orderBy(sortBy, sortOrder)
                    limit?.run { limit(limit) }
                    offset?.run { offset(offset) }
                }.map { it.toEntity() }
                .apply {
                    log.debug { "DB에서 엔티티를 조회했습니다. entities=$this" }
                    cache.putAll(associateBy { it.id })
                }
        }
    }

    /**
     * 주어진 ID 목록을 batchSize 단위로 나누어 캐시에서 엔티티를 조회합니다.
     *
     * @param ids 조회할 엔티티의 ID 목록
     * @param batchSize 한 번에 조회할 배치 크기
     * @return 조회된 엔티티 목록
     */
    override fun getAll(ids: Collection<ID>, batchSize: Int): List<T> {
        val chunkedIds = ids.chunked(batchSize)

        return transaction {
            chunkedIds.flatMap { chunk ->
                log.debug { "캐시에서 ${chunk.size}개의 엔티티를 가져옵니다. chunk=$chunk" }
                cache.getAll(chunk.toSet()).values.filterNotNull()
            }
        }
    }
}
