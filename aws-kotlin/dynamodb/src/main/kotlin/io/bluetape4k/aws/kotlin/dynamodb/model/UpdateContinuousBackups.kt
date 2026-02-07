package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.PointInTimeRecoverySpecification
import aws.sdk.kotlin.services.dynamodb.model.UpdateContinuousBackupsRequest
import io.bluetape4k.support.requireNotBlank

inline fun updateContinuousBackupsRequestOf(
    tableName: String,
    pointInTimeRecoverySpecification: PointInTimeRecoverySpecification? = null,
    @BuilderInference crossinline builder: UpdateContinuousBackupsRequest.Builder.() -> Unit = {},
): UpdateContinuousBackupsRequest {
    tableName.requireNotBlank("tableName")

    return UpdateContinuousBackupsRequest {
        this.tableName = tableName
        this.pointInTimeRecoverySpecification = pointInTimeRecoverySpecification

        builder()
    }
}
