package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.BatchStatementRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnValuesOnConditionCheckFailure
import io.bluetape4k.support.requireNotBlank

@JvmName("batchStatementRequestOfAttributeValue")
fun batchStatementRequestOf(
    statement: String,
    parameters: List<AttributeValue>? = null,
    consistentRead: Boolean? = null,
    returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure? = null,
    configurer: BatchStatementRequest.Builder.() -> Unit = {},
): BatchStatementRequest {
    statement.requireNotBlank("statement")

    return BatchStatementRequest {
        this.statement = statement
        this.parameters = parameters
        this.consistentRead = consistentRead
        this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure
        configurer()
    }
}

@JvmName("batchStatementRequestOfAny")
fun batchStatementRequestOf(
    statement: String,
    parameters: List<Any?>? = null,
    consistentRead: Boolean? = null,
    returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure? = null,
    configurer: BatchStatementRequest.Builder.() -> Unit = {},
): BatchStatementRequest {
    statement.requireNotBlank("statement")

    return BatchStatementRequest {
        this.statement = statement
        this.parameters = parameters?.map { it.toAttributeValue() }
        this.consistentRead = consistentRead
        this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure
        configurer()
    }
}
