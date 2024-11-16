package io.bluetape4k.support

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

fun Type.actualIteratorTypeArgument(): Type {
    val self = this

    return when {
        self !is ParameterizedType           ->
            throw IllegalArgumentException("Not supported type $self")

        self.rawType != Iterator::class.java ->
            throw IllegalArgumentException("Not an iterator type ${self.rawType}")

        else                                 ->
            self.actualTypeArguments[0]
    }
}
