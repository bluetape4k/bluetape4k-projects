package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.map.DefaultExposedMapWriter
import io.bluetape4k.exposed.redisson.map.DefaultSuspendedExposedMapLoader
import io.bluetape4k.exposed.redisson.map.DefaultSuspendedExposedMapWriter
import io.bluetape4k.exposed.redisson.map.ExposedMapLoader
import io.bluetape4k.exposed.redisson.map.ExposedMapWriter
import io.bluetape4k.exposed.redisson.map.SuspendedExposedMapLoader
import io.bluetape4k.exposed.redisson.map.SuspendedExposedMapWriter
import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import io.bluetape4k.redis.redisson.cache.localCachedMap
import io.bluetape4k.redis.redisson.cache.mapCache
import io.bluetape4k.support.requireNotNull
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.redisson.api.EvictionMode
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import org.redisson.api.map.WriteMode
import java.time.Duration

/**
 * RedisCacheRepository는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type   Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 */
interface SuspendedRedisCacheRepository<T: HasIdentifier<ID>, ID: Any> {
    val cacheName: String

    suspend fun existsById(id: ID): Boolean

    suspend fun findById(id: ID): T
    suspend fun findByIdOrNull(id: ID): T?


    suspend fun findAll(): List<T>
    suspend fun findAll(sortBy: String, order: SortOrder = SortOrder.ASC): List<T>
    suspend fun findAll(where: Op<Boolean>): List<T>
    suspend fun findAll(where: Op<Boolean>, sortBy: String, order: SortOrder = SortOrder.ASC): List<T>

    suspend fun save(entity: T)

    suspend fun delete(entity: T)
    suspend fun deleteById(id: ID)

}

/**
 * ExposedRedisRepository는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type      Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 *
 * @param redissonClient Redisson Client
 * @param cacheName Redis Cache Name
 * @param config ExposedRedisCacheConfig
 */
abstract class BaseSuspendedExposedCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    private val config: RedisCacheConfig,
): ExposedCacheRepository<T, ID> {

    /**
     * DB의 정보를 Read Through로 캐시에 로딩하는 [ExposedMapLoader] 입니다.
     */
    protected open val mapLoaderAsync: SuspendedExposedMapLoader<ID, T> by lazy {
        DefaultSuspendedExposedMapLoader(entityTable) { toEntity() }
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

    /**
     * Write Through 모드라면 [DefaultExposedMapWriter]를 생성하여 제공합니다.
     * Read Through Only 라면 null을 반환합니다.
     */
    protected val mapWriterAsync: SuspendedExposedMapWriter<ID, T>? by lazy {
        when {
            config.isReadWrite -> DefaultSuspendedExposedMapWriter(
                entityTable = entityTable,
                toEntity = { toEntity() },
                updateBody = { stmt, entity -> doUpdateEntity(stmt, entity) },
                batchInsertBody = { entity -> doBatchInsertEntity(this, entity) },
                deleteFromDBOnInvalidate = config.deleteFromDBOnInvalidate,  // 캐시 invalidated 시 DB에서도 삭제할 것인지 여부
            )
            else -> null
        }
    }

    override fun findFreshById(id: ID): T? =
        entityTable.selectAll().where { entityTable.id eq id }.singleOrNull()?.toEntity()

    override fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: SqlExpressionBuilder.() -> Op<Boolean>,
    ): List<T> {
        return if (config.isReadOnly) {
            entityTable.selectAll().where(where).apply {
                orderBy(sortBy, sortOrder)
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }.map { it.toEntity() }.apply {
                cache.putAll(associateBy { it.id })
            }
        } else {
            entityTable.select(entityTable.id).where(where).apply {
                orderBy(sortBy, sortOrder)
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }.map { it[entityTable.id].value }.let {
                cache.getAll(it.toSet()).values.filterNotNull()
            }
        }
    }

    override fun getAllBatch(ids: Collection<ID>, batchSize: Int): List<T> {
        val chunkedIds = ids.chunked(batchSize)

        return chunkedIds.flatMap { chunk ->
            when {
                config.isReadOnly -> {
                    entityTable.selectAll().where { entityTable.id inList chunk }.map { it.toEntity() }.apply {
                        cache.putAll(associateBy { it.id })
                    }
                }
                config.isReadWrite -> {
                    entityTable.select(entityTable.id).where { entityTable.id inList chunk }
                        .map { it[entityTable.id].value }.let {
                            cache.getAll(it.toSet()).values.filterNotNull()
                        }
                }
                else -> emptyList()
            }
        }
    }
}

/**
 * RedisRemoteCacheRepository는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type      Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 *
 * @param redissonClient Redisson Client
 * @param cacheName Redis Cache Name
 * @param config ExposedRedisCacheConfig
 */
abstract class SuspendedExposedRemoteCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    redissonClient: RedissonClient,
    cacheName: String,
    config: RedisCacheConfig,
): BaseSuspendedExposedCacheRepository<T, ID>(redissonClient, cacheName, config) {

    override val cache: RMapCache<ID, T?> by lazy {
        mapCache(cacheName, redissonClient) {
            if (config.isReadOnly) {
                loaderAsync(mapLoaderAsync)
            }
            if (config.isReadWrite) {
                mapWriterAsync.requireNotNull("mapWriter")
                writerAsync(mapWriterAsync)
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

/**
 * RedisNearCacheRepository는 Exposed와 Redisson을 사용하여 **Near Cache** 방식으로 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type      Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 *
 * @param redissonClient Redisson Client
 * @param cacheName Redis Cache Name
 * @param config ExposedRedisCacheConfig
 */
abstract class SuspendedExposedNearCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    redissonClient: RedissonClient,
    cacheName: String,
    config: RedisCacheConfig,
): BaseSuspendedExposedCacheRepository<T, ID>(redissonClient, cacheName, config) {

    override val cache: RLocalCachedMap<ID, T?> by lazy {
        localCachedMap(cacheName, redissonClient) {
            if (config.isReadOnly) {
                loaderAsync(mapLoaderAsync)
            }
            if (config.isReadWrite) {
                mapWriterAsync.requireNotNull("mapWriter")
                writerAsync(mapWriterAsync)
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
}
