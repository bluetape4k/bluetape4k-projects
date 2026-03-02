package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.KsuidTable
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID


/** KSUID 문자열 기반 `EntityID` 별칭입니다. */
typealias KsuidEntityID = EntityID<String>

/**
 * [KsuidEntityID]를 사용하는 문자열 기반 DAO 엔티티입니다.
 */
open class KsuidEntity(id: KsuidEntityID): StringEntity(id)

/**
 * [KsuidTable] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - 테이블 PK는 `String` KSUID로 고정됩니다.
 * - 생성자 인자 생략 시 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 *
 * ```kotlin
 * object Users: KsuidEntityClass<UserEntity>(UsersTable, ::UserEntity)
 * // Users.table == UsersTable
 * ```
 */
open class KsuidEntityClass<out E: KsuidEntity>(
    table: KsuidTable,
    entityType: Class<E>? = null,
    entityCtor: ((KsuidEntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
