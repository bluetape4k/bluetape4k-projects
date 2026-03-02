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
 * - 테이블 PK는 `String` KSUID(millis)로 고정됩니다.
 * - 생성자 인자 생략 시 Exposed 기본 엔티티 타입/생성자 추론을 사용합니다.
 *
 * ```kotlin
 * object Logs: KsuidMillisEntityClass<LogEntity>(LogsTable, ::LogEntity)
 * // Logs.table == LogsTable
 * ```
 */
open class KsuidMillisEntityClass<out E: KsuidMillisEntity>(
    table: KsuidMillisTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
