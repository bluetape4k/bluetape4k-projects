package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.model.MessageAttributeValue

/**
 * DSL 블록으로 [MessageAttributeValue]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `stringValue`, `dataType` 등을 직접 설정한다.
 *
 * ```kotlin
 * val attr = messageAttributeValue {
 *     stringValue("Hello")
 *     dataType("String")
 * }
 * ```
 */
inline fun messageAttributeValue(
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit,
): MessageAttributeValue =
    MessageAttributeValue.builder().apply(builder).build()

/**
 * 문자열 값과 데이터 타입으로 [MessageAttributeValue]를 생성합니다.
 *
 * ## 동작/계약
 * - [valueAsString]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [dataType] 기본값은 `"String"`이다.
 *
 * ```kotlin
 * val attr = messageAttributeValueOf("Hello SNS")
 * // attr.stringValue() == "Hello SNS"
 * // attr.dataType() == "String"
 * ```
 */
inline fun messageAttributeValueOf(
    valueAsString: String,
    dataType: String = "String",
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue {
    valueAsString.requireNotBlank("valueAsString")

    return messageAttributeValue {
        stringValue(valueAsString)
        dataType(dataType)

        builder()
    }
}

/**
 * 이 [String]을 SNS 메시지 속성 [MessageAttributeValue]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신자 문자열이 blank이면 `IllegalArgumentException`을 던진다.
 * - [dataType] 기본값은 `"String"`이다.
 *
 * ```kotlin
 * val attr = "Hello SNS".toMessageAttributeValue()
 * // attr.stringValue() == "Hello SNS"
 * ```
 */
inline fun String.toMessageAttributeValue(
    dataType: String = "String",
    @BuilderInference builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    messageAttributeValueOf(this, dataType, builder)
