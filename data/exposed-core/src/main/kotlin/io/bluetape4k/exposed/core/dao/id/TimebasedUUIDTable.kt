package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.core.timebasedGenerated
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import java.util.*

/**
 * UUIDv7(Time-based UUID)를 기본키로 사용하는 Exposed `IdTable` 구현입니다.
 *
 * ## 동작/계약
 * - `id` 컬럼은 `javaUUID` 타입이며 `timebasedGenerated()`로 client-side 기본값을 생성합니다.
 * - 기본키는 단일 `id` 컬럼으로 고정됩니다.
 *
 * ```kotlin
 * object Orders: TimebasedUUIDTable("orders")
 * // Orders.id.name == "id"
 * ```
 */
open class TimebasedUUIDTable(
    name: String = "",
    columnName: String = "id",
): IdTable<UUID>(name) {

    /**
     * UUID v7 을 Client 에서 생성합니다.
     */
    final override val id: Column<EntityID<UUID>> =
        javaUUID(columnName).timebasedGenerated().entityId()

    /** 테이블 기본키 정의입니다. */
    final override val primaryKey = PrimaryKey(id)
}
