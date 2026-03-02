package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.SnowflakeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/** Snowflake Long 기반 `EntityID` 별칭입니다. */
typealias SnowflakeIdEntityID = EntityID<Long>

/**
 * Snowflake Long PK를 사용하는 DAO 엔티티입니다.
 */
open class SnowflakeIdEntity(id: SnowflakeIdEntityID): LongEntity(id)

/**
 * [SnowflakeIdTable] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - 테이블 PK는 `Long` Snowflake 값으로 고정됩니다.
 * - 생성자 인자 생략 시 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 *
 * ```kotlin
 * object Events: SnowflakeIdEntityClass<EventEntity>(EventsTable, ::EventEntity)
 * // Events.table == EventsTable
 * ```
 */
open class SnowflakeIdEntityClass<out E: SnowflakeIdEntity>(
    table: SnowflakeIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((SnowflakeIdEntityID) -> E)? = null,
): LongEntityClass<E>(table, entityType, entityCtor)
