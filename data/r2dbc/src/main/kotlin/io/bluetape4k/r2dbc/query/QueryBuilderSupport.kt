package io.bluetape4k.r2dbc.query

import kotlin.reflect.KProperty

inline fun <reified T: Any> QueryBuilder.parameterNullable(name: String, value: T? = null) {
    parameter(name, value, T::class.java)
}

inline fun <reified T: Any> QueryBuilder.parameterNullable(property: KProperty<*>, value: T? = null) {
    parameter(property.name, value, T::class.java)
}

inline fun query(
    sb: StringBuilder = StringBuilder(),
    crossinline block: QueryBuilder.() -> Unit,
): Query {
    return QueryBuilder().build(sb) { block() }
}

inline fun queryCount(
    sb: StringBuilder = StringBuilder(),
    crossinline block: QueryBuilder.() -> Unit,
): Query {
    return QueryBuilder().buildCount(sb) { block() }
}

inline fun queryWithCount(
    sb: StringBuilder = StringBuilder(),
    crossinline block: QueryBuilder.() -> Unit,
): Pair<Query, Query> {
    val originalSql = sb.toString()
    return query(sb) { block() } to queryCount(StringBuilder(originalSql)) { block() }
}
