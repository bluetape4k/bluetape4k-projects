package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes
import io.bluetape4k.support.requireNotEmpty

@JvmName("keysAndAttributesOfAttributeValue")
fun keysAndAttributesOf(
    keys: List<Map<String, AttributeValue>>,
    @BuilderInference builder: KeysAndAttributes.Builder.() -> Unit = {},
): KeysAndAttributes {
    keys.requireNotEmpty("keys")

    return KeysAndAttributes {
        this.keys = keys

        builder()
    }
}

@JvmName("keysAndAttributesOfAny")
fun keysAndAttributesOf(
    keys: List<Map<String, Any?>>,
    @BuilderInference builder: KeysAndAttributes.Builder.() -> Unit = {},
): KeysAndAttributes {
    keys.requireNotEmpty("keys")

    return KeysAndAttributes {
        this.keys = keys.map { it.mapValues { it.toAttributeValue() } }

        builder()
    }
}
