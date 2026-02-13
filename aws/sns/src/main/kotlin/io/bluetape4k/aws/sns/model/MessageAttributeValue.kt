package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.model.MessageAttributeValue

inline fun messageAttributeValue(
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit,
): MessageAttributeValue =
    MessageAttributeValue.builder().apply(builder).build()

inline fun messageAttributeValueOf(
    valueAsString: String,
    dataType: String = "String",
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue {
    valueAsString.requireNotBlank("valueAsString")

    return messageAttributeValue {
        stringValue(valueAsString)
        dataType(dataType)

        builder()
    }
}

inline fun String.toMessageAttributeValue(
    dataType: String = "String",
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    messageAttributeValueOf(this, dataType, builder)
