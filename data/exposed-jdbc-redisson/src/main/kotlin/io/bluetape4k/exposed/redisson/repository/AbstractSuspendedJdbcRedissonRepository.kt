package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.SuspendedJdbcCacheRepository
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
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import io.bluetape4k.redis.redisson.cache.localCachedMap
import io.bluetape4k.redis.redisson.cache.mapCache
import io.bluetape4k.support.requireNotNull
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.redisson.api.EvictionMode
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import java.io.Serializable
import java.time.Duration

/**
 * Exposed JDBC와 Redisson을 결합한 코루틴 기반 비동기 캐시 Repository의 추상 기반 클래스입니다.
 *
 * ## 사용 방법
 * 이 클래스를 상속하고 [table], [ResultRow.toEntity], [UpdateStatement.updateEntity], [BatchInsertStatement.insertEntity]를 구현하세요.
 * Read-Only 모드에서는 [UpdateStatement.updateEntity]/[BatchInsertStatement.insertEntity] 구현이 불필요합니다.
 *
 * ## 동작/계약
 * - [config]의 `cacheMode`가 [RedissonCacheConfig.CacheMode.READ_ONLY]이면 suspendedMapWriter를 생성하지 않습니다.
 * - [config]의 `isNearCacheEnabled`가 true이면 `RLocalCachedMap`, 그렇지 않으면 `RMapCache`를 사용합니다.
 * - [UpdateStatement.updateEntity]와 [BatchInsertStatement.insertEntity]는 Write-Through/Write-Behind 모드에서만 호출됩니다.
 * - DB 조회 및 캐시 저장은 [scope]의 코루틴 컨텍스트에서 실행됩니다.
 *
 * ```kotlin
 * class SuspendedUserCacheRepository(
 *     redissonClient: RedissonClient,
 *     config: RedisCacheConfig,
 * ): AbstractSuspendedJdbcRedissonRepository<Long, UserRecord>(redissonClient, config) {
 *     override val table = UserTable
 *     override fun ResultRow.toEntity() = toUserRecord()
 *     override fun UpdateStatement.updateEntity(entity: UserRecord) {
 *         this[UserTable.email] = entity.email
 *     }
 * }
 * ```
 *
 * @param ID 엔티티 ID 타입
 * @param E 엔티티 타입. Redis 저장 시 직렬화 문제로 인해 반드시 Serializable data class를 사용해야 합니다.
 * @param redissonClient Redisson 클라이언트
 * @param config 캐시 설정 ([RedissonCacheConfig]). 캐시 이름은 [RedissonCacheConfig.name]으로 지정합니다.
 * @param scope DB 조회 및 캐시 비동기 처리에 사용할 [CoroutineScope]. 기본값은 `Dispatchers.IO` 기반 스코프입니다.
 */
