package io.bluetape4k.exposed.dao.id

import io.bluetape4k.idgenerators.ksuid.Ksuid
import org.jetbrains.exposed.dao.id.IdTable

/**
 * Entity ID 값을 [Ksuid]로 생성한 문자열을 사용하는 Table
 *
 * @sample io.bluetape4k.exposed.dao.id.KsuidTableTest.T1
 */
open class KsuidTable(name: String = "", columnName: String = "id"): IdTable<String>(name) {
    final override val id =
        varchar(columnName, 27).clientDefault { Ksuid.nextId() }.entityId()

    final override val primaryKey = PrimaryKey(id)
}
