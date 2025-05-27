package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.exposed.core.HasIdentifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.AbstractQuery
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ISqlExpressionBuilder
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.deleteIgnoreWhere
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.update

/**
 * Exposed R2dbc를 사용하는 Repository 의 기본 인터페이스입니다.
 *
 * ```
 * class MyRepository: ExposedR2dbcRepository<ActorDTO, Long> {
 *    override val table = ActorTable
 *    ...
 * }
 * ```
 */
interface ExposedR2dbcRepository<T: HasIdentifier<ID>, ID: Any> {

    val table: IdTable<ID>

    fun currentTransaction(): R2dbcTransaction =
        TransactionManager.current()

    fun currentTransactionOrNull(): R2dbcTransaction? =
        TransactionManager.currentOrNull()

    suspend fun ResultRow.toEntity(): T

    suspend fun count(): Long = table.selectAll().count()

    suspend fun countBy(predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where(predicate).count()

    suspend fun countBy(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    suspend fun isEmpty(): Boolean =
        table.selectAll().empty()

    suspend fun exists(query: AbstractQuery<*>): Boolean {
        val exists = org.jetbrains.exposed.v1.core.exists(query)
        return table.select(exists).firstOrNull()?.getOrNull(exists) ?: false
    }

    suspend fun existsById(id: ID): Boolean =
        !table.selectAll().where { table.id eq id }.empty()

    suspend fun findById(id: ID): T =
        table.selectAll().where { table.id eq id }.single().toEntity()

    suspend fun findByIdOrNull(id: ID): T? =
        table.selectAll().where { table.id eq id }.singleOrNull()?.toEntity()

    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): Flow<T> =
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
    ): Flow<T> {
        val condition: Op<Boolean> = filters.fold(Op.TRUE as Op<Boolean>) { acc, filter ->
            acc.and(filter.invoke(SqlExpressionBuilder))
        }
        return findAll(limit, offset, sortOrder) { condition }
    }

    suspend fun findFirstOrNull(
        offset: Long? = null,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): T? =
        table.selectAll()
            .where(predicate)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }
            .firstOrNull()
            ?.toEntity()

    suspend fun findLastOrNull(
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
            .lastOrNull()
            ?.toEntity()

    fun <V> findByField(field: Column<V>, value: V): Flow<T> = table.selectAll()
        .where { field eq value }
        .map { it.toEntity() }

    suspend fun delete(entity: T): Int =
        table.deleteWhere { table.id eq entity.id }

    suspend fun deleteById(id: ID): Int =
        table.deleteWhere { table.id eq id }

    suspend fun deleteAll(
        limit: Int? = null,
        op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean> = { Op.TRUE },
    ): Int =
        table.deleteWhere(limit = limit, op = op)

    suspend fun deleteIgnore(entity: T): Int = table.deleteIgnoreWhere { table.id eq entity.id }

    suspend fun deleteByIdIgnore(id: ID): Int = table.deleteIgnoreWhere { table.id eq id }

    suspend fun deleteAllIgnore(
        limit: Int? = null,
        op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean> = { Op.TRUE },
    ): Int =
        table.deleteIgnoreWhere(limit, op = op)


    suspend fun updateById(
        id: ID,
        limit: Int? = null,
        updateStatement: IdTable<ID>.(UpdateStatement) -> Unit,
    ): Int =
        table.update(where = { table.id eq id }, limit = limit, body = updateStatement)

    suspend fun <E> batchInsert(
        entities: Iterable<E>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(E) -> Unit,
    ): List<T> =
        table
            .batchInsert(entities, ignore, shouldReturnGeneratedValues, insertStatement)
            .map { it.toEntity() }

    suspend fun <E> batchInsert(
        entities: Sequence<E>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(E) -> Unit,
    ): List<T> =
        table
            .batchInsert(entities, ignore, shouldReturnGeneratedValues, insertStatement)
            .map { it.toEntity() }

    suspend fun batchUpdate(
        entities: Iterable<T>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        updateStatement: BatchInsertStatement.(T) -> Unit,
    ): List<T> =
        table
            .batchInsert(entities, ignore, shouldReturnGeneratedValues, updateStatement)
            .map { it.toEntity() }

    suspend fun batchUpdate(
        entities: Sequence<T>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        updateStatement: BatchInsertStatement.(T) -> Unit,
    ): List<T> =
        table
            .batchInsert(entities, ignore, shouldReturnGeneratedValues, updateStatement)
            .map { it.toEntity() }
}
