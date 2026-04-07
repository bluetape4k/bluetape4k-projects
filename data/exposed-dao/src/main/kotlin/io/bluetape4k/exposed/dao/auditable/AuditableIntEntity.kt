package io.bluetape4k.exposed.dao.auditable

import org.jetbrains.exposed.v1.core.dao.id.EntityID

/**
 * `Int` 기본키를 사용하는 감사(Auditing) DAO 엔티티 추상 클래스입니다.
 *
 * [AuditableEntity]를 상속하며 기본키 타입을 `Int`로 고정합니다.
 * [io.bluetape4k.exposed.core.auditable.AuditableIntIdTable]과 함께 사용합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * object ArticleTable : AuditableIntIdTable("articles") {
 *     val title = varchar("title", 255)
 *     val content = text("content")
 * }
 *
 * class Article(id: EntityID<Int>) : AuditableIntEntity(id) {
 *     companion object : AuditableIntEntityClass<Article>(ArticleTable)
 *     var title by ArticleTable.title
 *     var content by ArticleTable.content
 *     override var createdBy by ArticleTable.createdBy
 *     override var createdAt by ArticleTable.createdAt
 *     override var updatedBy by ArticleTable.updatedBy
 *     override var updatedAt by ArticleTable.updatedAt
 * }
 * ```
 *
 * @param id `Int` 타입 엔티티 식별자
 */
abstract class AuditableIntEntity(id: EntityID<Int>): AuditableEntity<Int>(id)
