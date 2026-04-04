@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import io.bluetape4k.io.getAllBytes
import io.bluetape4k.io.toByteArray
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Kotlin 값을 DynamoDB의 [AttributeValue]로 변환합니다.
 *
 * ## 동작/계약
 * - null → `AttributeValue.Null(true)`
 * - [ByteArray]/[ByteBuffer] → `AttributeValue.B`
 * - [String] → `AttributeValue.S`
 * - [Number] → `AttributeValue.N` (toString 변환)
 * - [Boolean] → `AttributeValue.Bool`
 * - [Iterable] → `AttributeValue.L`
 * - [Map] → `AttributeValue.M`
 * - 그 외 → toString() 후 `AttributeValue.S`
 *
 * ```kotlin
 * val s = "hello".toAttributeValue()   // AttributeValue.S("hello")
 * val n = 42.toAttributeValue()        // AttributeValue.N("42")
 * val b = true.toAttributeValue()      // AttributeValue.Bool(true)
 * ```
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


/**
 * [ByteArray]를 DynamoDB [AttributeValue.B]로 변환합니다.
 *
 * ```kotlin
 * val av = byteArrayOf(1, 2, 3).toAttributeValue()   // AttributeValue.B(...)
 * ```
 */
inline fun ByteArray.toAttributeValue(): AttributeValue = AttributeValue.B(this)

/**
 * [ByteBuffer]를 DynamoDB [AttributeValue.B]로 변환합니다.
 *
 * ```kotlin
 * val buf = ByteBuffer.wrap(byteArrayOf(1, 2, 3))
 * val av = buf.toAttributeValue()   // AttributeValue.B(...)
 * ```
 */
inline fun ByteBuffer.toAttributeValue(): AttributeValue = AttributeValue.B(this.getAllBytes())

/**
 * [String]을 DynamoDB [AttributeValue.S]로 변환합니다.
 *
 * ```kotlin
 * val av = "hello".toAttributeValue()   // AttributeValue.S("hello")
 * ```
 */
inline fun String.toAttributeValue(): AttributeValue = AttributeValue.S(this)

/**
 * [Number]를 DynamoDB [AttributeValue.N]으로 변환합니다.
 *
 * ```kotlin
 * val av = 42.toAttributeValue()   // AttributeValue.N("42")
 * ```
 */
inline fun Number.toAttributeValue(): AttributeValue = AttributeValue.N(this.toString())

/**
 * [Boolean]을 DynamoDB [AttributeValue.Bool]으로 변환합니다.
 *
 * ```kotlin
 * val av = true.toAttributeValue()   // AttributeValue.Bool(true)
 * ```
 */
inline fun Boolean.toAttributeValue(): AttributeValue = AttributeValue.Bool(this)

/**
 * [ByteArray] 목록을 DynamoDB [AttributeValue.Bs]로 변환합니다.
 *
 * ```kotlin
 * val av = listOf(byteArrayOf(1), byteArrayOf(2)).toAttributeValue()   // AttributeValue.Bs(...)
 * ```
 */
@JvmName("toAttributeValueByteArrayList")
inline fun Iterable<ByteArray>.toAttributeValue(): AttributeValue = AttributeValue.Bs(this.toList())

/**
 * [CharSequence] 목록을 DynamoDB [AttributeValue.Ss]로 변환합니다.
 *
 * ```kotlin
 * val av = listOf("a", "b").toAttributeValue()   // AttributeValue.Ss(["a", "b"])
 * ```
 */
@JvmName("toAttributeValueStringList")
inline fun <T: CharSequence> Iterable<T>.toAttributeValue(): AttributeValue =
    AttributeValue.Ss(this.map { it.toString() })

/**
 * [Number] 목록을 DynamoDB [AttributeValue.Ns]로 변환합니다.
 *
 * ```kotlin
 * val av = listOf(1, 2, 3).toAttributeValue()   // AttributeValue.Ns(["1", "2", "3"])
 * ```
 */
@JvmName("toAttributeValueNumberList")
fun <T: Number> Iterable<T>.toAttributeValue(): AttributeValue = AttributeValue.Ns(this.map { it.toString() })

/**
 * [InputStream]을 DynamoDB [AttributeValue.B]로 변환합니다.
 *
 * ```kotlin
 * val stream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
 * val av = stream.toAttributeValue()   // AttributeValue.B(...)
 * ```
 */
inline fun InputStream.toAttributeValue(): AttributeValue =
    AttributeValue.B(this.toByteArray())

/**
 * [Iterable] 요소들을 DynamoDB [AttributeValue.L] 리스트로 변환합니다.
 *
 * ```kotlin
 * val av = listOf("a", 1, true).toAttributeValue()   // AttributeValue.L(...)
 * ```
 */
inline fun <T> Iterable<T>.toAttributeValue() = AttributeValue.L(this.map { it.toAttributeValue() })

/**
 * [Map]을 DynamoDB [AttributeValue.M]으로 변환합니다.
 *
 * ```kotlin
 * val av = mapOf("name" to "Alice", "age" to 30).toAttributeValue()
 * // (av as AttributeValue.M).value["name"] == AttributeValue.S("Alice")
 * ```
 */
inline fun <K: Any, V> Map<K, V>.toAttributeValue(): AttributeValue.M =
    AttributeValue.M(this.entries.associate { it.key.toString() to it.value.toAttributeValue() })


/**
 * Iterable의 요소들을 [AttributeValue] 목록으로 변환합니다.
 *
 * ## 동작/계약
 * - 각 요소에 [toAttributeValue]를 적용한 새 리스트를 반환한다.
 *
 * ```kotlin
 * val list = listOf("a", "b").toAttributeValueList()
 * // list == [AttributeValue.S("a"), AttributeValue.S("b")]
 * ```
 */
inline fun <T> Iterable<T>.toAttributeValueList(): List<AttributeValue> = this.map { it.toAttributeValue() }

/**
 * Map을 `Map<String, AttributeValue>` 형태로 변환합니다.
 *
 * ## 동작/계약
 * - 키는 `toString()`, 값은 [toAttributeValue]로 변환해 새 맵을 반환한다.
 *
 * ```kotlin
 * val attrMap = mapOf("id" to "u1", "age" to 30).toAttributeValueMap()
 * // attrMap["id"] == AttributeValue.S("u1")
 * // attrMap["age"] == AttributeValue.N("30")
 * ```
 */
inline fun <K: Any, V> Map<K, V>.toAttributeValueMap(): Map<String, AttributeValue> =
    this.entries.associate { it.key.toString() to it.value.toAttributeValue() }
