package io.bluetape4k.aws.kotlin.sqs.model

import aws.sdk.kotlin.services.sqs.model.MessageAttributeValue

@JvmName("messageAttributeValueOfNullableString")
fun messageAttributeValueOf(
    value: String?,
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        stringValue = value
        builder()
    }

@JvmName("messageAttributeValueOfNullableStringList")
fun messageAttributeValueOf(
    values: List<String>?,
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        stringListValues = values

        builder()
    }

@JvmName("messageAttributeValueOfNullableByteArray")
fun messageAttributeValueOf(
    value: ByteArray?,
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        binaryValue = value

        builder()
    }

@JvmName("messageAttributeValueOfNullableByteArrayList")
fun messageAttributeValueOf(
    values: List<ByteArray>?,
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        binaryListValues = values

        builder()
    }
