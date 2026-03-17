package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.map.EntityMapLoader
import io.bluetape4k.exposed.redisson.map.EntityMapWriter
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapLoader
import io.bluetape4k.exposed.redisson.map.ExposedEntityMapWriter
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import io.bluetape4k.redis.redisson.cache.localCachedMap
import io.bluetape4k.redis.redisson.cache.mapCache
import io.bluetape4k.support.requireNotNull
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.redisson.api.EvictionMode
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import java.time.Duration

/**
 * Exposed JDBC와 Redisson을 결합한 동기 캐시 Repository의 추상 기반 클래스입니다.
 *
 * ## 사용 방법
 * 이 클래스를 상속하고 [table], [ResultRow.toEntity], [doUpdateEntity], [doInsertEntity]를 구현하세요.
 * Read-Only 모드에서는 [doUpdateEntity]/[doInsertEntity] 구현이 불필요합니다.
 *
 * ## 동작/계약
 * - [config]의 `cacheMode`가 [RedissonCacheConfig.CacheMode.READ_ONLY]이면 mapWriter를 생성하지 않습니다.
 * - [config]의 `isNearCacheEnabled`가 true이면 `RLocalCachedMap`, 그렇지 않으면 `RMapCache`를 사용합니다.
 * - [doUpdateEntity]와 [doInsertEntity]는 Write-Through/Write-Behind 모드에서만 호출되며, Read-Only 모드에서 호출 시 오류가 발생합니다.
 *
 * ```kotlin
 * class UserCacheRepository(
 *     redissonClient: RedissonClient,
 *     cacheName: String,
 *     config: RedisCacheConfig,
 * ): AbstractJdbcRedissonRepository<Long, UserTable, UserRecord>(redissonClient, cacheName, config) {
 *     override val table = UserTable
 *     override fun ResultRow.toEntity() = toUserRecord()
 *     override fun doUpdateEntity(statement: UpdateStatement, entity: UserRecord) {
 *         statement[UserTable.email] = entity.email
 *     }
 * }
 * ```
 *
 * @param ID 엔티티 ID 타입
 * @param T 엔티티 테이블 타입 ([IdTable] 하위 타입)
 * @param E 엔티티 타입. Redis 저장 시 직렬화 문제로 인해 반드시 Serializable data class를 사용해야 합니다.
 * @param redissonClient Redisson 클라이언트
 * @param cacheName Redis 캐시 이름
 * @param config 캐시 설정 ([RedissonCacheConfig])
 */
abstract class AbstractJdbcRedissonRepository<ID: Any, E: Any>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    protected val config: RedissonCacheConfig,
): JdbcRedissonRepository<ID, E> {

    companion object: KLogging()

    /**
     * DB의 정보를 Read Through로 캐시에 로딩하는 [EntityMapLoader] 입니다.
     */
    protected open val mapLoader: EntityMapLoader<ID, E> by lazy {
        ExposedEntityMapLoader(table) { toEntity() }
    }

    /**
     * [EntityMapWriter] 에서 캐시에서 변경된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doUpdateEntity(
        statement: UpdateStatement,
        entity: E,
    ) {
        if (config.isReadWrite) {
            error("MapWriter 에서 변경된 cache item을 DB에 반영할 수 있도록 재정의해주세요. ")
        }
    }

    /**
     * [EntityMapWriter] 에서 캐시에서 추가된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: E,
    ) {
        if (config.isReadWrite) {
            error("MapWriter 에서 추가된 cache item을 DB에 추가할 수 있도록 재정의해주세요. ")
        }
    }

    /**
     * Write Through 모드라면 [ExposedEntityMapWriter]를 생성하여 제공합니다.
     * Read Through Only 라면 null을 반환합니다.
     */
    protected val mapWriter: EntityMapWriter<ID, E>? by lazy {
        when (config.cacheMode) {
            RedissonCacheConfig.CacheMode.READ_ONLY -> {
                null
            }
            RedissonCacheConfig.CacheMode.READ_WRITE -> {
                ExposedEntityMapWriter(
                    entityTable = table,
                    updateBody = { stmt, entity -> doUpdateEntity(stmt, entity) },
                    batchInsertBody = { entity -> doInsertEntity(this, entity) },
                    deleteFromDBOnInvalidate = config.deleteFromDBOnInvalidate, // 캐시 invalidated 시 DB에서도 삭제할 것인지 여부
                    writeMode = config.writeMode // Write Through 모드
                )
            }
        }
    }

    override val cache: RMap<ID, E?> by lazy {
        if (config.isNearCacheEnabled) {
            createLocalCacheMap()
        } else {
            createMapCache()
        }
    }

    /**
     * Near Cache(로컬 캐시)가 활성화된 [RLocalCachedMap]을 생성합니다.
     * Read-Only 모드에서는 loader만, Read-Write 모드에서는 loader + writer를 설정합니다.
     */
    protected fun createLocalCacheMap(): RLocalCachedMap<ID, E?> = localCachedMap(cacheName, redissonClient) {
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

    /**
     * 원격 캐시 [RMapCache]를 생성합니다.
     * Read-Only 모드에서는 loader만, Read-Write 모드에서는 loader + writer를 설정합니다.
     * [RedissonCacheConfig.nearCacheMaxSize]가 0보다 크면 LRU 방식으로 최대 크기를 제한합니다.
     */
    protected fun createMapCache(): RMapCache<ID, E?> = mapCache(cacheName, redissonClient) {
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
    ): List<E> {
        val entities =
            transaction {
                table
                    .selectAll()
                    .where(where)
                    .apply {
                        orderBy(sortBy, sortOrder)
                        limit?.run { limit(limit) }
                        offset?.run { offset(offset) }
                    }.map { it.toEntity() }
            }

        if (entities.isNotEmpty()) {
            log.debug { "DB에서 엔티티를 조회했습니다. entities=$entities" }
            cache.putAll(entities.associateBy { extractId(it) })
        }
        return entities
    }

    /**
     * 주어진 ID 목록을 batchSize 단위로 나누어 캐시에서 엔티티를 조회합니다.
     *
     * @param ids 조회할 엔티티의 ID 목록
     * @param batchSize 한 번에 조회할 배치 크기
     * @return 조회된 엔티티 목록
     */
    override fun getAll(
        ids: Collection<ID>,
        batchSize: Int,
    ): List<E> {
        batchSize.requirePositiveNumber("batchSize")

        if (ids.isEmpty()) return emptyList()
        val chunkedIds = ids.chunked(batchSize)

        return chunkedIds.flatMap { chunk ->
            log.debug { "캐시에서 ${chunk.size}개의 엔티티를 가져옵니다. chunk=$chunk" }
            cache.getAll(chunk.toSet()).values.filterNotNull()
        }
    }
}
