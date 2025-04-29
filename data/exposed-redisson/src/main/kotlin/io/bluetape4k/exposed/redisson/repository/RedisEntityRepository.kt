package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.map.DefaultExposedMapLoader
import io.bluetape4k.exposed.redisson.map.ExposedMapLoader
import io.bluetape4k.exposed.redisson.map.ExposedMapWriter
import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.selectAll
import org.redisson.api.RMap
import org.redisson.api.RedissonClient

interface RedisEntityRepository<T: HasIdentifier<ID>, ID: Any> {

    val cacheName: String

    val entityTable: IdTable<ID>
    fun ResultRow.toEntity(): T

    val cache: RMap<ID, T?>

    fun exists(id: ID): Boolean = cache.containsKey(id)

    fun findFreshByIdOrNull(id: ID): T?

    fun getOrNull(id: ID): T? = cache[id]

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
    fun invalidateByPattern(patterns: String, count: Int = 10) {
        val keys = cache.keySet(patterns, count)
        cache.fastRemove(*keys.toTypedArray())
    }
}

abstract class BaseRedisEntityRepository<T: HasIdentifier<ID>, ID: Any>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    private val config: RedisCacheConfig,
): RedisEntityRepository<T, ID> {

    /**
     * DB의 정보를 Read Through로 캐시에 로딩하는 [ExposedMapLoader] 입니다.
     */
    protected open val mapLoader: ExposedMapLoader<ID, T> by lazy {
        DefaultExposedMapLoader(entityTable) { toEntity() }
    }

    /**
     * 캐시의 정보를 Write Through 로 DB에 반영하는 [MapWriter] 입니다.
     */
    protected open val mapWriter: ExposedMapWriter<ID, T>? = null

    override fun findFreshByIdOrNull(id: ID): T? =
        entityTable.selectAll().where { entityTable.id eq id }.singleOrNull()?.toEntity()

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
                }
                .map { it.toEntity() }
                .apply {
                    cache.putAll(associateBy { it.id })
                }
        } else {
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
                    entityTable
                        .selectAll()
                        .where { entityTable.id inList chunk }
                        .map { it.toEntity() }
                        .apply {
                            cache.putAll(associateBy { it.id })
                        }
                }
                config.isReadWrite -> {
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

abstract class RedisRemoteEntityRepository<T: HasIdentifier<ID>, ID: Any>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    private val config: RedisCacheConfig,
): RedisEntityRepository<T, ID> {

    // TODO: cache 구현
}

abstract class RedisNearEntityRepository<T: HasIdentifier<ID>, ID: Any>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    private val config: RedisCacheConfig,
): RedisEntityRepository<T, ID> {

    // TODO: cache 구현

}
