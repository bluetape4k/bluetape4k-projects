package io.bluetape4k.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.json.JsonException
import java.math.BigDecimal
import java.math.BigInteger

internal val nodeFactory = JsonNodeFactory.instance

/**
 * 현재 노드에 객체 노드를 생성해 추가합니다.
 *
 * ## 동작/계약
 * - 수신 노드가 [ObjectNode]면 [fieldName] 키에 객체 노드를 생성합니다.
 * - 수신 노드가 [ArrayNode]면 배열 끝에 객체 노드를 추가합니다.
 * - 그 외 노드 타입이면 독립된 새 객체 노드를 반환합니다.
 *
 * ```kotlin
 * val root = nodeFactory.objectNode()
 * val child = root.createNode("data")
 * // child.isObject == true
 * ```
 */
fun JsonNode.createNode(fieldName: String?): JsonNode =
    when (this) {
        is ObjectNode -> putObject(fieldName)
        is ArrayNode -> addObject()
        else         -> nodeFactory.objectNode()
    }

/**
 * 현재 노드에 배열 노드를 생성해 추가합니다.
 *
 * ## 동작/계약
 * - 수신 노드가 [ObjectNode]면 [fieldName] 키에 배열 노드를 생성합니다.
 * - 수신 노드가 [ArrayNode]면 배열 끝에 배열 노드를 추가합니다.
 * - 그 외 노드 타입이면 독립된 새 배열 노드를 반환합니다.
 *
 * ```kotlin
 * val root = nodeFactory.objectNode()
 * val arr = root.createArray("items")
 * // arr.isArray == true
 * ```
 */
fun JsonNode.createArray(fieldName: String?): JsonNode =
    when (this) {
        is ObjectNode -> putArray(fieldName)
        is ArrayNode -> addArray()
        else         -> nodeFactory.arrayNode()
    }

/**
 * 현재 노드에 값을 추가하거나 교체합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]에서 [fieldName]이 null/blank이면 [IllegalArgumentException]이 발생합니다.
 * - 지원 타입(Boolean, Number, String, ByteArray 등)은 해당 JsonNode로 변환합니다.
 * - 지원하지 않는 타입이면 [IllegalArgumentException]이 발생합니다.
 * - 수신 노드가 Object/Array가 아니면 [JsonException]이 발생합니다.
 *
 * ```kotlin
 * val root = nodeFactory.objectNode()
 * root.addValue(1, "id")
 * // root["id"].asInt() == 1
 * ```
 *
 * @param value 추가할 값
 * @param fieldName ObjectNode일 때 사용할 필드 이름
 */
fun <T> JsonNode.addValue(
    value: T,
    fieldName: String?,
) = apply {
    require(this !is ObjectNode || !fieldName.isNullOrBlank()) {
        "Field name must not be null or blank for ObjectNode"
    }
    val node =
        when (value) {
            null         -> nodeFactory.nullNode()
            is Boolean   -> nodeFactory.booleanNode(value)
            is Char      -> nodeFactory.textNode(value.toString())
            is Byte      -> nodeFactory.numberNode(value)
            is Short     -> nodeFactory.numberNode(value)
            is Int       -> nodeFactory.numberNode(value)
            is Long      -> nodeFactory.numberNode(value)
            is Double    -> nodeFactory.numberNode(value)
            is Float     -> nodeFactory.numberNode(value)
            is BigDecimal -> nodeFactory.numberNode(value)
            is BigInteger -> nodeFactory.numberNode(value)
            is String    -> nodeFactory.textNode(value)
            is ByteArray -> nodeFactory.binaryNode(value)
            else         -> throw IllegalArgumentException("Unsupported value type: ${value::class}")
        }
    when (this) {
        is ObjectNode -> replace(fieldName, node)
        is ArrayNode -> add(node)
        else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
    }
}

/**
 * [fieldName]을 키로 하여 Boolean 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBoolean(
    value: Boolean,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Char 수형의 [value]를 추가합니다.
 */
fun JsonNode.addChar(
    value: Char,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Byte 수형의 [value]를 추가합니다.
 */
fun JsonNode.addByte(
    value: Byte,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Short 수형의 [value]를 추가합니다.
 */
fun JsonNode.addShort(
    value: Short,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Int 수형의 [value]를 추가합니다.
 */
fun JsonNode.addInt(
    value: Int,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Long 수형의 [value]를 추가합니다.
 */
fun JsonNode.addLong(
    value: Long,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 문자열 [value]를 추가합니다.
 */
fun JsonNode.addString(
    value: String,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Float 수형의 [value]를 추가합니다.
 */
fun JsonNode.addFloat(
    value: Float,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 Double 수형의 [value]를 추가합니다.
 */
fun JsonNode.addDouble(
    value: Double,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 BigDecimal 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBigDecimal(
    value: BigDecimal,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 BigInteger 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBigInteger(
    value: BigInteger,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * [fieldName]을 키로 하여 ByteArray 수형의 [value]를 추가합니다.
 */
fun JsonNode.addBinary(
    value: ByteArray,
    fieldName: String?,
) = addValue(value, fieldName)

/**
 * 현재 노드에 `null` 값을 추가합니다.
 *
 * ## 동작/계약
 * - [ObjectNode]에서 [fieldName]이 null/blank이면 [IllegalArgumentException]이 발생합니다.
 * - [ArrayNode]면 배열 끝에 null을 추가합니다.
 * - 수신 노드가 Object/Array가 아니면 [JsonException]이 발생합니다.
 *
 * ```kotlin
 * val root = nodeFactory.objectNode()
 * root.addNull("deletedAt")
 * // root["deletedAt"].isNull == true
 * ```
 */
fun JsonNode.addNull(fieldName: String?) =
    apply {
        require(this !is ObjectNode || !fieldName.isNullOrBlank()) {
            "Field name must not be null or blank for ObjectNode"
        }

        when (this) {
            is ObjectNode -> putNull(fieldName)
            is ArrayNode -> addNull()
            else         -> throw JsonException("Unknown json node type. ${this.nodeType}")
        }
    }
