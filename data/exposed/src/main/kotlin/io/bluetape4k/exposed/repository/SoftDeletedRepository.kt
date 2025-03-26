package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.dao.id.SoftDeletedIdTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update

interface SoftDeletedRepository<T: Entity<ID>, ID: Any>: ExposedRepository<T, ID> {

    override val table: SoftDeletedIdTable<ID>

    fun softDeleteById(id: ID) {
        table.update({ table.id eq id }) {
            it[isDeleted] = true
        }
    }

    fun restoreById(id: ID) {
        table.update({ table.id eq id }) {
            it[isDeleted] = false
        }
    }

    fun findActive(
        limit: Int?,
        offset: Long?,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T> =
        findAll(limit, offset, sortOrder) {
            (table.isDeleted eq false).and(predicate)
        }

}
