package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ExecuteStatementRequest
import io.bluetape4k.support.requireNotBlank

@JvmName("executeStatementRequestOfAttributeValue")
inline fun executeStatementRequestOf(
    statement: String,
    parameters: List<AttributeValue>? = null,
    consistentRead: Boolean? = null,
    limit: Int? = null,
    nextToken: String? = null,
    crossinline builder: ExecuteStatementRequest.Builder.() -> Unit = {},
): ExecuteStatementRequest {
    statement.requireNotBlank("statement")

    return ExecuteStatementRequest {
        this.statement = statement
        this.parameters = parameters
        this.consistentRead = consistentRead
        this.limit = limit
        this.nextToken = nextToken

        builder()
    }
}

@JvmName("executeStatementRequestOfAny")
inline fun executeStatementRequestOf(
    statement: String,
    parameters: List<Any?>? = null,
    consistentRead: Boolean? = null,
    limit: Int? = null,
    nextToken: String? = null,
    crossinline builder: ExecuteStatementRequest.Builder.() -> Unit = {},
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
