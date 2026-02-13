package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Get
import io.bluetape4k.support.requireNotBlank

@JvmName("getOfAttributeValue")
inline fun getOf(
    tableName: String,
    key: Map<String, AttributeValue>? = null,
    expressionAttributeNames: Map<String, String>? = null,
    projectionExpression: String? = null,
    @BuilderInference crossinline builder: Get.Builder.() -> Unit = {},
): Get {
    tableName.requireNotBlank("tableName")

    return Get {
        this.tableName = tableName
        this.key = key
        this.expressionAttributeNames = expressionAttributeNames
        this.projectionExpression = projectionExpression

        builder()
    }
}

@JvmName("getOfAny")
inline fun getOf(
    tableName: String,
    key: Map<String, Any?>? = null,
    expressionAttributeNames: Map<String, String>? = null,
    projectionExpression: String? = null,
    @BuilderInference crossinline builder: Get.Builder.() -> Unit = {},
): Get = getOf(
    tableName,
    key?.toAttributeValueMap(),
    expressionAttributeNames,
    projectionExpression,
    builder,
)
