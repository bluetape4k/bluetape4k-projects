package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.core.timebasedGenerated
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import java.util.*

/**
 * Entity ID 값을 Timebased UUID 를 사용하는 Table
 */
open class TimebasedUUIDTable(
    name: String = "",
    columnName: String = "id",
): IdTable<UUID>(name) {

    /**
     * UUID v7 을 Client 에서 생성합니다.
     */
    final override val id: Column<EntityID<UUID>> =
        javaUUID(columnName).timebasedGenerated().entityId()

    final override val primaryKey = PrimaryKey(id)
}
