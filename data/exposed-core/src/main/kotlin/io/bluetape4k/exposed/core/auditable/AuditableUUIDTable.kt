package io.bluetape4k.exposed.core.auditable

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.java.javaUUID
import java.util.UUID

/**
 * `java.util.UUID` 기본키와 감사 컬럼을 포함한 Exposed 테이블 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - `id`는 `javaUUID` 타입이며 INSERT 시 `UUID.randomUUID()`로 client-side 기본값이 생성됩니다.
 * - 기본키는 단일 `id` 컬럼으로 고정됩니다.
 * - 감사 컬럼(`created_by`, `created_at`, `updated_by`, `updated_at`)은
 *   [AuditableIdTable]에서 상속됩니다.
 *
 * ```kotlin
 * object Documents : AuditableUUIDTable("documents") {
 *     val title = varchar("title", 255)
 *     val body = text("body")
 * }
 * ```
 */
abstract class AuditableUUIDTable(
    name: String = "",
    columnName: String = "id",
) : AuditableIdTable<UUID>(name) {

    /**
     * `java.util.UUID`를 사용하는 기본 키 컬럼입니다.
     * INSERT 시 `UUID.randomUUID()`로 client-side에서 자동 생성됩니다.
     */
    final override val id: Column<EntityID<UUID>> =
        javaUUID(columnName).clientDefault { UUID.randomUUID() }.entityId()

    /** 테이블 기본키 정의입니다. */
    override val primaryKey = PrimaryKey(id)
}
