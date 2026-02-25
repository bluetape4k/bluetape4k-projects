package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.SnowflakeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

typealias SnowflakeIdEntityID = EntityID<Long>

open class SnowflakeIdEntity(id: SnowflakeIdEntityID): LongEntity(id)

open class SnowflakeIdEntityClass<out E: SnowflakeIdEntity>(
    table: SnowflakeIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((SnowflakeIdEntityID) -> E)? = null,
): LongEntityClass<E>(table, entityType, entityCtor)
