package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.map.ExposedEntityMapWriter
import io.bluetape4k.exposed.redisson.map.ExposedMapLoader
import io.bluetape4k.exposed.redisson.map.ExposedMapWriter
import io.bluetape4k.exposed.redisson.map.SuspendedExposedEntityMapLoader
import io.bluetape4k.exposed.redisson.map.SuspendedExposedEntityMapWriter
import io.bluetape4k.exposed.redisson.map.SuspendedExposedMapLoader
import io.bluetape4k.exposed.redisson.map.SuspendedExposedMapWriter
import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import io.bluetape4k.redis.redisson.cache.localCachedMap
import io.bluetape4k.redis.redisson.cache.mapCache
import io.bluetape4k.redis.redisson.coroutines.coAwait
import io.bluetape4k.support.requireNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.redisson.api.EvictionMode
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
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
interface SuspendedExposedCacheRepository<T: HasIdentifier<ID>, ID: Any> {

    val cacheName: String

    val entityTable: IdTable<ID>
    fun ResultRow.toEntity(): T

    val cache: RMap<ID, T?>

    suspend fun exists(id: ID): Boolean = cache.containsKeyAsync(id).coAwait()

    suspend fun findFreshById(id: ID): T? =
        entityTable.selectAll().where { entityTable.id eq id }.singleOrNull()?.toEntity()

    suspend fun get(id: ID): T? = cache.getAsync(id).coAwait()

    suspend fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = entityTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T>

    suspend fun getAllBatch(ids: Collection<ID>, batchSize: Int = 100): List<T>

    suspend fun put(entity: T) = cache.fastPutAsync(entity.id, entity).coAwait()
    suspend fun putAll(entities: Collection<T>, batchSize: Int = 100) {
        cache.putAllAsync(entities.associateBy { it.id }, batchSize).coAwait()
    }

    suspend fun invalidate(vararg ids: ID): Long = cache.fastRemoveAsync(*ids).coAwait()
    suspend fun invalidateAll() = cache.clearAsync().coAwait()
    suspend fun invalidateByPattern(patterns: String, count: Int = 10) {
        val keys = cache.keySet(patterns, count)
        cache.fastRemoveAsync(*keys.toTypedArray()).coAwait()
    }
}

/**
 * AbstractSuspendedExposedCacheRepository 는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type      Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 *
 * @param redissonClient Redisson Client
 * @param cacheName Redis Cache Name
 * @param config ExposedRedisCacheConfig
 */
abstract class AbstractSuspendedExposedCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    private val config: RedisCacheConfig,
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): SuspendedExposedCacheRepository<T, ID> {

    /**
     * DB의 정보를 Read Through로 캐시에 로딩하는 [ExposedMapLoader] 입니다.
     */
    protected open val mapLoaderAsync: SuspendedExposedMapLoader<ID, T> by lazy {
        SuspendedExposedEntityMapLoader(scope, entityTable) { toEntity() }
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
     * Write Through 모드라면 [ExposedEntityMapWriter]를 생성하여 제공합니다.
     * Read Through Only 라면 null을 반환합니다.
     */
    protected val mapWriterAsync: SuspendedExposedMapWriter<ID, T>? by lazy {
        when {
            config.isReadWrite -> SuspendedExposedEntityMapWriter(
                scope = scope,
                entityTable = entityTable,
                updateBody = { stmt, entity -> doUpdateEntity(stmt, entity) },
                batchInsertBody = { entity -> doBatchInsertEntity(this, entity) },
                deleteFromDBOnInvalidate = config.deleteFromDBOnInvalidate,  // 캐시 invalidated 시 DB에서도 삭제할 것인지 여부
            )
            else -> null
        }
    }

    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: SqlExpressionBuilder.() -> Op<Boolean>,
    ): List<T> {
        return if (config.isReadOnly) {
            suspendedTransactionAsync(scope.coroutineContext) {
                entityTable.selectAll()
                    .where(where)
                    .apply {
                        orderBy(sortBy, sortOrder)
                        limit?.run { limit(limit) }
                        offset?.run { offset(offset) }
                    }
                    .map { it.toEntity() }
            }.await().apply {
                cache.putAllAsync(associateBy { it.id }).coAwait()
            }
        } else {
            suspendedTransactionAsync(scope.coroutineContext) {
                entityTable.select(entityTable.id)
                    .where(where)
                    .apply {
                        orderBy(sortBy, sortOrder)
                        limit?.run { limit(limit) }
                        offset?.run {
                            offset(offset)
                        }
                    }.map { it[entityTable.id].value }
            }.await().let {
                cache.getAllAsync(it.toSet()).coAwait().values.filterNotNull()
            }
        }
    }

    override suspend fun getAllBatch(ids: Collection<ID>, batchSize: Int): List<T> {
        val chunkedIds = ids.chunked(batchSize)

        return chunkedIds.flatMap { chunk ->
            when {
                config.isReadOnly -> {
                    suspendedTransactionAsync(scope.coroutineContext) {
                        entityTable.selectAll()
                            .where { entityTable.id inList chunk }
                            .map { it.toEntity() }
                    }.await().apply {
                        cache.putAllAsync(associateBy { it.id }).coAwait()
                    }
                }
                config.isReadWrite -> {
                    suspendedTransactionAsync(scope.coroutineContext) {
                        entityTable.select(entityTable.id)
                            .where { entityTable.id inList chunk }
                            .map { it[entityTable.id].value }
                    }.await().let {
                        cache.getAllAsync(it.toSet()).coAwait().values.filterNotNull()
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
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): AbstractSuspendedExposedCacheRepository<T, ID>(redissonClient, cacheName, config, scope) {

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
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): AbstractSuspendedExposedCacheRepository<T, ID>(redissonClient, cacheName, config, scope) {

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
