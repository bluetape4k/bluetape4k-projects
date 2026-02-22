package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.ksuidMillisGenerated
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * KSUID (K-Sortable Unique Identifier) 기반의 기본 키를 사용하는 Exposed Table 구현입니다.
 *
 * @param name 테이블 이름
 * @param columnName 기본 키 컬럼명 (기본값: "id")
 */
open class KsuidMillisTable(
    name: String = "",
    columnName: String = "id",
): IdTable<String>(name) {

    /**
     * KSUID 문자열(27자)로 구성된 기본 키 컬럼입니다.
     * 기본값은 `KsuidMillis.nextId()`로 자동 생성됩니다.
     */
    final override val id =
        varchar(columnName, 27).ksuidMillisGenerated().entityId()

    /**
     * 테이블의 기본 키를 지정합니다.
     */
    final override val primaryKey = PrimaryKey(id)
}

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
