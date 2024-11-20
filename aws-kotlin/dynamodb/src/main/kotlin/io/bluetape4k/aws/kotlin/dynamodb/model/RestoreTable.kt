package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.RestoreTableFromBackupRequest
import io.bluetape4k.support.requireNotBlank

fun restoreTableFromBackupRequestOf(
    backupArn: String,
    targetTableName: String,
    configurer: RestoreTableFromBackupRequest.Builder.() -> Unit = {},
): RestoreTableFromBackupRequest {
    backupArn.requireNotBlank("backupArn")
    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableFromBackupRequest.invoke {
        this.backupArn = backupArn
        this.targetTableName = targetTableName

        configurer()
    }
}

fun restoreTableToPointInTimeRequestOf(
    backupArn: String,
    targetTableName: String,
    configurer: RestoreTableFromBackupRequest.Builder.() -> Unit,
): RestoreTableFromBackupRequest {
    backupArn.requireNotBlank("backupArn")
    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableFromBackupRequest.invoke {
        this.backupArn = backupArn
        this.targetTableName = targetTableName

        configurer()
    }
}
