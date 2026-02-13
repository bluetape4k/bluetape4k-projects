package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeAction
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.AttributeValueUpdate

inline fun <T> attributeValueUpdateOf(
    value: T,
    action: AttributeAction,
    @BuilderInference crossinline builder: AttributeValueUpdate.Builder.() -> Unit = {},
): AttributeValueUpdate =
    attributeValueUpdateOf(value.toAttributeValue(), action, builder)

inline fun attributeValueUpdateOf(
    value: AttributeValue,
    action: AttributeAction,
    @BuilderInference crossinline builder: AttributeValueUpdate.Builder.() -> Unit = {},
): AttributeValueUpdate = AttributeValueUpdate {
    this.value = value
    this.action = action

    builder()
}
