package io.bluetape4k.exposed.core.auditable

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/**
 * `Long` 자동 증가 기본키와 감사 컬럼을 포함한 Exposed 테이블 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - `id`는 `long` 타입 자동 증가 컬럼으로 DB에서 값이 생성됩니다.
 * - 기본키는 단일 `id` 컬럼으로 고정됩니다.
 * - 감사 컬럼(`created_by`, `created_at`, `updated_by`, `updated_at`)은
 *   [AuditableIdTable]에서 상속됩니다.
 *
 * ```kotlin
 * object Orders : AuditableLongIdTable("orders") {
 *     val amount = decimal("amount", 10, 2)
 *     val status = enumeration<OrderStatus>("status")
 * }
 * ```
 */
abstract class AuditableLongIdTable(
    name: String = "",
    columnName: String = "id",
): AuditableIdTable<Long>(name) {

    /**
     * 자동 증가 `Long` 기본 키 컬럼입니다.
     */
    final override val id: Column<EntityID<Long>> = long(columnName).autoIncrement().entityId()

    /** 테이블 기본키 정의입니다. */
    override val primaryKey = PrimaryKey(id)
}
