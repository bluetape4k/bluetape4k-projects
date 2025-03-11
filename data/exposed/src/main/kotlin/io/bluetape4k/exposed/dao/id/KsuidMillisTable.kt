package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import io.bluetape4k.idgenerators.ksuid.KsuidMillis
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

open class KsuidMillisTable(name: String = "", columnName: String = "id"): IdTable<String>(name) {
    final override val id =
        varchar(columnName, 27).clientDefault { KsuidMillis.nextId() }.entityId()

    final override val primaryKey = PrimaryKey(id)
}

open class KsuidMillisEntity(id: EntityID<String>): StringEntity(id)

open class KsuidMillisEntityClass<out E: KsuidMillisEntity>(
    table: KsuidMillisTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
