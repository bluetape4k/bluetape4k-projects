package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.DeleteTableRequest
import io.bluetape4k.support.requireNotBlank

fun deleteTableRequest(
    tableName: String,
    configurer: DeleteTableRequest.Builder.() -> Unit = {},
): DeleteTableRequest {
    tableName.requireNotBlank("tableName")

    return DeleteTableRequest {
        this.tableName = tableName
        configurer()
    }
}
