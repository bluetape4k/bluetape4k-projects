package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.RestoreTableFromBackupRequest
import io.bluetape4k.support.requireNotBlank

fun restoreTableFromBackupRequestOf(
    backupArn: String,
    targetTableName: String,
    @BuilderInference builder: RestoreTableFromBackupRequest.Builder.() -> Unit = {},
): RestoreTableFromBackupRequest {
    backupArn.requireNotBlank("backupArn")
    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableFromBackupRequest {
        this.backupArn = backupArn
        this.targetTableName = targetTableName

        builder()
    }
}

fun restoreTableToPointInTimeRequestOf(
    backupArn: String,
    targetTableName: String,
    @BuilderInference builder: RestoreTableFromBackupRequest.Builder.() -> Unit,
): RestoreTableFromBackupRequest {
    backupArn.requireNotBlank("backupArn")
    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableFromBackupRequest {
        this.backupArn = backupArn
        this.targetTableName = targetTableName

        builder()
    }
}
