package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.SnowflakeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/** Snowflake Long 기반 `EntityID` 별칭입니다. */
typealias SnowflakeIdEntityID = EntityID<Long>

/**
 * Snowflake Long PK를 사용하는 DAO 엔티티입니다.
 *
 * ```kotlin
 * class MyEntity(id: SnowflakeIdEntityID) : SnowflakeIdEntity(id) {
 *     companion object : SnowflakeIdEntityClass<MyEntity>(MyTable)
 *     var name by MyTable.name
 * }
 * ```
 */
open class SnowflakeIdEntity(id: SnowflakeIdEntityID): LongEntity(id)

/**
 * [SnowflakeIdTable] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - 테스트의 `T1: SnowflakeIdTable`/`E1: SnowflakeIdEntity`처럼 `companion object`에서 엔티티 팩토리로 사용합니다.
 * - PK는 Snowflake `Long` 값이며, `new { ... }` 시 테이블의 ID 생성 전략을 따라 유니크 값이 생성됩니다.
 * - [entityType], [entityCtor]를 생략하면 Exposed 기본 추론 규칙으로 엔티티를 생성합니다.
 *
 * ```kotlin
 * object T1: SnowflakeIdTable() { val name = varchar("name", 255) }
 * class E1(id: SnowflakeIdEntityID): SnowflakeIdEntity(id) {
 *     companion object: SnowflakeIdEntityClass<E1>(T1)
 * }
 * // E1.new { name = "debop" }.id.value > 0L
 * ```
 */
open class SnowflakeIdEntityClass<out E: SnowflakeIdEntity>(
    table: SnowflakeIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((SnowflakeIdEntityID) -> E)? = null,
): LongEntityClass<E>(table, entityType, entityCtor)
