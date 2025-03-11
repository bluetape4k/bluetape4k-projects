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

/**
 * Exposed 를 사용하는 Repository 의 기본 추상 클래스입니다.
 */
abstract class AbstractExposedRepository<T: Entity<ID>, ID: Any>(
    override val table: IdTable<ID>,
): ExposedRepository<T, ID> {

    companion object: KLogging()

    override fun count(): Long = table.selectAll().count()

    override fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long =
        table.selectAll().where(predicate).count()

    override fun count(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    override fun isEmpty(): Boolean =
        table.selectAll().empty()

    override fun exists(query: AbstractQuery<*>): Boolean {
        val exists = org.jetbrains.exposed.sql.exists(query)
        return table.select(exists).first()[exists]
    }

    override fun findById(id: ID): T =
        table.selectAll().where { table.id eq id }.single().toEntity()

    override fun findByIdOrNull(id: ID): T? =
        table.selectAll().where { table.id eq id }.singleOrNull()?.toEntity()

    override fun findAll(limit: Int?, offset: Long?, predicate: SqlExpressionBuilder.() -> Op<Boolean>): List<T> =
        table.selectAll()
            .where(predicate)
            .apply {
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }
            .map { it.toEntity() }

    override fun delete(entity: T): Int =
        table.deleteWhere { table.id eq entity.id }

    override fun deleteById(id: ID): Int =
        table.deleteWhere { table.id eq id }

    override fun deleteAll(limit: Int?, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int =
        table.deleteWhere(limit = limit, op = op)

    override fun deleteIgnore(entity: T): Int =
        table.deleteIgnoreWhere { table.id eq entity.id }

    override fun deleteByIdIgnore(id: ID): Int =
        table.deleteIgnoreWhere { table.id eq id }

    override fun deleteAllIgnore(limit: Int?, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int =
        table.deleteIgnoreWhere(limit = limit, op = op)
}
