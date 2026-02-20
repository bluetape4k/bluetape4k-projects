package io.bluetape4k.opentelemetry.common

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder

/**
 * [Attributes] 인스턴스를 빌드합니다.
 *
 * @param builder Attributes 빌더
 * @return [Attributes] 인스턴스
 */
inline fun attributes(
    @BuilderInference builder: AttributesBuilder.() -> Unit,
): Attributes {
    return Attributes.builder().apply(builder).build()
}

/**
 * 키-값 쌍으로 [Attributes]를 생성합니다.
 * @param key 속성 키
 * @param value 속성 값
 * @return [Attributes] 인스턴스
 */
fun attributesOf(key: String, value: String): Attributes = Attributes.of(key.toAttributeKey(), value)

/**
 * 키-값 쌍으로 [Attributes]를 생성합니다.
 * @param key 속성 키
 * @param value 속성 값
 * @return [Attributes] 인스턴스
 */
fun <T: Any> attributesOf(
    key: AttributeKey<T>, value: T,
): Attributes =
    Attributes.of(key, value)

/**
 * 2개의 키-값 쌍으로 [Attributes]를 생성합니다.
 */
fun <T: Any, U: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
): Attributes =
    Attributes.of(key1, value1, key2, value2)

/**
 * 3개의 키-값 쌍으로 [Attributes]를 생성합니다.
 */
fun <T: Any, U: Any, V: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
    key3: AttributeKey<V>, value3: V,
): Attributes =
    Attributes.of(key1, value1, key2, value2, key3, value3)

/**
 * 4개의 키-값 쌍으로 [Attributes]를 생성합니다.
 */
fun <T: Any, U: Any, V: Any, W: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
    key3: AttributeKey<V>, value3: V,
    key4: AttributeKey<W>, value4: W,
): Attributes =
    Attributes.of(key1, value1, key2, value2, key3, value3, key4, value4)

/**
 * 5개의 키-값 쌍으로 [Attributes]를 생성합니다.
 */
fun <T: Any, U: Any, V: Any, W: Any, X: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
    key3: AttributeKey<V>, value3: V,
    key4: AttributeKey<W>, value4: W,
    key5: AttributeKey<X>, value5: X,
): Attributes =
    Attributes.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5)

/**
 * 6개의 키-값 쌍으로 [Attributes]를 생성합니다.
 */
fun <T: Any, U: Any, V: Any, W: Any, X: Any, Y: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
    key3: AttributeKey<V>, value3: V,
    key4: AttributeKey<W>, value4: W,
    key5: AttributeKey<X>, value5: X,
    key6: AttributeKey<Y>, value6: Y,
): Attributes =
    Attributes.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5, key6, value6)

/**
 * [Map]을 [Attributes]로 변환합니다.
 * 숫자, 부울, 문자열 외에도 primitive array, iterable, sequence 등을 안정적으로 처리합니다.
 */
fun Map<*, *>.toAttributes(): Attributes = attributes {
    forEach { (key, value) ->
        val attributeKey = key?.toString() ?: "null"
        putAttribute(attributeKey, value)
    }
}

private fun AttributesBuilder.putAttribute(attributeKey: String, value: Any?) {
    when (value) {
        null            -> put(attributeKey, "null")
        is String       -> put(attributeKey, value)
        is CharSequence -> put(attributeKey, value.toString())
        is Char         -> put(attributeKey, value.toString())
        is Boolean      -> put(attributeKey, value)
        is Byte         -> put(attributeKey, value.toLong())
        is Short        -> put(attributeKey, value.toLong())
        is Int          -> put(attributeKey, value.toLong())
        is Long         -> put(attributeKey, value)
        is Float        -> put(attributeKey, value.toDouble())
        is Double       -> put(attributeKey, value)
        is BooleanArray -> putBooleanList(attributeKey, value.toList())
        is IntArray     -> putLongList(attributeKey, value.map { it.toLong() })
        is LongArray    -> putLongList(attributeKey, value.toList())
        is ShortArray   -> putLongList(attributeKey, value.map { it.toLong() })
        is ByteArray    -> putLongList(attributeKey, value.map { it.toLong() })
        is FloatArray   -> putDoubleList(attributeKey, value.map { it.toDouble() })
        is DoubleArray  -> putDoubleList(attributeKey, value.toList())
        is CharArray    -> putStringList(attributeKey, value.map { it.toString() })
        is Array<*>     -> putStringList(attributeKey, value.map { it?.toString() ?: "null" })
        is Sequence<*>  -> putStringList(attributeKey, value.toSafeStringList())
        is Iterable<*>  -> putStringList(attributeKey, value.toSafeStringList())
        else            -> put(attributeKey, value.toString())
    }
}

private fun AttributesBuilder.putBooleanList(attributeKey: String, values: List<Boolean>) {
    put(AttributeKey.booleanArrayKey(attributeKey), values)
}

private fun AttributesBuilder.putLongList(attributeKey: String, values: List<Long>) {
    put(AttributeKey.longArrayKey(attributeKey), values)
}

private fun AttributesBuilder.putDoubleList(attributeKey: String, values: List<Double>) {
    put(AttributeKey.doubleArrayKey(attributeKey), values)
}

private fun AttributesBuilder.putStringList(attributeKey: String, values: List<String>) {
    put(AttributeKey.stringArrayKey(attributeKey), values)
}

private fun Sequence<*>.toSafeStringList(): List<String> = map { it?.toString() ?: "null" }.toList()
private fun Iterable<*>.toSafeStringList(): List<String> = map { it?.toString() ?: "null" }
