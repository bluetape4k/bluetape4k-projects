package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.core.auditable.AuditableIdTable
import io.bluetape4k.exposed.core.auditable.AuditableIntIdTable
import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable
import io.bluetape4k.exposed.core.auditable.AuditableUUIDTable
import io.bluetape4k.exposed.core.auditable.UserContext
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import java.util.UUID

/**
 * 감사(Auditing) 기능이 포함된 JDBC Repository 인터페이스입니다.
 *
 * [auditedUpdateById] 및 [auditedUpdateAll]을 통해 UPDATE 시
 * DB `CURRENT_TIMESTAMP`(UTC)로 [AuditableIdTable.updatedAt]과
 * [UserContext.getCurrentUser]로 [AuditableIdTable.updatedBy]를 자동 설정합니다.
 *
 * 일반 [JdbcRepository.updateById]를 사용하면 감사 필드가 자동 설정되지 않으므로
 * UPDATE 시에는 반드시 [auditedUpdateById] 또는 [auditedUpdateAll]을 사용하세요.
 *
 * @param ID 기본키 타입
 * @param E 엔티티 타입
 * @param T [AuditableIdTable] 구현체
 *
 * ## 사용 예
 *
 * ```kotlin
 * object UserTable : AuditableLongIdTable("users") {
 *     val name = varchar("name", 128)
 *     val email = varchar("email", 256)
 * }
 *
 * data class UserRecord(
 *     val id: Long = 0L,
 *     val name: String,
 *     val email: String,
 * )
 *
 * class UserRepository : LongAuditableJdbcRepository<UserRecord, UserTable> {
 *     override val table = UserTable
 *
 *     override fun extractId(entity: UserRecord) = entity.id
 *
 *     override fun ResultRow.toEntity() = UserRecord(
 *         id    = this[UserTable.id].value,
 *         name  = this[UserTable.name],
 *         email = this[UserTable.email],
 *     )
 * }
 *
 * transaction {
 *     UserContext.withUser("admin") {
 *         repo.auditedUpdateById(1L) {
 *             it[name] = "Alice"
 *         }
 *     }
 * }
 * ```
 */
interface AuditableJdbcRepository<ID : Any, E : Any, T : AuditableIdTable<ID>> : JdbcRepository<ID, E> {

    override val table: T

    /**
     * ID로 엔티티를 업데이트하고 감사 필드를 자동 설정합니다.
     *
     * `updatedAt`은 DB `CURRENT_TIMESTAMP`(UTC)로, `updatedBy`는 [UserContext.getCurrentUser]로 설정됩니다.
     *
     * @param id 업데이트할 엔티티의 ID
     * @param limit 최대 수정 개수
     * @param updateStatement 수정할 컬럼과 값을 지정하는 람다 (감사 필드 제외)
     * @return 수정된 행 수
     *
     * ## 사용 예
     *
     * ```kotlin
     * transaction {
     *     UserContext.withUser("admin") {
     *         repo.auditedUpdateById(42L) {
     *             it[UserTable.name] = "Bob"
     *         }
     *     }
     * }
     * ```
     */
    fun auditedUpdateById(
        id: ID,
        limit: Int? = null,
        updateStatement: T.(UpdateStatement) -> Unit,
    ): Int = table.update(where = { table.id eq id }, limit = limit) {
        it[table.updatedAt] = CurrentTimestamp
        it[table.updatedBy] = UserContext.getCurrentUser()
        updateStatement(table, it)
    }

    /**
     * 조건에 맞는 엔티티들을 업데이트하고 감사 필드를 자동 설정합니다.
     *
     * `updatedAt`은 DB `CURRENT_TIMESTAMP`(UTC)로, `updatedBy`는 [UserContext.getCurrentUser]로 설정됩니다.
     *
     * @param predicate WHERE 절 조건
     * @param limit 최대 수정 개수
     * @param updateStatement 수정할 컬럼과 값을 지정하는 람다 (감사 필드 제외)
     * @return 수정된 행 수
     *
     * ## 사용 예
     *
     * ```kotlin
     * transaction {
     *     UserContext.withUser("batch-job") {
     *         repo.auditedUpdateAll(predicate = { UserTable.email like "%@example.com%" }) {
     *             it[UserTable.name] = "Migrated"
     *         }
     *     }
     * }
     * ```
     */
    fun auditedUpdateAll(
        predicate: () -> Op<Boolean>,
        limit: Int? = null,
        updateStatement: T.(UpdateStatement) -> Unit,
    ): Int = table.update(where = predicate, limit = limit) {
        it[table.updatedAt] = CurrentTimestamp
        it[table.updatedBy] = UserContext.getCurrentUser()
        updateStatement(table, it)
    }
}

/**
 * `Int` 기본키를 사용하는 [AuditableJdbcRepository]의 편의 인터페이스입니다.
 *
 * @param E 엔티티 타입
 * @param T [AuditableIntIdTable] 구현체
 */
interface IntAuditableJdbcRepository<E : Any, T : AuditableIntIdTable> : AuditableJdbcRepository<Int, E, T>

/**
 * `Long` 기본키를 사용하는 [AuditableJdbcRepository]의 편의 인터페이스입니다.
 *
 * @param E 엔티티 타입
 * @param T [AuditableLongIdTable] 구현체
 */
interface LongAuditableJdbcRepository<E : Any, T : AuditableLongIdTable> : AuditableJdbcRepository<Long, E, T>

/**
 * `java.util.UUID` 기본키를 사용하는 [AuditableJdbcRepository]의 편의 인터페이스입니다.
 *
 * @param E 엔티티 타입
 * @param T [AuditableUUIDTable] 구현체
 */
interface UUIDAuditableJdbcRepository<E : Any, T : AuditableUUIDTable> : AuditableJdbcRepository<UUID, E, T>
