package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.core.HasIdentifier
import org.jetbrains.exposed.v1.core.AbstractQuery
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ISqlExpressionBuilder
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteIgnoreWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Exposed 를 사용하는 Repository 의 기본 인터페이스입니다.
 *
 * ```
 * class MyRepository: ExposedRepository<MyEntity, Long> {
 *    override val table = MyTable
 *    ...
 * }
 * ```
 */
interface ExposedRepository<T: HasIdentifier<ID>, ID: Any> {

    val table: IdTable<ID>

    fun currentTransaction(): Transaction =
        TransactionManager.current()

    fun currentTransactionOrNull(): Transaction? =
        TransactionManager.currentOrNull()

    fun ResultRow.toEntity(): T

    fun count(): Long = table.selectAll().count()

    @Deprecated("Use countBy() instead", replaceWith = ReplaceWith("countBy(predicate)"))
    fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where(predicate).count()

    @Deprecated("Use countBy() instead", replaceWith = ReplaceWith("countBy(op)"))
    fun count(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    fun countBy(predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where(predicate).count()

    fun countBy(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    fun isEmpty(): Boolean =
        table.selectAll().empty()

    fun exists(query: AbstractQuery<*>): Boolean {
        val exists = org.jetbrains.exposed.v1.core.exists(query)
        return table.select(exists).firstOrNull()?.getOrNull(exists) ?: false
    }

    fun existsById(id: ID): Boolean =
        !table.selectAll().where { table.id eq id }.empty()

    fun findById(id: ID): T =
        table.selectAll().where { table.id eq id }.single().toEntity()

    fun findByIdOrNull(id: ID): T? =
        table.selectAll().where { table.id eq id }.singleOrNull()?.toEntity()

    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T> =
        table.selectAll()
            .where(predicate)
            .apply {
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }
            .orderBy(table.id, sortOrder)
            .map { it.toEntity() }

    fun findWithFilters(
        vararg filters: SqlExpressionBuilder.() -> Op<Boolean>,
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
    ): List<T> {
        val condition: Op<Boolean> = filters.fold(Op.TRUE as Op<Boolean>) { acc, filter ->
            acc.and(filter.invoke(SqlExpressionBuilder))
        }
        return findAll(limit, offset, sortOrder) { condition }
    }

    fun findFirstOrNull(
        offset: Long? = null,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): T? =
        table.selectAll()
            .where(predicate)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }
            .firstOrNull()?.toEntity()

    fun findLastOrNull(
        offset: Long? = null,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): T? =
        table.selectAll()
            .where(predicate)
            .orderBy(table.id, SortOrder.DESC)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }
            .lastOrNull()?.toEntity()

    fun <V> findByField(field: Column<V>, value: V): List<T> = table.selectAll()
        .where { field eq value }
        .map { it.toEntity() }

    fun delete(entity: T): Int =
        table.deleteWhere { table.id eq entity.id }

    fun deleteById(id: ID): Int =
        table.deleteWhere { table.id eq id }

    fun deleteAll(limit: Int? = null, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean> = { Op.TRUE }): Int =
        table.deleteWhere(limit = limit, op = op)


    fun deleteIgnore(entity: T): Int = table.deleteIgnoreWhere { table.id eq entity.id }

    fun deleteByIdIgnore(id: ID): Int = table.deleteIgnoreWhere { table.id eq id }

    fun deleteAllIgnore(
        limit: Int? = null,
        op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean> = { Op.TRUE },
    ): Int = table.deleteIgnoreWhere(limit, op = op)


    fun updateById(id: ID, limit: Int? = null, updateStatement: IdTable<ID>.(UpdateStatement) -> Unit): Int =
        table.update(where = { table.id eq id }, limit = limit, body = updateStatement)

    fun <E> batchInsert(
        entities: Iterable<E>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(E) -> Unit,
    ): List<T> = table
        .batchInsert(entities, ignore, shouldReturnGeneratedValues, insertStatement)
        .map { it.toEntity() }

    fun <E> batchInsert(
        entities: Sequence<E>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(E) -> Unit,
    ): List<T> = table
        .batchInsert(entities, ignore, shouldReturnGeneratedValues, insertStatement)
        .map { it.toEntity() }

    fun batchUpdate(
        entities: Iterable<T>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        updateStatement: BatchInsertStatement.(T) -> Unit,
    ): List<T> = table
        .batchInsert(entities, ignore, shouldReturnGeneratedValues, updateStatement)
        .map { it.toEntity() }

    fun batchUpdate(
        entities: Sequence<T>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        updateStatement: BatchInsertStatement.(T) -> Unit,
    ): List<T> = table
        .batchInsert(entities, ignore, shouldReturnGeneratedValues, updateStatement)
        .map { it.toEntity() }
}
