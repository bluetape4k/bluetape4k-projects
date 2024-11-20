package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ListTablesRequest

fun listTablesRequestOf(
    exclusiveStartTableName: String? = null,
    limit: Int? = null,
    configurer: ListTablesRequest.Builder.() -> Unit = {},
): ListTablesRequest {
    return ListTablesRequest.invoke {
        this.exclusiveStartTableName = exclusiveStartTableName
        this.limit = limit
        configurer()
    }
}
