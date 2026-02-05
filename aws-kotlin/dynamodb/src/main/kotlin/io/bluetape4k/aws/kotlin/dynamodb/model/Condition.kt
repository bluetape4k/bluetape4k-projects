package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ComparisonOperator
import aws.sdk.kotlin.services.dynamodb.model.Condition

@JvmName("conditionOfAttributeValue")
fun conditionOf(
    comparisonOperator: ComparisonOperator,
    attributeValueList: List<AttributeValue>,
    @BuilderInference builder: Condition.Builder.() -> Unit = {},
): Condition {

    return Condition {
        this.comparisonOperator = comparisonOperator
        this.attributeValueList = attributeValueList

        builder()
    }
}

@JvmName("conditionOfAny")
fun conditionOf(
    comparisonOperator: ComparisonOperator,
    attributeValueList: List<Any?>,
    @BuilderInference builder: Condition.Builder.() -> Unit = {},
): Condition {

    return Condition {
        this.comparisonOperator = comparisonOperator
        this.attributeValueList = attributeValueList.map { it.toAttributeValue() }

        builder()
    }
}
