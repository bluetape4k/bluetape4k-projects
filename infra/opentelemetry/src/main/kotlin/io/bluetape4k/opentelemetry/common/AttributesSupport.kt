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
 *
 * @return [Attributes] 인스턴스
 */
fun Map<*, *>.toAttributes(): Attributes = attributes {
    forEach { (key, value) ->
        val keyStr = key.toString()
        when (value) {
            is Int         -> put(keyStr, value.toLong())
            is Long        -> put(keyStr, value)
            is Float       -> put(keyStr, value.toDouble())
            is Double      -> put(keyStr, value)
            is Boolean     -> put(keyStr, value)
            is LongArray   -> put(AttributeKey.longArrayKey(keyStr), value.toList())
            is DoubleArray -> put(AttributeKey.doubleArrayKey(keyStr), value.toList())
            is BooleanArray -> put(AttributeKey.booleanArrayKey(keyStr), value.toList())
            is Array<*>    -> put(
                AttributeKey.stringArrayKey(keyStr),
                *value.map { it.toString() }.toTypedArray()
            )

            else           -> put(keyStr, value.toString())
        }
    }
}
