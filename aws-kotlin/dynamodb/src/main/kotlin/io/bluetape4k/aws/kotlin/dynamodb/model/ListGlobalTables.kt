package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ListGlobalTablesRequest

fun listGlobalTableRequestOf(
    exclusiveStartGlobalTableName: String? = null,
    regionName: String? = null,
    limit: Int? = null,
    configurer: ListGlobalTablesRequest.Builder.() -> Unit = {},
): ListGlobalTablesRequest {
    return ListGlobalTablesRequest {
        this.exclusiveStartGlobalTableName = exclusiveStartGlobalTableName
        this.regionName = regionName
        this.limit = limit
        configurer()
    }
}
