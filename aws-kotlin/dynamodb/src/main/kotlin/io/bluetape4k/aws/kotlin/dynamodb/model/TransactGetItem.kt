package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Get
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import aws.sdk.kotlin.services.dynamodb.model.TransactGetItem
import aws.sdk.kotlin.services.dynamodb.model.TransactGetItemsRequest
import io.bluetape4k.support.requireNotEmpty

fun transactGetItemOf(get: Get): TransactGetItem = TransactGetItem {
    this.get = get
}


inline fun transactGetItemsRequestOf(
    transactItems: List<TransactGetItem>,
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    crossinline configurer: TransactGetItemsRequest.Builder.() -> Unit = {},
): TransactGetItemsRequest {
    transactItems.requireNotEmpty("transactItems")

    return TransactGetItemsRequest {
        this.transactItems = transactItems
        this.returnConsumedCapacity = returnConsumedCapacity
        configurer()
    }
}
