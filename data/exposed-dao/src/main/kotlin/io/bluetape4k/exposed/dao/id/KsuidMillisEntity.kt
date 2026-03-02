package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.KsuidMillisTable
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID


/**
 * 밀리초 기반 KSUID 문자열 PK를 사용하는 DAO 엔티티입니다.
 */
open class KsuidMillisEntity(id: EntityID<String>): StringEntity(id)

/**
 * [KsuidMillisTable] 기반 엔티티를 관리하는 DAO `EntityClass`입니다.
 *
 * ## 동작/계약
 * - 테스트의 `T1: KsuidMillisTable`/`E1: KsuidMillisEntity`와 같은 패턴으로 `companion object`에서 사용합니다.
 * - PK는 밀리초 기반 KSUID 문자열(`VARCHAR(27)`)이며, `new { ... }` 호출 시 테이블 쪽 기본 ID 생성 규칙을 따릅니다.
 * - [entityType], [entityCtor]를 생략하면 Exposed 기본 추론 규칙으로 엔티티를 생성합니다.
 *
 * ```kotlin
 * object T1: KsuidMillisTable() { val name = varchar("name", 255) }
 * class E1(id: EntityID<String>): KsuidMillisEntity(id) {
 *     companion object: KsuidMillisEntityClass<E1>(T1)
 * }
 * // E1.new { name = "debop" }.id.value.length == 27
 * ```
 */
open class KsuidMillisEntityClass<out E: KsuidMillisEntity>(
    table: KsuidMillisTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
