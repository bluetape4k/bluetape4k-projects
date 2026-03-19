package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest

/**
 * DSL 블록으로 DynamoDB [GetItemRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [key]에 `Map<String, AttributeValue>` 형태의 파티션 키(+정렬 키)를 전달한다.
 * - 추가 설정은 [builder] 블록으로 확장할 수 있다.
 *
 * ```kotlin
 * val req = getItemRequestOf(key = mapOf("id" to AttributeValue.S("u1"))) {
 *     tableName = "users"
 * }
 * ```
 */
@JvmName("getItemRequestOfString")
inline fun getItemRequestOf(
    attributesToGet: List<String>? = null,
    consistentRead: Boolean? = null,
    expressionAttributeNames: Map<String, String>? = null,
    key: Map<String, AttributeValue>? = null,
    crossinline builder: GetItemRequest.Builder.() -> Unit = {},
): GetItemRequest {

    return GetItemRequest {
        this.attributesToGet = attributesToGet
        this.consistentRead = consistentRead
        this.expressionAttributeNames = expressionAttributeNames
        this.key = key

        builder()
    }
}

@JvmName("getItemRequestOfAny")
inline fun getItemRequestOf(
    attributesToGet: List<String>? = null,
    consistentRead: Boolean? = null,
    expressionAttributeNames: Map<String, String>? = null,
    key: Map<String, Any>? = null,
    crossinline builder: GetItemRequest.Builder.() -> Unit = {},
): GetItemRequest = getItemRequestOf(
    attributesToGet,
    consistentRead,
    expressionAttributeNames,
    key?.toAttributeValueMap(),
    builder
)
