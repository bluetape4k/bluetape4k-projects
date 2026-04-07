package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.bluetape4k.cache.nearcache.LettuceSuspendNearCache
import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.R2dbcCacheRepository
import io.bluetape4k.exposed.r2dbc.lettuce.map.R2dbcExposedEntityMapLoader
import io.bluetape4k.exposed.r2dbc.lettuce.map.R2dbcExposedEntityMapWriter
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.LettuceSuspendedLoadedMap
import io.bluetape4k.redis.lettuce.map.WriteMode
import io.lettuce.core.RedisClient
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.io.Serializable

/**
 * Exposed R2DBC + Lettuce Redis 캐시를 결합한 추상 레포지토리.
 *
 * `runBlocking` 없이 코루틴 네이티브 [LettuceSuspendedLoadedMap]을 사용한다.
 *
 * 서브클래스는 4개 추상 멤버를 구현한다:
 * - [table]: Exposed [IdTable]
 * - [ResultRow.toEntity]: ResultRow → E 변환 (suspend)
 * - [UpdateStatement.updateEntity]: UPDATE 컬럼 매핑
 * - [BatchInsertStatement.insertEntity]: INSERT 컬럼 매핑
 *
 * @param ID PK 타입
 * @param E 엔티티(DTO) 타입. 분산 캐시 저장을 위해 [Serializable] 구현 필수.
 * @param client Lettuce [RedisClient]
 * @param config [LettuceCacheConfig] 설정
 */
