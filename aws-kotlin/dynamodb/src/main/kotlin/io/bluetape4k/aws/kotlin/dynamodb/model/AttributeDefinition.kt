package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeDefinition
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType
import io.bluetape4k.support.requireNotBlank

/**
 * 속성 이름과 타입으로 DynamoDB [AttributeDefinition]을 생성합니다.
 *
 * ## 동작/계약
 * - [name]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val def = attributeDefinitionOf("userId", ScalarAttributeType.S)
 * // def.attributeName == "userId", def.attributeType == ScalarAttributeType.S
 * ```
 *
 * @throws IllegalArgumentException [name]이 blank인 경우
 */
fun attributeDefinitionOf(
    name: String,
    type: ScalarAttributeType,
): AttributeDefinition {
    name.requireNotBlank("name")

    return AttributeDefinition {
        attributeName = name
        attributeType = type
    }
}

fun numberAttrDefinitionOf(name: String): AttributeDefinition =
    attributeDefinitionOf(name, ScalarAttributeType.N)

fun stringAttrDefinitionOf(name: String): AttributeDefinition =
    attributeDefinitionOf(name, ScalarAttributeType.S)

fun binaryAttrDefinitionOf(name: String): AttributeDefinition =
    attributeDefinitionOf(name, ScalarAttributeType.B)

fun String.stringAttributeDefinition(): AttributeDefinition = stringAttrDefinitionOf(this)
fun String.numberAttributeDefinition(): AttributeDefinition = numberAttrDefinitionOf(this)
fun String.binaryAttributeDefinition(): AttributeDefinition = binaryAttrDefinitionOf(this)
