package io.bluetape4k.aws.dynamodb.model

import io.bluetape4k.aws.core.toSdkBytes
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
fun ByteArray.toAttributeValue(): AttributeValue = AttributeValue.builder()
    .b(this.toSdkBytes())
    .build()

/**
 * [ByteBuffer]를 [AttributeValue]로 변환합니다.
 */
fun ByteBuffer.toAttributeValue(): AttributeValue = AttributeValue.builder()
    .b(this.toSdkBytes())
    .build()

/**
 * [String]을 [AttributeValue]로 변환합니다.
 */
fun String.toAttributeValue(): AttributeValue = AttributeValue.builder()
    .s(this)
    .build()

/**
 * [Number]를 [AttributeValue]로 변환합니다.
 */
fun Number.toAttributeValue(): AttributeValue = AttributeValue.builder()
    .n(this.toString())
    .build()

/**
 * [Boolean]을 [AttributeValue]로 변환합니다.
 */
fun Boolean.toAttributeValue(): AttributeValue = AttributeValue.builder()
    .bool(this)
    .build()

/**
 * Nullable 인지 여부를 [AttributeValue]로 변환합니다.
 */
fun Boolean.toNullAttributeValue(): AttributeValue = AttributeValue.builder()
    .nul(this)
    .build()

/**
 * [Iterable]을 [AttributeValue]로 변환합니다.
 */
fun Iterable<*>.toAttributeValue(): AttributeValue = AttributeValue.builder()
    .l(this.map { it.toAttributeValue() })
    .build()

/**
 * [Map]을 [AttributeValue]로 변환합니다.
 */
fun Map<*, *>.toAttributeValue(): AttributeValue {
    val mapped = this.entries.associate { entry ->
        val key = entry.key ?: throw IllegalArgumentException("Map key must be non-null String")
        require(key is String) { "Map key must be String but was ${key::class.java.name}" }
        key to entry.value.toAttributeValue()
    }
    return AttributeValue.builder()
        .m(mapped)
        .build()
}

/**
 * [InputStream]을 읽어, ByteArray로 표현되는 [AttributeValue]로 변환합니다.
 */
fun InputStream.toAttributeValue(): AttributeValue = AttributeValue.builder()
    .b(toSdkBytes())
    .build()

/**
 * [T]를 [AttributeValue]로 변환합니다.
 * 준비되지 않은 알 수 없는 수형에 대해서는 `String`으로 변환합니다.
 *
 * 만약 `data class` 등의 사용자 정의 클래스 정보에 대해서는
 * Binary Serialization을 통해 ByteArray로 변환한 후 `toAttributeValue()`를 호출해야 합니다.
 */
fun <T> T.toAttributeValue(): AttributeValue = when (this) {
    null -> AttributeValue.builder().nul(true).build()
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
