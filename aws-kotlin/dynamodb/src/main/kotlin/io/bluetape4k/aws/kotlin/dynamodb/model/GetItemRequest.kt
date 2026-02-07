package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest

inline fun getItemRequestOf(
    attributesToGet: List<String>? = null,
    consistentRead: Boolean? = null,
    expressionAttributeNames: Map<String, String>? = null,
    key: Map<String, AttributeValue>? = null,
    @BuilderInference crossinline builder: GetItemRequest.Builder.() -> Unit = {},
): GetItemRequest {

    return GetItemRequest {
        attributesToGet?.let { this.attributesToGet = it }
        consistentRead?.let { this.consistentRead = it }
        expressionAttributeNames?.let { this.expressionAttributeNames = it }
        key?.let { this.key = it }

        builder()
    }
}
