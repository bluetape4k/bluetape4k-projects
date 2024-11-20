package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnValue
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

@JvmName("putItemRequestOfAttributeValue")
inline fun putItemRequestOf(
    tableName: String,
    item: Map<String, AttributeValue>,
    returnValues: ReturnValue? = null,
    crossinline configurer: PutItemRequest.Builder.() -> Unit = {},
): PutItemRequest {
    tableName.requireNotBlank("tableName")
    item.requireNotEmpty("item")

    return PutItemRequest.invoke {
        this.tableName = tableName
        this.item = item
        this.returnValues = returnValues

        configurer()
    }
}

@JvmName("putItemRequestOfAny")
inline fun putItemRequestOf(
    tableName: String,
    item: Map<String, Any?>,
    returnValues: ReturnValue? = null,
    crossinline configurer: PutItemRequest.Builder.() -> Unit = {},
): PutItemRequest {
    return putItemRequestOf(
        tableName,
        item.mapValues { it.toAttributeValue() },
        returnValues,
        configurer,
    )
}
