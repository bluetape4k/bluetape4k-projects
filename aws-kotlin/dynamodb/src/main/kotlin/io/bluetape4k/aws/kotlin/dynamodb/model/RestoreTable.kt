package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.RestoreTableFromBackupRequest
import io.bluetape4k.support.requireNotBlank

inline fun restoreTableFromBackupRequestOf(
    backupArn: String,
    targetTableName: String,
    crossinline configurer: RestoreTableFromBackupRequest.Builder.() -> Unit = {},
): RestoreTableFromBackupRequest {
    backupArn.requireNotBlank("backupArn")
    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableFromBackupRequest {
        this.backupArn = backupArn
        this.targetTableName = targetTableName

        configurer()
    }
}

inline fun restoreTableToPointInTimeRequestOf(
    backupArn: String,
    targetTableName: String,
    crossinline configurer: RestoreTableFromBackupRequest.Builder.() -> Unit,
): RestoreTableFromBackupRequest {
    backupArn.requireNotBlank("backupArn")
    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableFromBackupRequest {
        this.backupArn = backupArn
        this.targetTableName = targetTableName

        configurer()
    }
}
