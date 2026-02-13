package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchExecuteStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.BatchStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import io.bluetape4k.support.ifTrue

@JvmName("batchExecutionStatementRequestOfList")
inline fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    statements: List<BatchStatementRequest>? = null,
    @BuilderInference crossinline builder: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest {
        returnConsumedCapacity?.let { this.returnConsumedCapacity = it }
        statements?.let { this.statements = it }

        builder()
    }

@JvmName("batchExecutionStatementRequestOfArray")
inline fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    vararg statements: BatchStatementRequest,
    @BuilderInference crossinline builder: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest {
        returnConsumedCapacity?.let { this.returnConsumedCapacity = it }
        statements.isNotEmpty().ifTrue {
            this.statements = statements.toList()
        }

        builder()
    }
