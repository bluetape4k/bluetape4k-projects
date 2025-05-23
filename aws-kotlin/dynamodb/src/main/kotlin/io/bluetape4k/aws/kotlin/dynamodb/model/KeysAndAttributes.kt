package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes
import io.bluetape4k.support.requireNotEmpty

@JvmName("keysAndAttributesOfAttributeValue")
inline fun keysAndAttributesOf(
    keys: List<Map<String, AttributeValue>>,
    crossinline configurer: KeysAndAttributes.Builder.() -> Unit = {},
): KeysAndAttributes {
    keys.requireNotEmpty("keys")

    return KeysAndAttributes {
        this.keys = keys
        configurer()
    }
}

@JvmName("keysAndAttributesOfAny")
inline fun keysAndAttributesOf(
    keys: List<Map<String, Any?>>,
    crossinline configurer: KeysAndAttributes.Builder.() -> Unit = {},
): KeysAndAttributes {
    keys.requireNotEmpty("keys")

    return KeysAndAttributes {
        this.keys = keys.map { it.mapValues { it.toAttributeValue() } }
        configurer()
    }
}
