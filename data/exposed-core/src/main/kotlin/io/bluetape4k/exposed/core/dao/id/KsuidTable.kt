package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.core.ksuidGenerated
import io.bluetape4k.idgenerators.ksuid.Ksuid
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * Entity ID 값을 [Ksuid]로 생성한 문자열을 사용하는 Table
 */
open class KsuidTable(
    name: String = "",
    columnName: String = "id",
): IdTable<String>(name) {

    /**
     * KSUID 문자열(27자)로 구성된 기본 키 컬럼입니다.
     * 기본값은 `Ksuid.nextId()`로 자동 생성됩니다.
     */
    final override val id = varchar(columnName, 27).ksuidGenerated().entityId()

    final override val primaryKey = PrimaryKey(id)
}
