package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.exposed.core.ExposedPage
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.core.dao.id.SoftDeletedIdTable
import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
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
     * 활성 상태(`isDeleted = false`)인 엔티티 개수를 반환합니다.
     * @param predicate 추가 조건
     * @return 활성 엔티티 개수
     */
    suspend fun countActive(predicate: () -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where { (table.isDeleted eq false).and(predicate) }.count()

    /**
     * soft delete 된(`isDeleted = true`) 엔티티 개수를 반환합니다.
     * @param predicate 추가 조건
     * @return 삭제된 엔티티 개수
     */
    suspend fun countDeleted(predicate: () -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where { (table.isDeleted eq true).and(predicate) }.count()

    /**
     * 해당 id 의 엔티티의 `isDeleted` 속성이 false 인 엔티티만 조회합니다.
     */
    fun findActive(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): Flow<T> =
        findAll(limit, offset, sortOrder) {
            (table.isDeleted eq false).and(predicate)
        }

    /**
     * soft delete 된(`isDeleted = true`) 엔티티만 조회합니다.
     * @param limit 최대 개수
     * @param offset 시작 위치
     * @param sortOrder 정렬 순서
     * @param predicate 추가 조건
     * @return soft delete 된 엔티티 Flow
     */
    fun findDeleted(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): Flow<T> =
        findAll(limit, offset, sortOrder) {
            (table.isDeleted eq true).and(predicate)
        }

    /**
     * 조건에 맞는 엔티티를 일괄 soft delete 합니다.
     * @param predicate 조건
     * @return 수정된 행 수
     */
    suspend fun softDeleteAll(predicate: () -> Op<Boolean> = { Op.TRUE }): Int =
        table.update(where = predicate) {
            it[isDeleted] = true
        }

    /**
     * 조건에 맞는 soft delete 된 엔티티를 일괄 복원합니다.
     * @param predicate 조건
     * @return 수정된 행 수
     */
    suspend fun restoreAll(predicate: () -> Op<Boolean> = { Op.TRUE }): Int =
        table.update(where = predicate) {
            it[isDeleted] = false
        }

    /**
     * 활성 상태(`isDeleted = false`)인 엔티티를 페이징하여 조회합니다.
     * @param pageNumber 페이지 번호 (0부터 시작)
     * @param pageSize 페이지 크기
     * @param sortOrder 정렬 순서
     * @param predicate 추가 조건
     * @return 페이징 결과 [ExposedPage]
     */
    suspend fun findActivePage(
        pageNumber: Int,
        pageSize: Int,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): ExposedPage<T> =
        findPage(pageNumber, pageSize, sortOrder) {
            (table.isDeleted eq false).and(predicate)
        }
}
