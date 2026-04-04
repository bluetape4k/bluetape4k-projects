package io.bluetape4k.aws.kotlin.sqs.model

import aws.sdk.kotlin.services.sqs.model.MessageAttributeValue

/**
 * 문자열 값으로 SQS [MessageAttributeValue]를 생성합니다.
 *
 * ```kotlin
 * val attr = messageAttributeValueOf("hello")
 * // attr.stringValue == "hello"
 * ```
 *
 * @param value 문자열 값 (null 허용)
 * @param builder [MessageAttributeValue.Builder]를 설정하는 람다
 * @return [MessageAttributeValue] 인스턴스
 */
@JvmName("messageAttributeValueOfNullableString")
inline fun messageAttributeValueOf(
    value: String?,
    crossinline builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        stringValue = value
        builder()
    }

/**
 * 문자열 목록으로 SQS [MessageAttributeValue]를 생성합니다.
 *
 * ```kotlin
 * val attr = messageAttributeValueOf(listOf("a", "b", "c"))
 * // attr.stringListValues == ["a", "b", "c"]
 * ```
 *
 * @param values 문자열 목록 (null 허용)
 * @param builder [MessageAttributeValue.Builder]를 설정하는 람다
 * @return [MessageAttributeValue] 인스턴스
 */
@JvmName("messageAttributeValueOfNullableStringList")
inline fun messageAttributeValueOf(
    values: List<String>?,
    crossinline builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        stringListValues = values

        builder()
    }

/**
 * 바이너리 값으로 SQS [MessageAttributeValue]를 생성합니다.
 *
 * ```kotlin
 * val attr = messageAttributeValueOf(byteArrayOf(1, 2, 3))
 * // attr.binaryValue?.contentEquals(byteArrayOf(1, 2, 3)) == true
 * ```
 *
 * @param value 바이너리 값 (null 허용)
 * @param builder [MessageAttributeValue.Builder]를 설정하는 람다
 * @return [MessageAttributeValue] 인스턴스
 */
@JvmName("messageAttributeValueOfNullableByteArray")
inline fun messageAttributeValueOf(
    value: ByteArray?,
    crossinline builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        binaryValue = value

        builder()
    }

/**
 * 바이너리 목록으로 SQS [MessageAttributeValue]를 생성합니다.
 *
 * ```kotlin
 * val attr = messageAttributeValueOf(listOf(byteArrayOf(1), byteArrayOf(2)))
 * // attr.binaryListValues?.size == 2
 * ```
 *
 * @param values 바이너리 목록 (null 허용)
 * @param builder [MessageAttributeValue.Builder]를 설정하는 람다
 * @return [MessageAttributeValue] 인스턴스
 */
@JvmName("messageAttributeValueOfNullableByteArrayList")
inline fun messageAttributeValueOf(
    values: List<ByteArray>?,
    crossinline builder: MessageAttributeValue.Builder.() -> Unit = {},
): MessageAttributeValue =
    MessageAttributeValue {
        binaryListValues = values

        builder()
    }
