package io.bluetape4k.exposed.core.auditable

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/**
 * `Int` 자동 증가 기본키와 감사 컬럼을 포함한 Exposed 테이블 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - `id`는 `integer` 타입 자동 증가 컬럼으로 DB에서 값이 생성됩니다.
 * - 기본키는 단일 `id` 컬럼으로 고정됩니다.
 * - 감사 컬럼(`created_by`, `created_at`, `updated_by`, `updated_at`)은
 *   [AuditableIdTable]에서 상속됩니다.
 *
 * ```kotlin
 * object Articles : AuditableIntIdTable("articles") {
 *     val title = varchar("title", 255)
 *     val content = text("content")
 * }
 * ```
 */
abstract class AuditableIntIdTable(
    name: String = "",
    columnName: String = "id",
) : AuditableIdTable<Int>(name) {

    /**
     * 자동 증가 `Int` 기본 키 컬럼입니다.
     */
    final override val id: Column<EntityID<Int>> = integer(columnName).autoIncrement().entityId()

    /** 테이블 기본키 정의입니다. */
    override val primaryKey = PrimaryKey(id)
}
