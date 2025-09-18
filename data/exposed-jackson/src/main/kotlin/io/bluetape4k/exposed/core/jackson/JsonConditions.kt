package io.bluetape4k.exposed.core.jackson

import org.jetbrains.exposed.v1.core.ComplexExpression
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.asLiteral
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.vendors.currentDialect


// Operator Classes

/**
 * JSON 컬럼에서 특정 값이 포함되어 있는지 검사하는 연산자입니다.
 *
 * @property target 검사 대상이 되는 JSON 컬럼 Expression
 * @property candidate 포함 여부를 검사할 값 Expression
 * @property path JSON Path (선택 사항)
 * @property jsonType JSON 컬럼 타입
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
 * JSON 컬럼에서 지정한 경로의 값이 존재하는지 검사하는 연산자입니다.
 *
 * @property expression 검사 대상이 되는 JSON 컬럼 Expression
 * @property path JSON Path (가변 인자)
 * @property optional 옵션 값 (선택 사항)
 * @property jsonType JSON 컬럼 타입
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

// Extension Functions

/**
 * JSON 컬럼이 특정 값을 포함하는지 검사하는 확장 함수입니다.
 *
 * @receiver 검사 대상이 되는 JSON 컬럼 Expression
 * @param candidate 포함 여부를 검사할 값 Expression
 * @param path JSON Path (선택 사항)
 * @return Contains 연산자
 */
fun ExpressionWithColumnType<*>.contains(
    candidate: Expression<*>,
    path: String? = null,
): Contains =
    Contains(this, candidate, path, columnType)

/**
 * JSON 컬럼이 특정 값을 포함하는지 검사하는 확장 함수입니다.
 *
 * @receiver 검사 대상이 되는 JSON 컬럼 Expression
 * @param candidate 포함 여부를 검사할 값
 * @param path JSON Path (선택 사항)
 * @return Contains 연산자
 */
fun <T> ExpressionWithColumnType<*>.contains(
    candidate: T,
    path: String? = null,
): Contains = when (candidate) {
    // is Iterable<*>, is Array<*> -> Contains(this, stringLiteral(asLiteral(candidate).toString()), path, columnType)
    is String -> Contains(this, stringLiteral(candidate), path, columnType)
    else -> Contains(this, asLiteral(candidate), path, columnType)
}

/**
 * JSON 컬럼에서 지정한 경로의 값이 존재하는지 검사하는 확장 함수입니다.
 *
 * @receiver 검사 대상이 되는 JSON 컬럼 Expression
 * @param path JSON Path (가변 인자)
 * @param optional 옵션 값 (선택 사항)
 * @return Exists 연산자
 */
fun ExpressionWithColumnType<*>.exists(vararg path: String, optional: String? = null): Exists =
    Exists(this, path = path, optional, columnType)
