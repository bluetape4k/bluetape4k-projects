package io.bluetape4k.opentelemetry.common

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder

inline fun attributes(
    @BuilderInference builder: AttributesBuilder.() -> Unit,
): Attributes {
    return Attributes.builder().apply(builder).build()
}

fun attributesOf(key: String, value: String): Attributes = Attributes.of(key.toAttributeKey(), value)

fun <T: Any> attributesOf(
    key: AttributeKey<T>, value: T,
): Attributes =
    Attributes.of(key, value)

fun <T: Any, U: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
): Attributes =
    Attributes.of(key1, value1, key2, value2)

fun <T: Any, U: Any, V: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
    key3: AttributeKey<V>, value3: V,
): Attributes =
    Attributes.of(key1, value1, key2, value2, key3, value3)

fun <T: Any, U: Any, V: Any, W: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
    key3: AttributeKey<V>, value3: V,
    key4: AttributeKey<W>, value4: W,
): Attributes =
    Attributes.of(key1, value1, key2, value2, key3, value3, key4, value4)

fun <T: Any, U: Any, V: Any, W: Any, X: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
    key3: AttributeKey<V>, value3: V,
    key4: AttributeKey<W>, value4: W,
    key5: AttributeKey<X>, value5: X,
): Attributes =
    Attributes.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5)

fun <T: Any, U: Any, V: Any, W: Any, X: Any, Y: Any> attributesOf(
    key1: AttributeKey<T>, value1: T,
    key2: AttributeKey<U>, value2: U,
    key3: AttributeKey<V>, value3: V,
    key4: AttributeKey<W>, value4: W,
    key5: AttributeKey<X>, value5: X,
    key6: AttributeKey<Y>, value6: Y,
): Attributes =
    Attributes.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5, key6, value6)

fun Map<*, *>.toAttributes(): Attributes = attributes {
    forEach { (key, value) ->
        val keyStr = key.toString()
        when (value) {
            is Int -> put(AttributeKey.longArrayKey(keyStr), value.toLong())
            is Long -> put(keyStr, value)
            is Float -> put(keyStr, value.toDouble())
            is Double -> put(keyStr, value)
            is Boolean -> put(keyStr, value)
            is LongArray -> put<List<Long>>(AttributeKey.longArrayKey(keyStr), value.toList())
            is DoubleArray -> put<List<Double>>(AttributeKey.doubleArrayKey(keyStr), value.toList())
            is BooleanArray -> put<List<Boolean>>(AttributeKey.booleanArrayKey(keyStr), value.toList())
            is Array<*> -> put(
                AttributeKey.stringArrayKey(keyStr),
                *value.map { it.toString() }.toTypedArray()
            )

            else -> put(keyStr, value.toString())
        }
    }
}
