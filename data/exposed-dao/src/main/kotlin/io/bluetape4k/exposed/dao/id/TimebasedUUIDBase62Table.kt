package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDBase62Table
import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDBase62TableMySql
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID


/** UUIDv7 Base62 문자열 기반 `EntityID` 별칭입니다. */
typealias TimebasedUUIDBase62EntityID = EntityID<String>

/**
 * UUIDv7 Base62 문자열 PK를 사용하는 DAO 엔티티입니다.
 */
open class TimebasedUUIDBase62Entity(id: TimebasedUUIDBase62EntityID): StringEntity(id)

/**
 * [TimebasedUUIDBase62Table] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - 테이블 PK는 Base62 문자열(`String`)로 고정됩니다.
 * - 생성자 인자 생략 시 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 */
open class TimebasedUUIDBase62EntityClass<out E: TimebasedUUIDBase62Entity>(
    table: TimebasedUUIDBase62Table,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDBase62EntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)

/**
 * MySQL용 Base62 UUID 테이블을 사용하는 DAO 엔티티입니다.
 */
open class TimebasedUUIDBase62EntityMySql(id: EntityID<String>): StringEntity(id)

/**
 * [TimebasedUUIDBase62TableMySql] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - PK는 Base62 문자열이며 테이블 쪽에서 `utf8mb4_bin` collation을 사용합니다.
 * - 생성자 인자 생략 시 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 */
open class TimebasedUUIDBase62EntityClassMySql<out E: TimebasedUUIDBase62EntityMySql>(
    table: TimebasedUUIDBase62TableMySql,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
