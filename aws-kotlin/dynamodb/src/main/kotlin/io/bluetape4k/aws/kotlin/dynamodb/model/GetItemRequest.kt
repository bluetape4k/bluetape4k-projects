package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest

@JvmName("getItemRequestOfString")
inline fun getItemRequestOf(
    attributesToGet: List<String>? = null,
    consistentRead: Boolean? = null,
    expressionAttributeNames: Map<String, String>? = null,
    key: Map<String, AttributeValue>? = null,
    @BuilderInference crossinline builder: GetItemRequest.Builder.() -> Unit = {},
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
    @BuilderInference crossinline builder: GetItemRequest.Builder.() -> Unit = {},
): GetItemRequest = getItemRequestOf(
    attributesToGet,
    consistentRead,
    expressionAttributeNames,
    key?.toAttributeValueMap(),
    builder
)
