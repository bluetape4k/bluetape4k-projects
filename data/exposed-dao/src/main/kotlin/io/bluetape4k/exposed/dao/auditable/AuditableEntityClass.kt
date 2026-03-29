package io.bluetape4k.exposed.dao.auditable

import io.bluetape4k.exposed.core.auditable.AuditableIntIdTable
import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable
import io.bluetape4k.exposed.core.auditable.AuditableUUIDTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.EntityClass
import java.util.UUID

/**
 * [AuditableIntIdTable] 기반 엔티티를 관리하는 DAO `EntityClass` 추상 클래스입니다.
 *
 * ## 동작/계약
 * - 테이블 PK는 자동 증가 `Int` 타입으로 고정됩니다.
 * - `entityType`, `entityCtor`를 생략하면 Exposed 기본 추론(리플렉션 포함)을 사용합니다.
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
 * @param E 관리할 엔티티 타입 (`AuditableIntEntity` 하위 클래스)
 * @param table 연결할 [AuditableIntIdTable]
 * @param entityType 엔티티 클래스 (생략 시 Exposed 자동 추론)
 * @param entityCtor 엔티티 생성자 (생략 시 Exposed 자동 추론)
 */
abstract class AuditableIntEntityClass<E : AuditableIntEntity>(
    table: AuditableIntIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<Int>) -> E)? = null,
) : EntityClass<Int, E>(table, entityType, entityCtor)

/**
 * [AuditableLongIdTable] 기반 엔티티를 관리하는 DAO `EntityClass` 추상 클래스입니다.
 *
 * ## 동작/계약
 * - 테이블 PK는 자동 증가 `Long` 타입으로 고정됩니다.
 * - `entityType`, `entityCtor`를 생략하면 Exposed 기본 추론(리플렉션 포함)을 사용합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * object OrderTable : AuditableLongIdTable("orders") {
 *     val amount = decimal("amount", 10, 2)
 *     val status = enumeration<OrderStatus>("status")
 * }
 *
 * class Order(id: EntityID<Long>) : AuditableLongEntity(id) {
 *     companion object : AuditableLongEntityClass<Order>(OrderTable)
 *     var amount by OrderTable.amount
 *     var status by OrderTable.status
 *     override var createdBy by OrderTable.createdBy
 *     override var createdAt by OrderTable.createdAt
 *     override var updatedBy by OrderTable.updatedBy
 *     override var updatedAt by OrderTable.updatedAt
 * }
 * ```
 *
 * @param E 관리할 엔티티 타입 (`AuditableLongEntity` 하위 클래스)
 * @param table 연결할 [AuditableLongIdTable]
 * @param entityType 엔티티 클래스 (생략 시 Exposed 자동 추론)
 * @param entityCtor 엔티티 생성자 (생략 시 Exposed 자동 추론)
 */
abstract class AuditableLongEntityClass<E : AuditableLongEntity>(
    table: AuditableLongIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<Long>) -> E)? = null,
) : EntityClass<Long, E>(table, entityType, entityCtor)

/**
 * [AuditableUUIDTable] 기반 엔티티를 관리하는 DAO `EntityClass` 추상 클래스입니다.
 *
 * ## 동작/계약
 * - 테이블 PK는 `java.util.UUID` 타입이며 INSERT 시 `UUID.randomUUID()`로 client-side 자동 생성됩니다.
 * - `entityType`, `entityCtor`를 생략하면 Exposed 기본 추론(리플렉션 포함)을 사용합니다.
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
 * @param E 관리할 엔티티 타입 (`AuditableUUIDEntity` 하위 클래스)
 * @param table 연결할 [AuditableUUIDTable]
 * @param entityType 엔티티 클래스 (생략 시 Exposed 자동 추론)
 * @param entityCtor 엔티티 생성자 (생략 시 Exposed 자동 추론)
 */
abstract class AuditableUUIDEntityClass<E : AuditableUUIDEntity>(
    table: AuditableUUIDTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<UUID>) -> E)? = null,
) : EntityClass<UUID, E>(table, entityType, entityCtor)
