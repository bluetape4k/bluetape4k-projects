package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.collections.toVarargArray
import io.bluetape4k.exposed.repository.HasIdentifier
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.selectAll
import org.redisson.api.RMap

internal interface ExposedRedisRepository<T: HasIdentifier<ID>, ID: Any> {

    val table: IdTable<ID>
    fun ResultRow.toEntity(): T

    val cache: RMap<ID, T?>

    fun findFreshById(id: ID): T = findFreshByIdOrNull(id)!!
    fun findFreshByIdOrNull(id: ID): T? =
        table.selectAll().where { table.id eq id }.singleOrNull()?.toEntity()

    fun existsById(id: ID): Boolean = cache.containsKey(id)
    fun findById(id: ID): T = cache[id]!! // ?: findFreshById(id)
    fun findByIdOrNull(id: ID): T? = cache.get(id) // ?: findFreshByIdOrNull(id)

    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T> {
        val ids = table
            .select(table.id)
            .where(predicate)
            .apply {
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }
            .orderBy(table.id, sortOrder)
            .map { it[table.id].value }

        return cache.getAll(ids.toSet()).mapNotNull { it.value }
    }

    fun findFirstOrNull(
        offset: Long?,
        predicate: SqlExpressionBuilder.() -> Op<Boolean>,
    ): T? = table.select(table.id)
        .where(predicate)
        .limit(1)
        .apply {
            offset?.run { offset(offset) }
        }
        .firstOrNull()
        ?.let { cache[it[table.id].value] }

    fun findLastOrNull(
        offset: Long?,
        predicate: SqlExpressionBuilder.() -> Op<Boolean>,
    ): T? =
        table.select(table.id)
            .where(predicate)
            .orderBy(table.id, SortOrder.DESC)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }
            .firstOrNull()
            ?.let { cache[it[table.id].value] }

    fun <V> findByField(field: Column<V>, value: V): List<T> {
        val ids = table.select(table.id)
            .where { field eq value }
            .map { it[table.id].value }

        return cache.getAll(ids.toSet()).mapNotNull { it.value }
    }

    fun delete(entity: T): Int {
        val removedEntity = cache.remove(entity.id)
        return if (removedEntity != null) 1 else 0
    }

    fun deleteIgnore(entity: T): Int {
        return delete(entity)
    }

    fun deleteByIdIgnore(id: ID): Int {
        val removedEntity = cache.remove(id)
        return if (removedEntity != null) 1 else 0
    }

    fun deleteAllIgnore(
        limit: Int?,
        op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>,
    ): Int {
        val ids: List<ID> = table.select(table.id)
            .where { op(table, this) }
            .apply {
                limit?.run { limit(limit) }
            }
            .map { it[table.id].value }

        if (ids.isEmpty()) return 0

        return cache.fastRemove(*ids.toVarargArray()).toInt()
    }

    fun evictAll() {
        cache.clear()
    }

    fun evictExpired(): Boolean {
        return cache.clearExpire()
    }
}
