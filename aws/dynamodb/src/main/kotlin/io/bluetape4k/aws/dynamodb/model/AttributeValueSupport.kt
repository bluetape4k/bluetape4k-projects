package io.bluetape4k.aws.dynamodb.model

import io.bluetape4k.aws.core.toSdkBytes
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * [AttributeValue.Builder]를 이용하여 [AttributeValue]를 생성합니다.
 *
 * ```
 * val strValue = attributeValue { s("Hello, World!") }
 * val intValue = attributeValue { n("123") }
 * val boolValue = attributeValue { bool(true) }
 * ```
 *
 * @param builder [AttributeValue.Builder]를 초기화하는 람다 함수입니다.
 */
inline fun attributeValue(
    @BuilderInference builder: AttributeValue.Builder.() -> Unit,
): AttributeValue {
    return AttributeValue.builder().apply(builder).build()
}

/**
 * [ByteArray]를 [AttributeValue]로 변환합니다.
 */
fun ByteArray.toAttributeValue(): AttributeValue = AttributeValues.binaryValue(this.toSdkBytes())

/**
 * [ByteBuffer]를 [AttributeValue]로 변환합니다.
 */
fun ByteBuffer.toAttributeValue(): AttributeValue = AttributeValues.binaryValue(this.toSdkBytes())

/**
 * [String]을 [AttributeValue]로 변환합니다.
 */
fun String.toAttributeValue(): AttributeValue = AttributeValues.stringValue(this)

/**
 * [Number]를 [AttributeValue]로 변환합니다.
 */
fun Number.toAttributeValue(): AttributeValue = AttributeValues.numberValue(this)

/**
 * [Boolean]을 [AttributeValue]로 변환합니다.
 */
fun Boolean.toAttributeValue(): AttributeValue = attributeValue { bool(this@toAttributeValue) }

/**
 * Nullable 인지 여부를 [AttributeValue]로 변환합니다.
 */
fun Boolean.toNullAttributeValue(): AttributeValue = attributeValue { nul(this@toNullAttributeValue) }

/**
 * [Iterable]을 [AttributeValue]로 변환합니다.
 */
fun Iterable<*>.toAttributeValue(): AttributeValue = attributeValue {
    l(this@toAttributeValue.map { it.toAttributeValue() })
}

/**
 * [Map]을 [AttributeValue]로 변환합니다.
 */
fun Map<*, *>.toAttributeValue(): AttributeValue = attributeValue {
    m(this@toAttributeValue.entries.associate { it.key as String to it.value.toAttributeValue() })
}

/**
 * [InputStream]을 읽어, ByteArray로 표현되는 [AttributeValue]로 변환합니다.
 */
fun InputStream.toAttributeValue(): AttributeValue = attributeValue { b(toSdkBytes()) }

/**
 * [T]를 [AttributeValue]로 변환합니다.
 * 준비되지 않은 알 수 없는 수형에 대해서는 `String`으로 변환합니다.
 *
 * 만약 `data class` 등의 사용자 정의 클래스 정보에 대해서는
 * Binary Serialization을 통해 ByteArray로 변환한 후 `toAttributeValue()`를 호출해야 합니다.
 */
fun <T> T.toAttributeValue(): AttributeValue = when (this) {
    null -> AttributeValues.nullAttributeValue()
    is ByteArray -> toAttributeValue()
    is ByteBuffer -> toAttributeValue()
    is String -> toAttributeValue()
    is Number -> toAttributeValue()
    is Boolean -> toAttributeValue()
    is Iterable<*> -> toAttributeValue()
    is Map<*, *> -> toAttributeValue()
    is InputStream -> toAttributeValue()
    else -> attributeValue { s(this@toAttributeValue.toString()) }
}
