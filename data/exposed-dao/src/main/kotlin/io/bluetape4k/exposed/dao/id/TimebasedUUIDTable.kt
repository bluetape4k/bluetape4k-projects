package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.timebasedGenerated
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.*

/**
 * Entity ID 값을 Timebased UUID 를 사용하는 Table
 */
open class TimebasedUUIDTable(name: String = "", columnName: String = "id"): IdTable<UUID>(name) {
    final override val id: Column<EntityID<UUID>> =
        javaUUID(columnName).timebasedGenerated().entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias TimebasedUUIDEntityID = EntityID<UUID>

open class TimebasedUUIDEntity(id: TimebasedUUIDEntityID): UUIDEntity(id)

open class TimebasedUUIDEntityClass<out E: TimebasedUUIDEntity>(
    table: TimebasedUUIDTable,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDEntityID) -> E)? = null,
): UUIDEntityClass<E>(table, entityType, entityCtor)
