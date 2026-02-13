package io.bluetape4k.support

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * `Iterator<T>` 타입에서 실제 타입 인자 `T`를 반환합니다.
 *
 * @throws IllegalArgumentException 파라미터화된 Iterator 타입이 아닌 경우
 */
fun Type.actualIteratorTypeArgument(): Type {
    val self = this

    return when {
        self !is ParameterizedType ->
            throw IllegalArgumentException("Not a parameterized type. type=$self")

        (self.rawType as? Class<*>)?.let { Iterator::class.java.isAssignableFrom(it) } != true ->
            throw IllegalArgumentException("Not an iterator type. rawType=${self.rawType}")

        else ->
            self.actualTypeArguments[0]
    }
}
