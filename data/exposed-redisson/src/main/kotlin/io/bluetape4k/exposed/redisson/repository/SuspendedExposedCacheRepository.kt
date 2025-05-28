package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.coroutines.coAwait
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.redisson.api.RMap

/**
 * SuspendedExposedCacheRepository 는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type   Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 */
interface SuspendedExposedCacheRepository<T: HasIdentifier<ID>, ID: Any> {

    companion object: KLoggingChannel() {
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
