package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ExecuteStatementRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [ExecuteStatementRequest]를 빌드합니다 ([AttributeValue] 파라미터 오버로드).
 *
 * ## 동작/계약
 * - [statement]가 blank이면 `IllegalArgumentException`을 던진다.
 * - [parameters]는 PartiQL 구문의 바인딩 파라미터로 순서대로 매핑된다.
 * - [nextToken]을 통해 페이지네이션을 지원한다.
 *
 * ```kotlin
 * val req = executeStatementRequestOf(
 *     statement = "SELECT * FROM users WHERE id = ?",
 *     parameters = listOf(AttributeValue.S("u1")),
 *     limit = 10
 * )
 * // req.statement == "SELECT * FROM users WHERE id = ?"
 * // req.limit == 10
 * ```
 *
 * @param statement 실행할 PartiQL 구문 (blank이면 예외)
 * @param parameters PartiQL 바인딩 파라미터 목록
 * @param consistentRead 강력한 일관성 읽기 사용 여부
 * @param limit 반환할 최대 항목 수
 * @param nextToken 페이지네이션 토큰
 */
@JvmName("executeStatementRequestOfAttributeValue")
inline fun executeStatementRequestOf(
    statement: String,
    parameters: List<AttributeValue>?,
    consistentRead: Boolean? = null,
    limit: Int? = null,
    nextToken: String? = null,
    crossinline builder: ExecuteStatementRequest.Builder.() -> Unit = {},
): ExecuteStatementRequest {
    statement.requireNotBlank("statement")

    return ExecuteStatementRequest {
        this.statement = statement
        this.parameters = parameters
        this.consistentRead = consistentRead
        this.limit = limit
        this.nextToken = nextToken

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [ExecuteStatementRequest]를 빌드합니다 (Any? 파라미터 오버로드).
 *
 * ## 동작/계약
 * - [statement]가 blank이면 `IllegalArgumentException`을 던진다.
 * - [parameters]의 각 원소는 [toAttributeValueList]를 통해 [AttributeValue]로 자동 변환된다.
 * - [nextToken]을 통해 페이지네이션을 지원한다.
 *
 * ```kotlin
 * val req = executeStatementRequestOf(
 *     statement = "SELECT * FROM users WHERE id = ?",
 *     parameters = listOf("u1"),
 *     limit = 10
 * )
 * // req.parameters?.first() == AttributeValue.S("u1")
 * ```
 *
 * @param statement 실행할 PartiQL 구문 (blank이면 예외)
 * @param parameters PartiQL 바인딩 파라미터 목록 (자동으로 [AttributeValue]로 변환)
 * @param consistentRead 강력한 일관성 읽기 사용 여부
 * @param limit 반환할 최대 항목 수
 * @param nextToken 페이지네이션 토큰
 */
@JvmName("executeStatementRequestOfAny")
inline fun executeStatementRequestOf(
    statement: String,
    parameters: List<Any?>?,
    consistentRead: Boolean? = null,
    limit: Int? = null,
    nextToken: String? = null,
    crossinline builder: ExecuteStatementRequest.Builder.() -> Unit = {},
): ExecuteStatementRequest = executeStatementRequestOf(
    statement,
    parameters?.toAttributeValueList(),
    consistentRead,
    limit,
    nextToken,
    builder,
)
