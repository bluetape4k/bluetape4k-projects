package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ConditionCheck

/**
 * DSL 블록으로 DynamoDB [ConditionCheck]를 빌드합니다 ([AttributeValue] 맵 오버로드).
 *
 * ## 동작/계약
 * - [conditionExpression]은 조건 표현식 문자열로, null이면 설정되지 않는다.
 * - [key]와 [expressionAttributeValues]는 [AttributeValue] 맵을 직접 받는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val check = conditionCheckOf(
 *     conditionExpression = "attribute_exists(id)",
 *     key = mapOf("id" to AttributeValue.S("u1"))
 * )
 * // check.conditionExpression == "attribute_exists(id)"
 * ```
 *
 * @param conditionExpression 조건 표현식 문자열
 * @param expressionAttributeNames 표현식 속성 이름 치환 맵
 * @param expressionAttributeValues 표현식 속성 값 치환 맵
 * @param key 조건을 적용할 항목의 기본 키 맵
 */
@JvmName("conditionCheckOfAttributeValue")
inline fun conditionCheckOf(
    conditionExpression: String? = null,
    expressionAttributeNames: Map<String, String>? = null,
    expressionAttributeValues: Map<String, AttributeValue>? = null,
    key: Map<String, AttributeValue>? = null,
    crossinline builder: ConditionCheck.Builder.() -> Unit = {},
): ConditionCheck = ConditionCheck {
    this.conditionExpression = conditionExpression
    this.expressionAttributeNames = expressionAttributeNames
    this.expressionAttributeValues = expressionAttributeValues
    this.key = key

    builder()
}

/**
 * DSL 블록으로 DynamoDB [ConditionCheck]를 빌드합니다 (Any? 맵 오버로드).
 *
 * ## 동작/계약
 * - [expressionAttributeValues]와 [key]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 * - [conditionExpression]은 null이면 설정되지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val check = conditionCheckOf(
 *     conditionExpression = "attribute_exists(id)",
 *     key = mapOf("id" to "u1")
 * )
 * // check.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param conditionExpression 조건 표현식 문자열
 * @param expressionAttributeNames 표현식 속성 이름 치환 맵
 * @param expressionAttributeValues 표현식 속성 값 치환 맵 (자동으로 [AttributeValue]로 변환)
 * @param key 조건을 적용할 항목의 기본 키 맵 (자동으로 [AttributeValue]로 변환)
 */
@JvmName("conditionCheckOfAny")
inline fun conditionCheckOf(
    conditionExpression: String? = null,
    expressionAttributeNames: Map<String, String>? = null,
    expressionAttributeValues: Map<String, Any?>? = null,
    key: Map<String, Any?>? = null,
    crossinline builder: ConditionCheck.Builder.() -> Unit = {},
): ConditionCheck = ConditionCheck {
    this.conditionExpression = conditionExpression
    this.expressionAttributeNames = expressionAttributeNames
    this.expressionAttributeValues = expressionAttributeValues?.toAttributeValueMap()
    this.key = key?.toAttributeValueMap()

    builder()
}
