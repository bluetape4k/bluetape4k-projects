package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ComparisonOperator
import aws.sdk.kotlin.services.dynamodb.model.Condition

@JvmName("conditionOfAttributeValue")
inline fun conditionOf(
    comparisonOperator: ComparisonOperator,
    attributeValueList: List<AttributeValue>,
    crossinline configurer: Condition.Builder.() -> Unit = {},
): Condition {

    return Condition.invoke {
        this.comparisonOperator = comparisonOperator
        this.attributeValueList = attributeValueList

        configurer()
    }
}

@JvmName("conditionOfAny")
inline fun conditionOf(
    comparisonOperator: ComparisonOperator,
    attributeValueList: List<Any?>,
    crossinline configurer: Condition.Builder.() -> Unit = {},
): Condition {

    return Condition.invoke {
        this.comparisonOperator = comparisonOperator
        this.attributeValueList = attributeValueList.map { it.toAttributeValue() }

        configurer()
    }
}
