package io.bluetape4k.exposed.dao

import io.bluetape4k.idgenerators.snowflake.Snowflakers
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

/**
 * Snowflow ID 를 Primary Key 로 사용하는 Table
 */
open class SnowflowIdTable(name: String = "", columnName: String = "id"): IdTable<Long>(name) {

    final override val id: Column<EntityID<Long>> =
        long(columnName).clientDefault { Snowflakers.Global.nextId() }.entityId()

    final override val primaryKey = PrimaryKey(id)
}
