package io.bluetape4k

/**
 * `(K) -> V` 를 나타내는 getter 기능을 표현한 interface 입니다.
 * @property getter Function1<K, V>
 */
interface GetterOperator<in K, out V> {
    val getter: (K) -> V
    operator fun get(key: K): V = getter(key)
}

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
