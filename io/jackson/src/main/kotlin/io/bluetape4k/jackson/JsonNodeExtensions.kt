package io.bluetape4k.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.math.BigInteger

/**
 * [fieldName]을 키로 하여 [JsonNode]를 추가합니다.
 */
fun JsonNode.createNode(fieldName: String?): JsonNode = when (this) {
    is ObjectNode -> putObject(fieldName)
    is ArrayNode  -> addObject()
    else          -> JsonNodeFactory.instance.objectNode()
}

/**
 * [fieldName]을 키로 하여 [JsonNode] 배열을 추가합니다.
 */
fun JsonNode.createArray(fieldName: String?): JsonNode = when (this) {
    is ObjectNode -> putArray(fieldName)
    is ArrayNode  -> addArray()
    else          -> JsonNodeFactory.instance.arrayNode()
}

/**
 * [fieldName]을 키로 하여 Long 수형의 [value]를 추가합니다.
 */
fun JsonNode.addLong(value: Long, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode  -> add(value)
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Int 수형의 [value]를 추가합니다.
 */
fun JsonNode.addInt(value: Int, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode  -> add(value)
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 문자열 [value]를 추가합니다.
 */
fun JsonNode.addString(value: String, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode  -> add(value)
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Float 수형의 [value]를 추가합니다.
 */
fun JsonNode.addFloat(value: Float, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode  -> add(value)
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Double 수형의 [value]를 추가합니다.
 */
fun JsonNode.addDouble(value: Double, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode  -> add(value)
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 BigDecimal 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBigDecimal(value: BigDecimal, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode  -> add(value)
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 BigInteger 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBigInteger(value: BigInteger, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode  -> add(value)
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Short 수형의 [value]를 추가합니다.
 */
fun JsonNode.addShort(value: Short, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode  -> add(value)
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Boolean 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBoolean(value: Boolean, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode  -> add(value)
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 `Null` 값을 가지는 JsonNode를 추가합니다.
 */
fun JsonNode.addNull(fieldName: String?) {
    when (this) {
        is ObjectNode -> putNull(fieldName)
        is ArrayNode  -> addNull()
        else          -> throw RuntimeException("Unknown json node type. ${this.nodeType}")
    }
}
