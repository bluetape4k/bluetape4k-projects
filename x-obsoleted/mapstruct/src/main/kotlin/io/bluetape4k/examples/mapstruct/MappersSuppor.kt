package io.bluetape4k.examples.mapstruct

import org.mapstruct.factory.Mappers

inline fun <reified T> mapper(): T = Mappers.getMapper(T::class.java)
