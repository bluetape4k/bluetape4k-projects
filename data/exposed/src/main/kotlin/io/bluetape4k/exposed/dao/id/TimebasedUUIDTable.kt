package io.bluetape4k.exposed.dao.id

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import java.util.*

/**
 * Entity ID 값을 Timebased UUID 를 사용하는 Table
 *
 * @sample io.bluetape4k.exposed.dao.id.TimebasedUUIDTableTest.T1
 */
open class TimebasedUUIDTable(name: String = "", columnName: String = "id"): IdTable<UUID>(name) {

    final override val id: Column<EntityID<UUID>> =
        uuid(columnName).clientDefault { TimebasedUuid.Reordered.nextId() }.entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias TimebasedUUIDEntityID = EntityID<UUID>

open class TimebasedUUIDEntity(id: TimebasedUUIDEntityID): UUIDEntity(id)

open class TimebasedUUIDEntityClass<out E: TimebasedUUIDEntity>(
    table: TimebasedUUIDTable,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDEntityID) -> E)? = null,
): UUIDEntityClass<E>(table, entityType, entityCtor)
