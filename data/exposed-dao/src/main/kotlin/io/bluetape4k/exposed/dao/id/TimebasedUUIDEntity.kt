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
 *
 * ```kotlin
 * class MyEntity(id: TimebasedUUIDEntityID) : TimebasedUUIDEntity(id) {
 *     companion object : TimebasedUUIDEntityClass<MyEntity>(MyTable)
 *     var name by MyTable.name
 * }
 * ```
 */
open class TimebasedUUIDEntity(id: TimebasedUUIDEntityID): UUIDEntity(id)

/**
 * [TimebasedUUIDTable] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - 테스트의 `T1: TimebasedUUIDTable`/`E1: TimebasedUUIDEntity` 패턴으로 `companion object`에서 사용합니다.
 * - PK는 UUID(`uuid`)이며, `new { ... }` 호출 시 테이블의 time-based UUID 생성 규칙을 따릅니다.
 * - [entityType], [entityCtor]를 생략하면 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 *
 * ```kotlin
 * object T1: TimebasedUUIDTable() { val name = varchar("name", 255) }
 * class E1(id: TimebasedUUIDEntityID): TimebasedUUIDEntity(id) {
 *     companion object: TimebasedUUIDEntityClass<E1>(T1)
 * }
 * // E1.new { name = "debop" }.id.value.toString().length == 36
 * ```
 */
open class TimebasedUUIDEntityClass<out E: TimebasedUUIDEntity>(
    table: TimebasedUUIDTable,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDEntityID) -> E)? = null,
): UUIDEntityClass<E>(table, entityType, entityCtor)
