package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.collections.toVarargArray
import io.bluetape4k.exposed.core.HasIdentifier
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.redisson.api.RMap

/**
 * ExposedCacheRepository는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type   Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 */
interface ExposedCacheRepository<T: HasIdentifier<ID>, ID: Any> {

    val cacheName: String

    val entityTable: IdTable<ID>
    fun ResultRow.toEntity(): T

    val cache: RMap<ID, T?>

    /**
     * 캐시에 존재하지 않으면 Read Through 로 DB에서 읽어온다. DB에도 없을 때 false 를 반환한다
     */
    fun exists(id: ID): Boolean = cache.containsKey(id)

    fun findFreshById(id: ID): T? = transaction {
        entityTable.selectAll().where { entityTable.id eq id }.singleOrNull()?.toEntity()
    }

    fun findFreshAll(vararg ids: ID): List<T> = transaction {
        entityTable.selectAll().where { entityTable.id inList ids.toList() }.map { it.toEntity() }
    }

    fun findFreshAll(ids: Collection<ID>): List<T> = transaction {
        entityTable.selectAll().where { entityTable.id inList ids }.map { it.toEntity() }
    }


    fun get(id: ID): T? = cache[id]

    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortBy: Expression<*> = entityTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        where: () -> Op<Boolean> = { Op.TRUE },
    ): List<T>

    fun getAll(ids: Collection<ID>, batchSize: Int = 100): List<T>

    fun put(entity: T) = cache.fastPut(entity.id, entity)
    fun putAll(entities: Collection<T>, batchSize: Int = 100) {
        cache.putAll(entities.associateBy { it.id }, batchSize)
    }

    fun invalidate(vararg ids: ID): Long = cache.fastRemove(*ids)
    fun invalidateAll() = cache.clear()
    fun invalidateByPattern(patterns: String, count: Int = 100): Long {
        val keys = cache.keySet(patterns, count)
        return cache.fastRemove(*keys.toVarargArray())
    }
}
