package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Update
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

@JvmName("updateOfAttributeValue")
inline fun updateOf(
    tableName: String,
    key: Map<String, AttributeValue>,
    updateExpression: String,
    expressionAttributeValues: Map<String, AttributeValue>,
    expressionAttributeNames: Map<String, String>? = null,
    conditionExpression: String? = null,
    @BuilderInference crossinline builder: Update.Builder.() -> Unit = {},
): Update {
    tableName.requireNotBlank("tableName")
    key.requireNotEmpty("key")
    updateExpression.requireNotBlank("updateExpression")

    return Update {
        this.tableName = tableName
        this.key = key
        this.updateExpression = updateExpression
        this.expressionAttributeValues = expressionAttributeValues

        this.expressionAttributeNames = expressionAttributeNames
        this.conditionExpression = conditionExpression

        builder()
    }
}

@JvmName("updateOfAny")
inline fun updateOf(
    tableName: String,
    key: Map<String, Any?>,
    updateExpression: String,
    expressionAttributeValues: Map<String, AttributeValue>,
    expressionAttributeNames: Map<String, String>? = null,
    conditionExpression: String? = null,
    @BuilderInference crossinline builder: Update.Builder.() -> Unit = {},
): Update {
    return updateOf(
        tableName,
        key.toAttributeValueMap(),
        updateExpression,
        expressionAttributeValues,
        expressionAttributeNames,
        conditionExpression,
        builder
    )
}
