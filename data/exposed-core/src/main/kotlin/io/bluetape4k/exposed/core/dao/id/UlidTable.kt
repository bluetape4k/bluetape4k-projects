package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.core.ulidGenerated
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * 상태 기반 단조 증가 ULID 문자열을 기본키로 사용하는 Exposed `IdTable` 구현입니다.
 *
 * ## 동작/계약
 * - `id`는 길이 26의 `varchar`이며 `ulidGenerated()`로 client-side 기본값을 생성합니다.
 * - 내부적으로 `ULID.StatefulMonotonic`을 공유하여 동일 밀리초 내에서도 단조 증가를 유지합니다.
 * - 기본키는 단일 `id` 컬럼으로 고정됩니다.
 *
 * ```kotlin
 * object Orders: UlidTable("orders")
 * // Orders.id.name == "id"
 * ```
 */
open class UlidTable(
    name: String = "",
    columnName: String = "id",
): IdTable<String>(name) {

    /**
     * ULID 문자열(26자)로 구성된 기본 키 컬럼입니다.
     * 기본값은 상태 기반 단조 증가 ULID 생성기로 자동 생성됩니다.
     */
    final override val id = varchar(columnName, 26).ulidGenerated().entityId()

    /** 테이블 기본키 정의입니다. */
    final override val primaryKey = PrimaryKey(id)
}
