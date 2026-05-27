package io.bluetape4k.ktor.core

import io.bluetape4k.support.requireNotBlank
import io.ktor.server.application.ApplicationCall

/**
 * Returns a required path parameter or throws [IllegalArgumentException].
 */
fun ApplicationCall.requiredPathParameter(name: String): String {
    name.requireNotBlank("name")
    return parameters[name]
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Required path parameter '$name' is missing.")
}

/**
 * Returns a required query parameter or throws [IllegalArgumentException].
 */
fun ApplicationCall.requiredQueryParameter(name: String): String {
    name.requireNotBlank("name")
    return request.queryParameters[name]
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Required query parameter '$name' is missing.")
}

/**
 * Parses an optional integer query parameter and validates its range.
 *
 * ## Contract
 * - Missing parameters return [defaultValue].
 * - Non-integer values throw [IllegalArgumentException].
 * - Values outside [range] throw [IllegalArgumentException].
 */
fun ApplicationCall.intQueryParameter(
    name: String,
    defaultValue: Int? = null,
    range: IntRange? = null,
): Int? {
    name.requireNotBlank("name")
    val rawValue = request.queryParameters[name] ?: return defaultValue
    val value = rawValue.toIntOrNull()
        ?: throw IllegalArgumentException("Query parameter '$name' must be an integer.")

    if (range != null && value !in range) {
        throw IllegalArgumentException("Query parameter '$name' must be in $range.")
    }
    return value
}
