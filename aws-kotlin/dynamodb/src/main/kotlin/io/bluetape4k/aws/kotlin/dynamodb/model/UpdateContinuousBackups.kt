package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.PointInTimeRecoverySpecification
import aws.sdk.kotlin.services.dynamodb.model.UpdateContinuousBackupsRequest
import io.bluetape4k.support.requireNotBlank

fun updateContinuousBackupsRequestOf(
    tableName: String,
    pointInTimeRecoverySpecification: PointInTimeRecoverySpecification? = null,
    @BuilderInference builder: UpdateContinuousBackupsRequest.Builder.() -> Unit = {},
): UpdateContinuousBackupsRequest {
    tableName.requireNotBlank("tableName")

    return UpdateContinuousBackupsRequest {
        this.tableName = tableName
        this.pointInTimeRecoverySpecification = pointInTimeRecoverySpecification

        builder()
    }
}
