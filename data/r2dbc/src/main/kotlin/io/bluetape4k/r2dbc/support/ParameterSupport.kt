package io.bluetape4k.r2dbc.support

import io.r2dbc.spi.Parameter
import io.r2dbc.spi.Parameters


@PublishedApi
internal fun Any.toParameter(): Parameter = when (this) {
    is Parameter -> this
    else -> Parameters.`in`(this)
}

@PublishedApi
internal fun <V: Any> Any?.toParameter(type: Class<V>): Parameter = when (this) {
    null -> Parameters.`in`(type)
    is Parameter -> this
    else -> Parameters.`in`(this)
}

@PublishedApi
internal fun Class<*>.toParameter(): Parameter = Parameters.`in`(this)
