package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.core.snowflakeGenerated
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable


/**
 * Entity ID 값을 Snowflow ID 값을 사용하는 Table
 */
open class SnowflakeIdTable(
    name: String = "",
    columnName: String = "id",
): IdTable<Long>(name) {

    final override val id: Column<EntityID<Long>> =
        long(columnName).snowflakeGenerated().entityId()

    final override val primaryKey = PrimaryKey(id)
}
