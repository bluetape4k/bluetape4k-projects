package io.bluetape4k.exposed.dao

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import java.util.*

/**
 * Timebased UUID 를 Primary Key 로 사용하는 Table
 */
open class TimebasedUUIDTable(name: String = "", columnName: String = "id"): IdTable<UUID>(name) {

    final override val id: Column<EntityID<UUID>> =
        uuid(columnName).clientDefault { TimebasedUuid.Reordered.nextId() }.entityId()

    final override val primaryKey = PrimaryKey(id)
}
