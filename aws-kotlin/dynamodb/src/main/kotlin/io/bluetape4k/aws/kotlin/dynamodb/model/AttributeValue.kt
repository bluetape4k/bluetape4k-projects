package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.io.getAllBytes
import io.bluetape4k.io.toByteArray
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Kotlin 수형을 DynamoDB의 [AttributeValue]로 변환합니다.
 */
fun <T> T.toAttributeValue(): AttributeValue = when (this) {
    is AttributeValue -> this
    null           -> AttributeValue.Null(true)
    is ByteArray      -> AttributeValue.B(this)
    is ByteBuffer  -> this.toAttributeValue()
    is String         -> AttributeValue.S(this)
    is Number         -> AttributeValue.N(this.toString())
    is Boolean        -> AttributeValue.Bool(this)

    is Iterable<*> -> this.toAttributeValue()
    is Map<*, *>   -> this.toAttributeValue()
    else           -> this.toString().toAttributeValue()
}


fun ByteArray.toAttributeValue(): AttributeValue = AttributeValue.B(this)
fun ByteBuffer.toAttributeValue(): AttributeValue = AttributeValue.B(this.getAllBytes())
fun String.toAttributeValue(): AttributeValue = AttributeValue.S(this)
fun Number.toAttributeValue(): AttributeValue = AttributeValue.N(this.toString())
fun Boolean.toAttributeValue(): AttributeValue = AttributeValue.Bool(this)

@JvmName("toAttributeValueByteArrayList")
fun Iterable<ByteArray>.toAttributeValue(): AttributeValue = AttributeValue.Bs(this.toFastList())

@JvmName("toAttributeValueStringList")
fun <T: CharSequence> Iterable<T>.toAttributeValue(): AttributeValue = AttributeValue.Ss(this.map { it.toString() })

@JvmName("toAttributeValueNumberList")
fun <T: Number> Iterable<T>.toAttributeValue(): AttributeValue = AttributeValue.Ns(this.map { it.toString() })

fun Iterable<*>.toAttributeValue() = AttributeValue.L(this.map { it.toAttributeValue() })

fun Map<*, *>.toAttributeValue() =
    AttributeValue.M(this.entries.associate { it.key as String to it.value.toAttributeValue() })

fun InputStream.toAttributeValue(): AttributeValue = AttributeValue.B(this.toByteArray())
