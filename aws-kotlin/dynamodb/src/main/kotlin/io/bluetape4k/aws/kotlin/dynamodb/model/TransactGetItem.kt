package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Get
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import aws.sdk.kotlin.services.dynamodb.model.TransactGetItem
import aws.sdk.kotlin.services.dynamodb.model.TransactGetItemsRequest
import io.bluetape4k.support.requireNotEmpty

fun transactGetItemOf(get: Get): TransactGetItem =
    TransactGetItem {
        this.get = get
    }

inline fun transactGetItemOf(
    tableName: String,
    key: Map<String, KeysAndAttributes> = emptyMap(),
    expressionAttributeNames: Map<String, String>? = null,
    projectionExpression: String? = null,
    crossinline builder: Get.Builder.() -> Unit,
): TransactGetItem =
    transactGetItemOf(getOf(tableName, key, expressionAttributeNames, projectionExpression, builder))

inline fun transactGetItemsRequestOf(
    transactItems: List<TransactGetItem>,
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    @BuilderInference crossinline builder: TransactGetItemsRequest.Builder.() -> Unit = {},
): TransactGetItemsRequest {
    transactItems.requireNotEmpty("transactItems")

    return TransactGetItemsRequest {
        this.transactItems = transactItems
        this.returnConsumedCapacity = returnConsumedCapacity

        builder()
    }
}
