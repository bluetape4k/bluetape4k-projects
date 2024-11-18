package io.bluetape4k.r2dbc.query

import io.bluetape4k.ToStringBuilder
import java.io.Serializable

class Query(
    val sqlBuffer: StringBuilder,
    val parameters: Map<String, Any?>,
): Serializable {

    val sql: String by lazy {
        sqlBuffer.toString().trim()
    }

    override fun toString(): String {
        return ToStringBuilder(this)
            .add("sql", sql)
            .add("parameters", parameters)
            .toString()
    }
}
