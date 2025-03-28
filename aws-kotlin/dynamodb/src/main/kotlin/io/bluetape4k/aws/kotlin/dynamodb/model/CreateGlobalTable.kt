package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.CreateGlobalTableRequest
import aws.sdk.kotlin.services.dynamodb.model.Replica
import io.bluetape4k.support.requireNotBlank

inline fun createGlobalTableRequestOf(
    globalTableName: String,
    replicationGroup: List<Replica>? = null,
    crossinline configurer: CreateGlobalTableRequest.Builder.() -> Unit = {},
): CreateGlobalTableRequest {
    globalTableName.requireNotBlank("globalTableName")

    return CreateGlobalTableRequest {
        this.globalTableName = globalTableName
        this.replicationGroup = replicationGroup

        configurer()
    }
}
