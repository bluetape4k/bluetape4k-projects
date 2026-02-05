package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchWriteItemRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest
import io.bluetape4k.support.requireNotEmpty

fun batchWriteItemRequestOf(
    requestItems: Map<String, List<WriteRequest>>,
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    @BuilderInference builder: BatchWriteItemRequest.Builder.() -> Unit = {},
): BatchWriteItemRequest {
    requestItems.requireNotEmpty("requestItems")

    return BatchWriteItemRequest {
        this.requestItems = requestItems
        this.returnConsumedCapacity = returnConsumedCapacity

        builder()
    }
}
