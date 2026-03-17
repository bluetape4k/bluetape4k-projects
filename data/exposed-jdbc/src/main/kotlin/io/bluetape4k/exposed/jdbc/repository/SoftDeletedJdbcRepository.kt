package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.core.ExposedPage
import io.bluetape4k.exposed.core.dao.id.SoftDeletedIdTable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Soft Delete를 지원하는 [JdbcRepository] 확장 인터페이스입니다.
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
 * val repo = object : SoftDeletedJdbcRepository<Long, ContactTable, ContactRecord> {
 *     override val table = ContactTable
 *     override fun ResultRow.toEntity() = ContactRecord(
 *         id        = this[ContactTable.id].value,
 *         name      = this[ContactTable.name],
 *         isDeleted = this[ContactTable.isDeleted],
 *     )
 * }
 *
 * transaction {
 *     repo.softDeleteById(1L)        // 논리 삭제
 *     repo.restoreById(1L)           // 복원
 *     val actives = repo.findActive() // isDeleted = false 인 엔티티만 조회
 * }
 * ```
 */
interface SoftDeletedJdbcRepository<ID : Comparable<ID>, T : SoftDeletedIdTable<ID>, E : Any> : JdbcRepository<ID, E> {
    override val table: T

    /**
     * 해당 ID의 엔티티를 soft delete 합니다 (`isDeleted = true`로 업데이트).
     *
     * @param id 논리 삭제할 엔티티의 ID
     *
     * ## 사용 예
     *
     * ```kotlin
     * transaction {
     *     repo.softDeleteById(1L)
     *     repo.findActive().none { it.id == 1L } // true
     * }
     * ```
     */
    fun softDeleteById(id: ID) {
        table.update({ table.id eq id }) {
            it[isDeleted] = true
        }
    }

    /**
     * 논리 삭제(`isDeleted = true`)된 엔티티를 복원합니다 (`isDeleted = false`로 업데이트).
     *
     * @param id 복원할 엔티티의 ID
     *
     * ## 사용 예
     *
     * ```kotlin
     * transaction {
     *     repo.softDeleteById(1L)
     *     repo.restoreById(1L)
     *     repo.findActive().any { it.id == 1L } // true
     * }
     * ```
     */
    fun restoreById(id: ID) {
        table.update({ table.id eq id }) {
            it[isDeleted] = false
        }
    }

    /**
     * 활성 상태(`isDeleted = false`)인 엔티티 개수를 반환합니다.
     * @param predicate 추가 조건
     * @return 활성 엔티티 개수
     */
    fun countActive(predicate: () -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where { (table.isDeleted eq false).and(predicate) }.count()

    /**
     * soft delete 된(`isDeleted = true`) 엔티티 개수를 반환합니다.
     * @param predicate 추가 조건
     * @return 삭제된 엔티티 개수
     */
    fun countDeleted(predicate: () -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where { (table.isDeleted eq true).and(predicate) }.count()

    /**
     * 활성 상태(`isDeleted = false`)인 엔티티만 조회합니다.
     *
     * @param limit 최대 개수
     * @param offset 시작 위치
     * @param sortOrder 정렬 순서 (기본값: [SortOrder.ASC])
     * @param predicate 추가 조건 (기본값: `Op.TRUE`)
     * @return 활성 엔티티 목록
     *
     * ## 사용 예
     *
     * ```kotlin
     * transaction {
     *     // isDeleted = false 이고 name이 "Alice"인 엔티티 조회
     *     val actives = repo.findActive { ContactTable.name eq "Alice" }
     * }
     * ```
     */
    fun findActive(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): List<E> =
        findAll(limit, offset, sortOrder) {
            (table.isDeleted eq false).and(predicate)
        }

    /**
     * soft delete 된(`isDeleted = true`) 엔티티만 조회합니다.
     * @param limit 최대 개수
     * @param offset 시작 위치
     * @param sortOrder 정렬 순서
     * @param predicate 추가 조건
     * @return soft delete 된 엔티티 목록
     */
    fun findDeleted(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): List<E> =
        findAll(limit, offset, sortOrder) {
            (table.isDeleted eq true).and(predicate)
        }

    /**
     * 조건에 맞는 엔티티를 일괄 soft delete 합니다.
     * @param predicate 조건
     * @return 수정된 행 수
     */
    fun softDeleteAll(predicate: () -> Op<Boolean> = { Op.TRUE }): Int =
        table.update(where = predicate) {
            it[isDeleted] = true
        }

    /**
     * 조건에 맞는 soft delete 된 엔티티를 일괄 복원합니다.
     * @param predicate 조건
     * @return 수정된 행 수
     */
    fun restoreAll(predicate: () -> Op<Boolean> = { Op.TRUE }): Int =
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
    fun findActivePage(
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
 * [Long] 기본키를 사용하는 [SoftDeletedJdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<Long> 구현체
 * @param E 엔티티 타입
 */
interface LongSoftDeletedJdbcRepository<T : SoftDeletedIdTable<Long>, E : Any> : SoftDeletedJdbcRepository<Long, T, E>

/**
 * [Int] 기본키를 사용하는 [SoftDeletedJdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<Int> 구현체
 * @param E 엔티티 타입
 */
interface IntSoftDeletedJdbcRepository<T : SoftDeletedIdTable<Int>, E : Any> : SoftDeletedJdbcRepository<Int, T, E>

/**
 * Kotlin [kotlin.uuid.Uuid] 기본키를 사용하는 [SoftDeletedJdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<Uuid> 구현체
 * @param E 엔티티 타입
 */
@OptIn(ExperimentalUuidApi::class)
interface UuidSoftDeletedJdbcRepository<T : SoftDeletedIdTable<Uuid>, E : Any> : SoftDeletedJdbcRepository<Uuid, T, E>

/**
 * [java.util.UUID] 기본키를 사용하는 [SoftDeletedJdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<UUID> 구현체
 * @param E 엔티티 타입
 */
interface UUIDSoftDeletedJdbcRepository<T : SoftDeletedIdTable<UUID>, E : Any> : SoftDeletedJdbcRepository<UUID, T, E>

/**
 * [String] 기본키를 사용하는 [SoftDeletedJdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [SoftDeletedIdTable]<String> 구현체
 * @param E 엔티티 타입
 */
interface StringSoftDeletedJdbcRepository<T : SoftDeletedIdTable<String>, E : Any> :
    SoftDeletedJdbcRepository<String, T, E>