abstract class AbstractR2dbcLettuceRepository<ID: Any, E: Serializable>(
    private val client: RedisClient,
    override val config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
): R2dbcLettuceRepository<ID, E> {
    abstract override val table: IdTable<ID>

    /** [ResultRow]를 엔티티 [E]로 변환하는 suspend 함수 */
    abstract override suspend fun ResultRow.toEntity(): E

    /** 기존 엔티티 UPDATE 시 컬럼 매핑 */
    abstract fun UpdateStatement.updateEntity(entity: E)

    /** 신규 엔티티 INSERT 시 컬럼 매핑 */
    abstract fun BatchInsertStatement.insertEntity(entity: E)

    /** 엔티티 ID를 Redis 키 문자열로 직렬화한다 (기본: toString()) */
    open fun serializeKey(id: ID): String = id.toString()

    // -------------------------------------------------------------------------
    // R2dbcCacheRepository 필수 프로퍼티 구현
    // -------------------------------------------------------------------------

    /** Redis 캐시 이름 (키 접두사로 사용) */
    override val cacheName: String
        get() = config.keyPrefix

    /** 캐시 저장 방식 */
    override val cacheMode: CacheMode
        get() = if (config.nearCacheEnabled) CacheMode.NEAR_CACHE else CacheMode.REMOTE

    /** 캐시 쓰기 전략 */
    override val cacheWriteMode: CacheWriteMode
        get() = when (config.writeMode) {
            WriteMode.NONE -> CacheWriteMode.READ_ONLY
            WriteMode.WRITE_THROUGH -> CacheWriteMode.WRITE_THROUGH
            WriteMode.WRITE_BEHIND -> CacheWriteMode.WRITE_BEHIND
        }

    /** [config.nearCacheEnabled]가 true일 때 Caffeine 로컬 캐시(front) */
    protected val nearCache: LettuceSuspendNearCache<E>? by lazy {
        if (config.nearCacheEnabled) {
            LettuceSuspendNearCache(
                redisClient = client,
                config =
                    LettuceNearCacheConfig(
                        cacheName = config.nearCacheName,
                        maxLocalSize = config.nearCacheMaxSize,
                        redisTtl = config.nearCacheTtl
                    )
            )
        } else {
            null
        }
    }

    override val cache: LettuceSuspendedLoadedMap<ID, E> by lazy {
        LettuceSuspendedLoadedMap(
            client = client,
            loader =
                R2dbcExposedEntityMapLoader(
                    table = table,
                    toEntity = { with(this@AbstractR2dbcLettuceRepository) { toEntity() } }
                ),
            writer =
                R2dbcExposedEntityMapWriter(
                    table = table,
                    writeMode = config.writeMode,
                    updateEntity = { stmt, e -> with(this@AbstractR2dbcLettuceRepository) { stmt.updateEntity(e) } },
                    insertEntity = { stmt, e -> with(this@AbstractR2dbcLettuceRepository) { stmt.insertEntity(e) } },
                    retryAttempts = config.writeRetryAttempts,
                    retryInterval = config.writeRetryInterval
                ),
            config = config,
            keySerializer = ::serializeKey
        )
    }

    // -------------------------------------------------------------------------
    // DB 직접 조회 (캐시 우회)
    // -------------------------------------------------------------------------

    override suspend fun findByIdFromDb(id: ID): E? =
        suspendTransaction {
            table
                .selectAll()
                .where { table.id eq id }
                .singleOrNull()
                ?.toEntity()
        }

    override suspend fun findAllFromDb(ids: Collection<ID>): List<E> =
        suspendTransaction {
            if (ids.isEmpty()) return@suspendTransaction emptyList()
            table
                .selectAll()
                .where { table.id inList ids }
                .map { it.toEntity() }
                .toList()
        }

    override suspend fun countFromDb(): Long = suspendTransaction { table.selectAll().count() }

    // -------------------------------------------------------------------------
    // 캐시 기반 조회 (Read-through)
    // -------------------------------------------------------------------------

    override suspend fun containsKey(id: ID): Boolean = get(id) != null

    override suspend fun get(id: ID): E? {
        nearCache?.get(serializeKey(id))?.let { return it }
        val value = cache.get(id) ?: return null
        nearCache?.put(serializeKey(id), value)
        return value
    }

    override suspend fun getAll(ids: Collection<ID>): Map<ID, E> {
        val nc = nearCache ?: return cache.getAll(ids.toSet())
        val result = mutableMapOf<ID, E>()
        val missedIds = mutableListOf<ID>()
        for (id in ids) {
            val local = nc.get(serializeKey(id))
            if (local != null) result[id] = local else missedIds.add(id)
        }
        if (missedIds.isNotEmpty()) {
            cache.getAll(missedIds.toSet()).forEach { (id, value) ->
                result[id] = value
                nc.put(serializeKey(id), value)
            }
        }
        return result
    }

    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<E> {
        val entities =
            suspendTransaction {
                table
                    .selectAll()
                    .where(where)
                    .apply {
                        orderBy(sortBy, sortOrder)
                        limit?.let { limit(it) }
                        offset?.let { offset(it) }
                    }.map { with(this@AbstractR2dbcLettuceRepository) { it.toEntity() } }
                    .toList()
            }
        // 조회 결과를 캐시에 적재
        if (entities.isNotEmpty()) {
            entities.forEach { entity ->
                runCatching { cache.set(extractId(entity), entity) }
            }
        }
        return entities
    }

    /**
     * 엔티티에서 ID를 추출한다.
     * [findAll] (where 조건 버전) 사용 시 서브클래스에서 override 필요.
     */
    override fun extractId(entity: E): ID =
        error(
            "findAll(where) 사용 시 extractId(entity)를 오버라이드하거나 " +
                    "엔티티에서 ID를 추출하는 방법을 제공해야 합니다."
        )

    // -------------------------------------------------------------------------
    // 쓰기 (캐시 + DB)
    // -------------------------------------------------------------------------

    override suspend fun put(
        id: ID,
        entity: E,
    ) {
        cache.set(id, entity)
        nearCache?.put(serializeKey(id), entity)
    }

    override suspend fun putAll(entities: Map<ID, E>, batchSize: Int) {
        entities.forEach { (id, entity) ->
            cache.set(id, entity)
            nearCache?.put(serializeKey(id), entity)
        }
    }

    // -------------------------------------------------------------------------
    // 삭제
    // -------------------------------------------------------------------------

    override suspend fun invalidate(id: ID) {
        cache.delete(id)
        nearCache?.remove(serializeKey(id))
    }

    override suspend fun invalidateAll(ids: Collection<ID>) {
        cache.deleteAll(ids)
        nearCache?.removeAll(ids.map { serializeKey(it) }.toSet())
    }

    override suspend fun invalidateByPattern(patterns: String, count: Int): Long {
        return cache.invalidateByPattern(patterns, count.toLong())
    }

    // -------------------------------------------------------------------------
    // 캐시 관리
    // -------------------------------------------------------------------------

    override suspend fun clear() {
        nearCache?.clearAll()
        cache.clear()
    }

    override fun close() {
        nearCache?.let { runBlocking { it.close() } }
        cache.close()
    }
}
