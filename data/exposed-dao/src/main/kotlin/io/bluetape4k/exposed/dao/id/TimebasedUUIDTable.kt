package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.*


typealias TimebasedUUIDEntityID = EntityID<UUID>

open class TimebasedUUIDEntity(id: TimebasedUUIDEntityID): UUIDEntity(id)

open class TimebasedUUIDEntityClass<out E: TimebasedUUIDEntity>(
    table: TimebasedUUIDTable,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDEntityID) -> E)? = null,
): UUIDEntityClass<E>(table, entityType, entityCtor)
