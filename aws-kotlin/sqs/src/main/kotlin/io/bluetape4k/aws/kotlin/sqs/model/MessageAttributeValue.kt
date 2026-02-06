package io.bluetape4k.aws.kotlin.sqs.model

import aws.sdk.kotlin.services.sqs.model.MessageAttributeValue

@JvmName("messageAttributeValueOfNullableString")
inline fun messageAttributeValueOf(
    value: String?,
    @BuilderInference crossinline builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        stringValue = value
        builder()
    }

@JvmName("messageAttributeValueOfNullableStringList")
inline fun messageAttributeValueOf(
    values: List<String>?,
    @BuilderInference crossinline builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        stringListValues = values

        builder()
    }

@JvmName("messageAttributeValueOfNullableByteArray")
inline fun messageAttributeValueOf(
    value: ByteArray?,
    @BuilderInference crossinline builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        binaryValue = value

        builder()
    }

@JvmName("messageAttributeValueOfNullableByteArrayList")
inline fun messageAttributeValueOf(
    values: List<ByteArray>?,
    @BuilderInference crossinline builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        binaryListValues = values

        builder()
    }
