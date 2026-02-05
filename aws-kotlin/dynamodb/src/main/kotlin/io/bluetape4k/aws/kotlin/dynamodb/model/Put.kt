package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Put
import aws.sdk.kotlin.services.dynamodb.model.PutRequest
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

@JvmName("putOfAttributeValue")
fun putOf(
    tableName: String,
    item: Map<String, AttributeValue>,
    @BuilderInference builder: Put.Builder.() -> Unit = {},
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
fun putOf(
    tableName: String,
    item: Map<String, Any?>,
    @BuilderInference builder: Put.Builder.() -> Unit = {},
): Put =
    putOf(tableName, item.mapValues { it.toAttributeValue() }, builder)

@JvmName("putRequestOfAttributeValue")
fun putRequestOf(
    item: Map<String, AttributeValue>,
    @BuilderInference builder: PutRequest.Builder.() -> Unit = {},
): PutRequest {
    item.requireNotEmpty("item")

    return PutRequest.invoke {
        this.item = item
        builder()
    }
}

@JvmName("putRequestOfAny")
fun putRequestOf(
    item: Map<String, Any?>,
    @BuilderInference builder: PutRequest.Builder.() -> Unit = {},
): PutRequest =
    putRequestOf(item.mapValues { it.toAttributeValue() }, builder)
