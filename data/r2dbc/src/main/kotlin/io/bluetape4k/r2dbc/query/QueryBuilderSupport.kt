package io.bluetape4k.r2dbc.query

import kotlin.reflect.KProperty

/**
 * nullable 값을 타입 정보를 유지한 named 파라미터로 바인딩합니다.
 *
 * ## 동작/계약
 * - [value]가 null이면 `Parameter.fromOrEmpty(value, T::class.java)` 경로로 typed-null이 바인딩됩니다.
 * - [name]을 그대로 사용해 `QueryBuilder.parameter`에 위임합니다.
 * - 수신 [QueryBuilder]를 mutate 하며 반환값 없이 내부 파라미터 상태를 갱신합니다.
 *
 * ```kotlin
 * val q = query {
 *   parameterNullable<Long>("id", null)
 *   append("select * from users where id=:id")
 * }
 * // q.parameters["id"] == null
 * ```
 */
inline fun <reified T: Any> QueryBuilder.parameterNullable(name: String, value: T? = null) {
    parameter(name, value, T::class.java)
}

/**
 * 프로퍼티 이름을 파라미터 이름으로 사용해 nullable 값을 바인딩합니다.
 *
 * ## 동작/계약
 * - [property]의 `name`을 파라미터 키로 사용합니다.
 * - null 값 처리 규칙은 [parameterNullable]과 동일합니다.
 * - 수신 [QueryBuilder] 내부 상태를 갱신합니다.
 *
 * ```kotlin
 * val q = query {
 *   parameterNullable<Long>(User::id, 1L)
 *   append("select * from users where id=:id")
 * }
 * // q.parameters["id"] == 1L
 * ```
 */
inline fun <reified T: Any> QueryBuilder.parameterNullable(property: KProperty<*>, value: T? = null) {
    parameter(property.name, value, T::class.java)
}

/**
 * DSL 블록으로 일반 조회용 [Query]를 생성합니다.
 *
 * ## 동작/계약
 * - [sb]를 기반 SQL 버퍼로 사용해 `QueryBuilder.build`를 호출합니다.
 * - 빌드 과정에서 [QueryBuilder] 내부 파라미터 맵이 새로 구성됩니다.
 * - 수신 버퍼를 재사용하므로 호출자가 동일 버퍼를 공유하면 내용이 누적될 수 있습니다.
 *
 * ```kotlin
 * val q = query {
 *   append("select * from users where id=:id")
 *   parameter("id", 1L)
 * }
 * // q.sql == "select * from users where id=:id"
 * ```
 */
inline fun query(
    sb: StringBuilder = StringBuilder(),
    @BuilderInference crossinline block: QueryBuilder.() -> Unit,
): Query {
    return QueryBuilder().build(sb) { block() }
}

/**
 * 동일 DSL로 count 조회용 [Query]를 생성합니다.
 *
 * ## 동작/계약
 * - [QueryBuilder.buildCount]를 호출해 `count(*)` 쿼리를 만듭니다.
 * - 필터/파라미터 구성은 일반 query와 동일하게 적용됩니다.
 * - [sb]를 직접 사용하므로 기존 문자열이 있으면 앞부분에 포함됩니다.
 *
 * ```kotlin
 * val countQ = queryCount {
 *   append("from users where active = :active")
 *   parameter("active", true)
 * }
 * // countQ.sql.contains("count") == true
 * ```
 */
inline fun queryCount(
    sb: StringBuilder = StringBuilder(),
    @BuilderInference crossinline block: QueryBuilder.() -> Unit,
): Query {
    return QueryBuilder().buildCount(sb) { block() }
}

/**
 * 동일 DSL로 일반 조회 쿼리와 count 쿼리를 함께 생성합니다.
 *
 * ## 동작/계약
 * - 먼저 [sb]로 일반 query를 만들고, 원본 문자열을 복제해 count query를 만듭니다.
 * - 반환값은 `Pair<조회 Query, 카운트 Query>` 순서입니다.
 * - 블록을 두 번 실행하므로 블록 내부에 부수효과가 있으면 중복 수행됩니다.
 *
 * ```kotlin
 * val (q, cq) = queryWithCount {
 *   append("from users where active = :active")
 *   parameter("active", true)
 * }
 * // q.parameters == cq.parameters
 * ```
 */
inline fun queryWithCount(
    sb: StringBuilder = StringBuilder(),
    @BuilderInference crossinline block: QueryBuilder.() -> Unit,
): Pair<Query, Query> {
    val originalSql = sb.toString()
    return query(sb) { block() } to queryCount(StringBuilder(originalSql)) { block() }
}
