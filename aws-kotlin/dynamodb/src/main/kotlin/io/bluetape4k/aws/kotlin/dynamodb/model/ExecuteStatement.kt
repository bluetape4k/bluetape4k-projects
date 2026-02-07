package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ExecuteStatementRequest
import io.bluetape4k.support.requireNotBlank

@JvmName("executeStatementRequestOfAttributeValue")
inline fun executeStatementRequestOf(
    statement: String,
    parameters: List<AttributeValue>?,
    consistentRead: Boolean? = null,
    limit: Int? = null,
    nextToken: String? = null,
    @BuilderInference crossinline builder: ExecuteStatementRequest.Builder.() -> Unit = {},
): ExecuteStatementRequest {
    statement.requireNotBlank("statement")

    return ExecuteStatementRequest {
        this.statement = statement
        parameters?.let { this.parameters = it }
        consistentRead?.let { this.consistentRead = it }
        limit?.let { this.limit = it }
        nextToken?.let { this.nextToken = it }

        builder()
    }
}

@JvmName("executeStatementRequestOfAny")
inline fun executeStatementRequestOf(
    statement: String,
    parameters: List<Any?>?,
    consistentRead: Boolean? = null,
    limit: Int? = null,
    nextToken: String? = null,
    @BuilderInference crossinline builder: ExecuteStatementRequest.Builder.() -> Unit = {},
): ExecuteStatementRequest {
    return executeStatementRequestOf(
        statement,
        parameters?.map { it.toAttributeValue() },
        consistentRead,
        limit,
        nextToken,
        builder,
    )
}
