package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AutoScalingSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.GlobalTableGlobalSecondaryIndexSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.ReplicaUpdate
import aws.sdk.kotlin.services.dynamodb.model.UpdateGlobalTableRequest
import aws.sdk.kotlin.services.dynamodb.model.UpdateGlobalTableSettingsRequest
import io.bluetape4k.support.requireNotBlank

fun updateGlobalTableRequestOf(
    globalTableName: String,
    replicaUpdates: List<ReplicaUpdate>?,
    @BuilderInference builder: UpdateGlobalTableRequest.Builder.() -> Unit = {},
): UpdateGlobalTableRequest {
    globalTableName.requireNotBlank("globalTableName")

    return UpdateGlobalTableRequest {
        this.globalTableName = globalTableName
        this.replicaUpdates = replicaUpdates

        builder()
    }
}

fun updateGlobalTableSettingsRequestOf(
    globalTableName: String,
    globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate: AutoScalingSettingsUpdate? = null,
    globalTableGlobalSecondaryIndexSettingsUpdate: List<GlobalTableGlobalSecondaryIndexSettingsUpdate>? = null,
    @BuilderInference builder: UpdateGlobalTableSettingsRequest.Builder.() -> Unit = {},
): UpdateGlobalTableSettingsRequest {
    globalTableName.requireNotBlank("globalTableName")

    return UpdateGlobalTableSettingsRequest {
        this.globalTableName = globalTableName
        this.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate =
            globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate
        this.globalTableGlobalSecondaryIndexSettingsUpdate = globalTableGlobalSecondaryIndexSettingsUpdate

        builder()
    }
}
