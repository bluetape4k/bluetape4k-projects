package io.bluetape4k.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.json.JsonException
import java.math.BigDecimal
import java.math.BigInteger

private val nodeFactory = JsonNodeFactory.instance

/**
 * [fieldName]을 키로 하여 [JsonNode]를 추가합니다.
 */
fun JsonNode.createNode(fieldName: String?): JsonNode = when (this) {
    is ObjectNode -> putObject(fieldName)
    is ArrayNode -> addObject()
    else -> nodeFactory.objectNode()
}

/**
 * [fieldName]을 키로 하여 [JsonNode] 배열을 추가합니다.
 */
fun JsonNode.createArray(fieldName: String?): JsonNode = when (this) {
    is ObjectNode -> putArray(fieldName)
    is ArrayNode -> addArray()
    else -> nodeFactory.arrayNode()
}

fun <T> JsonNode.addValue(value: T, fieldName: String?) = apply {
    if (this is ObjectNode && fieldName.isNullOrBlank()) {
        throw IllegalArgumentException("Field name must not be null for ObjectNode")
    }
    val node = when (value) {
        null -> nodeFactory.nullNode()
        is Boolean -> nodeFactory.booleanNode(value)
        is Char -> nodeFactory.textNode(value.toString())
        is Byte -> nodeFactory.numberNode(value)
        is Short -> nodeFactory.numberNode(value)
        is Int -> nodeFactory.numberNode(value)
        is Long -> nodeFactory.numberNode(value)
        is Double -> nodeFactory.numberNode(value)
        is Float -> nodeFactory.numberNode(value)
        is BigDecimal -> nodeFactory.numberNode(value)
        is BigInteger -> nodeFactory.numberNode(value)
        is String -> nodeFactory.textNode(value)
        is ByteArray -> nodeFactory.binaryNode(value)
        else -> throw IllegalArgumentException("Unsupported value type: ${value?.let { it::class }}")
    }
    when (this) {
        is ObjectNode -> replace(fieldName, node)
        is ArrayNode -> add(node)
        else -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Boolean 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBoolean(value: Boolean, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Char 수형의 [value]를 추가합니다.
 */
fun JsonNode.addChar(value: Char, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Byte 수형의 [value]를 추가합니다.
 */
fun JsonNode.addByte(value: Byte, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Short 수형의 [value]를 추가합니다.
 */
fun JsonNode.addShort(value: Short, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Int 수형의 [value]를 추가합니다.
 */
fun JsonNode.addInt(value: Int, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Long 수형의 [value]를 추가합니다.
 */
fun JsonNode.addLong(value: Long, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 문자열 [value]를 추가합니다.
 */
fun JsonNode.addString(value: String, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Float 수형의 [value]를 추가합니다.
 */
fun JsonNode.addFloat(value: Float, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Double 수형의 [value]를 추가합니다.
 */
fun JsonNode.addDouble(value: Double, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 BigDecimal 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBigDecimal(value: BigDecimal, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 BigInteger 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBigInteger(value: BigInteger, fieldName: String?) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 ByteArray 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBigInteger(value: ByteArray, fieldName: String?) = addValue(value, fieldName)



/**
 * [fieldName]을 키로 하여 `Null` 값을 가지는 JsonNode를 추가합니다.
 */
fun JsonNode.addNull(fieldName: String?) = apply {
    if (this is ObjectNode && fieldName.isNullOrBlank()) {
        throw IllegalArgumentException("Field name must not be null for ObjectNode")
    }

    when (this) {
        is ObjectNode -> putNull(fieldName)
        is ArrayNode -> addNull()
        else -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}
