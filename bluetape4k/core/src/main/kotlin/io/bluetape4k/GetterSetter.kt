package io.bluetape4k

/**
 * `(K) -> V` 를 나타내는 getter 기능을 표현한 interface 입니다.
 * @property getter Function1<K, V>
 */
interface GetterOperator<in K, out V> {
    val getter: (K) -> V
    operator fun get(key: K): V = getter(key)
}

/**
 * getterOperator 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val getter = getterOperator<String, Int> { it.length }
 * val result = getter["abc"]
 * // result == 3
 * ```
 */
fun <K, V> getterOperator(func: (K) -> V): GetterOperator<K, V> {
    return object: GetterOperator<K, V> {
        override val getter: (K) -> V
            get() = func
    }
}

/**
 * `(K, V) -> Unit` 으로 Setter 를 표현하는 interface 입니다.
 *
 * @param K key type
 * @param V value type
 */
interface SetterOperator<in K, in V> {
    val setter: (K, V) -> Unit
    operator fun set(key: K, value: V) {
        setter(key, value)
    }
}

/**
 * setterOperator 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val values = mutableMapOf<String, Int>()
 * val setter = setterOperator<String, Int> { k, v -> values[k] = v }
 * setter["a"] = 1
 * // values["a"] == 1
 * ```
 */
fun <K, V> setterOperator(func: (K, V) -> Unit): SetterOperator<K, V> {
    return object: SetterOperator<K, V> {
        override val setter: (K, V) -> Unit
            get() = func
    }
}

/**
 * Getter, Setter 작업을 Kotlin 스타일로 표현할 수 있도록 했습니다
 *
 * @see io.bluetape4k.support.sysProperty
 *
 * @param K  key type
 * @param V     value type
 * @property getter getter 함수
 * @property setter setter 함수
 */
class GetterSetterOperator<in K, V>(
    override val getter: (K) -> V,
    override val setter: (K, V) -> Unit,
): GetterOperator<K, V>, SetterOperator<K, V>
