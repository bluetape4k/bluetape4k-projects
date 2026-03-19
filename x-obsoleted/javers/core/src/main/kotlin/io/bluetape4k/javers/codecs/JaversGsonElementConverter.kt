package io.bluetape4k.javers.codecs

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.math.BigDecimal

internal object JaversGsonElementConverter {

    fun fromJsonObject(jsonObject: JsonObject): Map<String, Any?> {
        return jsonObject.entrySet()
            .associate { (key, jsonElement) ->
                key to extractValue(jsonElement)
            }
    }

    fun toJsonObject(map: Map<String, Any?>): JsonObject {
        val jsonObject = JsonObject()
        map.forEach { (key, value) ->
            jsonObject.add(key, createJsonElement(value))
        }
        return jsonObject
    }

    private fun extractValue(element: JsonElement): Any? {
        if (element == JsonNull.INSTANCE) {
            return null
        }
        if (element is JsonObject) {
            return fromJsonObject(element)
        }
        if (element is JsonPrimitive) {
            if (element.isString) {
                return element.asString
            }
            if (element.isNumber && element.asNumber is BigDecimal) {
                val value = element.asNumber as BigDecimal
                return try {
                    value.longValueExact()
                } catch (e: ArithmeticException) {
                    value.toDouble()
                }
            }
            if (element.isNumber) {
                return element.asNumber
            }
            if (element.isBoolean) {
                return element.asBoolean
            }
        }
        if (element is JsonArray) {
            return mutableListOf<Any?>().apply {
                element.forEach { elem ->
                    add(extractValue(elem))
                }
            }
        }

        throw IllegalArgumentException("unsupported JsonElement type - ${element.javaClass.simpleName}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun createJsonElement(value: Any?): JsonElement {
        if (value == null) {
            return JsonNull.INSTANCE
        }
        if (value is Map<*, *>) {
            return toJsonObject(value as Map<String, Any?>)
        }
        if (value is String) {
            return JsonPrimitive(value)
        }
        if (value is Number) {
            return JsonPrimitive(value)
        }
        if (value is Boolean) {
            return JsonPrimitive(value)
        }
        if (value is List<*>) {
            return JsonArray().apply {
                value.forEach { item ->
                    add(createJsonElement(item))
                }
            }
        }
        throw IllegalArgumentException("unsupported object type - ${value.javaClass.simpleName}")
    }
}
