package io.bluetape4k.exposed.repository

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.AbstractQuery
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteIgnoreWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

abstract class AbstractExposedCoroutineRepository<T: Entity<ID>, ID: Any>(
    val table: IdTable<ID>,
): ExposedCoroutineRepository<T, ID> {

    companion object: KLogging()

    override suspend fun count(): Long = table.selectAll().count()

    override suspend fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long =
        table.selectAll().where(predicate).count()

    override suspend fun count(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    override suspend fun isEmpty(): Boolean =
        table.selectAll().empty()

    override suspend fun exists(query: AbstractQuery<*>): Boolean {
        val exists = org.jetbrains.exposed.sql.exists(query)
        return table.select(exists).first()[exists]
    }

    override suspend fun findById(id: ID): T =
        table.selectAll()
            .where { table.id eq id }
            .single().let { toEntity(it) }

    override suspend fun findByIdOrNull(id: ID): T? =
        table.selectAll()
            .where { table.id eq id }
            .singleOrNull()?.let { toEntity(it) }

    override suspend fun findAll(): List<T> =
        table.selectAll().map { toEntity(it) }

    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        predicate: SqlExpressionBuilder.() -> Op<Boolean>,
    ): List<T> =
        table.selectAll()
            .where(predicate)
            .apply {
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }
            .map { toEntity(it) }

    override suspend fun delete(entity: T): Int =
        table.deleteWhere { table.id eq entity.id }

    override suspend fun deleteById(id: ID): Int =
        table.deleteWhere { table.id eq id }

    override suspend fun deleteAll(limit: Int?, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int =
        table.deleteWhere(limit = limit, op = op)

    override suspend fun deleteIgnore(entity: T): Int =
        table.deleteIgnoreWhere { table.id eq entity.id }

    override suspend fun deleteByIdIgnore(id: ID): Int =
        table.deleteIgnoreWhere { table.id eq id }

    override suspend fun deleteAllIgnore(limit: Int?, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int =
        table.deleteIgnoreWhere(limit = limit, op = op)
}
