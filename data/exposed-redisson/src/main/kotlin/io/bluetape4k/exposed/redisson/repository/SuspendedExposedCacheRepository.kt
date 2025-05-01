package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.dao.HasIdentifier
import io.bluetape4k.exposed.redisson.map.EntityMapLoader
import io.bluetape4k.exposed.redisson.map.EntityMapWriter
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapWriter
import io.bluetape4k.exposed.redisson.map.SuspendedEntityMapLoader
import io.bluetape4k.exposed.redisson.map.SuspendedEntityMapWriter
import io.bluetape4k.exposed.redisson.map.SuspendedExposedEntityMapLoader
import io.bluetape4k.exposed.redisson.map.SuspendedExposedEntityMapWriter
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
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
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import java.time.Duration

/**
 * RedisCacheRepository는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type   Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 */
interface SuspendedExposedCacheRepository<T: HasIdentifier<ID>, ID: Any> {

    companion object: KLogging() {
        const val DefaultBatchSize = 100
    }

    val cacheName: String

    val entityTable: IdTable<ID>
    fun ResultRow.toEntity(): T

    val cache: RMap<ID, T?>

    suspend fun exists(id: ID): Boolean = cache.containsKeyAsync(id).coAwait()

    suspend fun findFreshById(id: ID): T? =
        entityTable.selectAll().where { entityTable.id eq id }.singleOrNull()?.toEntity()

    suspend fun findFreshAll(vararg ids: ID): List<T> =
        entityTable.selectAll().where { entityTable.id inList ids.toList() }.map { it.toEntity() }

    suspend fun findFreshAll(ids: Collection<ID>): List<T> =
        entityTable.selectAll().where { entityTable.id inList ids }.map { it.toEntity() }

    suspend fun get(id: ID): T? = cache.getAsync(id).coAwait()

    suspend fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = entityTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T>

    suspend fun getAll(ids: Collection<ID>, batchSize: Int = DefaultBatchSize): List<T>

    suspend fun put(entity: T) = cache.fastPutAsync(entity.id, entity).coAwait()
    suspend fun putAll(entities: Collection<T>, batchSize: Int = DefaultBatchSize) {
        cache.putAllAsync(entities.associateBy { it.id }, batchSize).coAwait()
    }

    suspend fun invalidate(vararg ids: ID): Long = cache.fastRemoveAsync(*ids).coAwait()
    suspend fun invalidateAll(): Boolean = cache.clearAsync().coAwait()
    suspend fun invalidateByPattern(patterns: String, count: Int = DefaultBatchSize): Long {
        val keys = cache.keySet(patterns, count)
        return cache.fastRemoveAsync(*keys.toTypedArray()).coAwait()
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

    companion object: KLogging()

    /**
     * DB의 정보를 Read Through로 캐시에 로딩하는 [EntityMapLoader] 입니다.
     */
    protected open val suspendedMapLoader: SuspendedEntityMapLoader<ID, T> by lazy {
        SuspendedExposedEntityMapLoader(entityTable, scope) { toEntity() }
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
    protected val suspendedMapWriter: SuspendedEntityMapWriter<ID, T>? by lazy {
        when (config.cacheMode) {
            RedisCacheConfig.CacheMode.READ_ONLY -> null
            RedisCacheConfig.CacheMode.READ_WRITE -> SuspendedExposedEntityMapWriter(
                scope = scope,
                entityTable = entityTable,
                updateBody = { stmt, entity -> doUpdateEntity(stmt, entity) },
                batchInsertBody = { entity -> doInsertEntity(this, entity) },
                deleteFromDBOnInvalidate = config.deleteFromDBOnInvalidate,  // 캐시 invalidated 시 DB에서도 삭제할 것인지 여부
                writeMode = config.writeMode,  // Write Through 모드
            )
        }
    }

    override val cache: RMap<ID, T?> by lazy {
        log.info { "RMapCache 를 생성합니다. config=$config" }

        when {
            config.isNearCacheEnabled -> {
                log.info { "RLocalCAcheMap 를 생성합니다. config=$config" }

                localCachedMap(cacheName, redissonClient) {
                    if (config.isReadOnly) {
                        loaderAsync(suspendedMapLoader)
                    } else {
                        loaderAsync(suspendedMapLoader)
                        suspendedMapWriter.requireNotNull("mapWriter")
                        writerAsync(suspendedMapWriter)
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
            }
            else -> {
                log.info { "RMapCache 를 생성합니다. config=$config" }

                mapCache(cacheName, redissonClient) {
                    if (config.isReadOnly) {
                        loaderAsync(suspendedMapLoader)
                    } else {
                        loaderAsync(suspendedMapLoader)
                        suspendedMapWriter.requireNotNull("mapWriter")
                        writerAsync(suspendedMapWriter)
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
            }
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

    override suspend fun getAll(ids: Collection<ID>, batchSize: Int): List<T> {
        val chunkedIds = ids.chunked(batchSize)

        return chunkedIds.flatMap { chunk ->
            cache.getAllAsync(chunk.toSet()).coAwait().values.filterNotNull()
        }
    }
}
