package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.QueryRequest
import io.bluetape4k.support.requireNotBlank

@JvmName("queryRequestOfAttributeValue")
inline fun queryRequestOf(
    tableName: String,
    attributesToGet: List<String>? = null,
    exclusiveStartKey: Map<String, AttributeValue>? = null,
    crossinline configurer: QueryRequest.Builder.() -> Unit = {},
): QueryRequest {
    tableName.requireNotBlank("tableName")

    return QueryRequest.invoke {
        this.tableName = tableName
        this.attributesToGet = attributesToGet
        this.exclusiveStartKey = exclusiveStartKey
        configurer()
    }
}

@JvmName("queryRequestOfAny")
inline fun queryRequestOf(
    tableName: String,
    attributesToGet: List<String>? = null,
    exclusiveStartKey: Map<String, Any?>? = null,
    crossinline configurer: QueryRequest.Builder.() -> Unit = {},
): QueryRequest {
    return queryRequestOf(
        tableName,
        attributesToGet,
        exclusiveStartKey?.mapValues { it.toAttributeValue() },
        configurer,
    )
}
