package io.bluetape4k.exposed.dao.id

import io.bluetape4k.idgenerators.snowflake.Snowflakers
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

/**
 * Entity ID 값을 Snowflow ID 값을 사용하는 Table
 *
 * @sample io.bluetape4k.exposed.dao.id.SnowflakeIdTableTest.T1
 */
open class SnowflakeIdTable(name: String = "", columnName: String = "id"): IdTable<Long>(name) {

    final override val id: Column<EntityID<Long>> =
        long(columnName).clientDefault { Snowflakers.Global.nextId() }.entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias SnowflakeIdEntityID = EntityID<Long>

open class SnowflakeIdEntity(id: SnowflakeIdEntityID): LongEntity(id)

open class SnowflakeIdEntityClass<out E: SnowflakeIdEntity>(
    table: SnowflakeIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((SnowflakeIdEntityID) -> E)? = null,
): LongEntityClass<E>(table, entityType, entityCtor)
