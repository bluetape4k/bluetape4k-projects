package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.core.ksuidGenerated
import io.bluetape4k.idgenerators.ksuid.Ksuid
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * KSUID 문자열을 기본키로 사용하는 Exposed `IdTable` 구현입니다.
 *
 * ## 동작/계약
 * - `id`는 길이 27의 `varchar`이며 `ksuidGenerated()`로 client-side 기본값을 생성합니다.
 * - 기본키는 단일 `id` 컬럼으로 고정됩니다.
 *
 * ```kotlin
 * object Users: KsuidTable("users")
 * // Users.id.name == "id"
 * ```
 */
open class KsuidTable(
    name: String = "",
    columnName: String = "id",
): IdTable<String>(name) {

    /**
     * KSUID 문자열(27자)로 구성된 기본 키 컬럼입니다.
     * 기본값은 `Ksuid.nextId()`로 자동 생성됩니다.
     */
    final override val id = varchar(columnName, 27).ksuidGenerated().entityId()

    /** 테이블 기본키 정의입니다. */
    final override val primaryKey = PrimaryKey(id)
}
