package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.TimeToLiveSpecification
import aws.sdk.kotlin.services.dynamodb.model.UpdateTimeToLiveRequest
import io.bluetape4k.support.requireNotBlank

fun updateTimeToLiveRequestOf(
    tableName: String,
    timeToLiveSpecification: TimeToLiveSpecification? = null,
    @BuilderInference builder: UpdateTimeToLiveRequest.Builder.() -> Unit = {},
): UpdateTimeToLiveRequest {
    tableName.requireNotBlank("tableName")

    return UpdateTimeToLiveRequest {
        this.tableName = tableName
        this.timeToLiveSpecification = timeToLiveSpecification

        builder()
    }
}
