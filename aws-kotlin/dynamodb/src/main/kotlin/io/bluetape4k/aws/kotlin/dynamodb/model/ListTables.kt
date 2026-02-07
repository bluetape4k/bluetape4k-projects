package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ListTablesRequest

inline fun listTablesRequestOf(
    exclusiveStartTableName: String? = null,
    limit: Int? = null,
    @BuilderInference crossinline builder: ListTablesRequest.Builder.() -> Unit = {},
): ListTablesRequest =
    ListTablesRequest {
        this.exclusiveStartTableName = exclusiveStartTableName
        this.limit = limit

        builder()
    }
