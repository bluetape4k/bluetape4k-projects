package io.bluetape4k.exposed.repository

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.AbstractQuery
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder

interface ExposedCoroutineRepository<T: Entity<ID>, ID: Any> {

    fun toEntity(row: ResultRow): T

    suspend fun count(): Long
    suspend fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long
    suspend fun count(op: Op<Boolean>): Long

    suspend fun isEmpty(): Boolean
    suspend fun exists(query: AbstractQuery<*>): Boolean

    suspend fun findById(id: ID): T
    suspend fun findByIdOrNull(id: ID): T?

    suspend fun findAll(): List<T>
    suspend fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        predicate: SqlExpressionBuilder.() -> Op<Boolean>,
    ): List<T>

    suspend fun delete(entity: T): Int
    suspend fun deleteById(id: ID): Int
    suspend fun deleteAll(limit: Int? = null, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int

    suspend fun deleteIgnore(entity: T): Int
    suspend fun deleteByIdIgnore(id: ID): Int
    suspend fun deleteAllIgnore(limit: Int? = null, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int
}
