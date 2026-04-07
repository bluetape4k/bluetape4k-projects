package io.bluetape4k.exposed.dao.auditable

import org.jetbrains.exposed.v1.core.dao.id.EntityID

/**
 * `Long` 기본키를 사용하는 감사(Auditing) DAO 엔티티 추상 클래스입니다.
 *
 * [AuditableEntity]를 상속하며 기본키 타입을 `Long`으로 고정합니다.
 * [io.bluetape4k.exposed.core.auditable.AuditableLongIdTable]과 함께 사용합니다.
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
 * @param id `Long` 타입 엔티티 식별자
 */
abstract class AuditableLongEntity(id: EntityID<Long>): AuditableEntity<Long>(id)
