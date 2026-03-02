package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType

/**
 * Jackson의 [JsonNode]를 DynamoDB [AttributeValue]로 변환합니다.
 *
 * ## 동작/계약
 * - NULL 노드는 `AttributeValue.Null(true)`로 변환된다.
 * - BOOLEAN/NUMBER/STRING 노드는 각각 대응하는 [AttributeValue] 스칼라 타입으로 변환된다.
 * - ARRAY 노드는 `AttributeValue.L` (리스트)으로, OBJECT/POJO 노드는 `AttributeValue.M` (맵)으로 변환된다.
 * - 지원하지 않는 노드 타입은 `IllegalStateException`을 던진다.
 *
 * ```kotlin
 * val node: JsonNode = ObjectMapper().readTree("""{"name":"Alice","age":30}""")
 * val av = node.toAttributeValue()
 * // av is AttributeValue.M
 * // (av as AttributeValue.M).value["name"] == AttributeValue.S("Alice")
 * ```
 *
 * @throws IllegalStateException 지원하지 않는 [JsonNodeType]인 경우
 */
fun JsonNode.toAttributeValue(): AttributeValue =
    when (this.nodeType) {
        JsonNodeType.NULL -> AttributeValue.Null(true)
        JsonNodeType.BOOLEAN -> this.booleanValue().toAttributeValue()
        JsonNodeType.NUMBER -> this.numberValue().toAttributeValue()
        JsonNodeType.STRING -> this.textValue().toAttributeValue()
        JsonNodeType.ARRAY -> AttributeValue.L(this.map { it.toAttributeValue() })
        JsonNodeType.OBJECT -> AttributeValue.M(
            this.properties().associate { (key, value) ->
                key to value.toAttributeValue()
            }
        )
        JsonNodeType.POJO -> AttributeValue.M(
            this.properties().associate { (key, value) ->
                key to value.toAttributeValue()
            }
        )

        else -> throw IllegalStateException("Unsupported JsonNode type: ${this.nodeType}")
    }
