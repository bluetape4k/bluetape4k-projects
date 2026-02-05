package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ExecuteTransactionRequest
import aws.sdk.kotlin.services.dynamodb.model.ParameterizedStatement
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import io.bluetape4k.support.requireNotEmpty

fun executeTransactionRequestOf(
    transactionStatements: List<ParameterizedStatement>,
    clientRequestToken: String? = null,
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    @BuilderInference builder: ExecuteTransactionRequest.Builder.() -> Unit = {},
): ExecuteTransactionRequest {
    transactionStatements.requireNotEmpty("transactionStatements")

    return ExecuteTransactionRequest {
        this.transactStatements = transactionStatements
        this.clientRequestToken = clientRequestToken
        this.returnConsumedCapacity = returnConsumedCapacity

        builder()
    }
}
