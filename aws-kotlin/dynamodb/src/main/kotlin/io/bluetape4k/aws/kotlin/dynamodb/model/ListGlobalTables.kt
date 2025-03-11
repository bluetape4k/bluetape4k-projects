package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ListGlobalTablesRequest

inline fun listGlobalTableRequestOf(
    exclusiveStartGlobalTableName: String? = null,
    regionName: String? = null,
    limit: Int? = null,
    crossinline configurer: ListGlobalTablesRequest.Builder.() -> Unit = {},
): ListGlobalTablesRequest {
    return ListGlobalTablesRequest {
        this.exclusiveStartGlobalTableName = exclusiveStartGlobalTableName
        this.regionName = regionName
        this.limit = limit
        configurer()
    }
}
