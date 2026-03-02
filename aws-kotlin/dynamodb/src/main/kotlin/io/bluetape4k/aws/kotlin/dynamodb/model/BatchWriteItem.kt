package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchWriteItemRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest
import io.bluetape4k.support.requireNotEmpty

/**
 * DSL 블록으로 DynamoDB [BatchWriteItemRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [requestItems]가 비어 있으면 `IllegalArgumentException`을 던진다.
 * - 각 테이블에 대해 쓰기(Put/Delete) 요청 목록을 [WriteRequest]로 지정한다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = batchWriteItemRequestOf(
 *     requestItems = mapOf("users" to listOf(writePutRequestOf(mapOf("id" to "u1"))))
 * )
 * // req.requestItems?.size == 1
 * ```
 *
 * @param requestItems 테이블 이름과 쓰기 요청 목록의 매핑 (비어 있으면 예외)
 * @param returnConsumedCapacity 소비된 용량 반환 여부
 */
inline fun batchWriteItemRequestOf(
    requestItems: Map<String, List<WriteRequest>>,
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    @BuilderInference crossinline builder: BatchWriteItemRequest.Builder.() -> Unit = {},
): BatchWriteItemRequest {
    requestItems.requireNotEmpty("requestItems")

    return BatchWriteItemRequest {
        this.requestItems = requestItems
        this.returnConsumedCapacity = returnConsumedCapacity

        builder()
    }
}
