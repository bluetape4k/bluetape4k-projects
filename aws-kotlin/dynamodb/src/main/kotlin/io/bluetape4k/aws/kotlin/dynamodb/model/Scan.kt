package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import io.bluetape4k.support.requireNotBlank

@JvmName("scanRequestOfAttributeValue")
fun scanRequestOf(
    tableName: String,
    attributesToGet: List<String>? = null,
    exclusiveStartKey: Map<String, AttributeValue>? = null,
    indexName: String? = null,
    @BuilderInference builder: ScanRequest.Builder.() -> Unit = {},
): ScanRequest {
    tableName.requireNotBlank("tableName")

    return ScanRequest {
        this.tableName = tableName
        this.attributesToGet = attributesToGet
        this.exclusiveStartKey = exclusiveStartKey
        this.indexName = indexName

        builder()
    }
}

@JvmName("scanRequestOfAny")
fun scanRequestOf(
    tableName: String,
    attributesToGet: List<String>? = null,
    exclusiveStartKey: Map<String, Any?>? = null,
    indexName: String? = null,
    @BuilderInference builder: ScanRequest.Builder.() -> Unit = {},
): ScanRequest {
    tableName.requireNotBlank("tableName")

    return ScanRequest {
        this.tableName = tableName
        this.attributesToGet = attributesToGet
        this.exclusiveStartKey = exclusiveStartKey?.mapValues { it.toAttributeValue() }
        this.indexName = indexName

        builder()
    }
}
