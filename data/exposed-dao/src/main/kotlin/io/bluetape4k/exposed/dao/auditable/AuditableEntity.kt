package io.bluetape4k.exposed.dao.auditable

import io.bluetape4k.exposed.core.auditable.Auditable
import io.bluetape4k.exposed.core.auditable.UserContext
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityBatchUpdate
import java.time.Instant

/**
 * 감사(Auditing) 정보를 자동으로 관리하는 Exposed DAO 엔티티 추상 클래스입니다.
 *
 * [Auditable] 인터페이스를 구현하며, [flush] 오버라이드를 통해
 * 생성자(`createdBy`) 및 수정자(`updatedBy`)를 자동 설정합니다.
 *
 * ## 자동 설정 동작
 * | 상황                     | 자동 설정 컬럼      | 비고                                              |
 * |------------------------|-----------------|--------------------------------------------------|
 * | 신규 엔티티 INSERT       | `createdBy`     | `createdAt`은 DB `defaultExpression(CurrentTimestamp)` 위임 |
 * | 기존 엔티티 UPDATE       | `updatedBy`     | `updatedAt`은 `AuditableJdbcRepository.auditedUpdateById()` 위임 |
 *
 * ## 주의 사항
 * - `flush()` 단독 호출 시 `updatedAt`은 자동 설정되지 않습니다.
 *   `updatedAt` 자동 설정은 `AuditableJdbcRepository.auditedUpdateById()` 사용 시에만 보장됩니다.
 * - `writeValues`는 Exposed `Entity`의 public API에 의존합니다.
 *   Exposed 메이저 업그레이드 시 호환성 검증이 필요합니다.
 *
 * ```kotlin
 * object ArticleTable : AuditableLongIdTable("articles") {
 *     val title = varchar("title", 255)
 * }
 *
 * class Article(id: EntityID<Long>) : AuditableLongEntity(id) {
 *     companion object : AuditableLongEntityClass<Article>(ArticleTable)
 *     var title by ArticleTable.title
 *     override var createdBy by ArticleTable.createdBy
 *     override var createdAt by ArticleTable.createdAt
 *     override var updatedBy by ArticleTable.updatedBy
 *     override var updatedAt by ArticleTable.updatedAt
 * }
 * ```
 *
 * @param ID 엔티티 식별자 타입
 * @param id 엔티티 식별자
 */
abstract class AuditableEntity<ID: Any>(id: EntityID<ID>): Entity<ID>(id), Auditable {

    companion object: KLogging()

    abstract override var createdBy: String
    abstract override var createdAt: Instant?
    abstract override var updatedBy: String?
    abstract override var updatedAt: Instant?

    /**
     * 엔티티를 DB에 플러시합니다.
     *
     * - 신규 엔티티(`createdAt == null`): `createdBy`를 [UserContext.getCurrentUser()]로 설정합니다.
     *   `createdAt`은 [io.bluetape4k.exposed.core.auditable.AuditableIdTable]의
     *   `defaultExpression(CurrentTimestamp)`이 DB INSERT 시 자동 설정합니다.
     * - 기존 엔티티 수정(`writeValues.isNotEmpty()`): `updatedBy`를 [UserContext.getCurrentUser()]로 설정합니다.
     *   `updatedAt`은 `AuditableJdbcRepository.auditedUpdateById()` 호출 시에만 설정됩니다.
     *
     * @param batch 배치 업데이트 컨텍스트 (없으면 단건 처리)
     * @return 실제 DB 쓰기가 발생하면 `true`, 변경 없으면 `false`
     */
    override fun flush(batch: EntityBatchUpdate?): Boolean {
        val isNew = createdAt == null
        val user = UserContext.getCurrentUser()

        if (isNew) {
            log.debug { "신규 엔티티 생성: createdBy=$user 설정" }
            createdBy = user
            // createdAt은 AuditableIdTable의 defaultExpression(CurrentTimestamp)이 DB INSERT 시 자동 설정
        } else if (writeValues.isNotEmpty()) {
            log.debug { "엔티티 수정: updatedBy=$user 설정" }
            updatedBy = user
            // updatedAt은 AuditableJdbcRepository.auditedUpdateById()에서 CurrentTimestamp로 설정
        }

        return super.flush(batch)
    }

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
}
