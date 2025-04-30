package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.collections.toVarargArray
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapLoader
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapWriter
import io.bluetape4k.exposed.redisson.map.ExposedMapLoader
import io.bluetape4k.exposed.redisson.map.ExposedMapWriter
import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import io.bluetape4k.redis.redisson.cache.localCachedMap
import io.bluetape4k.redis.redisson.cache.mapCache
import io.bluetape4k.support.requireNotNull
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.redisson.api.EvictionMode
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.api.map.WriteMode
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

    fun get(id: ID): T? = cache[id]

    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = entityTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T>

    fun getAllBatch(ids: Collection<ID>, batchSize: Int = 100): List<T>

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
     * DB의 정보를 Read Through로 캐시에 로딩하는 [ExposedMapLoader] 입니다.
     */
    protected open val mapLoader: ExposedMapLoader<ID, T> by lazy {
        ExposedEntityMapLoader(entityTable) { toEntity() }
    }

    /**
     * [ExposedMapWriter] 에서 캐시에서 변경된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doUpdateEntity(statement: UpdateStatement, entity: T) {
        if (config.isReadWrite) {
            error("MapWriter 에서 변경된 cache item을 DB에 반영할 수 있도록 재정의해주세요. ")
        }
    }

    /**
     * [ExposedMapWriter] 에서 캐시에서 추가된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doBatchInsertEntity(statement: BatchInsertStatement, entity: T) {
        if (config.isReadWrite) {
            error("MapWriter 에서 추가된 cache item을 DB에 추가할 수 있도록 재정의해주세요. ")
        }
    }

    override val cache: RMap<ID, T?> by lazy {
        log.info { "RMapCache 를 생성합니다. config=$config" }

        when {
            config.isNearCacheEnabled -> {
                log.info { "RLocalCAcheMap 를 생성합니다. config=$config" }
                localCachedMap(cacheName, redissonClient) {
                    if (config.isReadOnly) {
                        loader(mapLoader)
                    } else if (config.isReadWrite) {
                        loader(mapLoader)
                        mapWriter.requireNotNull("mapWriter")
                        writer(mapWriter)
                        writeMode(WriteMode.WRITE_THROUGH)
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
            }
            else -> {
                log.info { "RMapCache 를 생성합니다. config=$config" }

                mapCache(cacheName, redissonClient) {
                    if (config.isReadOnly) {
                        loader(mapLoader)
                    } else if (config.isReadWrite) {
                        loader(mapLoader)
                        mapWriter.requireNotNull("mapWriter")
                        writer(mapWriter)
                        writeMode(WriteMode.WRITE_THROUGH)
                    }
                    codec(config.codec)
                    writeRetryAttempts(config.writeRetryAttempts)
                    writeRetryInterval(config.writeRetryInterval)
                }.apply {
                    if (config.nearCacheMaxSize > 0) {
                        setMaxSize(config.nearCacheMaxSize, EvictionMode.LRU)
                    }
                }
            }
        }
    }

    /**
     * Write Through 모드라면 [ExposedEntityMapWriter]를 생성하여 제공합니다.
     * Read Through Only 라면 null을 반환합니다.
     */
    protected val mapWriter: ExposedMapWriter<ID, T>? by lazy {
        if (config.isReadWrite)
            ExposedEntityMapWriter(
                entityTable = entityTable,
                updateBody = { stmt, entity -> doUpdateEntity(stmt, entity) },
                batchInsertBody = { entity -> doBatchInsertEntity(this, entity) },
                deleteFromDBOnInvalidate = config.deleteFromDBOnInvalidate,  // 캐시 invalidated 시 DB에서도 삭제할 것인지 여부
            )
        else null
    }

    override fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: SqlExpressionBuilder.() -> Op<Boolean>,
    ): List<T> {
        return if (config.isReadOnly) {
            entityTable
                .selectAll()
                .where(where)
                .apply {
                    orderBy(sortBy, sortOrder)
                    limit?.run { limit(limit) }
                    offset?.run { offset(offset) }
                }.map { it.toEntity() }
                .apply {
                    cache.putAll(associateBy { it.id })
                }
        } else {
            // write-through 모드라면 cache.putAll()을 하면 다시 DB에 Write를 하므로 이런 방식을 써야 한다.
            entityTable
                .select(entityTable.id)
                .where(where)
                .apply {
                    orderBy(sortBy, sortOrder)
                    limit?.run { limit(limit) }
                    offset?.run { offset(offset) }
                }
                .map { it[entityTable.id].value }
                .let {
                    cache.getAll(it.toSet()).values.filterNotNull()
                }
        }
    }

    override fun getAllBatch(ids: Collection<ID>, batchSize: Int): List<T> {
        val chunkedIds = ids.chunked(batchSize)

        return chunkedIds.flatMap { chunk ->
            when {
                config.isReadOnly -> {
                    entityTable.selectAll()
                        .where { entityTable.id inList chunk }
                        .map { it.toEntity() }
                        .apply {
                            cache.putAll(associateBy { it.id })
                        }
                }
                config.isReadWrite -> {
                    // write-through 모드라면 DB에서 ID만 조회한 후 캐시에서 가져와야 한다 
                    entityTable.select(entityTable.id)
                        .where { entityTable.id inList chunk }
                        .map { it[entityTable.id].value }
                        .let {
                            cache.getAll(it.toSet()).values.filterNotNull()
                        }
                }
                else -> emptyList()
            }
        }
    }
}
