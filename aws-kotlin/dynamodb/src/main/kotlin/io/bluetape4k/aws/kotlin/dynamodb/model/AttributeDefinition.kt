package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeDefinition
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType
import io.bluetape4k.support.requireNotBlank

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
