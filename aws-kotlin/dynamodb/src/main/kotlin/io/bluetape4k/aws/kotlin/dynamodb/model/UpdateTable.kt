package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.TableClass
import aws.sdk.kotlin.services.dynamodb.model.UpdateTableRequest
import io.bluetape4k.support.requireNotBlank

inline fun updateTableRequestOf(
    tableName: String,
    tableClass: TableClass? = null,
    @BuilderInference crossinline builder: UpdateTableRequest.Builder.() -> Unit = {},
): UpdateTableRequest {
    tableName.requireNotBlank("tableName")

    return UpdateTableRequest {
        this.tableName = tableName
        this.tableClass = tableClass

        builder()
    }
}
