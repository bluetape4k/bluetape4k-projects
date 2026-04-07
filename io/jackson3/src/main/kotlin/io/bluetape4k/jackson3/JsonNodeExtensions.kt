package io.bluetape4k.jackson3

import jakarta.json.JsonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.math.BigInteger

/**
 * 현재 노드에 객체 노드를 생성해 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 객체 노드를 생성합니다.
 * - [ArrayNode]면 배열 끝에 객체 노드를 추가합니다.
 * - 그 외 타입이면 독립된 새 객체 노드를 반환합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * val child = root.createNode("data")
 * // child.isObject == true
 * ```
 */
fun JsonNode.createNode(fieldName: String?): JsonNode = when (this) {
    is ObjectNode -> putObject(fieldName)
    is ArrayNode -> addObject()
    else         -> JsonNodeFactory.instance.objectNode()
}

/**
 * 현재 노드에 배열 노드를 생성해 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 배열 노드를 생성합니다.
 * - [ArrayNode]면 배열 끝에 배열 노드를 추가합니다.
 * - 그 외 타입이면 독립된 새 배열 노드를 반환합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * val arr = root.createArray("items")
 * // arr.isArray == true
 * ```
 */
fun JsonNode.createArray(fieldName: String?): JsonNode = when (this) {
    is ObjectNode -> putArray(fieldName)
    is ArrayNode -> addArray()
    else         -> JsonNodeFactory.instance.arrayNode()
}

/**
 * [fieldName]을 키로 하여 Long 수형의 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 숫자 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 추가합니다.
 * - 그 외 타입이면 [JsonException]이 발생합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addLong(42L, "count")
 * // root["count"].asLong() == 42L
 * ```
 */
fun JsonNode.addLong(value: Long, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode -> add(value)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Int 수형의 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 숫자 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 추가합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addInt(10, "score")
 * // root["score"].asInt() == 10
 * ```
 */
fun JsonNode.addInt(value: Int, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode -> add(value)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 문자열 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 텍스트 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 추가합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addString("hello", "greeting")
 * // root["greeting"].asText() == "hello"
 * ```
 */
fun JsonNode.addString(value: String, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode -> add(value)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Float 수형의 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 숫자 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 추가합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addFloat(3.14f, "pi")
 * // root["pi"].floatValue() == 3.14f
 * ```
 */
fun JsonNode.addFloat(value: Float, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode -> add(value)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Double 수형의 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 숫자 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 추가합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addDouble(3.14159, "pi")
 * // root["pi"].doubleValue() == 3.14159
 * ```
 */
fun JsonNode.addDouble(value: Double, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode -> add(value)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 BigDecimal 수형의 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 숫자 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 추가합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addBigDecimal(BigDecimal("9999.99"), "price")
 * // root["price"].decimalValue() == BigDecimal("9999.99")
 * ```
 */
fun JsonNode.addBigDecimal(value: BigDecimal, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode -> add(value)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 BigInteger 수형의 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 숫자 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 추가합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addBigInteger(BigInteger("123456789"), "big")
 * // root["big"].bigIntegerValue() == BigInteger("123456789")
 * ```
 */
fun JsonNode.addBigInteger(value: BigInteger, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode -> add(value)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Short 수형의 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 숫자 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 추가합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addShort(100.toShort(), "priority")
 * // root["priority"].shortValue() == 100.toShort()
 * ```
 */
fun JsonNode.addShort(value: Short, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode -> add(value)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Boolean 수형의 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 불리언 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 추가합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addBoolean(true, "active")
 * // root["active"].booleanValue() == true
 * ```
 */
fun JsonNode.addBoolean(value: Boolean, fieldName: String?) {
    when (this) {
        is ObjectNode -> put(fieldName, value)
        is ArrayNode -> add(value)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 `Null` 값을 가지는 JsonNode를 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]면 [fieldName] 키에 null 노드를 추가합니다.
 * - [ArrayNode]면 배열 끝에 null을 추가합니다.
 * - 그 외 타입이면 [JsonException]이 발생합니다.
 *
 * ```kotlin
 * val root = JsonNodeFactory.instance.objectNode()
 * root.addNull("deletedAt")
 * // root["deletedAt"].isNull == true
 * ```
 */
fun JsonNode.addNull(fieldName: String?) {
    when (this) {
        is ObjectNode -> putNull(fieldName)
        is ArrayNode -> addNull()
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}
