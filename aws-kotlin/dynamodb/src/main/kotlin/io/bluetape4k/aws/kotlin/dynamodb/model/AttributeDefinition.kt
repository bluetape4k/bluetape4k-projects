package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeDefinition
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType

fun attributeDefinitionOf(
    name: String,
    type: ScalarAttributeType,
): AttributeDefinition = AttributeDefinition {
    attributeName = name
    attributeType = type
}


fun numberAttrDefinitionOf(name: String): AttributeDefinition =
    attributeDefinitionOf(name, ScalarAttributeType.N)

fun stringAttrDefinitionOf(name: String): AttributeDefinition =
    attributeDefinitionOf(name, ScalarAttributeType.S)

fun binaryAttrDefinitionOf(name: String): AttributeDefinition =
    attributeDefinitionOf(name, ScalarAttributeType.B)
