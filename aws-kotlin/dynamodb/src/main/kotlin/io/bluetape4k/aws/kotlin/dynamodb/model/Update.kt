package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Update
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

@JvmName("updateOfAttributeValue")
fun updateOf(
    tableName: String,
    key: Map<String, AttributeValue>,
    updateExpression: String,
    expressionAttributeValues: Map<String, AttributeValue>,
    expressionAttributeNames: Map<String, String>? = null,
    conditionExpression: String? = null,
    @BuilderInference builder: Update.Builder.() -> Unit = {},
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
fun updateOf(
    tableName: String,
    key: Map<String, Any?>,
    updateExpression: String,
    expressionAttributeValues: Map<String, AttributeValue>,
    expressionAttributeNames: Map<String, String>? = null,
    conditionExpression: String? = null,
    @BuilderInference builder: Update.Builder.() -> Unit = {},
): Update {
    return updateOf(
        tableName,
        key.mapValues { it.toAttributeValue() },
        updateExpression,
        expressionAttributeValues,
        expressionAttributeNames,
        conditionExpression,
        builder
    )
}
