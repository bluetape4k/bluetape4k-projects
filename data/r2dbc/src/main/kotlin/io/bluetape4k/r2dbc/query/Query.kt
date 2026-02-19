package io.bluetape4k.r2dbc.query

import io.bluetape4k.ToStringBuilder
import java.io.Serializable

/**
 * SQL 쿼리와 파라미터를 캡슐화하는 데이터 클래스입니다.
 *
 * @property sqlBuffer SQL 문을 담고 있는 StringBuilder
 * @property parameters 이름 기반 파라미터 맵
 */
data class Query(
    val sqlBuffer: StringBuilder,
    val parameters: Map<String, Any?>,
): Serializable {
    /**
     * 완성된 SQL 문자열을 반환합니다.
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
