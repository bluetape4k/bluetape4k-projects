@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import io.bluetape4k.io.getAllBytes
import io.bluetape4k.io.toByteArray
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Kotlin 수형을 DynamoDB의 [AttributeValue]로 변환합니다.
 */
fun <T> T.toAttributeValue(): AttributeValue = when (this) {
    null              -> AttributeValue.Null(true)
    is AttributeValue -> this
    is ByteArray      -> AttributeValue.B(this)
    is ByteBuffer     -> this.toAttributeValue()
    is String         -> AttributeValue.S(this)
    is Number         -> AttributeValue.N(this.toString())
    is Boolean        -> AttributeValue.Bool(this)

    is Iterable<*>    -> this.toAttributeValue()
    is Map<*, *>      -> this.toAttributeValue()
    else              -> this.toString().toAttributeValue()
}


inline fun ByteArray.toAttributeValue(): AttributeValue = AttributeValue.B(this)
inline fun ByteBuffer.toAttributeValue(): AttributeValue = AttributeValue.B(this.getAllBytes())
inline fun String.toAttributeValue(): AttributeValue = AttributeValue.S(this)
inline fun Number.toAttributeValue(): AttributeValue = AttributeValue.N(this.toString())
inline fun Boolean.toAttributeValue(): AttributeValue = AttributeValue.Bool(this)

@JvmName("toAttributeValueByteArrayList")
inline fun Iterable<ByteArray>.toAttributeValue(): AttributeValue = AttributeValue.Bs(this.toList())

@JvmName("toAttributeValueStringList")
inline fun <T: CharSequence> Iterable<T>.toAttributeValue(): AttributeValue =
    AttributeValue.Ss(this.map { it.toString() })

@JvmName("toAttributeValueNumberList")
fun <T: Number> Iterable<T>.toAttributeValue(): AttributeValue = AttributeValue.Ns(this.map { it.toString() })

inline fun InputStream.toAttributeValue(): AttributeValue =
    AttributeValue.B(this.toByteArray())

inline fun <T> Iterable<T>.toAttributeValue() = AttributeValue.L(this.map { it.toAttributeValue() })

inline fun <K: Any, V> Map<K, V>.toAttributeValue(): AttributeValue.M =
    AttributeValue.M(this.entries.associate { it.key.toString() to it.value.toAttributeValue() })


inline fun <T> Iterable<T>.toAttributeValueList(): List<AttributeValue> = this.map { it.toAttributeValue() }

inline fun <K: Any, V> Map<K, V>.toAttributeValueMap(): Map<String, AttributeValue> =
    this.entries.associate { it.key.toString() to it.value.toAttributeValue() }
