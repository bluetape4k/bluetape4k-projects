package io.bluetape4k.exposed.dao.id

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.dao.id.IdTable

/**
 * Entity ID 값을 Timebased UUID 를 Base62로 인코딩한 문자열을 사용하는 Table
 */
open class TimebasedUUIDBase62Table(name: String = "", columnName: String = "id"): IdTable<String>(name) {

    final override val id =
        varchar(columnName, 22).clientDefault { TimebasedUuid.Reordered.nextIdAsString() }.entityId()

    final override val primaryKey = PrimaryKey(id)
}
