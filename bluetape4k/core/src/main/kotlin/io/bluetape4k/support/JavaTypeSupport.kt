package io.bluetape4k.support

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * `Iterator<T>` 타입에서 실제 타입 인자 `T`를 반환합니다.
 *
 * 제네릭 타입의 런타임 타입 인자를 추출할 때 사용합니다.
 *
 * ```kotlin
 * // List<String>의 Iterator<String> 타입에서 String 타입 추출
 * val listType: ParameterizedType = object : TypeToken<Iterator<String>>() {}.type as ParameterizedType
 * val elementType = listType.actualIteratorTypeArgument() // class java.lang.String
 * ```
 *
 * @throws IllegalArgumentException 파라미터화된 Iterator 타입이 아닌 경우
 */
fun Type.actualIteratorTypeArgument(): Type {
    return when {
        this !is ParameterizedType ->
            throw IllegalArgumentException("Not a parameterized type. type=$this")

        (rawType as? Class<*>)?.let { Iterator::class.java.isAssignableFrom(it) } != true ->
            throw IllegalArgumentException("Not an iterator type. rawType=$rawType")

        else                       ->
            actualTypeArguments[0]
    }
}
