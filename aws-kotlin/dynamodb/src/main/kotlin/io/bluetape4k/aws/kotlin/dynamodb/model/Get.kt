package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Get
import io.bluetape4k.support.requireNotBlank

@JvmName("getOfAttributeValue")
fun getOf(
    tableName: String,
    key: Map<String, AttributeValue>? = null,
    expressionAttributeNames: Map<String, String>? = null,
    projectionExpression: String? = null,
    @BuilderInference builder: Get.Builder.() -> Unit = {},
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
fun getOf(
    tableName: String,
    key: Map<String, Any?>? = null,
    expressionAttributeNames: Map<String, String>? = null,
    projectionExpression: String? = null,
    @BuilderInference builder: Get.Builder.() -> Unit = {},
): Get {
    return Get {
        this.tableName = tableName
        this.key = key?.mapValues { it.toAttributeValue() }
        this.expressionAttributeNames = expressionAttributeNames
        this.projectionExpression = projectionExpression

        builder()
    }
}