abstract class AbstractSuspendedJdbcRedissonRepository<ID: Any, E: Serializable>(
    val redissonClient: RedissonClient,
    private val config: RedissonCacheConfig,
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : SuspendedJdbcRedissonRepository<ID, E> {
    companion object : KLoggingChannel() {
        const val DEFAULT_BATCH_SIZE = SuspendedJdbcCacheRepository.DEFAULT_BATCH_SIZE
    }

    override val cacheName: String get() = config.name

    override val cacheMode: CacheMode
        get() = if (config.isNearCacheEnabled) CacheMode.NEAR_CACHE else CacheMode.REMOTE

    override val cacheWriteMode: CacheWriteMode
        get() = when {
            config.isReadOnly -> CacheWriteMode.READ_ONLY
            config.writeMode == org.redisson.api.map.WriteMode.WRITE_BEHIND -> CacheWriteMode.WRITE_BEHIND
            else -> CacheWriteMode.WRITE_THROUGH
        }

    /**
     * DB의 정보를 Read Through로 캐시에 로딩하는 [EntityMapLoader] 입니다.
     */
    protected open val suspendedMapLoader: SuspendedEntityMapLoader<ID, E> by lazy {
        SuspendedExposedEntityMapLoader(table, scope) { toEntity() }
    }

    /**
     * [EntityMapWriter] 에서 캐시에서 변경된 내용을 Write Through로 DB에 반영하는 extension 함수입니다.
     * Write-Read 모드에서는 반드시 재정의해야 합니다.
     */
    abstract fun UpdateStatement.updateEntity(entity: E)

    /**
     * [EntityMapWriter] 에서 캐시에서 추가된 내용을 Write Through로 DB에 반영하는 extension 함수입니다.
     * Write-Read 모드에서는 반드시 재정의해야 합니다.
     */
    abstract fun BatchInsertStatement.insertEntity(entity: E)

    /**
     * Write Through 모드라면 [ExposedEntityMapWriter]를 생성하여 제공합니다.
     * Read Through Only 라면 null을 반환합니다.
     */
    protected val suspendedMapWriter: SuspendedEntityMapWriter<ID, E>? by lazy {
        when (config.cacheMode) {
            RedissonCacheConfig.CacheMode.READ_ONLY -> {
                null
            }
            RedissonCacheConfig.CacheMode.READ_WRITE -> {
                SuspendedExposedEntityMapWriter(
                    scope = scope,
                    entityTable = table,
                    updateBody = { stmt, entity -> with(this@AbstractSuspendedJdbcRedissonRepository) { stmt.updateEntity(entity) } },
                    batchInsertBody = { entity -> val stmt = this; with(this@AbstractSuspendedJdbcRedissonRepository) { stmt.insertEntity(entity) } },
                    deleteFromDBOnInvalidate = config.deleteFromDBOnInvalidate, // 캐시 invalidated 시 DB에서도 삭제할 것인지 여부
                    writeMode = config.writeMode // Write Through 모드
                )
            }
        }
    }

    override val cache: RMap<ID, E?> by lazy {
        log.info { "캐시용 RMap을 생성합니다. config=$config" }

        if (config.isNearCacheEnabled) {
            createLocalCacheMap()
        } else {
            createMapCache()
        }
    }

    /**
     * Near Cache(로컬 캐시)가 활성화된 [RLocalCachedMap]을 생성합니다.
     * Read-Only 모드에서는 loaderAsync만, Read-Write 모드에서는 loaderAsync + writerAsync를 설정합니다.
     */
    protected fun createLocalCacheMap(): RLocalCachedMap<ID, E?> =
        localCachedMap(cacheName, redissonClient) {
            log.info { "RLocalCacheMap 를 생성합니다. config=$config" }

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

    /**
     * 원격 캐시 [RMapCache]를 생성합니다.
     * Read-Only 모드에서는 loaderAsync만, Read-Write 모드에서는 loaderAsync + writerAsync를 설정합니다.
     * [RedissonCacheConfig.nearCacheMaxSize]가 0보다 크면 LRU 방식으로 최대 크기를 제한합니다.
     */
    protected fun createMapCache(): RMapCache<ID, E?> =
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
     * DB에서 직접 단건 엔티티를 조회합니다 (캐시 우회).
     *
     * @param id 엔티티 식별자
     * @return 엔티티 또는 null
     */
    @Suppress("DEPRECATION")
    override suspend fun findByIdFromDb(id: ID): E? =
        suspendedTransactionAsync(Dispatchers.IO) {
            table
                .selectAll()
                .where { table.id eq id }
                .singleOrNull()
                ?.toEntity()
        }.await()

    /**
     * DB에서 직접 여러 엔티티를 조회합니다 (캐시 우회).
     *
     * @param ids 엔티티 식별자 컬렉션
     * @return 엔티티 리스트
     */
    @Suppress("DEPRECATION")
    override suspend fun findAllFromDb(ids: Collection<ID>): List<E> =
        suspendedTransactionAsync(Dispatchers.IO) {
            table
                .selectAll()
                .where { table.id inList ids }
                .map { it.toEntity() }
        }.await()

    /**
     * DB에서 전체 레코드 수를 조회합니다 (캐시 우회).
     *
     * @return 전체 레코드 수
     */
    @Suppress("DEPRECATION")
    override suspend fun countFromDb(): Long =
        suspendedTransactionAsync(Dispatchers.IO) {
            table.selectAll().count()
        }.await()

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
    ): List<E> {
        @Suppress("DEPRECATION")
        return suspendedTransactionAsync(scope.coroutineContext) {
            table
                .selectAll()
                .where(where)
                .apply {
                    orderBy(sortBy, sortOrder)
                    limit?.run { limit(limit) }
                    offset?.run { offset(offset) }
                }.map { it.toEntity() }
        }.await().also { entities ->
            if (entities.isNotEmpty()) {
                cache.putAllAsync(entities.associateBy { extractId(it) }).await()
            }
        }
    }

    /**
     * 주어진 ID 목록을 DEFAULT_BATCH_SIZE 단위로 나누어 캐시에서 엔티티를 조회합니다.
     *
     * @param ids 조회할 엔티티 ID 목록
     * @return ID를 키, 엔티티를 값으로 하는 맵
     */
    override suspend fun getAll(
        ids: Collection<ID>,
    ): Map<ID, E> = getAll(ids, DEFAULT_BATCH_SIZE)

    /**
     * 주어진 ID 목록을 batchSize 단위로 나누어 캐시에서 엔티티를 조회합니다.
     *
     * @param ids 조회할 엔티티 ID 목록
     * @param batchSize 한 번에 조회할 배치 크기
     * @return ID를 키, 엔티티를 값으로 하는 맵
     */
    suspend fun getAll(
        ids: Collection<ID>,
        batchSize: Int,
    ): Map<ID, E> {
        batchSize.requirePositiveNumber("batchSize")
        if (ids.isEmpty()) {
            return emptyMap()
        }
        return ids.chunked(batchSize).flatMap { chunk ->
            log.debug { "캐시에서 ${chunk.size}개의 엔티티를 가져옵니다. chunk=$chunk" }
            cache
                .getAllAsync(chunk.toSet())
                .await()
                .entries
                .filter { it.value != null }
                .map { it.key to it.value!! }
        }.toMap()
    }
}
