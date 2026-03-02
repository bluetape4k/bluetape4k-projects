package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.*


/** UUIDv7 기반 `EntityID` 별칭입니다. */
typealias TimebasedUUIDEntityID = EntityID<UUID>

/**
 * UUIDv7(time-based UUID) PK를 사용하는 DAO 엔티티입니다.
 */
open class TimebasedUUIDEntity(id: TimebasedUUIDEntityID): UUIDEntity(id)

/**
 * [TimebasedUUIDTable] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - 테이블 PK는 `UUID`로 고정됩니다.
 * - 생성자 인자 생략 시 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 *
 * ```kotlin
 * object Orders: TimebasedUUIDEntityClass<OrderEntity>(OrdersTable, ::OrderEntity)
 * // Orders.table == OrdersTable
 * ```
 */
open class TimebasedUUIDEntityClass<out E: TimebasedUUIDEntity>(
    table: TimebasedUUIDTable,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDEntityID) -> E)? = null,
): UUIDEntityClass<E>(table, entityType, entityCtor)
