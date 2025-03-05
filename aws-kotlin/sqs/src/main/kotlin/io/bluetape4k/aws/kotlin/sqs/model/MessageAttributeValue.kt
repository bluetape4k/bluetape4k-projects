package io.bluetape4k.aws.kotlin.sqs.model

import aws.sdk.kotlin.services.sqs.model.MessageAttributeValue

@JvmName("messageAttributeValueOfNullableString")
inline fun messageAttributeValueOf(
    value: String?,
    crossinline configurer: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        stringValue = value
        configurer()
    }

@JvmName("messageAttributeValueOfNullableStringList")
inline fun messageAttributeValueOf(
    values: List<String>?,
    crossinline configurer: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        stringListValues = values

        configurer()
    }

@JvmName("messageAttributeValueOfNullableByteArray")
inline fun messageAttributeValueOf(
    value: ByteArray?,
    crossinline configurer: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        binaryValue = value

        configurer()
    }

@JvmName("messageAttributeValueOfNullableByteArrayList")
inline fun messageAttributeValueOf(
    values: List<ByteArray>?,
    crossinline configurer: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        binaryListValues = values

        configurer()
    }
