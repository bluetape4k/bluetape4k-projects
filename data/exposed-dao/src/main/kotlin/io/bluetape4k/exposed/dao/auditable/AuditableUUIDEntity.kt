package io.bluetape4k.exposed.dao.auditable

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * `java.util.UUID` 기본키를 사용하는 감사(Auditing) DAO 엔티티 추상 클래스입니다.
 *
 * [AuditableEntity]를 상속하며 기본키 타입을 `UUID`로 고정합니다.
 * [io.bluetape4k.exposed.core.auditable.AuditableUUIDTable]과 함께 사용합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * object DocumentTable : AuditableUUIDTable("documents") {
 *     val title = varchar("title", 255)
 *     val body = text("body")
 * }
 *
 * class Document(id: EntityID<UUID>) : AuditableUUIDEntity(id) {
 *     companion object : AuditableUUIDEntityClass<Document>(DocumentTable)
 *     var title by DocumentTable.title
 *     var body by DocumentTable.body
 *     override var createdBy by DocumentTable.createdBy
 *     override var createdAt by DocumentTable.createdAt
 *     override var updatedBy by DocumentTable.updatedBy
 *     override var updatedAt by DocumentTable.updatedAt
 * }
 * ```
 *
 * @param id `UUID` 타입 엔티티 식별자
 */
abstract class AuditableUUIDEntity(id: EntityID<UUID>) : AuditableEntity<UUID>(id)
