package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchExecuteStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.BatchStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity

fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    statements: List<BatchStatementRequest>? = null,
    configurer: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest {
    return BatchExecuteStatementRequest.invoke {
        this.returnConsumedCapacity = returnConsumedCapacity
        this.statements = statements

        configurer()
    }
}

fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    vararg statements: BatchStatementRequest,
    configurer: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest {
    return BatchExecuteStatementRequest.invoke {
        this.returnConsumedCapacity = returnConsumedCapacity
        this.statements = statements.toList()

        configurer()
    }
}
