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
    @BuilderInference crossinline builder: PutItemRequest.Builder.() -> Unit = {},
): PutItemRequest {
    tableName.requireNotBlank("tableName")
    item.requireNotEmpty("item")

    return PutItemRequest {
        this.tableName = tableName
        this.item = item
        this.returnValues = returnValues

        builder()
    }
}

@JvmName("putItemRequestOfAny")
inline fun putItemRequestOf(
    tableName: String,
    item: Map<String, Any?>,
    returnValues: ReturnValue? = null,
    @BuilderInference crossinline builder: PutItemRequest.Builder.() -> Unit = {},
): PutItemRequest =
    putItemRequestOf(
        tableName,
        item.toAttributeValueMap(),
        returnValues,
        builder,
    )
