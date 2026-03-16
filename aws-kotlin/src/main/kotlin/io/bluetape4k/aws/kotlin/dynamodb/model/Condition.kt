package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ComparisonOperator
import aws.sdk.kotlin.services.dynamodb.model.Condition

/**
 * DSL 블록으로 DynamoDB [Condition]을 빌드합니다 ([AttributeValue] 리스트 오버로드).
 *
 * ## 동작/계약
 * - [comparisonOperator]와 [attributeValueList]는 필수 파라미터로 항상 설정된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val cond = conditionOf(ComparisonOperator.Eq, listOf(AttributeValue.S("active")))
 * // cond.comparisonOperator == ComparisonOperator.Eq
 * // cond.attributeValueList?.size == 1
 * ```
 *
 * @param comparisonOperator 비교 연산자
 * @param attributeValueList 비교에 사용할 [AttributeValue] 목록
 */
@JvmName("conditionOfAttributeValue")
inline fun conditionOf(
    comparisonOperator: ComparisonOperator,
    attributeValueList: List<AttributeValue>,
    crossinline builder: Condition.Builder.() -> Unit = {},
): Condition = Condition {
    this.comparisonOperator = comparisonOperator
    this.attributeValueList = attributeValueList

    builder()
}

/**
 * DSL 블록으로 DynamoDB [Condition]을 빌드합니다 (Any? 리스트 오버로드).
 *
 * ## 동작/계약
 * - [attributeValueList]의 각 원소는 [toAttributeValue]를 통해 [AttributeValue]로 자동 변환된다.
 * - [comparisonOperator]와 변환된 [attributeValueList]는 항상 설정된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val cond = conditionOf(ComparisonOperator.Eq, listOf("active"))
 * // cond.attributeValueList?.first() == AttributeValue.S("active")
 * ```
 *
 * @param comparisonOperator 비교 연산자
 * @param attributeValueList 비교에 사용할 값 목록 (자동으로 [AttributeValue]로 변환)
 */
@JvmName("conditionOfAny")
inline fun conditionOf(
    comparisonOperator: ComparisonOperator,
    attributeValueList: List<Any?>,
    crossinline builder: Condition.Builder.() -> Unit = {},
): Condition = Condition {
    this.comparisonOperator = comparisonOperator
    this.attributeValueList = attributeValueList.map { it.toAttributeValue() }

    builder()
}
