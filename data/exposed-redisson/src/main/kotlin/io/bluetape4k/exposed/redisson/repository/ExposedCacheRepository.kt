package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.collections.toVarargArray
import io.bluetape4k.exposed.dao.HasIdentifier
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
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.redisson.api.EvictionMode
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import java.time.Duration

/**
 * ExposedCacheRepository는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type   Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 */
interface ExposedCacheRepository<T: HasIdentifier<ID>, ID: Any> {

    val cacheName: String

    val entityTable: IdTable<ID>
    fun ResultRow.toEntity(): T

    val cache: RMap<ID, T?>

    /**
     * 캐시에 존재하지 않으면 Read Through 로 DB에서 읽어온다. DB에도 없을 때 false 를 반환한다
     */
    fun exists(id: ID): Boolean = cache.containsKey(id)

    fun findFreshById(id: ID): T? =
        entityTable.selectAll().where { entityTable.id eq id }.singleOrNull()?.toEntity()

    fun findFreshAll(vararg ids: ID): List<T> =
        entityTable.selectAll().where { entityTable.id inList ids.toList() }.map { it.toEntity() }

    fun findFreshAll(ids: Collection<ID>): List<T> =
        entityTable.selectAll().where { entityTable.id inList ids }.map { it.toEntity() }


    fun get(id: ID): T? = cache[id]

    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = entityTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T>

    fun getAll(ids: Collection<ID>, batchSize: Int = 100): List<T>

    fun put(entity: T) = cache.fastPut(entity.id, entity)
    fun putAll(entities: Collection<T>, batchSize: Int = 100) {
        cache.putAll(entities.associateBy { it.id }, batchSize)
    }

    fun invalidate(vararg ids: ID): Long = cache.fastRemove(*ids)
    fun invalidateAll() = cache.clear()
    fun invalidateByPattern(patterns: String, count: Int = 100): Long {
        val keys = cache.keySet(patterns, count)
        return cache.fastRemove(*keys.toVarargArray())
    }
}

/**
 * BaseExposedCacheRepository는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
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

    override fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: SqlExpressionBuilder.() -> Op<Boolean>,
    ): List<T> {
        return entityTable
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

    override fun getAll(ids: Collection<ID>, batchSize: Int): List<T> {
        val chunkedIds = ids.chunked(batchSize)

        return chunkedIds.flatMap { chunk ->
            log.debug { " 캐시에서 ${chunk.size} 개의 엔티티를 가져옵니다. chunk=${chunk}" }
            cache.getAll(chunk.toSet()).values.filterNotNull()
        }
    }
}
