package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.dao.id.SoftDeletedIdTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update

/**
 * 엔티티에 대해 Soft delete를 지원하는 [ExposedRepository] 인터페이스입니다.
 */
interface SoftDeletedRepository<T: Entity<ID>, ID: Any>: ExposedRepository<T, ID> {

    override val table: SoftDeletedIdTable<ID>

    /**
     * 해당 id 의 엔티티를 soft delete 합니다.
     */
    fun softDeleteById(id: ID) {
        table.update({ table.id eq id }) {
            it[isDeleted] = true
        }
    }

    /**
     * 해당 id 의 엔티티를 soft delete 에서 복원합니다.
     */
    fun restoreById(id: ID) {
        table.update({ table.id eq id }) {
            it[isDeleted] = false
        }
    }

    /**
     * 해당 id 의 엔티티의 `isDeleted` 속성이 false 인 엔티티만 조회합니다.
     */
    fun findActive(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T> =
        findAll(limit, offset, sortOrder) {
            (table.isDeleted eq false).and(predicate)
        }

}
