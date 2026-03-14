package io.bluetape4k.r2dbc.query

import io.bluetape4k.ToStringBuilder
import java.io.Serializable

/**
 * SQL 본문과 named 파라미터를 함께 보관하는 쿼리 객체입니다.
 *
 * ## 동작/계약
 * - [sqlBuffer]와 [parameters]를 그대로 보관하며 생성 시점에 SQL 문자열을 확정하지 않습니다.
 * - [sql]은 첫 접근 시 `trim()`된 문자열을 계산해 캐시(`lazy`)합니다.
 * - `sqlBuffer`를 외부에서 변경하면 [sql] 계산 시점에 그 변경분이 반영됩니다.
 *
 * ```kotlin
 * val q = Query(StringBuilder("select * from users where id=:id"), mapOf("id" to 1L))
 * val sql = q.sql
 * // sql == "select * from users where id=:id"
 * ```
 */
data class Query(
    val sqlBuffer: StringBuilder,
    val parameters: Map<String, Any?>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    /**
     * 트림된 SQL 문자열을 지연 계산해 반환합니다.
     *
     * ## 동작/계약
     * - 최초 접근 시 `sqlBuffer.toString().trim()`을 계산합니다.
     * - 이후 접근은 캐시된 문자열을 재사용합니다.
     * - 계산 시점 이후 `sqlBuffer` 변경은 반영되지 않습니다.
     *
     * ```kotlin
     * val query = Query(StringBuilder(" select 1 "), emptyMap())
     * val sql = query.sql
     * // sql == "select 1"
     * ```
     */
    val sql: String by lazy {
        sqlBuffer.toString().trim()
    }

    override fun toString(): String =
        ToStringBuilder(this)
            .add("sql", sql)
            .add("parameters", parameters)
            .toString()
}
