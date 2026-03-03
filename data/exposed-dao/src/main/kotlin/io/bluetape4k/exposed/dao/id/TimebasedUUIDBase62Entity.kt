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
 * - 테스트의 `T1: TimebasedUUIDBase62Table`/`E1: TimebasedUUIDBase62Entity` 패턴으로 `companion object`에서 사용합니다.
 * - PK는 Base62 UUID 문자열(`VARCHAR(22)`)이며, `new { ... }` 호출 시 테이블 기본 ID 생성 규칙을 따릅니다.
 * - [entityType], [entityCtor]를 생략하면 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 *
 * ```kotlin
 * object T1: TimebasedUUIDBase62Table() { val name = varchar("name", 255) }
 * class E1(id: TimebasedUUIDBase62EntityID): TimebasedUUIDBase62Entity(id) {
 *     companion object: TimebasedUUIDBase62EntityClass<E1>(T1)
 * }
 * // E1.new { name = "debop" }.id.value.length == 22
 * ```
 */
open class TimebasedUUIDBase62EntityClass<out E: TimebasedUUIDBase62Entity>(
    table: TimebasedUUIDBase62Table,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDBase62EntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)

/**
 * MySQL용 Base62 UUID 테이블을 사용하는 DAO 엔티티입니다.
 */
open class TimebasedUUIDBase62EntityMySql(id: TimebasedUUIDBase62EntityID): StringEntity(id)

/**
 * [TimebasedUUIDBase62TableMySql] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - PK는 Base62 UUID 문자열이며 테이블 정의 쪽에서 `utf8mb4_bin` collation을 적용해 대소문자를 구분합니다.
 * - MySQL 계열에서 문자열 PK 비교를 바이너리 기준으로 맞추고 싶을 때 `companion object` 엔티티 클래스에 사용합니다.
 * - [entityType], [entityCtor]를 생략하면 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 *
 * ```kotlin
 * object T1: TimebasedUUIDBase62TableMySql()
 * class E1(id: TimebasedUUIDBase62EntityID): TimebasedUUIDBase62EntityMySql(id) {
 *     companion object: TimebasedUUIDBase62EntityClassMySql<E1>(T1)
 * }
 * // E1.new { }.id.value.isNotBlank()
 * ```
 */
open class TimebasedUUIDBase62EntityClassMySql<out E: TimebasedUUIDBase62EntityMySql>(
    table: TimebasedUUIDBase62TableMySql,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDBase62EntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
