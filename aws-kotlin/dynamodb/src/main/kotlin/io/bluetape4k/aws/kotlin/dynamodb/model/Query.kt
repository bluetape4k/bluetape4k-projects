package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.QueryRequest
import io.bluetape4k.support.requireNotBlank

@JvmName("queryRequestOfAttributeValue")
fun queryRequestOf(
    tableName: String,
    attributesToGet: List<String>? = null,
    exclusiveStartKey: Map<String, AttributeValue>? = null,
    @BuilderInference builder: QueryRequest.Builder.() -> Unit = {},
): QueryRequest {
    tableName.requireNotBlank("tableName")

    return QueryRequest {
        this.tableName = tableName
        this.attributesToGet = attributesToGet
        this.exclusiveStartKey = exclusiveStartKey

        builder()
    }
}

@JvmName("queryRequestOfAny")
fun queryRequestOf(
    tableName: String,
    attributesToGet: List<String>? = null,
    exclusiveStartKey: Map<String, Any?>? = null,
    @BuilderInference builder: QueryRequest.Builder.() -> Unit = {},
): QueryRequest =
    queryRequestOf(
        tableName,
        attributesToGet,
        exclusiveStartKey?.mapValues { it.toAttributeValue() },
        builder,
    )
