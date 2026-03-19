package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.aws.dynamodb.model.toAttributeValue
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator
import software.amazon.awssdk.services.dynamodb.model.Condition

/**
 * DynamoDB 비교 조건을 [Condition]으로 변환하는 공통 계약입니다.
 */
@DynamoDslMarker
interface DynamoComparator {
    /** DSL 비교식을 AWS SDK [Condition]으로 변환합니다. */
    fun toCondition(): Condition
}

/**
 * 단일 우측 피연산자를 갖는 비교 연산자 계약입니다.
 */
@DynamoDslMarker
interface SingleValueDynamoComparator: DynamoComparator {
    /** 비교식의 우측 값입니다. */
    val right: Any
}

/** `SortKey`가 사용할 비교식 마커 인터페이스입니다. */
@DynamoDslMarker
interface ComparableBuilder

/**
 * [Condition.Builder] DSL을 이용해 [Condition]을 생성합니다.
 *
 * ```kotlin
 * val condition = Condition {
 *     comparisonOperator(ComparisonOperator.EQ)
 * }
 *
 * check(condition.comparisonOperator() == ComparisonOperator.EQ)
 * ```
 */
inline fun Condition(builder: Condition.Builder.() -> Unit): Condition {
    return Condition.builder().apply(builder).build()
}

/** `BEGINS_WITH` 비교식입니다. */
@DynamoDslMarker
class BeginsWith(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.BEGINS_WITH)
        attributeValueList(right.toAttributeValue())
    }
}

/** `EQ` 비교식입니다. */
@DynamoDslMarker
class Equals(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.EQ)
        attributeValueList(right.toAttributeValue())
    }
}

/** `NE` 비교식입니다. */
@DynamoDslMarker
class NotEquals(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.NE)
        attributeValueList(right.toAttributeValue())
    }
}

/** `GT` 비교식입니다. */
@DynamoDslMarker
class GreaterThan(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.GT)
        attributeValueList(right.toAttributeValue())
    }
}

/** `GE` 비교식입니다. */
@DynamoDslMarker
class GreaterThanOrEquals(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.GE)
        attributeValueList(right.toAttributeValue())
    }
}

/** `LT` 비교식입니다. */
@DynamoDslMarker
class LessThan(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.LT)
        attributeValueList(right.toAttributeValue())
    }
}

/** `LE` 비교식입니다. */
@DynamoDslMarker
class LessThanOrEquals(override val right: Any): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.LE)
        attributeValueList(right.toAttributeValue())
    }
}

/** `IN` 비교식입니다. */
@DynamoDslMarker
class InList(override val right: List<Any>): SingleValueDynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.IN)
        attributeValueList(right.toAttributeValue())
    }
}

/** `BETWEEN` 비교식입니다. */
@DynamoDslMarker
class Between(val left: Any, val right: Any): DynamoComparator {
    override fun toCondition(): Condition = Condition {
        comparisonOperator(ComparisonOperator.BETWEEN)
        attributeValueList(left.toAttributeValue(), right.toAttributeValue())
    }
}
