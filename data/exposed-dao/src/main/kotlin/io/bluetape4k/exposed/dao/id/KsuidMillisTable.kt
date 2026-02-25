package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.KsuidMillisTable
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID


/**
 * KSUID 기반의 Entity 클래스입니다.
 *
 * @param id KSUID 문자열을 기본 키로 사용합니다.
 */
open class KsuidMillisEntity(id: EntityID<String>): StringEntity(id)

/**
 * KSUID 기반 Entity를 위한 EntityClass 구현입니다.
 *
 * @param E KSUID 기반 Entity 타입
 * @param table 사용할 테이블
 * @param entityType Entity 클래스 타입 (nullable)
 * @param entityCtor Entity 생성자 (nullable)
 */
open class KsuidMillisEntityClass<out E: KsuidMillisEntity>(
    table: KsuidMillisTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
