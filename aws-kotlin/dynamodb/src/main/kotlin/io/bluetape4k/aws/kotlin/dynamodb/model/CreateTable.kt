package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.CreateTableRequest
import aws.sdk.kotlin.services.dynamodb.model.StreamSpecification
import aws.sdk.kotlin.services.dynamodb.model.TableClass
import aws.sdk.kotlin.services.dynamodb.model.Tag
import io.bluetape4k.support.requireNotBlank

inline fun createTableRequestOf(
    tableName: String,
    tableClass: TableClass = TableClass.Standard,
    tags: List<Tag>? = null,
    streamSpecification: StreamSpecification? = null,
    crossinline builder: CreateTableRequest.Builder.() -> Unit = {},
): CreateTableRequest {
    tableName.requireNotBlank("tableName")

    return CreateTableRequest {
        this.tableName = tableName
        this.tableClass = tableClass
        this.tags = tags
        this.streamSpecification = streamSpecification

        builder()
    }
}
