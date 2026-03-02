package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.core.timebasedGenerated
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * UUIDv7 Base62 문자열을 기본키로 사용하는 Exposed `IdTable` 구현입니다.
 *
 * ## 동작/계약
 * - `id`는 길이 24의 `varchar`이며 `timebasedGenerated()`로 기본값을 생성합니다.
 * - 기본키는 단일 `id` 컬럼으로 고정됩니다.
 *
 * ```kotlin
 * object Links: TimebasedUUIDBase62Table("links")
 * // Links.id.columnType.sqlType().contains("VARCHAR")
 * ```
 */
open class TimebasedUUIDBase62Table(
    name: String = "",
    columnName: String = "id",
): IdTable<String>(name) {

    /**
     * UUID v7 을 Base62로 인코딩한 문자열을 Client 에서 생성합니다.
     */
    final override val id =
        varchar(columnName, 24).timebasedGenerated().entityId()

    /** 테이블 기본키 정의입니다. */
    final override val primaryKey = PrimaryKey(id)
}

/**
 * MySQL 계열에서 Base62 문자열 PK 충돌을 피하기 위해 바이너리 collation을 강제한 `IdTable` 구현입니다.
 *
 * ## 동작/계약
 * - `id` 컬럼 collation을 `utf8mb4_bin`으로 지정해 대소문자를 구분합니다.
 * - 기본값 생성 로직은 [TimebasedUUIDBase62Table]과 동일합니다.
 *
 * ```kotlin
 * object LinksMySql: TimebasedUUIDBase62TableMySql("links")
 * // LinksMySql.id.columnType.sqlType().contains("utf8mb4_bin")
 * ```
 */
open class TimebasedUUIDBase62TableMySql(
    name: String = "",
    columnName: String = "id",
): IdTable<String>(name) {

    /**
     * UUID v7 을 Base62로 인코딩한 문자열을 Client 에서 생성합니다.
     */
    final override val id =
        varchar(columnName, 24, "utf8mb4_bin").timebasedGenerated().entityId()

    /** 테이블 기본키 정의입니다. */
    final override val primaryKey = PrimaryKey(id)
}
