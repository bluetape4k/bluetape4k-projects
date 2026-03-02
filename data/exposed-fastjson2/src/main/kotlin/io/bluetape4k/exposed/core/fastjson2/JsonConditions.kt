package io.bluetape4k.exposed.core.fastjson2

import org.jetbrains.exposed.v1.core.ComplexExpression
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.asLiteral
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * JSON 대상에 후보 값이 포함되는지 판별하는 SQL 연산식입니다.
 *
 * ## 동작/계약
 * - SQL 렌더링은 현재 Dialect의 `jsonContains(...)` 구현에 위임됩니다.
 * - [path]가 `null`이면 Dialect 기본 루트 경로 규칙을 따릅니다.
 * - 객체 상태를 변경하지 않는 불변 식 객체이며, 쿼리 빌드 시점에만 문자열이 생성됩니다.
 *
 * ```kotlin
 * val op = Contains(targetExpr, candidateExpr, "$.items", jsonType)
 * // op is Op<Boolean>
 * ```
 *
 * @param target 포함 여부를 검사할 JSON 대상 표현식입니다.
 * @param candidate 포함 여부를 판별할 후보 표현식입니다.
 * @param path JSON 경로입니다. `null`이면 벤더 기본 규칙을 사용합니다.
 * @param jsonType JSON 캐스트/렌더링에 사용할 컬럼 타입입니다.
 */
class Contains(
    val target: Expression<*>,
    val candidate: Expression<*>,
    val path: String?,
    val jsonType: IColumnType<*>,
): Op<Boolean>(), ComplexExpression {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) =
        currentDialect.functionProvider.jsonContains(target, candidate, path, jsonType, queryBuilder)
}

/**
 * JSON 경로가 존재하는지 판별하는 SQL 연산식입니다.
 *
 * ## 동작/계약
 * - 전달된 [path] 가변 인자를 Dialect의 `jsonExists(...)`로 그대로 전달합니다.
 * - [optional]은 Dialect별 추가 옵션 문자열로 처리됩니다.
 * - 불변 연산식이므로 생성 후 내부 값은 변경되지 않습니다.
 *
 * ```kotlin
 * val op = Exists(expr, "$.name", optional = null, jsonType = expr.columnType)
 * // op is Op<Boolean>
 * ```
 *
 * @param expression 존재 여부를 검사할 JSON 표현식입니다.
 * @param path 검사할 JSON 경로 목록입니다.
 * @param optional Dialect 확장 옵션 문자열입니다.
 * @param jsonType JSON 타입 정보입니다.
 */
class Exists(
    val expression: Expression<*>,
    vararg val path: String,
    val optional: String?,
    val jsonType: IColumnType<*>,
): Op<Boolean>(), ComplexExpression {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) =
        currentDialect.functionProvider.jsonExists(expression, path = path, optional, jsonType, queryBuilder)
}

/**
 * JSON 표현식에 후보 표현식이 포함되는지 검사하는 연산식을 생성합니다.
 *
 * ## 동작/계약
 * - 수신 컬럼의 [ExpressionWithColumnType.columnType]을 JSON 타입으로 사용합니다.
 * - [candidate]를 그대로 사용하므로 별도 직렬화/문자열 변환이 없습니다.
 * - 결과는 SQL 조건식 객체이며 즉시 DB 조회를 수행하지 않습니다.
 *
 * ```kotlin
 * val condition = table.payload.contains(otherExpr)
 * // condition is Contains
 * ```
 *
 * @param candidate 포함 여부를 검사할 후보 표현식입니다.
 * @param path JSON 경로입니다. 기본값은 `null`입니다.
 */
fun ExpressionWithColumnType<*>.contains(
    candidate: Expression<*>,
    path: String? = null,
): Contains =
    Contains(this, candidate, path, columnType)

/**
 * JSON 표현식에 후보 값이 포함되는지 검사하는 연산식을 생성합니다.
 *
 * ## 동작/계약
 * - 문자열 후보는 SQL 문자열 리터럴로, 그 외 타입은 `asLiteral`로 변환합니다.
 * - [path]가 지정되면 해당 경로 기준 포함 여부를 계산합니다.
 * - 리터럴 변환이 지원되지 않는 타입은 Exposed 리터럴 생성 시 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val condition = table.payload.contains("admin", "$.roles")
 * // condition is Contains
 * ```
 *
 * @param candidate 포함 여부를 검사할 후보 값입니다.
 * @param path JSON 경로입니다. 기본값은 `null`입니다.
 */
fun <T> ExpressionWithColumnType<*>.contains(
    candidate: T,
    path: String? = null,
): Contains = when (candidate) {
    is String -> Contains(this, stringLiteral(candidate), path, columnType)
    else -> Contains(this, asLiteral(candidate), path, columnType)
}

/**
 * JSON 표현식에 지정 경로가 존재하는지 검사하는 연산식을 생성합니다.
 *
 * ## 동작/계약
 * - [path]는 가변 인자로 전달되며 빈 배열도 허용됩니다.
 * - [optional]은 Dialect별 추가 옵션으로 그대로 전달됩니다.
 * - 반환값은 즉시 실행되지 않는 SQL 조건식 객체입니다.
 *
 * ```kotlin
 * val condition = table.payload.exists("$.profile", "$.name")
 * // condition is Exists
 * ```
 *
 * @param path 존재 여부를 검사할 JSON 경로 목록입니다.
 * @param optional 벤더 옵션 문자열입니다.
 */
fun ExpressionWithColumnType<*>.exists(vararg path: String, optional: String? = null): Exists =
    Exists(this, path = path, optional, columnType)
