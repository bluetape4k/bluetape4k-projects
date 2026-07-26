package io.bluetape4k.r2dbc.support

import io.r2dbc.spi.Parameter
import io.r2dbc.spi.Parameters

/**
 * Creates an R2DBC [Parameter] that carries a typed NULL value.
 *
 * Use this helper with map-based binding APIs when a nullable value needs to
 * preserve its database type information.
 *
 * ```kotlin
 * val parameters = mapOf("description" to typedNullParameter<String>())
 * ```
 *
 * @param type Java type to expose to the R2DBC driver.
 * @return typed NULL [Parameter].
 */
fun typedNullParameter(type: Class<*>): Parameter = Parameters.`in`(type)

/**
 * Creates an R2DBC [Parameter] that carries a typed NULL value for [T].
 *
 * @param T Kotlin type to expose to the R2DBC driver.
 * @return typed NULL [Parameter].
 */
inline fun <reified T: Any> typedNullParameter(): Parameter = typedNullParameter(T::class.java)

internal fun rawNullBindingException(name: String): IllegalArgumentException =
    IllegalArgumentException(
        "Raw null value for [$name] does not preserve R2DBC type information. " +
                "Use typedNullParameter<T>() or io.r2dbc.spi.Parameters.in(type).",
    )

/**
 * Converts the receiver to an R2DBC [Parameter].
 *
 * Existing [Parameter] instances are returned as-is; other values are wrapped
 * with [Parameters. in].
 */
@PublishedApi
internal fun Any.toParameter(): Parameter =
    when (this) {
        is Parameter -> this
        else -> Parameters.`in`(this)
    }

/**
 * Converts a nullable value to an R2DBC [Parameter].
 *
 * - Null values become typed NULL [Parameter] instances.
 * - Existing [Parameter] instances are returned as-is.
 * - Other values are wrapped with [Parameters. in].
 *
 * @param type Java type for the parameter value.
 */
@PublishedApi
internal fun <V: Any> Any?.toParameter(type: Class<V>): Parameter =
    when (this) {
        null -> Parameters.`in`(type)
        is Parameter -> this
        else -> Parameters.`in`(this)
    }

/**
 * Converts [Class] to an R2DBC [Parameter] that carries a typed NULL value.
 */
@PublishedApi
internal fun Class<*>.toParameter(): Parameter = Parameters.`in`(this)
