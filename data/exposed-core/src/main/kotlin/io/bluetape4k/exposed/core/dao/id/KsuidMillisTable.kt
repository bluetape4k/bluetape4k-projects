package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.core.ksuidMillisGenerated
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * 밀리초 기반 KSUID 문자열을 기본키로 사용하는 Exposed `IdTable` 구현입니다.
 *
 * ## 동작/계약
 * - `id`는 길이 27의 `varchar`이며 `ksuidMillisGenerated()`로 기본값을 생성합니다.
 * - 기본키는 단일 `id` 컬럼으로 고정됩니다.
 *
 * ```kotlin
 * object Logs: KsuidMillisTable("logs")
 * // Logs.id.name == "id"
 * ```
 */
open class KsuidMillisTable(
    name: String = "",
    columnName: String = "id",
) : IdTable<String>(name) {
    /**
     * KSUID 문자열(27자)로 구성된 기본 키 컬럼입니다.
     * 기본값은 `Ksuid.Millis.nextId()`로 자동 생성됩니다.
     */
    final override val id =
        varchar(columnName, 27).ksuidMillisGenerated().entityId()

    /** 테이블 기본키 정의입니다. */
    final override val primaryKey = PrimaryKey(id)
}
