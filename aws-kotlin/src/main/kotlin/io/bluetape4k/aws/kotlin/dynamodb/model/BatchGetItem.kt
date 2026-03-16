package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchGetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import io.bluetape4k.support.requireNotEmpty

/**
 * DSL 블록으로 DynamoDB [BatchGetItemRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [requestItems]가 비어 있으면 `IllegalArgumentException`을 던진다.
 * - 각 테이블에 대해 가져올 키 집합을 [KeysAndAttributes]로 지정한다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = batchGetItemRequestOf(
 *     requestItems = mapOf("users" to keysAndAttributesOf(listOf(mapOf("id" to AttributeValue.S("u1")))))
 * )
 * // req.requestItems?.size == 1
 * ```
 *
 * @param requestItems 테이블 이름과 읽을 키 집합의 매핑 (비어 있으면 예외)
 * @param returnConsumedCapacity 소비된 용량 반환 여부
 */
inline fun batchGetItemRequestOf(
    requestItems: Map<String, KeysAndAttributes>,
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    @BuilderInference crossinline builder: BatchGetItemRequest.Builder.() -> Unit = {},
): BatchGetItemRequest {
    requestItems.requireNotEmpty("requestItems")

    return BatchGetItemRequest {
        this.requestItems = requestItems
        this.returnConsumedCapacity = returnConsumedCapacity

        builder()
    }
}
