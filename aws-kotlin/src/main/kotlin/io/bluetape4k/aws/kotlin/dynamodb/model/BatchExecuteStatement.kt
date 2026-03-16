package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchExecuteStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.BatchStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import io.bluetape4k.support.ifTrue

/**
 * DSL 블록으로 DynamoDB [BatchExecuteStatementRequest]를 빌드합니다 (List 오버로드).
 *
 * ## 동작/계약
 * - [statements]가 null이면 요청 본문에 statements 필드가 설정되지 않는다.
 * - [returnConsumedCapacity]가 null이면 소비 용량 정보를 반환하지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = batchExecutionStatementRequestOf(
 *     returnConsumedCapacity = ReturnConsumedCapacity.Total,
 *     statements = listOf(batchStatementRequestOf("SELECT * FROM users WHERE id = ?", listOf("u1")))
 * )
 * // req.statements?.size == 1
 * ```
 *
 * @param returnConsumedCapacity 소비된 용량 반환 여부
 * @param statements 실행할 PartiQL 구문 목록
 */
@JvmName("batchExecutionStatementRequestOfList")
inline fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    statements: List<BatchStatementRequest>? = null,
    crossinline builder: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest {
        returnConsumedCapacity?.let { this.returnConsumedCapacity = it }
        statements?.let { this.statements = it }

        builder()
    }

/**
 * DSL 블록으로 DynamoDB [BatchExecuteStatementRequest]를 빌드합니다 (vararg 오버로드).
 *
 * ## 동작/계약
 * - [statements]가 비어 있으면 요청 본문에 statements 필드가 설정되지 않는다.
 * - [returnConsumedCapacity]가 null이면 소비 용량 정보를 반환하지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val stmt = batchStatementRequestOf("SELECT * FROM users WHERE id = ?", listOf("u1"))
 * val req = batchExecutionStatementRequestOf(statements = stmt)
 * // req.statements?.size == 1
 * ```
 *
 * @param returnConsumedCapacity 소비된 용량 반환 여부
 * @param statements 실행할 PartiQL 구문 (가변 인자)
 */
@JvmName("batchExecutionStatementRequestOfArray")
inline fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    vararg statements: BatchStatementRequest,
    crossinline builder: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest {
        returnConsumedCapacity?.let { this.returnConsumedCapacity = it }
        statements.isNotEmpty().ifTrue {
            this.statements = statements.toList()
        }

        builder()
    }
