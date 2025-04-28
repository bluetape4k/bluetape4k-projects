package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.ExposedEntityMapLoader
import io.bluetape4k.exposed.redisson.ExposedEntityMapWriter
import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.redis.redisson.cache.CacheInvalidationStrategy
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import io.bluetape4k.redis.redisson.cache.RedisCacheInvalidationStrategy
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.redisson.api.RMap
import org.redisson.api.RedissonClient

abstract class AbstractExposedRedisRepository<T: HasIdentifier<ID>, ID: Any>(
    protected val redissonClient: RedissonClient,
    protected val cacheName: String,
    protected val config: RedisCacheConfig,
) {

    abstract val entityTable: IdTable<ID>
    abstract fun ResultRow.toEntity(): T

    protected open val mapLoader: ExposedEntityMapLoader<ID, T> by lazy { ExposedEntityMapLoader(entityTable) { toEntity() } }
    protected open val mapWriter: ExposedEntityMapWriter<ID, T>? = null

    internal abstract val cache: RMap<ID, T?>

    protected val cacheInvalidator: CacheInvalidationStrategy<ID> by lazy {
        RedisCacheInvalidationStrategy(cache)
    }

    fun findFreshById(id: ID): T = findFreshByIdOrNull(id)!!
    fun findFreshByIdOrNull(id: ID): T? =
        entityTable.selectAll().where { entityTable.id eq id }.singleOrNull()?.toEntity()

    fun existsById(id: ID): Boolean = cache.containsKey(id)
    fun findById(id: ID): T = cache[id]!! // ?: findFreshById(id)
    fun findByIdOrNull(id: ID): T? = cache.get(id) // ?: findFreshByIdOrNull(id)

    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T> {
        val ids = entityTable
            .select(entityTable.id)
            .where(predicate)
            .apply {
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }
            .orderBy(entityTable.id, sortOrder)
            .map { it[entityTable.id].value }

        return cache.getAll(ids.toSet()).values.filterNotNull()
    }

    fun findAllBatch(ids: Collection<ID>, batchSize: Int = 100): List<T> {
        val batches = ids.chunked(batchSize)
        return batches.flatMap { batch ->
            cache.getAll(batch.toSet()).values.filterNotNull()
        }
    }


    fun findFirstOrNull(
        offset: Long?,
        predicate: SqlExpressionBuilder.() -> Op<Boolean>,
    ): T? =
        entityTable.select(entityTable.id)
            .where(predicate)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }
            .firstOrNull()
            ?.let { cache[it[entityTable.id].value] }

    fun findLastOrNull(
        offset: Long?,
        predicate: SqlExpressionBuilder.() -> Op<Boolean>,
    ): T? = transaction {
        entityTable.select(entityTable.id)
            .where(predicate)
            .orderBy(entityTable.id, SortOrder.DESC)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }
            .firstOrNull()
            ?.let { cache[it[entityTable.id].value] }
    }

    fun <V> findByField(field: Column<V>, value: V): List<T> {
        val ids = entityTable.select(entityTable.id)
            .where { field eq value }
            .map { it[entityTable.id].value }

        return cache.getAll(ids.toSet()).mapNotNull { it.value }
    }

    /**
     * 엔티티를 저장합니다.
     * - READ_THROUGH 설정: 캐시에만 저장됩니다.
     * - WRITE_THROUGH 설정: 캐시와 DB 모두에 저장됩니다.
     *
     * @param entity 저장할 엔티티
     * @return 저장 성공 여부
     */
    fun save(entity: T) {
        cache.fastPut(entity.id, entity)
    }

    fun saveAllBatch(entities: Collection<T>, batchSize: Int = 100) {
        cache.putAll(entities.associateBy { it.id }, batchSize)
    }

    /**
     * 엔티티를 삭제할 때 권장되는 패턴:
     * 1. DB에서 먼저 삭제
     * 2. 삭제 성공 시 캐시도 무효화
     */
    fun deleteWithCache(vararg ids: ID): Int {
        // 1. DB에서 삭제
        val deletedCount = entityTable.deleteWhere { entityTable.id inList ids.toSet() }

        // 2. 삭제 성공 시 캐시도 무효화
        if (deletedCount > 0) {
            invalidate(*ids)
        }
        return deletedCount
    }

    /**
     * 캐시에서만 엔티티를 제거합니다. DB에는 영향을 주지 않습니다.
     */
    fun invalidate(vararg ids: ID) {
        cacheInvalidator.invalidate(*ids)
    }

    /**
     * 캐시 전체를 비웁니다. DB에는 영향을 주지 않습니다.
     */
    fun invalidateAll() {
        cacheInvalidator.invalidateAll()
    }

    /**
     * 패턴에 일치하는 캐시 항목을 제거합니다. DB에는 영향을 주지 않습니다.
     */
    fun invalidateByPattern(pattern: String) {
        cacheInvalidator.invalidateByPattern(pattern)
    }

    fun clearExpired(): Boolean {
        return cache.clearExpire()
    }
}
