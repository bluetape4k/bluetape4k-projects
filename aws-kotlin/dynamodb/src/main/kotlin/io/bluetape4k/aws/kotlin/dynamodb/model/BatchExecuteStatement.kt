package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchExecuteStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.BatchStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity

@JvmName("batchExecutionStatementRequestOfList")
inline fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    statements: List<BatchStatementRequest>? = null,
    crossinline configurer: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest {
    return BatchExecuteStatementRequest.invoke {
        this.returnConsumedCapacity = returnConsumedCapacity
        this.statements = statements

        configurer()
    }
}

@JvmName("batchExecutionStatementRequestOfArray")
inline fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    vararg statements: BatchStatementRequest,
    crossinline configurer: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest {
    return BatchExecuteStatementRequest.invoke {
        this.returnConsumedCapacity = returnConsumedCapacity
        this.statements = statements.toList()

        configurer()
    }
}
