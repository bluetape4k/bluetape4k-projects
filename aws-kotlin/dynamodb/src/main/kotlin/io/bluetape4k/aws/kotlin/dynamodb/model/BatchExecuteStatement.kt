package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.BatchExecuteStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.BatchStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import io.bluetape4k.collections.eclipse.toFastList

@JvmName("batchExecutionStatementRequestOfList")
fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    statements: List<BatchStatementRequest>? = null,
    @BuilderInference builder: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest {
        this.returnConsumedCapacity = returnConsumedCapacity
        this.statements = statements

        builder()
    }

@JvmName("batchExecutionStatementRequestOfArray")
fun batchExecutionStatementRequestOf(
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    vararg statements: BatchStatementRequest,
    @BuilderInference builder: BatchExecuteStatementRequest.Builder.() -> Unit = {},
): BatchExecuteStatementRequest =
    BatchExecuteStatementRequest {
        this.returnConsumedCapacity = returnConsumedCapacity
        this.statements = statements.toFastList()

        builder()
    }
