package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import io.bluetape4k.exposed.sql.timebasedGenerated
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * Entity ID 값을 Timebased UUID 를 Base62로 인코딩한 문자열을 사용하는 Table
 *
 * @sample io.bluetape4k.exposed.dao.id.TimebasedUUIDBase62TableTest.T1
 */
open class TimebasedUUIDBase62Table(name: String = "", columnName: String = "id"): IdTable<String>(name) {

    final override val id =
        varchar(columnName, 22).timebasedGenerated().entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias TimebasedUUIDBase62EntityID = EntityID<String>

open class TimebasedUUIDBase62Entity(id: TimebasedUUIDBase62EntityID): StringEntity(id)

open class TimebasedUUIDBase62EntityClass<out E: TimebasedUUIDBase62Entity>(
    table: TimebasedUUIDBase62Table,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDBase62EntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
