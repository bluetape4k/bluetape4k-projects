package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType

/**
 * Jackson의 JsonNode를 DynamoDB의 AttributeValue로 변환합니다.
 */
fun JsonNode.toAttributeValue(): AttributeValue =
    when (this.nodeType) {
        JsonNodeType.NULL -> AttributeValue.Null(true)
        JsonNodeType.BOOLEAN -> this.booleanValue().toAttributeValue()
        JsonNodeType.NUMBER -> this.numberValue().toAttributeValue()
        JsonNodeType.STRING -> this.textValue().toAttributeValue()
        JsonNodeType.ARRAY -> AttributeValue.L(this.map { it.toAttributeValue() })
        JsonNodeType.OBJECT -> AttributeValue.M(
            this.properties().asSequence().associate { (key, value) ->
                key to value.toAttributeValue()
            }
        )
        JsonNodeType.POJO -> AttributeValue.M(
            this.properties().asSequence().associate { (key, value) ->
                key to value.toAttributeValue()
            }
        )

        else -> throw IllegalStateException("Unsupported JsonNode type: ${this.nodeType}")
    }
