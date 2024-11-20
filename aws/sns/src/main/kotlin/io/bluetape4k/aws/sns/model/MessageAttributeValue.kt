package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.services.sns.model.MessageAttributeValue

inline fun MessageAttributeValue(
    initializer: MessageAttributeValue.Builder.() -> Unit,
): MessageAttributeValue =
    MessageAttributeValue.builder().apply(initializer).build()

fun messageAttributeValueOf(
    valueAsString: String,
    dataType: String = "String",
    initializer: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue = MessageAttributeValue {
    stringValue(valueAsString)
    dataType(dataType)

    initializer()
}


fun String.toMessageAttributeValue(
    dataType: String = "String",
    initializer: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    messageAttributeValueOf(this, dataType, initializer)
