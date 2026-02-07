package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.RestoreTableFromBackupRequest
import aws.sdk.kotlin.services.dynamodb.model.RestoreTableToPointInTimeRequest
import io.bluetape4k.support.requireNotBlank

inline fun restoreTableFromBackupRequestOf(
    backupArn: String,
    targetTableName: String,
    @BuilderInference crossinline builder: RestoreTableFromBackupRequest.Builder.() -> Unit = {},
): RestoreTableFromBackupRequest {
    backupArn.requireNotBlank("backupArn")
    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableFromBackupRequest {
        this.backupArn = backupArn
        this.targetTableName = targetTableName

        builder()
    }
}

inline fun restoreTableToPointInTimeRequestOf(
    sourceTableArn: String? = null,
    sourceTableName: String? = null,
    targetTableName: String? = null,
    useLatestRestorableTime: Boolean? = null,
    @BuilderInference crossinline builder: RestoreTableToPointInTimeRequest.Builder.() -> Unit,
): RestoreTableToPointInTimeRequest {

    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableToPointInTimeRequest {
        this.sourceTableArn = sourceTableArn
        this.sourceTableName = sourceTableName
        this.targetTableName = targetTableName
        this.useLatestRestorableTime = useLatestRestorableTime

        builder()
    }
}
