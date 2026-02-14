package io.bluetape4k.r2dbc.query

import kotlin.reflect.KProperty

/**
 * nullable 값을 타입 정보와 함께 바인딩한다.
 * 값이 null 이면 typed-null [io.r2dbc.spi.Parameter] 를 생성해 드라이버가 타입을 추론할 수 있게 한다.
 */
inline fun <reified T: Any> QueryBuilder.parameterNullable(name: String, value: T? = null) {
    parameter(name, value, T::class.java)
}

/**
 * [KProperty] 이름 기반 nullable 파라미터 바인딩 버전.
 */
inline fun <reified T: Any> QueryBuilder.parameterNullable(property: KProperty<*>, value: T? = null) {
    parameter(property.name, value, T::class.java)
}

inline fun query(
    sb: StringBuilder = StringBuilder(),
    @BuilderInference crossinline block: QueryBuilder.() -> Unit,
): Query {
    return QueryBuilder().build(sb) { block() }
}

inline fun queryCount(
    sb: StringBuilder = StringBuilder(),
    @BuilderInference crossinline block: QueryBuilder.() -> Unit,
): Query {
    return QueryBuilder().buildCount(sb) { block() }
}

inline fun queryWithCount(
    sb: StringBuilder = StringBuilder(),
    @BuilderInference crossinline block: QueryBuilder.() -> Unit,
): Pair<Query, Query> {
    val originalSql = sb.toString()
    return query(sb) { block() } to queryCount(StringBuilder(originalSql)) { block() }
}
