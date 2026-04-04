package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.UlidTable
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/** ULID 문자열 기반 `EntityID` 별칭입니다. */
typealias UlidEntityID = EntityID<String>

/**
 * [UlidEntityID]를 사용하는 문자열 기반 DAO 엔티티입니다.
 *
 * ```kotlin
 * class MyEntity(id: UlidEntityID) : UlidEntity(id) {
 *     companion object : UlidEntityClass<MyEntity>(MyTable)
 *     var name by MyTable.name
 * }
 * ```
 */
open class UlidEntity(id: UlidEntityID): StringEntity(id)

/**
 * [UlidTable] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - 테이블 PK는 26자 ULID 문자열로 고정됩니다.
 * - ULID 생성은 테이블 쪽의 상태 기반 단조 증가 generator 규칙을 따릅니다.
 * - 생성자 인자 생략 시 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 *
 * ```kotlin
 * object T1: UlidTable() {
 *     val name = varchar("name", 255)
 * }
 * class E1(id: UlidEntityID): UlidEntity(id) {
 *     companion object: UlidEntityClass<E1>(T1)
 *     var name by T1.name
 * }
 * ```
 */
open class UlidEntityClass<out E: UlidEntity>(
    table: UlidTable,
    entityType: Class<E>? = null,
    entityCtor: ((UlidEntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
