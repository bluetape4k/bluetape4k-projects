package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.BatchStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnValuesOnConditionCheckFailure
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [BatchStatementRequest]를 빌드합니다 ([AttributeValue] 파라미터 오버로드).
 *
 * ## 동작/계약
 * - [statement]가 blank이면 `IllegalArgumentException`을 던진다.
 * - [parameters]는 PartiQL 구문의 바인딩 파라미터로 순서대로 매핑된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = batchStatementRequestOf(
 *     statement = "SELECT * FROM users WHERE id = ?",
 *     parameters = listOf(AttributeValue.S("u1"))
 * )
 * // req.statement == "SELECT * FROM users WHERE id = ?"
 * ```
 *
 * @param statement 실행할 PartiQL 구문 (blank이면 예외)
 * @param parameters PartiQL 바인딩 파라미터 목록
 * @param consistentRead 강력한 일관성 읽기 사용 여부
 * @param returnValuesOnConditionCheckFailure 조건 검사 실패 시 반환할 항목 설정
 */
@JvmName("batchStatementRequestOfAttributeValue")
inline fun batchStatementRequestOf(
    statement: String,
    parameters: List<AttributeValue>? = null,
    consistentRead: Boolean? = null,
    returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure? = null,
    @BuilderInference crossinline builder: BatchStatementRequest.Builder.() -> Unit = {},
): BatchStatementRequest {
    statement.requireNotBlank("statement")

    return BatchStatementRequest {
        this.statement = statement
        this.parameters = parameters
        this.consistentRead = consistentRead
        this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [BatchStatementRequest]를 빌드합니다 (Any? 파라미터 오버로드).
 *
 * ## 동작/계약
 * - [statement]가 blank이면 `IllegalArgumentException`을 던진다.
 * - [parameters]의 각 원소는 [toAttributeValue]를 통해 [AttributeValue]로 자동 변환된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = batchStatementRequestOf(
 *     statement = "SELECT * FROM users WHERE id = ?",
 *     parameters = listOf("u1")
 * )
 * // req.parameters?.first() == AttributeValue.S("u1")
 * ```
 *
 * @param statement 실행할 PartiQL 구문 (blank이면 예외)
 * @param parameters PartiQL 바인딩 파라미터 목록 (자동으로 [AttributeValue]로 변환)
 * @param consistentRead 강력한 일관성 읽기 사용 여부
 * @param returnValuesOnConditionCheckFailure 조건 검사 실패 시 반환할 항목 설정
 */
@JvmName("batchStatementRequestOfAny")
inline fun batchStatementRequestOf(
    statement: String,
    parameters: List<Any?>? = null,
    consistentRead: Boolean? = null,
    returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure? = null,
    @BuilderInference crossinline builder: BatchStatementRequest.Builder.() -> Unit = {},
): BatchStatementRequest {
    statement.requireNotBlank("statement")

    return BatchStatementRequest {
        this.statement = statement
        this.parameters = parameters?.map { it.toAttributeValue() }
        this.consistentRead = consistentRead
        this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure

        builder()
    }
}
