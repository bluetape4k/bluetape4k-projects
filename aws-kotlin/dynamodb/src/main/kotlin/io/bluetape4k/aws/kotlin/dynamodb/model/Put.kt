@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Put
import aws.sdk.kotlin.services.dynamodb.model.PutRequest
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

@JvmName("putOfAttributeValue")
inline fun putOf(
    tableName: String,
    item: Map<String, AttributeValue>? = null,
    @BuilderInference crossinline builder: Put.Builder.() -> Unit = {},
): Put {
    tableName.requireNotBlank("tableName")
    item.requireNotEmpty("item")

    return Put {
        this.tableName = tableName
        this.item = item

        builder()
    }
}

@JvmName("putOfAny")
inline fun putOf(
    tableName: String,
    item: Map<String, Any?>? = null,
    @BuilderInference crossinline builder: Put.Builder.() -> Unit = {},
): Put =
    putOf(tableName, item?.toAttributeValueMap(), builder)

@JvmName("putRequestOfAttributeValue")
inline fun putRequestOf(
    item: Map<String, AttributeValue>,
): PutRequest = PutRequest {
    this.item = item
}

@JvmName("putRequestOfAny")
inline fun putRequestOf(
    item: Map<String, Any?>,
    @BuilderInference crossinline builder: PutRequest.Builder.() -> Unit = {},
): PutRequest =
    putRequestOf(item.toAttributeValueMap())
