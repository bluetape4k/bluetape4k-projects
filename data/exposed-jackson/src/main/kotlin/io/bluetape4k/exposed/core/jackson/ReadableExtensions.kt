package io.bluetape4k.exposed.core.jackson

import com.fasterxml.jackson.databind.JsonNode
import io.bluetape4k.jackson.JacksonSerializer
import io.r2dbc.spi.Readable

@PublishedApi
internal fun requiredJacksonReadableError(column: Any, typeName: String): Nothing =
    error("Column[$column] is null or not convertible to $typeName.")

inline fun <reified T: Any> Readable.getJackson(
    index: Int,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T = getJacksonOrNull<T>(index, serializer) ?: requiredJacksonReadableError(index, T::class.simpleName ?: "T")

inline fun <reified T: Any> Readable.getJackson(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T = getJacksonOrNull<T>(name, serializer) ?: requiredJacksonReadableError(name, T::class.simpleName ?: "T")

inline fun <reified T: Any> Readable.getJacksonOrNull(
    index: Int,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T? {
    val value = get(index) ?: return null
    return when (value) {
        is T         -> value
        is String    -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        else         -> serializer.deserializeFromString<T>(value.toString())
    }
}

inline fun <reified T: Any> Readable.getJacksonOrNull(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T? {
    val value = get(name) ?: return null
    return when (value) {
        is T         -> value
        is String    -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        else         -> serializer.deserializeFromString<T>(value.toString())
    }
}

fun Readable.getJsonNode(
    index: Int,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode = getJsonNodeOrNull(index, serializer) ?: requiredJacksonReadableError(index, "JsonNode")

fun Readable.getJsonNode(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode = getJsonNodeOrNull(name, serializer) ?: requiredJacksonReadableError(name, "JsonNode")

fun Readable.getJsonNodeOrNull(
    index: Int,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode? {
    val value = get(index) ?: return null
    return when (value) {
        is JsonNode  -> value
        is String    -> serializer.mapper.readTree(value)
        is ByteArray -> serializer.mapper.readTree(value)
        else         -> serializer.mapper.readTree(value.toString())
    }
}

fun Readable.getJsonNodeOrNull(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode? {
    val value = get(name) ?: return null
    return when (value) {
        is JsonNode  -> value
        is String    -> serializer.mapper.readTree(value)
        is ByteArray -> serializer.mapper.readTree(value)
        else         -> serializer.mapper.readTree(value.toString())
    }
}
