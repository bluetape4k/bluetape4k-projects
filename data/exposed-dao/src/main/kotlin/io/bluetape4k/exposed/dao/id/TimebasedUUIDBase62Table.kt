package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.timebasedGenerated
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * Entity ID 값을 Timebased UUID 를 Base62로 인코딩한 문자열을 사용하는 Table
 */
open class TimebasedUUIDBase62Table(name: String = "", columnName: String = "id"): IdTable<String>(name) {

    final override val id =
        varchar(columnName, 24).timebasedGenerated().entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias TimebasedUUIDBase62EntityID = EntityID<String>

open class TimebasedUUIDBase62Entity(id: TimebasedUUIDBase62EntityID): StringEntity(id)

open class TimebasedUUIDBase62EntityClass<out E: TimebasedUUIDBase62Entity>(
    table: TimebasedUUIDBase62Table,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDBase62EntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)


/**
 * Entity ID 값을 Timebased UUID 를 Base62로 인코딩한 문자열을 사용하는 MySQL 계열용 Table
 *
 * MySQL은 collate 를 지정하지 않으면 대소문자 구분을 못해서 Base62 인코딩 문자열이 중복될 수 있습니다.
 * 이를 해결하기 위해 varchar 컬럼에 collate 를 `utf8mb4_bin` 등으로 지정하여 대소문자 구분을 할 수 있도록 해야 합니다.
 *
 * 아니면 테스트용 DB 서버의 기본 collate 를 `utf8mb4_bin` 으로 설정하면 됩니다.
 *
 * ```
 * val MySQL8: MySQL8Server by lazy {
 *     MySQL8Server()
 *         .withCommand(
 *             "--character-set-server=utf8mb4",
 *             "--collation-server=utf8mb4_bin"
 *         )
 *         .apply {
 *             start()
 *             ShutdownQueue.register(this)
 *         }
 * }
 * ```
 *
 */
open class TimebasedUUIDBase62TableMySql(name: String = "", columnName: String = "id"): IdTable<String>(name) {

    final override val id =
        varchar(columnName, 24, "utf8mb4_bin").timebasedGenerated().entityId()

    final override val primaryKey = PrimaryKey(id)
}

open class TimebasedUUIDBase62EntityMySql(id: EntityID<String>): StringEntity(id)

open class TimebasedUUIDBase62EntityClassMySql<out E: TimebasedUUIDBase62EntityMySql>(
    table: TimebasedUUIDBase62TableMySql,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
