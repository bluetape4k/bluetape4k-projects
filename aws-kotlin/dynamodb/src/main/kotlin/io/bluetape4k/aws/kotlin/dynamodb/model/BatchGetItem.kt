package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchGetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import io.bluetape4k.support.requireNotEmpty

inline fun batchGetItemRequestOf(
    requestItems: Map<String, KeysAndAttributes>,
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    crossinline builder: BatchGetItemRequest.Builder.() -> Unit = {},
): BatchGetItemRequest {
    requestItems.requireNotEmpty("requestItems")

    return BatchGetItemRequest {
        this.requestItems = requestItems
        this.returnConsumedCapacity = returnConsumedCapacity

        builder()
    }
}
