package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest

inline fun getItemRequestOf(
    attributesToGet: List<String>,
    consistentRead: Boolean? = null,
    expressionAttributeNames: Map<String, String>? = null,
    key: Map<String, AttributeValue>? = null,
    crossinline configurer: GetItemRequest.Builder.() -> Unit = {},
): GetItemRequest {

    return GetItemRequest.invoke {
        this.attributesToGet = attributesToGet
        this.consistentRead = consistentRead
        this.expressionAttributeNames = expressionAttributeNames
        this.key = key

        configurer()
    }
}
