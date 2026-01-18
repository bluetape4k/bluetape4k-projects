package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ConditionCheck

@JvmName("conditionCheckOfAttributeValue")
inline fun conditionCheckOf(
    conditionExpression: String? = null,
    expressionAttributeNames: Map<String, String>? = null,
    expressionAttributeValues: Map<String, AttributeValue>? = null,
    key: Map<String, AttributeValue>? = null,
    crossinline builder: ConditionCheck.Builder.() -> Unit = {},
): ConditionCheck {

    return ConditionCheck {
        this.conditionExpression = conditionExpression
        this.expressionAttributeNames = expressionAttributeNames
        this.expressionAttributeValues = expressionAttributeValues
        this.key = key

        builder()
    }
}

@JvmName("conditionCheckOfAny")
inline fun conditionCheckOf(
    conditionExpression: String? = null,
    expressionAttributeNames: Map<String, String>? = null,
    expressionAttributeValues: Map<String, Any?>? = null,
    key: Map<String, Any?>? = null,
    crossinline builder: ConditionCheck.Builder.() -> Unit = {},
): ConditionCheck {

    return ConditionCheck {
        this.conditionExpression = conditionExpression
        this.expressionAttributeNames = expressionAttributeNames
        this.expressionAttributeValues = expressionAttributeValues?.mapValues { it.toAttributeValue() }
        this.key = key?.mapValues { it.toAttributeValue() }

        builder()
    }
}
