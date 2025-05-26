package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.exposed.dao.HasIdentifier
import io.bluetape4k.exposed.dao.id.SoftDeletedIdTable
import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.r2dbc.update

/**
 * 엔티티에 대해 Soft delete를 지원하는 [ExposedR2dbcRepository] 인터페이스입니다.
 */
interface SoftDeletedR2dbcRepository<T: HasIdentifier<ID>, ID: Any>: ExposedR2dbcRepository<T, ID> {

    override val table: SoftDeletedIdTable<ID>

    /**
     * 해당 id 의 엔티티를 soft delete 합니다.
     */
    suspend fun softDeleteById(id: ID) {
        table.update({ table.id eq id }) {
            it[isDeleted] = true
        }
    }

    /**
     * 해당 id 의 엔티티를 soft delete 에서 복원합니다.
     */
    suspend fun restoreById(id: ID) {
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
    ): Flow<T> =
        findAll(limit, offset, sortOrder) {
            (table.isDeleted eq false).and(predicate)
        }
}
