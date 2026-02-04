package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.r2dbc.redisson.map.R2dbcEntityMapLoader
import io.bluetape4k.exposed.r2dbc.redisson.map.R2dbcEntityMapWriter
import io.bluetape4k.exposed.r2dbc.redisson.map.R2dbcExposedEntityMapLoader
import io.bluetape4k.exposed.r2dbc.redisson.map.R2dbcExposedEntityMapWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import io.bluetape4k.redis.redisson.cache.localCachedMap
import io.bluetape4k.redis.redisson.cache.mapCache
import io.bluetape4k.support.requireNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.redisson.api.EvictionMode
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import java.time.Duration

/**
 * AbstractR2dbcCacheRepository 는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type      Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable Record를 사용해 주세요.
 * @param ID Entity ID Type
 *
 * @param redissonClient Redisson Client
 * @param cacheName Redis Cache Name
 * @param config ExposedRedisCacheConfig
 */
abstract class AbstractR2dbcCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    private val config: RedisCacheConfig,
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): R2dbcCacheRepository<T, ID> {

    companion object: KLoggingChannel()

    /**
     * DB의 정보를 Read Through로 캐시에 로딩하는 [R2dbcEntityMapLoader] 입니다.
     */
    protected open val r2dbcEntityMapLoader: R2dbcEntityMapLoader<ID, T> by lazy {
        R2dbcExposedEntityMapLoader(entityTable, scope) { toEntity() }
    }

    /**
     * [R2dbcEntityMapWriter] 에서 캐시에서 변경된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doUpdateEntity(statement: UpdateStatement, entity: T) {
        if (config.isReadWrite) {
            error("MapWriter 에서 변경된 cache item을 DB에 반영할 수 있도록 재정의해주세요. ")
        }
    }

    /**
     * [R2dbcEntityMapWriter] 에서 캐시에서 추가된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doInsertEntity(statement: BatchInsertStatement, entity: T) {
        if (config.isReadWrite) {
            error("MapWriter 에서 추가된 cache item을 DB에 추가할 수 있도록 재정의해주세요. ")
        }
    }

    /**
     * Write Through 모드라면 [R2dbcEntityMapWriter]를 생성하여 제공합니다.
     * Read Through Only 라면 null을 반환합니다.
     */
    protected val r2dbcEntityMapWriter: R2dbcEntityMapWriter<ID, T>? by lazy {
        when (config.cacheMode) {
            RedisCacheConfig.CacheMode.READ_ONLY -> null
            RedisCacheConfig.CacheMode.READ_WRITE -> R2dbcExposedEntityMapWriter(
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
            log.info { "RLocalCacheMap 를 생성합니다. local cacheName=$cacheName, config=$config" }

            if (config.isReadOnly) {
                loaderAsync(r2dbcEntityMapLoader)
            } else {
                loaderAsync(r2dbcEntityMapLoader)
                r2dbcEntityMapWriter.requireNotNull("mapWriter")
                writerAsync(r2dbcEntityMapWriter)
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
            log.info { "RMapCache 를 생성합니다. remote cacheName=$cacheName, config=$config" }

            if (config.isReadOnly) {
                loaderAsync(r2dbcEntityMapLoader)
            } else {
                loaderAsync(r2dbcEntityMapLoader)
                r2dbcEntityMapWriter.requireNotNull("suspendedMapWriter")
                writerAsync(r2dbcEntityMapWriter)
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
     * 지정한 조건에 따라 DB에서 엔티티 목록을 조회하고, 조회된 엔티티를 캐시에 저장합니다.
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
    ): List<T> = suspendTransaction {
        entityTable
            .selectAll()
            .where(where)
            .apply {
                orderBy(sortBy, sortOrder)
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }
            .map { it.toEntity() }
            .onEach {
                cache.fastPutAsync(it.id, it).suspendAwait()
            }
            .toFastList()
    }

    /**
     * 주어진 ID 목록을 배치 단위로 캐시에서 조회합니다.
     *
     * @param ids 조회할 엔티티 ID 목록
     * @param batchSize 한 번에 조회할 배치 크기
     * @return 조회된 엔티티 목록
     */
    override suspend fun getAll(ids: Collection<ID>, batchSize: Int): List<T> {
        return ids
            .chunked(batchSize)
            .flatMap { chunk ->
                log.debug { "캐시에서 ${chunk.size} 개의 엔티티를 가져옵니다. chunk=${chunk}" }
                cache.getAllAsync(chunk.toUnifiedSet()).suspendAwait().values.filterNotNull()
            }
    }
}
