package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Put
import aws.sdk.kotlin.services.dynamodb.model.PutRequest
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

@JvmName("putOfAttributeValue")
inline fun putOf(
    tableName: String,
    item: Map<String, AttributeValue>,
    crossinline configurer: Put.Builder.() -> Unit = {},
): Put {
    tableName.requireNotBlank("tableName")
    item.requireNotEmpty("item")

    return Put {
        this.tableName = tableName
        this.item = item

        configurer()
    }
}

@JvmName("putOfAny")
inline fun putOf(
    tableName: String,
    item: Map<String, Any?>,
    crossinline configurer: Put.Builder.() -> Unit = {},
): Put {
    return putOf(tableName, item.mapValues { it.toAttributeValue() }, configurer)
}


@JvmName("putRequestOfAttributeValue")
inline fun putRequestOf(
    item: Map<String, AttributeValue>,
    crossinline configurer: PutRequest.Builder.() -> Unit = {},
): PutRequest {
    item.requireNotEmpty("item")

    return PutRequest.invoke {
        this.item = item
        configurer()
    }
}

@JvmName("putRequestOfAny")
inline fun putRequestOf(
    item: Map<String, Any?>,
    crossinline configurer: PutRequest.Builder.() -> Unit = {},
): PutRequest {
    return putRequestOf(item.mapValues { it.toAttributeValue() }, configurer)
}
