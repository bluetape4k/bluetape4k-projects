package io.bluetape4k.exposed.repository

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.AbstractQuery
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.TransactionManager

/**
 * Exposed 를 사용하는 Repository 의 기본 인터페이스입니다.
 */
interface ExposedRepository<T: Entity<ID>, ID: Any> {

    val table: IdTable<ID>

    fun currentTransaction(): org.jetbrains.exposed.sql.Transaction =
        TransactionManager.current()

    fun ResultRow.toEntity(): T

    fun count(): Long
    fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long
    fun count(op: Op<Boolean>): Long

    fun isEmpty(): Boolean
    fun exists(query: AbstractQuery<*>): Boolean

    fun findById(id: ID): T
    fun findByIdOrNull(id: ID): T?

    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T>

    fun delete(entity: T): Int
    fun deleteById(id: ID): Int
    fun deleteAll(limit: Int? = null, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean> = { Op.TRUE }): Int

    fun deleteIgnore(entity: T): Int
    fun deleteByIdIgnore(id: ID): Int
    fun deleteAllIgnore(limit: Int? = null, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean> = { Op.TRUE }): Int
}
