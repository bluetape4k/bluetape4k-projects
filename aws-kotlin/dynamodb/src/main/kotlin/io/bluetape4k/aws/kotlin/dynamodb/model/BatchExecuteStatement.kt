package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchExecuteStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.BatchStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity

@JvmName("batchExecutionStatementRequestOfList")
inline fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    statements: List<BatchStatementRequest>? = null,
    crossinline builder: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest {
        this.returnConsumedCapacity = returnConsumedCapacity
        this.statements = statements

        builder()
    }

@JvmName("batchExecutionStatementRequestOfArray")
inline fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    vararg statements: BatchStatementRequest,
    crossinline builder: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest {
        this.returnConsumedCapacity = returnConsumedCapacity
        this.statements = statements.toList()

        builder()
    }
