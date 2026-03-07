package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.exposed.core.ExposedPage
import io.bluetape4k.exposed.core.dao.id.SoftDeletedIdTable
import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Soft Delete를 지원하는 [R2dbcRepository] 확장 인터페이스입니다.
 *
 * [SoftDeletedIdTable]의 `isDeleted` 컬럼을 이용해 레코드를 물리적으로 삭제하지 않고
 * 논리적으로 삭제/복원하는 연산을 추가로 제공합니다.
 * 부모 인터페이스의 [findAll] 등은 `isDeleted` 필터를 적용하지 않으므로
 * soft delete 필터가 필요한 경우 [findActive] 또는 [findDeleted]를 사용하세요.
 *
 * @param ID 기본키 타입
 * @param T [SoftDeletedIdTable] 구현체
 * @param E 엔티티 타입
 *
 * ## 사용 예
 *
 * ```kotlin
 * object ContactTable : SoftDeletedIdTable<Long>("contacts") {
 *     override val id = long("id").autoIncrement().entityId()
 *     val name = varchar("name", 100)
 *     override val primaryKey = PrimaryKey(id)
 * }
 *
 * data class ContactRecord(
 *     val id: Long = 0L,
 *     val name: String,
 *     val isDeleted: Boolean = false,
 * )
 *
 * val repo = object : LongSoftDeletedR2dbcRepository<ContactTable, ContactRecord> {
 *     override val table = ContactTable
 *     override suspend fun ResultRow.toEntity() = ContactRecord(
 *         id        = this[ContactTable.id].value,
 *         name      = this[ContactTable.name],
 *         isDeleted = this[ContactTable.isDeleted],
 *     )
 * }
 *
 * suspendTransaction {
 *     repo.softDeleteById(1L)                   // 논리 삭제
 *     repo.restoreById(1L)                      // 복원
 *     val actives = repo.findActive().toList()  // isDeleted = false 인 엔티티만 조회
 * }
 * ```
 */
interface SoftDeletedR2dbcRepository<ID: Any, T: SoftDeletedIdTable<ID>, E: Any>: R2dbcRepository<ID, T, E> {

    override val table: T

    /**
     * 해당 id 의 엔티티를 soft delete 합니다.
     * @param id 삭제할 엔티티의 ID
     */
    suspend fun softDeleteById(id: ID) {
        table.update({ table.id eq id }) {
            it[isDeleted] = true
        }
    }

    /**
     * 해당 id 의 엔티티를 soft delete 에서 복원합니다.
     * @param id 복원할 엔티티의 ID
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
     * 활성 상태(`isDeleted = false`)인 엔티티만 조회합니다.
     * @param limit 최대 개수
     * @param offset 시작 위치
     * @param sortOrder 정렬 순서
     * @param predicate 추가 조건
     * @return 활성 엔티티 Flow
     */
    fun findActive(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): Flow<E> =
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
    ): Flow<E> =
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
    ): ExposedPage<E> =
        findPage(pageNumber, pageSize, sortOrder) {
            (table.isDeleted eq false).and(predicate)
        }
}


/**
 * [Int] 기본키를 사용하는 [SoftDeletedR2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<Int> 구현체
 * @param E 엔티티 타입
 */
interface IntSoftDeletedR2dbcRepository<T: SoftDeletedIdTable<Int>, E: Any>: SoftDeletedR2dbcRepository<Int, T, E>

/**
 * [Long] 기본키를 사용하는 [SoftDeletedR2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<Long> 구현체
 * @param E 엔티티 타입
 */
interface LongSoftDeletedR2dbcRepository<T: SoftDeletedIdTable<Long>, E: Any>: SoftDeletedR2dbcRepository<Long, T, E>

/**
 * Kotlin [kotlin.uuid.Uuid] 기본키를 사용하는 [SoftDeletedR2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<Uuid> 구현체
 * @param E 엔티티 타입
 */
@OptIn(ExperimentalUuidApi::class)
interface UuidSoftDeletedR2dbcRepository<T: SoftDeletedIdTable<Uuid>, E: Any>: SoftDeletedR2dbcRepository<Uuid, T, E>

/**
 * [java.util.UUID] 기본키를 사용하는 [SoftDeletedR2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<UUID> 구현체
 * @param E 엔티티 타입
 */
interface UUIDSoftDeletedR2dbcRepository<T: SoftDeletedIdTable<UUID>, E: Any>: SoftDeletedR2dbcRepository<UUID, T, E>

/**
 * [String] 기본키를 사용하는 [SoftDeletedR2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<String> 구현체
 * @param E 엔티티 타입
 */
interface StringSoftDeletedR2dbcRepository<T: SoftDeletedIdTable<String>, E: Any>: SoftDeletedR2dbcRepository<String, T, E>
