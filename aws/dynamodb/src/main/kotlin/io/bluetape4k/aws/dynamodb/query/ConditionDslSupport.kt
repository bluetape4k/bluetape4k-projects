package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.aws.dynamodb.model.toAttributeValue
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator
import software.amazon.awssdk.services.dynamodb.model.Condition

@DynamoDslMarker
interface DynamoComparator {
    fun toCondition(): Condition
}

@DynamoDslMarker
interface SingleValueDynamoComparator: DynamoComparator {
    val right: Any
}

@DynamoDslMarker
interface ComparableBuilder

inline fun Condition(@BuilderInference builder: Condition.Builder.() -> Unit): Condition {
    return Condition.builder().apply(builder).build()
}

@DynamoDslMarker
class BeginsWith(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.BEGINS_WITH)
        attributeValueList(right.toAttributeValue())
    }
}

@DynamoDslMarker
class Equals(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.EQ)
        attributeValueList(right.toAttributeValue())
    }
}

@DynamoDslMarker
class NotEquals(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.NE)
        attributeValueList(right.toAttributeValue())
    }
}

@DynamoDslMarker
class GreaterThan(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.GT)
        attributeValueList(right.toAttributeValue())
    }
}

@DynamoDslMarker
class GreaterThanOrEquals(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.GE)
        attributeValueList(right.toAttributeValue())
    }
}

@DynamoDslMarker
class LessThan(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.LT)
        attributeValueList(right.toAttributeValue())
    }
}

@DynamoDslMarker
class LessThanOrEquals(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.LE)
        attributeValueList(right.toAttributeValue())
    }
}

@DynamoDslMarker
class InList(override val right: List<Any>): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.IN)
        attributeValueList(right.toAttributeValue())
    }
}

@DynamoDslMarker
class Between(val left: Any, val right: Any): DynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.BETWEEN)
        attributeValueList(left.toAttributeValue(), right.toAttributeValue())
    }
}
