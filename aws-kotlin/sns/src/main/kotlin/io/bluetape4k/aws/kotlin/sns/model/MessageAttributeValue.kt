package io.bluetape4k.aws.kotlin.sns.model

import aws.sdk.kotlin.services.sns.model.MessageAttributeValue

/**
 * [stringValue] 문자열을 가지는 [MessageAttributeValue]를 생성합니다.
 *
 * ```
 * val messageAttributeValue = messageAttributeValueOf("stringValue")
 * ```
 *
 * @param stringValue 문자열 값
 * @param configurer [MessageAttributeValue.Builder]를 설정하는 람다
 * @return [MessageAttributeValue] 인스턴스
 */
inline fun messageAttributeValueOf(
    stringValue: String,
    crossinline configurer: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue = MessageAttributeValue {
    this.stringValue = stringValue
    this.dataType = "String"
    configurer()
}

/**
 * [binaryValue] 바이너리 값을 가지는 [MessageAttributeValue]를 생성합니다.
 *
 * ```
 * val messageAttributeValue = messageAttributeValueOf(byteArrayOf(0x01, 0x02, 0x03))
 * ```
 *
 * @param binaryValue 바이너리 값
 * @param configurer [MessageAttributeValue.Builder]를 설정하는 람다
 * @return [MessageAttributeValue] 인스턴스
 */
inline fun messageAttributeValueOf(
    binaryValue: ByteArray,
    crossinline configurer: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue = MessageAttributeValue {
    this.binaryValue = binaryValue
    this.dataType = "Binary"
    configurer()
}

/**
 * [numberValue] 숫자 값을 가지는 [MessageAttributeValue]를 생성합니다.
 *
 * ```
 * val messageAttributeValue = messageAttributeValueOf(123)
 * ```
 *
 * @param numberValue 숫자 값
 * @param configurer [MessageAttributeValue.Builder]를 설정하는 람다
 * @return [MessageAttributeValue] 인스턴스
 */
inline fun <T: Number> messageAttributeValueOf(
    numberValue: T,
    crossinline configurer: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue = MessageAttributeValue {
    this.stringValue = numberValue.toString()
    this.dataType = "Number"
    configurer()
}
