package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.bluetape4k.cache.nearcache.LettuceSuspendNearCache
import io.bluetape4k.exposed.lettuce.map.SuspendedExposedEntityMapLoader
import io.bluetape4k.exposed.lettuce.map.SuspendedExposedEntityMapWriter
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.LettuceSuspendedLoadedMap
import io.lettuce.core.RedisClient
import kotlinx.coroutines.Dispatchers
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
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync

/**
 * Exposed JDBC + Lettuce Redis 캐시를 결합한 suspend(코루틴) 기반 추상 레포지토리.
 *
 * [LettuceSuspendedLoadedMap]을 사용하여 `runBlocking` 없이 코루틴 네이티브로 동작한다.
 *
 * 서브클래스는 4개 추상 멤버를 구현한다:
 * - [table]: Exposed [IdTable]
 * - [ResultRow.toEntity]: ResultRow → E 변환
 * - [UpdateStatement.updateEntity]: UPDATE 컬럼 매핑
 * - [BatchInsertStatement.insertEntity]: INSERT 컬럼 매핑
 *
 * ```kotlin
 * class ActorSuspendedRepository(client: RedisClient) :
 *     AbstractSuspendedJdbcLettuceRepository<Long, ActorRecord>(client) {
 *     override val table = ActorTable
 *     override fun ResultRow.toEntity() = ActorRecord(
 *         id = this[ActorTable.id].value,
 *         name = this[ActorTable.name],
 *     )
 *     override fun UpdateStatement.updateEntity(entity: ActorRecord) {
 *         this[ActorTable.name] = entity.name
 *     }
 *     override fun BatchInsertStatement.insertEntity(entity: ActorRecord) {
 *         this[ActorTable.name] = entity.name
 *     }
 * }
 * ```
 *
 * @param ID PK 타입
 * @param E 엔티티(DTO) 타입
 * @param client Lettuce [RedisClient]
 * @param config [LettuceCacheConfig] 설정
 */
abstract class AbstractSuspendedJdbcLettuceRepository<ID: Any, E: Any>(
    private val client: RedisClient,
    override val config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
): SuspendedJdbcLettuceRepository<ID, E> {
    abstract override val table: IdTable<ID>

    abstract fun ResultRow.toEntity(): E

    abstract fun UpdateStatement.updateEntity(entity: E)

    abstract fun BatchInsertStatement.insertEntity(entity: E)

    open fun serializeKey(id: ID): String = id.toString()

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

    protected val cache: LettuceSuspendedLoadedMap<ID, E> by lazy {
        LettuceSuspendedLoadedMap(
            client = client,
            loader =
                SuspendedExposedEntityMapLoader(
                    table = table,
                    toEntity = { row -> with(this@AbstractSuspendedJdbcLettuceRepository) { row.toEntity() } }
                ),
            writer =
                SuspendedExposedEntityMapWriter(
                    table = table,
                    writeMode = config.writeMode,
                    updateEntity = { stmt, e ->
                        with(this@AbstractSuspendedJdbcLettuceRepository) { stmt.updateEntity(e) }
                    },
                    insertEntity = { stmt, e ->
                        with(this@AbstractSuspendedJdbcLettuceRepository) { stmt.insertEntity(e) }
                    },
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

    @Suppress("DEPRECATION")
    override suspend fun findByIdFromDb(id: ID): E? =
        suspendedTransactionAsync(Dispatchers.IO) {
            table
                .selectAll()
                .where { table.id eq id }
                .singleOrNull()
                ?.let { with(this@AbstractSuspendedJdbcLettuceRepository) { it.toEntity() } }
        }.await()

    @Suppress("DEPRECATION")
    override suspend fun findAllFromDb(ids: Collection<ID>): List<E> =
        suspendedTransactionAsync(Dispatchers.IO) {
            if (ids.isEmpty()) return@suspendedTransactionAsync emptyList()
            table
                .selectAll()
                .where { table.id inList ids }
                .map { with(this@AbstractSuspendedJdbcLettuceRepository) { it.toEntity() } }
        }.await()

    @Suppress("DEPRECATION")
    override suspend fun countFromDb(): Long =
        suspendedTransactionAsync(Dispatchers.IO) {
            table.selectAll().count()
        }.await()

    // -------------------------------------------------------------------------
    // 캐시 기반 조회 (Read-through)
    // -------------------------------------------------------------------------

    override suspend fun findById(id: ID): E? {
        nearCache?.get(serializeKey(id))?.let { return it }
        val value = cache.get(id) ?: return null
        nearCache?.put(serializeKey(id), value)
        return value
    }

    override suspend fun findAll(ids: Collection<ID>): Map<ID, E> {
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
        @Suppress("DEPRECATION")
        val entities =
            suspendedTransactionAsync(Dispatchers.IO) {
                table
                    .selectAll()
                    .where(where)
                    .apply {
                        orderBy(sortBy, sortOrder)
                        limit?.let { limit(it) }
                        offset?.let { offset(it) }
                    }.map { with(this@AbstractSuspendedJdbcLettuceRepository) { it.toEntity() } }
            }.await()

        // 조회 결과를 캐시에 적재
        entities.forEach { entity ->
            runCatching { cache.set(extractId(entity), entity) }
        }
        return entities
    }

    /**
     * 엔티티에서 ID를 추출한다.
     * 기본 구현은 에러를 던지며, [findAll] with where 사용 시 서브클래스에서 오버라이드해야 한다.
     */
    protected open fun extractId(entity: E): ID {
        error(
            "findAll(where) 사용 시 extractId(entity)를 오버라이드하거나 " +
                    "엔티티에서 ID를 추출하는 방법을 제공해야 합니다."
        )
    }

    // -------------------------------------------------------------------------
    // 쓰기
    // -------------------------------------------------------------------------

    override suspend fun save(
        id: ID,
        entity: E,
    ) {
        cache.set(id, entity)
        nearCache?.put(serializeKey(id), entity)
    }

    override suspend fun saveAll(entities: Map<ID, E>) {
        entities.forEach { (id, entity) ->
            cache.set(id, entity)
            nearCache?.put(serializeKey(id), entity)
        }
    }

    // -------------------------------------------------------------------------
    // 삭제
    // -------------------------------------------------------------------------

    override suspend fun delete(id: ID) {
        cache.delete(id)
        nearCache?.remove(serializeKey(id))
    }

    override suspend fun deleteAll(ids: Collection<ID>) {
        cache.deleteAll(ids)
        nearCache?.removeAll(ids.map { serializeKey(it) }.toSet())
    }

    // -------------------------------------------------------------------------
    // 캐시 관리
    // -------------------------------------------------------------------------

    override suspend fun clearCache() {
        nearCache?.clearAll()
        cache.clear()
    }

    override fun close() {
        nearCache?.let { runBlocking { it.close() } }
        cache.close()
    }
}
