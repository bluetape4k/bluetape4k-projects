package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeAction
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.AttributeValueUpdate

/**
 * DSL 블록으로 DynamoDB [AttributeValueUpdate]를 빌드합니다.
 *
 * ## 동작/계약
 * - [value]는 [toAttributeValue]를 통해 [AttributeValue]로 변환된다.
 * - [action]은 PUT, DELETE, ADD 중 하나를 지정한다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val update = attributeValueUpdateOf("newName", AttributeAction.Put)
 * // update.value == AttributeValue.S("newName")
 * // update.action == AttributeAction.Put
 * ```
 *
 * @param value 업데이트할 값 (자동으로 [AttributeValue]로 변환)
 * @param action 수행할 속성 업데이트 동작
 */
inline fun <T> attributeValueUpdateOf(
    value: T,
    action: AttributeAction,
    @BuilderInference crossinline builder: AttributeValueUpdate.Builder.() -> Unit = {},
): AttributeValueUpdate =
    attributeValueUpdateOf(value.toAttributeValue(), action, builder)

/**
 * DSL 블록으로 DynamoDB [AttributeValueUpdate]를 빌드합니다.
 *
 * ## 동작/계약
 * - [value]는 [AttributeValue] 타입을 직접 받아 변환 없이 설정된다.
 * - [action]은 PUT, DELETE, ADD 중 하나를 지정한다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val update = attributeValueUpdateOf(AttributeValue.S("hello"), AttributeAction.Put)
 * // update.value == AttributeValue.S("hello")
 * // update.action == AttributeAction.Put
 * ```
 *
 * @param value 업데이트할 [AttributeValue]
 * @param action 수행할 속성 업데이트 동작
 */
inline fun attributeValueUpdateOf(
    value: AttributeValue,
    action: AttributeAction,
    @BuilderInference crossinline builder: AttributeValueUpdate.Builder.() -> Unit = {},
): AttributeValueUpdate = AttributeValueUpdate {
    this.value = value
    this.action = action

    builder()
}
