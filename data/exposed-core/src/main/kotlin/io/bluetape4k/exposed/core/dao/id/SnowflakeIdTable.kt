package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.core.snowflakeGenerated
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable


/**
 * Snowflake Long 값을 기본키로 사용하는 Exposed `IdTable` 구현입니다.
 *
 * ## 동작/계약
 * - `id`는 `long` 컬럼이며 `snowflakeGenerated()`로 client-side 기본값을 생성합니다.
 * - 기본키는 단일 `id` 컬럼으로 고정됩니다.
 *
 * ```kotlin
 * object Events: SnowflakeIdTable("events")
 * // Events.id.name == "id"
 * ```
 */
open class SnowflakeIdTable(
    name: String = "",
    columnName: String = "id",
): IdTable<Long>(name) {

    final override val id: Column<EntityID<Long>> =
        long(columnName).snowflakeGenerated().entityId()

    /** 테이블 기본키 정의입니다. */
    final override val primaryKey = PrimaryKey(id)
}
