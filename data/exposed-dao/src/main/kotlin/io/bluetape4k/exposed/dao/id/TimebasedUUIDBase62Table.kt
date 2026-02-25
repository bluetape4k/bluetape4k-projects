package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDBase62Table
import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDBase62TableMySql
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID


typealias TimebasedUUIDBase62EntityID = EntityID<String>

open class TimebasedUUIDBase62Entity(id: TimebasedUUIDBase62EntityID): StringEntity(id)

open class TimebasedUUIDBase62EntityClass<out E: TimebasedUUIDBase62Entity>(
    table: TimebasedUUIDBase62Table,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDBase62EntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)

open class TimebasedUUIDBase62EntityMySql(id: EntityID<String>): StringEntity(id)

open class TimebasedUUIDBase62EntityClassMySql<out E: TimebasedUUIDBase62EntityMySql>(
    table: TimebasedUUIDBase62TableMySql,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
