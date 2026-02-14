package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.jackson3.JacksonSerializer
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import tools.jackson.databind.JsonNode

@PublishedApi
internal fun ResultRow.anyValueOrNull(expression: Expression<*>): Any? {
    @Suppress("UNCHECKED_CAST")
    return getOrNull(expression as Expression<Any?>)
}

@PublishedApi
internal fun requiredJacksonError(expression: Expression<*>, typeName: String): Nothing =
    error("Expression[$expression] is null or not convertible to $typeName.")

inline fun <reified T: Any> ResultRow.getJackson(
    expression: Expression<*>,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T = getJacksonOrNull<T>(expression, serializer) ?: requiredJacksonError(expression, T::class.simpleName ?: "T")

inline fun <reified T: Any> ResultRow.getJacksonOrNull(
    expression: Expression<*>,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is T         -> value
        is String    -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        else         -> serializer.deserializeFromString<T>(value.toString())
    }
}

fun ResultRow.getJsonNode(
    expression: Expression<*>,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode = getJsonNodeOrNull(expression, serializer) ?: requiredJacksonError(expression, "JsonNode")

fun ResultRow.getJsonNodeOrNull(
    expression: Expression<*>,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is JsonNode  -> value
        is String    -> serializer.mapper.readTree(value)
        is ByteArray -> serializer.mapper.readTree(value)
        else         -> serializer.mapper.readTree(value.toString())
    }
}
