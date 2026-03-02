package io.bluetape4k.exposed.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

/**
 * 문자열 식별자(`EntityID<String>`)를 사용하는 DAO 엔티티 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - Exposed [Entity] 구현을 그대로 따르며 식별자 타입만 `String`으로 고정합니다.
 * - 생성자는 이미 생성된 [EntityID]를 그대로 받아 상위 클래스에 전달합니다.
 *
 * ```kotlin
 * class UserEntity(id: EntityID<String>): StringEntity(id)
 * // UserEntity::class.simpleName == "UserEntity"
 * ```
 */
abstract class StringEntity(id: EntityID<String>): Entity<String>(id)


/**
 * 문자열 식별자 엔티티를 관리하는 DAO `EntityClass` 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - `table`의 PK 타입이 `String`인 엔티티를 생성/조회/캐시하는 표준 Exposed 동작을 따릅니다.
 * - `entityType`, `entityCtor`를 생략하면 Exposed 기본 추론(리플렉션 포함)을 사용합니다.
 *
 * ```kotlin
 * object Users: StringEntityClass<UserEntity>(UsersTable, ::UserEntity)
 * // Users.table == UsersTable
 * ```
 */
abstract class StringEntityClass<out E: StringEntity>(
    table: IdTable<String>,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null,
): EntityClass<String, E>(table, entityType, entityCtor)
