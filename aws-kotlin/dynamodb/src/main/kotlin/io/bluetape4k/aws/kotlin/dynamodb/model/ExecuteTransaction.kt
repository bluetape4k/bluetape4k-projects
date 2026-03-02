package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ExecuteTransactionRequest
import aws.sdk.kotlin.services.dynamodb.model.ParameterizedStatement
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import io.bluetape4k.support.requireNotEmpty

/**
 * DSL 블록으로 DynamoDB [ExecuteTransactionRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [transactionStatements]가 비어 있으면 `IllegalArgumentException`을 던진다.
 * - [clientRequestToken]은 중복 요청 방지를 위한 멱등성 토큰으로, null이면 설정되지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = executeTransactionRequestOf(
 *     transactionStatements = listOf(
 *         ParameterizedStatement { statement = "UPDATE users SET name = ? WHERE id = ?" }
 *     )
 * )
 * // req.transactStatements?.size == 1
 * ```
 *
 * @param transactionStatements 트랜잭션으로 실행할 [ParameterizedStatement] 목록 (비어 있으면 예외)
 * @param clientRequestToken 중복 요청 방지를 위한 멱등성 토큰
 * @param returnConsumedCapacity 소비된 용량 반환 여부
 */
inline fun executeTransactionRequestOf(
    transactionStatements: List<ParameterizedStatement>,
    clientRequestToken: String? = null,
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    @BuilderInference crossinline builder: ExecuteTransactionRequest.Builder.() -> Unit = {},
): ExecuteTransactionRequest {
    transactionStatements.requireNotEmpty("transactionStatements")

    return ExecuteTransactionRequest {
        this.transactStatements = transactionStatements
        this.clientRequestToken = clientRequestToken
        this.returnConsumedCapacity = returnConsumedCapacity

        builder()
    }
}
