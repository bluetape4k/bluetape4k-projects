package io.bluetape4k.aws.kotlin.sqs.model

import aws.sdk.kotlin.services.sqs.model.MessageAttributeValue

@JvmName("messageAttributeValueOfNullableString")
fun messageAttributeValueOf(value: String?): MessageAttributeValue =
    MessageAttributeValue {
        stringValue = value
    }

@JvmName("messageAttributeValueOfNullableStringList")
fun messageAttributeValueOf(values: List<String>?): MessageAttributeValue =
    MessageAttributeValue {
        stringListValues = values
    }

@JvmName("messageAttributeValueOfNullableByteArray")
fun messageAttributeValueOf(value: ByteArray?): MessageAttributeValue =
    MessageAttributeValue {
        binaryValue = value
    }

@JvmName("messageAttributeValueOfNullableByteArrayList")
fun messageAttributeValueOf(values: List<ByteArray>?): MessageAttributeValue =
    MessageAttributeValue {
        binaryListValues = values
    }
