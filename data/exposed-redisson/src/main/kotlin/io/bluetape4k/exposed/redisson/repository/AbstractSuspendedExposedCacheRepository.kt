package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.redisson.map.EntityMapLoader
import io.bluetape4k.exposed.redisson.map.EntityMapWriter
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapWriter
import io.bluetape4k.exposed.redisson.map.SuspendedEntityMapLoader
import io.bluetape4k.exposed.redisson.map.SuspendedEntityMapWriter
import io.bluetape4k.exposed.redisson.map.SuspendedExposedEntityMapLoader
import io.bluetape4k.exposed.redisson.map.SuspendedExposedEntityMapWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import io.bluetape4k.redis.redisson.cache.localCachedMap
import io.bluetape4k.redis.redisson.cache.mapCache
import io.bluetape4k.support.requireNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.redisson.api.EvictionMode
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import java.time.Duration

/**
 * AbstractSuspendedExposedCacheRepository 는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type      Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable type을 사용해 주세요.
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

    companion object: KLoggingChannel()

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
        log.info { "캐시용 RMap을 생성합니다. config=$config" }

        if (config.isNearCacheEnabled) {
            createLocalCacheMap()
        } else {
            createMapCache()
        }
    }

    protected fun createLocalCacheMap(): RLocalCachedMap<ID, T?> =
        localCachedMap(cacheName, redissonClient) {
            log.info { "RLocalCAcheMap 를 생성합니다. config=$config" }

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

    protected fun createMapCache(): RMapCache<ID, T?> =
        mapCache(cacheName, redissonClient) {
            if (config.isReadOnly) {
                loaderAsync(suspendedMapLoader)
            } else {
                loaderAsync(suspendedMapLoader)
                suspendedMapWriter.requireNotNull("suspendedMapWriter")
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

    /**
     * DB에서 조건에 맞는 엔티티 목록을 조회하고, 조회된 엔티티를 캐시에 저장합니다.
     *
     * @param limit 조회할 최대 개수 (nullable)
     * @param offset 조회 시작 위치 (nullable)
     * @param sortBy 정렬 기준 컬럼
     * @param sortOrder 정렬 순서
     * @param where 조회 조건을 반환하는 함수
     * @return 조회된 엔티티 목록
     */
    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<T> {
        @Suppress("DEPRECATION")
        return suspendedTransactionAsync(scope.coroutineContext) {
            entityTable.selectAll()
                .where(where)
                .apply {
                    orderBy(sortBy, sortOrder)
                    limit?.run { limit(limit) }
                    offset?.run { offset(offset) }
                }
                .map { it.toEntity() }
        }.await().apply {
            cache.putAllAsync(associateBy { it.id }).suspendAwait()
        }
    }

    /**
     * 주어진 ID 목록을 배치 단위로 캐시에서 조회합니다.
     *
     * @param ids 조회할 엔티티 ID 목록
     * @param batchSize 한 번에 조회할 배치 크기
     * @return 조회된 엔티티 목록
     */
    override suspend fun getAll(ids: Collection<ID>, batchSize: Int): List<T> {
        return ids.chunked(batchSize).flatMap { chunk ->
            log.debug { "캐시에서 ${chunk.size}개의 엔티티를 가져옵니다. chunk=$chunk" }
            cache.getAllAsync(chunk.toUnifiedSet()).suspendAwait().values.filterNotNull()
        }
    }
}
